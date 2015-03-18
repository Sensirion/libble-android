package com.sensirion.libble.services.sensirion.shtc1;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.listeners.services.HumidityListener;
import com.sensirion.libble.listeners.services.RHTListener;
import com.sensirion.libble.listeners.services.TemperatureListener;
import com.sensirion.libble.services.AbstractHistoryService;
import com.sensirion.libble.utils.HumidityUnit;
import com.sensirion.libble.utils.RHTDataPoint;
import com.sensirion.libble.utils.TemperatureUnit;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class SHTC1HistoryService extends AbstractHistoryService {

    public static final String SERVICE_UUID = "0000fa20-0000-1000-8000-00805f9b34fb";

    private static final int TIMEOUT_DOWNLOAD_DATA_MS = 7500;
    private static final int MAX_CONSECUTIVE_TRIES = 10;

    private static final String START_STOP_UUID = "0000fa21-0000-1000-8000-00805f9b34fb";
    private static final String LOGGING_INTERVAL_UUID = "0000fa22-0000-1000-8000-00805f9b34fb";
    private static final String CURRENT_POINTER_UUID = "0000fa23-0000-1000-8000-00805f9b34fb";
    private static final String START_POINTER_UUID = "0000fa24-0000-1000-8000-00805f9b34fb";
    private static final String END_POINTER_UUID = "0000fa25-0000-1000-8000-00805f9b34fb";
    private static final String LOGGED_DATA_UUID = "0000fa26-0000-1000-8000-00805f9b34fb";
    private static final String USER_DATA_UUID = "0000fa27-0000-1000-8000-00805f9b34fb";

    private static final int DATA_POINT_SIZE = 4;

    private static final int GADGET_RINGBUFFER_SIZE = 16384;

    private static final int MAX_WAITING_TIME_BETWEEN_REQUEST_MS = 1300;
    private static final int SLEEP_BETWEEN_DOWNLOAD_REQUEST_MS = 50;

    private final Set<HumidityListener> mHumidityListeners = Collections.synchronizedSet(new HashSet<HumidityListener>());
    private final Set<TemperatureListener> mTemperatureListeners = Collections.synchronizedSet(new HashSet<TemperatureListener>());
    private final Set<RHTListener> mRHTListeners = Collections.synchronizedSet(new HashSet<RHTListener>());

    private final BluetoothGattCharacteristic mStartStopCharacteristic;
    private final BluetoothGattCharacteristic mIntervalCharacteristic;
    private final BluetoothGattCharacteristic mCurrentPointerCharacteristic;
    private final BluetoothGattCharacteristic mStartPointerCharacteristic;
    private final BluetoothGattCharacteristic mEndPointerCharacteristic;
    private final BluetoothGattCharacteristic mLoggedDataCharacteristic;
    private final BluetoothGattCharacteristic mUserDataCharacteristic;

    private int mExtractedDataPointsCounter = 0;
    private long lastTimeLogged;

    @Nullable
    private Integer mStartPointDownload = null;
    @Nullable
    private Boolean mLoggingIsEnabled = null;
    @Nullable
    private Integer mInterval = null;
    @Nullable
    private Integer mCurrentPointer = null;
    @Nullable
    private Integer mStartPointer = null;
    @Nullable
    private Integer mEndPointer = null;
    @Nullable
    private Integer mUserData = null;

    private volatile boolean mDownloadInProgress = false;

    public SHTC1HistoryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mStartStopCharacteristic = super.getCharacteristic(START_STOP_UUID);
        mIntervalCharacteristic = super.getCharacteristic(LOGGING_INTERVAL_UUID);
        mCurrentPointerCharacteristic = super.getCharacteristic(CURRENT_POINTER_UUID);
        mStartPointerCharacteristic = super.getCharacteristic(START_POINTER_UUID);
        mEndPointerCharacteristic = super.getCharacteristic(END_POINTER_UUID);
        mLoggedDataCharacteristic = super.getCharacteristic(LOGGED_DATA_UUID);
        mUserDataCharacteristic = super.getCharacteristic(USER_DATA_UUID);
        addCharacteristicsTo(bluetoothGattService);
        prepareCharacteristics();
        isServiceSynchronized();
    }

    private void addCharacteristicsTo(@NonNull final BluetoothGattService bluetoothGattService) {
        bluetoothGattService.addCharacteristic(mStartStopCharacteristic);
        bluetoothGattService.addCharacteristic(mIntervalCharacteristic);
        bluetoothGattService.addCharacteristic(mCurrentPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mStartPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mEndPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mLoggedDataCharacteristic);
        bluetoothGattService.addCharacteristic(mUserDataCharacteristic);
    }

    private void prepareCharacteristics() {
        mPeripheral.readCharacteristic(mStartStopCharacteristic);
        mPeripheral.readCharacteristic(mIntervalCharacteristic);
        mPeripheral.readCharacteristic(mCurrentPointerCharacteristic);
        mPeripheral.readCharacteristic(mStartPointerCharacteristic);
        mPeripheral.readCharacteristic(mEndPointerCharacteristic);
        mPeripheral.readCharacteristic(mUserDataCharacteristic);
    }

    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicUpdate(characteristic);

        final String characteristicUUID = characteristic.getUuid().toString();

        switch (characteristicUUID) {
            case START_STOP_UUID:
                mLoggingIsEnabled = characteristic.getIntValue(FORMAT_UINT8, 0) == 1;
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s logging is %s", getDeviceAddress(), ((mLoggingIsEnabled) ? "enabled" : "disabled")));
                break;
            case LOGGING_INTERVAL_UUID:
                mInterval = characteristic.getIntValue(FORMAT_UINT16, 0);
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s interval it's at %d seconds.", getDeviceAddress(), mInterval));
                break;
            case CURRENT_POINTER_UUID:
                mCurrentPointer = characteristic.getIntValue(FORMAT_UINT32, 0);
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s current pointer is: %d", getDeviceAddress(), mCurrentPointer));
                break;
            case START_POINTER_UUID:
                mStartPointer = characteristic.getIntValue(FORMAT_UINT32, 0);
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s start pointer is: %d", getDeviceAddress(), mStartPointer));
                break;
            case END_POINTER_UUID:
                mEndPointer = characteristic.getIntValue(FORMAT_UINT32, 0);
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s end pointer is: %d", getDeviceAddress(), mEndPointer));
                break;
            case LOGGED_DATA_UUID:
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s received a log.", getDeviceAddress()));
                parseLoggedData(characteristic);
                break;
            case USER_DATA_UUID:
                mUserData = characteristic.getIntValue(FORMAT_UINT32, 0);
                Log.d(TAG, String.format("onCharacteristicUpdate -> Device %s received %d as it's user data.", mPeripheral.getAddress(), mUserData));
                break;
            default:
                return false;
        }
        return true;
    }

    /**
     * Obtains all the attributes from the device, this method should be called when initializing the gadget
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return <code>true</code> if data was synchronized correctly - <code>false</code> otherwise.
     */
    @Override
    public synchronized boolean isServiceSynchronized() {
        if (checkGadgetLoggingState() == null) {
            Log.w(TAG, "isServiceSynchronized -> Logging state is not synchronized");
        }
        if (getDownloadIntervalSeconds() == null) {
            Log.w(TAG, "isServiceSynchronized -> Download interval is not synchronized.");
        }
        if (getCurrentPoint() == null) {
            Log.w(TAG, "isServiceSynchronized -> Current pointer is not synchronized.");
        }
        if (getStartPointer() == null) {
            Log.w(TAG, "isServiceSynchronized -> Start pointer is not synchronized.");
        }
        if (getEndPointer() == null) {
            Log.w(TAG, "isServiceSynchronized -> End pointer is not synchronized.");
        }
        if (getUserData() == null) {
            Log.w(TAG, "isServiceSynchronized -> User data is not synchronized.");
        }

        if (mLoggingIsEnabled == null || mInterval == null || mCurrentPointer == null || mEndPointer == null || mUserData == null || mStartPointer == null) {
            Log.e(TAG, "isServiceSynchronized -> Service is not synchronized yet.");
            return false;
        }
        return true;
    }

    /**
     * This method checks if logging is enabled or disabled.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return <code>true</code> if logging is enabled - <code>false</code> if logging is disabled - <code>null</code> if the state is unknown.
     */
    @Nullable
    private Boolean checkGadgetLoggingState() {
        if (mLoggingIsEnabled == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            super.mPeripheral.readCharacteristic(mStartPointerCharacteristic);
        }
        return mLoggingIsEnabled;
    }

    /**
     * This method checks if logging is enabled or disabled.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return <code>true</code> if logging is enabled - <code>false</code> if logging is disabled.
     */
    @Override
    public boolean isGadgetLoggingEnabled() {
        checkGadgetLoggingState();
        if (mLoggingIsEnabled == null) {
            Log.e(TAG, String.format("isGadgetLoggingEnabled -> A problem was produced when trying to know is logging is enabled in device %s.", getDeviceAddress()));
            return false;
        }
        return mLoggingIsEnabled;
    }

    /**
     * Checks is the user can modify the logging state.
     *
     * @return <code>true</code> if the user can enable or disable logging - <code>false</code> otherwise.
     */
    @Override
    public boolean isLoggingStateEditable() {
        return true;
    }

    /**
     * This method checks the logging interval.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return [@link java.lang.Integer} with the interval - return <code>null</code> if the interval is not available yet.
     */
    @Nullable
    public Integer getDownloadIntervalSeconds() {
        if (mInterval == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            mPeripheral.readCharacteristic(mIntervalCharacteristic);
        }
        return mInterval;
    }

    /**
     * This method returns the logging interval in milliseconds to the client device.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the logging interval in milliseconds - <code>null</code> if not available yet.
     */
    @Override
    @Nullable
    public Integer getLoggingIntervalMs() {
        final Integer intervalInSeconds = getDownloadIntervalSeconds();
        if (intervalInSeconds == null) {
            return null;
        }
        return intervalInSeconds * 1000;
    }

    /**
     * This method gets the current pointer of the device.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the start pointer - <code>null</code> if it doesn't have one.
     */
    @Nullable
    public Integer getCurrentPoint() {
        if (mCurrentPointer == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            super.mPeripheral.readCharacteristic(mCurrentPointerCharacteristic);
        }
        return mCurrentPointer;
    }

    /**
     * This method gets the start pointer of the device.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the start pointer - <code>null</code> if it's null.
     */
    @Nullable
    public Integer getStartPointer() {
        if (mStartPointer == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mStartPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            super.mPeripheral.readCharacteristic(mStartPointerCharacteristic);
        }
        return mStartPointer;
    }

    /**
     * This method checks the end pointer in the device.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the end pointer - <code>-1</code> if it doesn't have one.
     */
    @Nullable
    public Integer getEndPointer() {
        if (mEndPointer == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            super.mPeripheral.readCharacteristic(mEndPointerCharacteristic);
        }
        return mEndPointer;
    }

    /**
     * This method checks the introduced user data.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the introduced user data. <code>null</code> if the user data is unknown.
     */
    @Nullable
    public Integer getUserData() {
        if (mUserData == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    SHTC1HistoryService.super.mPeripheral.forceReadCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
                }
            });
        } else {
            super.mPeripheral.readCharacteristic(mUserDataCharacteristic);
        }
        return mUserData;
    }

    /**
     * Enables or disables logging. Epoch and interval should be called first.
     *
     * @param enable <code>true</code> for enabling logging - <code>false</code> for disabling it.
     * @return <code>true</code> if the action was completed correctly, <code>false</code> otherwise.
     */
    @Override
    public boolean setLoggingState(final boolean enable) {
        if (mDownloadInProgress && enable) {
            Log.e(TAG, "setLoggingState -> We can't enable logging while a download its in progress.");
            return false;
        }
        // Prepares user data characteristic.
        mStartStopCharacteristic.setValue((byte) ((enable) ? 1 : 0), FORMAT_UINT8, 0);
        mPeripheral.forceWriteCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        return mLoggingIsEnabled != null && enable == mLoggingIsEnabled;
    }

    /**
     * Sets the interval of the logging, can be done when logging it's ongoing.
     *
     * @param intervalInMilliseconds that the device will adopt for logging.
     * @return <code>true</code> if the interval was set - <code>false</code> otherwise.
     */
    @Override
    public boolean setDownloadInterval(final int intervalInMilliseconds) {
        if (mLoggingIsEnabled != null && mLoggingIsEnabled) {
            Log.e(TAG, "setInterval -> We can't set the interval because logging it's enabled.");
            return false;
        }
        if (mDownloadInProgress) {
            Log.e(TAG, "setInterval -> We can't set the interval because there's a download in progress");
            return false;
        }
        final int intervalInSeconds = intervalInMilliseconds / 1000;

        if (mInterval != null && mInterval == intervalInSeconds) {
            Log.i(TAG, String.format("setInterval -> Interval was already %s on the peripheral %s", intervalInSeconds, mPeripheral.getAddress()));
            return true;
        }
        // Prepares logging interval characteristic.
        mIntervalCharacteristic.setValue(intervalInSeconds, FORMAT_UINT16, 0);
        mPeripheral.forceWriteCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);

        return mInterval != null && mInterval == intervalInSeconds;
    }

    /**
     * Sets the start pointer to the minimum possible value. It's done just before logging.
     *
     * @return <code>true</code> if the start pointer was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean resetStartPointer() {
        return setStartPointer(null);
    }

    /**
     * Sets the start pointer. It's done just before logging.
     *
     * @param userStartPoint with the start point selected by the user - <code>null</code> if the user wants the minimum possible start point.
     * @return <code>true</code> if the start pointer was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean setStartPointer(@Nullable final Integer userStartPoint) {
        mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        if (userStartPoint == null) {
            Log.d(TAG, "setStartPointer() -> userStartPoint == null, user wants all the data!");
            mStartPointDownload = calculateMinimumStartPoint();
        } else {
            if (userStartPoint < 0) {
                throw new IllegalArgumentException(String.format("%s: setStartPointer() -> userStartPoint has to be null or >= 0", TAG));
            }
            if (mCurrentPointer == null) {
                Log.e(TAG, "setStartPointer -> Service is not synchronized yet.");
                return false;
            }
            if (userStartPoint.equals(mStartPointer) && mCurrentPointer < GADGET_RINGBUFFER_SIZE) {
                Log.i(TAG, String.format("setStartPointer() -> Start pointer is already %d on the peripheral: %s", mStartPointer, mPeripheral.getAddress()));
                return true;
            }
            mStartPointDownload = calculateStartPoint(userStartPoint);
            if (mStartPointDownload == null) {
                Log.e(TAG, "setStartPointer() -> It was impossible to set the start point of the device.");
                return false;
            }
        }
        if (mStartPointDownload.equals(mStartPointer)) {
            Log.i(TAG, "setStartPointer() -> Start point was already set in the device.");
            return true;
        }
        // Prepares logging interval characteristic.
        Log.i(TAG, String.format("setStartPointer() -> Start point will be set to: %d", mStartPointDownload));
        mStartPointerCharacteristic.setValue(mStartPointDownload, FORMAT_UINT32, 0);
        return mPeripheral.forceWriteCharacteristic(mStartPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
    }

    private int calculateMinimumStartPoint() {
        if (mCurrentPointer == null) {
            Log.e(TAG, "Service is not synchronized yet.");
            return 0;
        }
        if (mCurrentPointer > GADGET_RINGBUFFER_SIZE) {
            return mCurrentPointer - GADGET_RINGBUFFER_SIZE;
        }
        return 1;
    }

    @Nullable
    private Integer calculateStartPoint(@NonNull final Integer userStartPoint) {
        if (mEndPointer == null) {
            Log.e(TAG, "calculateStartPoint -> Service is not synchronized yet. ");
            return null;
        }

        if (userStartPoint >= mEndPointer) {
            Log.w(TAG, "calculateStartPoint() -> userStartPoint is bigger or equal than EndPointer!");
            return null;
        }
        if (mEndPointer <= GADGET_RINGBUFFER_SIZE) {
            return userStartPoint;
        }
        if (mEndPointer - GADGET_RINGBUFFER_SIZE < userStartPoint) {
            return userStartPoint;
        }
        return mEndPointer - GADGET_RINGBUFFER_SIZE - 1;
    }

    /**
     * Prepare end pointer for logging obtaining the maximum number of values.
     *
     * @return <code>true</code> if the end pointer was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean resetEndPointer() {
        return setEndPointer(null);
    }

    /**
     * Prepare end pointer for logging obtaining.
     *
     * @param endPointer number of that have to be set in the endPointer of the device - <code>null</code> for setting the current value.
     * @return <code>true</code> if the end pointer was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean setEndPointer(@Nullable final Integer endPointer) {
        super.mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);

        if (mCurrentPointer != null && mCurrentPointer.equals(endPointer)) {
            Log.i(TAG, String.format("setEndPointer -> The device %s already had the end pointer set.", mPeripheral.getAddress()));
            return true;
        }

        if (endPointer == null) {
            Log.i(TAG, "setEndPointer -> Setting the end pointer to the maximum possible value.");
            mEndPointerCharacteristic.setValue(mCurrentPointer, FORMAT_UINT32, 0);
        } else {
            mEndPointerCharacteristic.setValue(endPointer, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        }
        mPeripheral.forceWriteCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        Log.i(TAG, String.format("setEndPointer -> New end pointer: %s", mEndPointer));
        return true;
    }

    /**
     * Sets the user data on the logging.
     *
     * @param userData integer, normally the epochTime time of the device.
     * @return <code>true</code> if the user data was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean setUserData(final int userData) {
        mUserDataCharacteristic.setValue(userData, FORMAT_UINT32, 0);
        mPeripheral.forceWriteCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        return mUserData != null && mUserData == userData;
    }

    /**
     * This method enables or disables logging in a smartgadget device. It will toggle off the log during data download.
     *
     * @param enable <code>true</code> for enabling logging - <code>false</code> for disabling it.
     * @return <code>true</code> if the request succeeded - <code>false</code> in case of failure.
     */
    public synchronized boolean setGadgetLoggingEnabled(final boolean enable) {
        Log.d(TAG, String.format("setGadgetLoggingEnabled -> Trying to %s logging service on the device %s", (enable ? "enable" : "disable"), getDeviceAddress()));
        if (enable == isGadgetLoggingEnabled()) {
            Log.e(TAG, String.format("setGadgetLoggingEnabled -> In the device %s logging is already %s.", getDeviceAddress(), (enable ? "enabled" : "disabled")));
            return true;
        }
        if (enable) {
            return enableLogging();
        }
        return disableLogging();
    }

    private boolean enableLogging() {
        if (updateLastSyncedTimestampOnDevice()) {
            Log.d(TAG, "setGadgetLoggingEnabled -> User data was set in the device.");
        } else {
            Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible to set interval correctly on the device.");
            return false;
        }
        if (setLoggingState(true)) {
            Log.i(TAG, String.format("setGadgetLoggingEnabled -> In the peripheral %s logging was enabled", getDeviceAddress()));
            return true;
        }
        Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible enable logging the device.");
        return false;
    }

    private boolean disableLogging() {
        if (setLoggingState(false)) {
            Log.i(TAG, String.format("setGadgetLoggingEnabled -> In the peripheral %s logging was disabled", mPeripheral.getAddress()));
            return true;
        }
        Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible enable logging the device.");
        return false;
    }

    private boolean updateLastSyncedTimestampOnDevice() {
        final int epochTime = getEpochTime();

        if (setUserData(epochTime)) {
            Log.i(TAG, String.format("updateLastSyncedTimestampOnDevice -> In peripheral %s the user data was set to the epochTime time: %s ", mPeripheral.getAddress(), epochTime));
            return true;
        }
        return false;
    }

    /**
     * Downloads all the data from the device.
     */
    @Override
    public synchronized boolean startDataDownload() {
        if (mRHTListeners.isEmpty()) {
            Log.e(TAG, "startDataDownload -> There's a need for at least one listener in order to start logging data from the device");
            return false;
        }

        if (isServiceSynchronized()) {
            Log.i(TAG, "startDataDownload -> Download will start.");
        } else {
            Log.e(TAG, "startDataDownload -> Service is not synchronized yet.");
            return false;
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final boolean wasDeviceLoggingEnabled = isGadgetLoggingEnabled();
                mDownloadInProgress = true;
                prepareDeviceToDownload(wasDeviceLoggingEnabled);
                downloadDataFromPeripheral();
                mPeripheral.cleanCharacteristicCache();
                mDownloadInProgress = false;
                if (wasDeviceLoggingEnabled) {
                    setGadgetLoggingEnabled(true);
                }
                onDownloadComplete();
            }
        });
        return true;
    }

    /**
     * Downloads all the data from the device.
     */
    @Override
    public synchronized boolean startDataDownload(final long oldestTimestampToDownload) {
        return startDataDownload();
    }

    private void prepareDeviceToDownload(final boolean wasDeviceLoggingEnabled) {
        //If the gadget logging is enabled it disables it for downloading the data.
        if (wasDeviceLoggingEnabled) {
            setGadgetLoggingEnabled(false);
        }
        resetStartPointer();
        resetEndPointer();
    }

    private synchronized void downloadDataFromPeripheral() {
        mExtractedDataPointsCounter = 0;
        final Integer totalValuesToDownload = calculateValuesToDownload();
        final Integer endPointer = getEndPointer();
        if (totalValuesToDownload == null || endPointer == null) {
            Log.e(TAG, "downloadDataFromPeripheral -> Service needs to be synchronized ");
            return;
        }
        while (getEndPointer() > 0 && mDownloadInProgress) {
            for (int i = totalValuesToDownload; i > 0; i--) {
                mPeripheral.readCharacteristic(mLoggedDataCharacteristic);
                /**
                 * A wait of 5 milliseconds is produced after asking the device for reading new logged data
                 * in order to improve the Android BLE stack stability. If this wait is not produced the stack
                 * receives too many request at a time making it really hard to process all the orders at the
                 * same time. With this wait it 'only' receives a maximum of 200 orders per second.
                 */
                try {
                    Thread.sleep(SLEEP_BETWEEN_DOWNLOAD_REQUEST_MS);
                } catch (InterruptedException ignored) {
                }
            }
            if (lastTimeLogged < System.currentTimeMillis() - TIMEOUT_DOWNLOAD_DATA_MS) {
                onDownloadFailure();
                return;
            }
        }
    }

    @Nullable
    private Integer calculateValuesToDownload() {
        final Integer totalNumberOfValues = getNumberLoggedElements();
        if (totalNumberOfValues == null) {
            Log.e(TAG, "calculateValuesToDownload -> Service is not synchronized yet.");
            return null;
        }
        notifyTotalNumberElements(totalNumberOfValues);
        lastTimeLogged = System.currentTimeMillis();
        Log.i(TAG, String.format("calculateValuesToDownload -> The user has to download %d values.", totalNumberOfValues));
        return totalNumberOfValues;
    }

    /**
     * Parses the logged data from the device.
     *
     * @param characteristic characteristic we want to parse.
     * @return <code>true</code> if it was able to parse it - <code>false</code> otherwise.
     */
    private boolean parseLoggedData(@NonNull final BluetoothGattCharacteristic characteristic) {
        final byte[] rhtRawData = characteristic.getValue();

        if (mUserData == null || mStartPointDownload == null || mInterval == null) {
            Log.e(TAG, "parseLoggedData -> Service is not synchronized yet.");
            return false;
        }

        if (rhtRawData.length == 0) {
            Log.i(TAG, "parseLoggedData -> Data download has finished successfully.");
            mDownloadInProgress = false;
            return true;
        } else if (rhtRawData.length % DATA_POINT_SIZE > 0) {
            Log.e(TAG, String.format("isRawDatapointCorrupted -> The received data don't have a valid length: %d", rhtRawData.length));
            return false;
        }

        for (int i = 0; i < rhtRawData.length; i += DATA_POINT_SIZE) {
            //Decrypts the humidity and temperature and adds it to the list and to the stack.
            final byte[] dataPoint = new byte[DATA_POINT_SIZE];
            final short[] humidityAndTemperature = new short[DATA_POINT_SIZE / 2];

            //Obtains the datapoint from the array.
            System.arraycopy(rhtRawData, i, dataPoint, 0, DATA_POINT_SIZE);

            //Separates the datapoint between humidity and temperature.
            ByteBuffer.wrap(dataPoint).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

            //Creates a datapoint object with the obtained data, and fills it with humidity and temperature.
            final float temperature = ((float) humidityAndTemperature[0]) / 100f;
            final float humidity = ((float) humidityAndTemperature[1]) / 100f;
            final int epoch = mUserData + (mStartPointDownload + mExtractedDataPointsCounter) * mInterval;
            final long timestamp = epoch * 1000l;

            final RHTDataPoint extractedDataPoint = new RHTDataPoint(temperature, humidity, timestamp);

            Log.i(TAG, String.format("parseLoggedData -> Logged in device %s values %s", mPeripheral.getAddress(), extractedDataPoint.toString()));

            //Adds the new datapoint to the the list.
            mExtractedDataPointsCounter++;
            lastTimeLogged = System.currentTimeMillis();

            onDatapointRead(extractedDataPoint);
        }
        return true;
    }

    /**
     * Checks the number of logged elements that the user can download.
     *
     * @return {@link java.lang.Integer} with the total number of elements to log.
     */
    @Override
    @Nullable
    public Integer getNumberLoggedElements() {
        if (mCurrentPointer == null || mCurrentPointer == 0) {
            Log.e(TAG, "getNumberLoggedElements -> The device is not synchronized yet. (hint -> Call 'isSynchronized()' first)");
            return null;
        }
        resetStartPointer();
        if (mStartPointer == null) {
            Log.e(TAG, "getNumberLoggedElements -> Cannot obtain the start pointer of the device.");
            return null;
        }
        int totalValues = mCurrentPointer - mStartPointer;
        if (totalValues > GADGET_RINGBUFFER_SIZE) {
            totalValues = GADGET_RINGBUFFER_SIZE;
        }
        Log.i(TAG, String.format("getNumberElementsToLog -> The device has %d values to log.", totalValues));
        notifyTotalNumberElements(totalValues);
        return totalValues;
    }

    /**
     * Adds a new download listener to the list.
     *
     * @param newListener listener that wants to listen for notifications.
     */
    @Override
    public boolean registerNotificationListener(@NonNull final NotificationListener newListener) {
        final boolean historyListenerFound = super.registerNotificationListener(newListener);
        boolean rhtListenerFound = false;
        boolean temperatureListenerFound = false;
        boolean humidityListenerFound = false;

        if (newListener instanceof RHTListener) {
            mRHTListeners.add((RHTListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new RHT listener: %s ", getDeviceAddress(), newListener));
            rhtListenerFound = true;
        }
        if (newListener instanceof TemperatureListener) {
            mTemperatureListeners.add((TemperatureListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new Temperature listener: %s ", getDeviceAddress(), newListener));
            temperatureListenerFound = true;
        }
        if (newListener instanceof HumidityListener) {
            mHumidityListeners.add((HumidityListener) newListener);
            Log.i(TAG, String.format("registerNotificationListener -> Peripheral %s received a new Humidity listener: %s ", getDeviceAddress(), newListener));
            humidityListenerFound = true;
        }
        return historyListenerFound || rhtListenerFound || temperatureListenerFound || humidityListenerFound;
    }

    /**
     * Removes a listener from the download notification list.
     *
     * @param listenerForRemoval listener that doesn't need the listen for notifications anymore.
     */
    @Override
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listenerForRemoval) {
        final boolean historyListener = super.unregisterNotificationListener(listenerForRemoval);
        boolean rhtListener = false;
        boolean temperatureListener = false;
        boolean humidityListener = false;

        if (listenerForRemoval instanceof RHTListener) {
            mRHTListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the RHTListener list.", getDeviceAddress(), listenerForRemoval));
            rhtListener = true;
        }
        if (listenerForRemoval instanceof TemperatureListener) {
            mTemperatureListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the Temperature list.", getDeviceAddress(), listenerForRemoval));
            temperatureListener = true;
        }

        if (listenerForRemoval instanceof HumidityListener) {
            mHumidityListeners.remove(listenerForRemoval);
            Log.i(TAG, String.format("unregisterNotificationListener -> Peripheral %s deleted %s listener from the Humidity list.", getDeviceAddress(), listenerForRemoval));
            humidityListener = true;
        }
        return historyListener || rhtListener || temperatureListener || humidityListener;
    }

    /**
     * Notifies download progress to the listeners.
     */
    private void onDatapointRead(@NonNull final RHTDataPoint dataPoint) {
        notifyDatapointRead(dataPoint);
        notifyHistoricalHumidity(dataPoint);
        notifyHistoricalTemperature(dataPoint);
        super.notifyDownloadProgress(mExtractedDataPointsCounter);
    }

    private void notifyDatapointRead(@NonNull final RHTDataPoint dataPoint) {
        final Iterator<RHTListener> iterator = mRHTListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final RHTListener listener = iterator.next();
                listener.onNewHistoricalRHTValue(mPeripheral, dataPoint, getSensorName());
            } catch (RuntimeException e) {
                Log.e(TAG, "onDatapointRead() -> Listener was removed from the list because the following exception was thrown -> ", e);
                iterator.remove();
            }
        }
    }

    private void notifyHistoricalTemperature(@NonNull final RHTDataPoint dataPoint) {
        final Iterator<TemperatureListener> iterator = mTemperatureListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final TemperatureListener listener = iterator.next();
                listener.onNewHistoricalTemperature(mPeripheral, dataPoint.getTemperatureCelsius(), dataPoint.getTimestamp(), getSensorName(), TemperatureUnit.CELSIUS);
            } catch (RuntimeException e) {
                Log.e(TAG, "onDatapointRead() -> Listener was removed from the list because the following exception was thrown -> ", e);
                iterator.remove();
            }
        }
    }

    private void notifyHistoricalHumidity(@NonNull final RHTDataPoint dataPoint) {
        final Iterator<HumidityListener> iterator = mHumidityListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final HumidityListener listener = iterator.next();
                listener.onNewHistoricalHumidity(mPeripheral, dataPoint.getRelativeHumidity(), dataPoint.getTimestamp(), getSensorName(), HumidityUnit.RELATIVE_HUMIDITY);
            } catch (final RuntimeException e) {
                Log.e(TAG, "onDatapointRead() -> Listener was removed from the list because the following exception was thrown -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Deletes the data from the device.
     */
    @Override
    public boolean resetDeviceData() {
        final Boolean initialLoggingState = mLoggingIsEnabled;
        if (initialLoggingState == null) {
            Log.e(TAG, "resetDeviceData -> Service is not synchronized yet.");
            return false;
        }
        if (mLoggingIsEnabled) {
            setGadgetLoggingEnabled(false);
        }
        setGadgetLoggingEnabled(true);
        if (initialLoggingState) {
            Log.i(TAG, String.format("resetDeviceData -> In device %s data was deleted.", getDeviceAddress()));
            return true;
        }
        return setGadgetLoggingEnabled(false);
    }

    /**
     * Checks if the user is downloading data from the service.
     *
     * @return <code>true</code> if the user is downloading data from the device - <code>false</code> otherwise.
     */
    @Override
    public boolean isDownloadInProgress() {
        return mDownloadInProgress;
    }

    @Override
    public void registerDeviceCharacteristicNotifications() {
        // This service does not receive direct notifications from the device.
    }

    /**
     * Ask for the Epoch time. (Unix time in seconds)
     *
     * @return <code>int</code> with the epochTime time.
     */
    private int getEpochTime() {
        return (int) (System.currentTimeMillis() / 1000l);
    }

    /**
     * Obtains the sensor name of the logging service.
     *
     * @return {@link java.lang.String} with the sensor name - <code>null</code> if the sensor name is not known.
     */
    @Nullable
    private String getSensorName() {
        switch (mPeripheral.getAdvertisedName()) {
            case "SHTC1 smart gadget":
                return "SHTC1";
            case "SHT31 Smart Gadget":
                return "SHT31";
            default:
                return null;
        }
    }
}