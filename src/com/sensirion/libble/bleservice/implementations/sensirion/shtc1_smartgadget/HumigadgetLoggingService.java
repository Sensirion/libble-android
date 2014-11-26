package com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.NotificationListener;
import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.PeripheralService;
import com.sensirion.libble.bleservice.implementations.sensirion.common.LogDownloadListener;
import com.sensirion.libble.bleservice.implementations.sensirion.common.RHTDataPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;

public class HumigadgetLoggingService extends PeripheralService {

    public static final String SERVICE_UUID = "0000fa20-0000-1000-8000-00805f9b34fb";

    private static final int TIMEOUT_DOWNLOAD_DATA_MS = 7500;
    private static final int MAX_CONSECUTIVE_TRIES = 10;

    private static final String TAG = HumigadgetLoggingService.class.getSimpleName();

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

    private final List<LogDownloadListener> mListeners = Collections.synchronizedList(new LinkedList<LogDownloadListener>());

    private final BluetoothGattCharacteristic mStartStopCharacteristic;
    private final BluetoothGattCharacteristic mIntervalCharacteristic;
    private final BluetoothGattCharacteristic mCurrentPointerCharacteristic;
    private final BluetoothGattCharacteristic mStartPointerCharacteristic;
    private final BluetoothGattCharacteristic mEndPointerCharacteristic;
    private final BluetoothGattCharacteristic mLoggedDataCharacteristic;
    private final BluetoothGattCharacteristic mUserDataCharacteristic;

    private int mExtractedDatapointsCounter = 0;
    private long lastTimeLogged;
    private byte mNumConsecutiveFailsReadingData = 0;

    private Boolean mLoggingIsEnabled = null;
    private Integer mInterval = null;
    private Integer mCurrentPointer = null;
    private Integer mStartPointer = null;
    private Integer mEndPointer = null;
    private Integer mUserData = null;

    private volatile boolean mDownloadInProgress = false;

    public HumigadgetLoggingService(final Peripheral parent, final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mStartStopCharacteristic = super.getCharacteristicFor(START_STOP_UUID);
        mIntervalCharacteristic = super.getCharacteristicFor(LOGGING_INTERVAL_UUID);
        mCurrentPointerCharacteristic = super.getCharacteristicFor(CURRENT_POINTER_UUID);
        mStartPointerCharacteristic = super.getCharacteristicFor(START_POINTER_UUID);
        mEndPointerCharacteristic = super.getCharacteristicFor(END_POINTER_UUID);
        mLoggedDataCharacteristic = super.getCharacteristicFor(LOGGED_DATA_UUID);
        mUserDataCharacteristic = super.getCharacteristicFor(USER_DATA_UUID);
        addCharacteristicsTo(bluetoothGattService);
    }

    /**
     * Ask for the Epoch time. (Unix time in seconds)
     *
     * @return <code>int</code> with the epochTime time.
     */
    private static int getEpochTime() {
        return (int) (System.currentTimeMillis() / 1000l);
    }

    private void addCharacteristicsTo(final BluetoothGattService bluetoothGattService) {
        bluetoothGattService.addCharacteristic(mStartStopCharacteristic);
        bluetoothGattService.addCharacteristic(mIntervalCharacteristic);
        bluetoothGattService.addCharacteristic(mCurrentPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mStartPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mEndPointerCharacteristic);
        bluetoothGattService.addCharacteristic(mLoggedDataCharacteristic);
        bluetoothGattService.addCharacteristic(mUserDataCharacteristic);
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicRead(characteristic);

        if (characteristic == null) {
            Log.e(TAG, "onCharacteristicRead -> Received a null characteristic");
            return false;
        }

        final String characteristicUUID = characteristic.getUuid().toString();

        if (characteristicUUID.equals(START_STOP_UUID)) {
            mLoggingIsEnabled = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 1;
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s logging is %s", mPeripheral.getAddress(), ((mLoggingIsEnabled) ? "enabled" : "disabled")));
        } else if (characteristicUUID.equals(LOGGING_INTERVAL_UUID)) {
            mInterval = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s interval it's at %d seconds.", mPeripheral.getAddress(), mInterval));
        } else if (characteristicUUID.equals(CURRENT_POINTER_UUID)) {
            mCurrentPointer = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s current pointer is: %d", mPeripheral.getAddress(), mCurrentPointer));
        } else if (characteristicUUID.equals(START_POINTER_UUID)) {
            mStartPointer = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s start pointer is: %d", mPeripheral.getAddress(), mStartPointer));
        } else if (characteristicUUID.equals(END_POINTER_UUID)) {
            mEndPointer = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s end pointer is: %d", mPeripheral.getAddress(), mEndPointer));
        } else if (characteristicUUID.equals(LOGGED_DATA_UUID)) {
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s received a log.", mPeripheral.getAddress()));
            parseLoggedData(characteristic);
        } else if (characteristicUUID.equals(USER_DATA_UUID)) {
            mUserData = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            Log.d(TAG, String.format("onCharacteristicRead -> Device %s received %d as it's user data.", mPeripheral.getAddress(), mUserData));
        } else {
            return false;
        }
        return true;
    }

    /**
     * Obtains all the attributes from the device, this method should be called when initializing the gadget.
     *
     * @return <code>true</code> if data was synchronized correctly - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public synchronized boolean synchronizeData() {
        if (isGadgetLoggingEnabled() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to read the enabling state of the device.");
            return false;
        }
        if (getInterval() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to read the interval of the device.");
            return false;
        }
        if (getCurrentPoint() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to read the current pointer of the device.");
            return false;
        }
        if (getStartPointer() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to obtain the start pointer of the device.");
            return false;
        }
        if (getEndPointer() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to obtain the end pointer of the device.");
            return false;
        }
        if (getUserData() == null) {
            Log.e(TAG, "synchronizeData -> A problem was produced when trying to user data of the device.");
            return false;
        }
        return true;
    }

    /**
     * This method checks if logging is enabled or disabled.
     *
     * @return <code>true</code> if logging is enabled - <code>false</code> if logging is disabled.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public Boolean isGadgetLoggingEnabled() {
        if (mLoggingIsEnabled == null) {
            super.mPeripheral.forceReadCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        } else {
            super.mPeripheral.readCharacteristic(mStartPointerCharacteristic);
        }
        return mLoggingIsEnabled;
    }

    /**
     * This method checks the logging interval.
     *
     * @return number if we know the interval - return <code>null</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public Integer getInterval() {
        if (mInterval == null) {
            super.mPeripheral.forceReadCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        } else {
            super.mPeripheral.readCharacteristic(mIntervalCharacteristic);
        }
        return mInterval;
    }

    /**
     * This method checks the current pointer of the device.
     *
     * @return {@link java.lang.Integer} with the start pointer - <code>null</code> if it doesn't have one.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public Integer getCurrentPoint() {
        if (mCurrentPointer == null) {
            super.mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        } else {
            super.mPeripheral.readCharacteristic(mCurrentPointerCharacteristic);
        }
        return mCurrentPointer;
    }

    /**
     * This method checks the start pointer of the device.
     *
     * @return {@link java.lang.Integer} with the start pointer - <code>-1</code> if it doesn't have one.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public Integer getStartPointer() {
        if (mStartPointer == null) {
            super.mPeripheral.forceReadCharacteristic(mStartPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        } else {
            super.mPeripheral.readCharacteristic(mStartPointerCharacteristic);
        }
        return mStartPointer;
    }

    /**
     * This method checks the end pointer in the device.
     *
     * @return {@link java.lang.Integer} with the end pointer - <code>-1</code> if it doesn't have one.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public Integer getEndPointer() {
        if (mEndPointer == null) {
            super.mPeripheral.forceReadCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        } else {
            super.mPeripheral.readCharacteristic(mEndPointerCharacteristic);
        }
        return mEndPointer;
    }

    /**
     * This method checks the introduced user data.
     * It blocks the UI thread until it receives a response or a timeout is produced.
     *
     * @return {@link java.lang.Integer} with the introduced user data.
     */
    public Integer getUserData() {
        if (mUserData == null) {
            super.mPeripheral.forceReadCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
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
    public boolean setLoggingEnabled(boolean enable) {
        if (mDownloadInProgress && enable) {
            Log.e(TAG, "We can't enable logging while a download it's in progress.");
        }

        // Prepares user data characteristic.
        mStartStopCharacteristic.setValue((byte) ((enable) ? 1 : 0), BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        mPeripheral.forceWriteCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mStartStopCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        return enable == mLoggingIsEnabled;
    }

    /**
     * Sets the interval of the logging, can be done when logging it's ongoing.
     *
     * @param interval in seconds for logging.
     * @return <code>true</code> if the interval was set - <code>false</code> otherwise.
     */
    public boolean setInterval(final int interval) {
        if (mLoggingIsEnabled) {
            Log.e(TAG, "We can't set the interval because logging it's enabled.");
            return false;
        }
        if (mDownloadInProgress) {
            Log.e(TAG, "We can't set the interval because there's a download in progress");
            return false;
        }
        if (interval == mInterval) {
            Log.i(TAG, String.format("Interval was already %s on the peripheral %s", interval, mPeripheral.getAddress()));
            return true;
        }
        // Prepares logging interval characteristic.
        mIntervalCharacteristic.setValue(interval, BluetoothGattCharacteristic.FORMAT_UINT16, 0);
        mPeripheral.forceWriteCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mIntervalCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        return getInterval() == interval;
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
     * Sets the start pointer using the timestamp in ms.
     *
     * @param timestamp from where the user wants to start to download the log - <code>null</code> if the user wants the minimum possible start point.
     * @return <code>true</code> if the user was able to set the pointer - <code>false</code> otherwise.
     */
    public boolean setStartPointerFromTimestampMs(final Long timestamp) {
        if (timestamp == null) {
            return resetStartPointer();
        }
        return setStartPointerFromEpochTime((int) (timestamp / 1000));
    }

    /**
     * Sets the start pointer using the epoch time.
     *
     * @param epochTime from where the user wants to start to download the log - <code>null</code> if the user wants the minimum possible start point.
     * @return <code>true</code> if the user was able to set the pointer - <code>false</code> otherwise.
     */
    public boolean setStartPointerFromEpochTime(final Integer epochTime) {
        if (epochTime == null || epochTime < mUserData) {
            return resetStartPointer();
        }
        return setStartPointer((epochTime - mUserData) / mInterval);
    }

    /**
     * Sets the start pointer. It's done just before logging.
     *
     * @param userStartPoint with the start point selected by the user - <code>null</code> if the user wants the minimum possible start point.
     * @return <code>true</code> if the start pointer was set - <code>false</code> otherwise.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean setStartPointer(final Integer userStartPoint) {
        mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        final Integer recalculatedStartPoint;
        if (userStartPoint == null) {
            Log.d(TAG, "setStartPointer() -> userStartPoint == null, user wants all the data!");
            recalculatedStartPoint = calculateMinimumStartPoint();
        } else {
            if (userStartPoint < 0) {
                throw new IllegalArgumentException(String.format("%s: setStartPointer() -> userStartPoint has to be null or >= 0", TAG));
            }
            if (userStartPoint.equals(mStartPointer) && mCurrentPointer < GADGET_RINGBUFFER_SIZE) {
                Log.i(TAG, String.format("setStartPointer() -> Start pointer is already %d on the peripheral: %s", mStartPointer, mPeripheral.getAddress()));
                return true;
            }
            recalculatedStartPoint = calculateStartPoint(userStartPoint);
            if (recalculatedStartPoint == null) {
                Log.e(TAG, "setStartPointer() -> It was impossible to set the start point of the device.");
                return false;
            }
        }
        if (recalculatedStartPoint.equals(mStartPointer)) {
            Log.i(TAG, "setStartPointer() -> Start point was already set in the device.");
            return true;
        }
        // Prepares logging interval characteristic.
        Log.i(TAG, String.format("setStartPointer() -> Start point will be set to: %d", recalculatedStartPoint));
        mStartPointerCharacteristic.setValue(recalculatedStartPoint, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        return mPeripheral.forceWriteCharacteristic(mStartPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
    }

    private int calculateMinimumStartPoint() {
        if (mCurrentPointer > GADGET_RINGBUFFER_SIZE) {
            return mCurrentPointer - GADGET_RINGBUFFER_SIZE;
        }
        return 1;
    }

    private Integer calculateStartPoint(final Integer userStartPoint) {
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
    public boolean setEndPointer(final Integer endPointer) {
        super.mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);

        if (mCurrentPointer.equals(endPointer)) {
            Log.i(TAG, String.format("setEndPointer -> The device %s already had the end pointer set.", mPeripheral.getAddress()));
            return true;
        }

        if (endPointer == null) {
            Log.i(TAG, "setEndPointer -> Setting the end pointer to the maximum possible value.");
            mEndPointerCharacteristic.setValue(mCurrentPointer, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        } else {
            mEndPointerCharacteristic.setValue(endPointer, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        }
        mPeripheral.forceWriteCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mEndPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        Log.i(TAG, "setEndPointer -> New end pointer: " + mEndPointer);
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
        mUserDataCharacteristic.setValue(userData, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        mPeripheral.forceWriteCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        mPeripheral.forceReadCharacteristic(mUserDataCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);
        return getUserData() == userData;
    }

    /**
     * This method enables or disables logging in a smartgadget device. It will toggle off the log during data download.
     *
     * @param enable <code>true</code> for enabling logging - <code>false</code> for disabling it.
     * @return <code>true</code> if the request succeeded - <code>false</code> in case of failure.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public synchronized boolean setGadgetLoggingEnabled(final boolean enable) {
        Log.d(TAG, String.format("setGadgetLoggingEnabled -> Trying to %s logging service on the device %s", (enable ? "enable" : "disable"), mPeripheral.getAddress()));
        if (enable == isGadgetLoggingEnabled()) {
            Log.e(TAG, String.format("setGadgetLoggingEnabled -> In the device %s logging is already %s.", mPeripheral.getAddress(), (enable ? "enabled" : "disabled")));
            return true;
        }

        if ((mIntervalCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0
                && (mIntervalCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {

            Log.e(TAG, String.format("setGadgetLoggingEnabled -> The device %s doesn't have enough permissions for writing.", mPeripheral.getAddress()));
            return false;
        }

        if (enable) {
            if (updateLastSyncedTimestampOnDevice()) {
                Log.d(TAG, "setGadgetLoggingEnabled -> User data was set in the device.");
            } else {
                Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible to set interval correctly on the device.");
                return false;
            }

            if (setLoggingEnabled(true)) {
                Log.i(TAG, String.format("setGadgetLoggingEnabled -> In the peripheral %s logging was enabled", mPeripheral.getAddress()));
            } else {
                Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible enable logging the device.");
                return false;
            }
        } else {
            if (setLoggingEnabled(false)) {
                Log.i(TAG, String.format("setGadgetLoggingEnabled -> In the peripheral %s logging was disabled", mPeripheral.getAddress()));
            } else {
                Log.e(TAG, "setGadgetLoggingEnabled -> It was impossible enable logging the device.");
                return false;
            }
        }
        return true;
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
    public synchronized void startDataDownload() {
        startDataDownload(null);
    }

    /**
     * Downloads all the data from the device after the given timestamp.
     *
     * @param timestamp from where the user wants to start to download the log.
     */
    public synchronized void startDataDownload(final long timestamp) {
        startDataDownload((Integer) (int) (timestamp / 1000));
    }

    /**
     * Downloads all the data from the device after the given epoch time.
     *
     * @param epochTime from where the user wants to start to download the log.
     */
    public synchronized void startDataDownload(final Integer epochTime) {
        if (mListeners.isEmpty()) {
            Log.e(TAG, "startDataDownload -> There's a need for at least one listener in order to start logging data from the device");
            return;
        }

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                final boolean wasDeviceLoggingEnabled = isGadgetLoggingEnabled();
                mDownloadInProgress = true;
                prepareDeviceToDownload(epochTime, wasDeviceLoggingEnabled);
                downloadDataFromPeripheral();
                mPeripheral.cleanCharacteristicCache();
                mDownloadInProgress = false;
                if (wasDeviceLoggingEnabled) {
                    setGadgetLoggingEnabled(true);
                }
                onDownloadComplete();
            }
        });
    }

    private void prepareDeviceToDownload(final Integer epochTime, final boolean wasDeviceLoggingEnabled) {
        //If the gadget logging is enabled it disables it for downloading the data.
        if (wasDeviceLoggingEnabled) {
            setGadgetLoggingEnabled(false);
        }
        resetEndPointer();
        setStartPointerFromEpochTime(epochTime);
    }

    private void downloadDataFromPeripheral() {
        final int totalValuesToDownload = calculateValuesToDownload();
        while (getEndPointer() > 0 && mNumConsecutiveFailsReadingData < MAX_CONSECUTIVE_TRIES) {
            for (int i = totalValuesToDownload - mExtractedDatapointsCounter; i > 0; i--) {
                mPeripheral.readCharacteristic(mLoggedDataCharacteristic);
                /**
                 * A wait of 5 milliseconds is produced after asking the device for reading new logged data
                 * in order to improve the Android BLE stack stability. If this wait is not produced the stack
                 * receives too many request at a time making it really hard to process all the orders at the
                 * same time. With this wait it 'only' receives a maximum of 200 orders per second.
                 */
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ignored) {
                }
            }
            if (lastTimeLogged < System.currentTimeMillis() - TIMEOUT_DOWNLOAD_DATA_MS) {
                onDownloadFailure();
                return;
            }
        }
    }

    private int calculateValuesToDownload() {
        final int totalNumberOfValues = getNumberElementsToLog();
        mExtractedDatapointsCounter = 0;
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
    private boolean parseLoggedData(final BluetoothGattCharacteristic characteristic) {
        final byte[] rhtRawData = characteristic.getValue();

        if (isRawDatapointCorrupted(rhtRawData)) {
            return false;
        }

        for (int i = 0; i < rhtRawData.length; i += DATA_POINT_SIZE) {
            //Decrypts the humidity and temperature and adds it to the list and to the stack.
            final byte[] dataPoint = new byte[DATA_POINT_SIZE];
            final short[] humidityAndTemperature = new short[DATA_POINT_SIZE / 2];

            //Obtains the datapoint from the array.
            System.arraycopy(rhtRawData, i, dataPoint, 0, DATA_POINT_SIZE);

            //Separates the datapoint between humidity and pressure.
            ByteBuffer.wrap(dataPoint).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

            //Creates a datapoint object with the obtained data, and fills it with humidity and pressure.
            final float temperature = ((float) humidityAndTemperature[0]) / 100f;
            final float humidity = ((float) humidityAndTemperature[1]) / 100f;
            final int epoch = getUserData() + (mStartPointer + mExtractedDatapointsCounter) * mInterval;

            final RHTDataPoint extractedDataPoint = new RHTDataPoint(humidity, temperature, epoch, true);

            Log.i(TAG, String.format("parseLoggedData -> Logged in device %s values %s", mPeripheral.getAddress(), extractedDataPoint.toString()));

            //Adds the new datapoint to the the list.
            mNumConsecutiveFailsReadingData = 0;
            mExtractedDatapointsCounter++;
            lastTimeLogged = System.currentTimeMillis();

            onDatapointRead(extractedDataPoint);
        }
        return true;
    }

    private boolean isRawDatapointCorrupted(byte[] rhtRawData) {
        if (rhtRawData.length == 0 || rhtRawData.length % DATA_POINT_SIZE > 0) {
            // The data received it's not valid, it has to have values and
            // the whole download has to be multiple of the data point size.
            Log.e(TAG, String.format("isRawDatapointCorrupted -> The received data don't have a valid length: %d", rhtRawData.length));
            mNumConsecutiveFailsReadingData++;
            return true;
        }
        return false;
    }

    /**
     * Checks if the device has logged elements.
     *
     * @return <code>true</code> if the device has logged elements - <code>false</code> otherwise.
     */
    public boolean hasLoggedElements() {
        return mCurrentPointer > 0;
    }

    /**
     * Checks if the device has elements to log.
     *
     * @return <code>true</code> if the device has elements to log - <code>false</code> otherwise.
     */
    public boolean hasElementsToLog() {
        return getCurrentPoint() - mStartPointer > 0;
    }

    /**
     * Obtains the maximum elements to log.
     *
     * @return <code>int</code> with the total number of elements to log.
     */
    public int getNumberElementsToLog() {
        return getNumberElementsToLog(0);
    }

    /**
     * Returns the total of elements to log.
     *
     * @param userStartPoint given by the user
     * @return number of elements to log.
     */
    public int getNumberElementsToLog(final int userStartPoint) {
        mPeripheral.forceReadCharacteristic(mCurrentPointerCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, MAX_CONSECUTIVE_TRIES);

        if (getCurrentPoint() == 0) {
            return 0;
        }
        setStartPointer(userStartPoint);
        int totalValues = getCurrentPoint() - getStartPointer();
        if (totalValues > GADGET_RINGBUFFER_SIZE) {
            totalValues = GADGET_RINGBUFFER_SIZE;
        }
        Log.i(TAG, String.format("getNumberElementsToLog -> The device has %d values to log.", totalValues));
        return totalValues;
    }

    /**
     * Obtains the total of elements to log since the given timestamp.
     *
     * @param timestamp since when the logs wants to be started.
     * @return <code>int</code> with the number of elements to log after the given timestamp.
     */
    public int getNumberElementsToLogFromTimestamp(final Long timestamp) {

        if (timestamp == null) {
            return getNumberElementsToLog(0);
        }

        final int epochTime = (int) (timestamp / 1000);

        if (epochTime < getUserData()) {
            return getNumberElementsToLog(0);
        }

        final int endTimestampGadget = mUserData + getCurrentPoint() * getInterval();
        if (epochTime >= endTimestampGadget) {
            return 0;
        }
        return getNumberElementsToLog((endTimestampGadget - epochTime) / getInterval());
    }

    /**
     * Adds a new download listener to the list.
     *
     * @param newListener listener that wants to listen for notifications.
     */
    public void registerDownloadListener(final NotificationListener newListener) {
        if (newListener == null) {
            Log.w(TAG, String.format("registerDownloadListener -> Received a null listener in peripheral: %s", mPeripheral.getAddress()));
            return;
        }
        if (newListener instanceof LogDownloadListener) {
            mListeners.add((LogDownloadListener) newListener);
            Log.i(TAG, String.format("registerDownloadListener -> Peripheral %s received a new download listener: %s ", mPeripheral.getAddress(), newListener));
        } else {
            Log.i(TAG, String.format("registerDownloadListener -> The download listener received by the peripheral %s is not a %s", mPeripheral.getAddress(), LogDownloadListener.class.getSimpleName()));
        }
    }

    /**
     * Removes a listener from the download notification list.
     *
     * @param listenerForRemove listener that doesn't need the listen for notifications anymore.
     */
    public void removeDownloadListener(final NotificationListener listenerForRemove) {
        if (mListeners.contains(listenerForRemove)) {
            mListeners.remove(listenerForRemove);
            Log.i(TAG, String.format("removeDownloadListener -> Peripheral %s deleted %s listener from the list.", mPeripheral.getAddress(), listenerForRemove));
        } else {
            Log.w(TAG, String.format("removeDownloadListener -> Peripheral %s did not have the listener %s.", mPeripheral.getAddress(), listenerForRemove));
        }
    }

    /**
     * Notifies download progress to the listeners.
     */
    private void onDatapointRead(final RHTDataPoint dataPoint) {
        final Iterator<LogDownloadListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final LogDownloadListener listener = iterator.next();
                listener.setDownloadProgress(mPeripheral, mExtractedDatapointsCounter);
                listener.onNewDatapointDownloaded(mPeripheral, dataPoint);
            } catch (RuntimeException e) {
                Log.e(TAG, "onDatapointRead() -> Listener was removed from the list because the following exception was thrown -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies total elements to download to the listeners.
     *
     * @param numberElementsToDownload number of elements to download.
     */
    private void notifyTotalNumberElements(final int numberElementsToDownload) {
        final Iterator<LogDownloadListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().setRequestedDatapointAmount(mPeripheral, numberElementsToDownload);
            } catch (RuntimeException e) {
                Log.e(TAG, "notifyTotalNumberElements -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies download progress to the listeners.
     */
    private void onDownloadFailure() {
        final Iterator<LogDownloadListener> iterator = mListeners.iterator();
        Log.i(TAG, String.format("onLogDownloadFailure -> Notifying to the %d listeners. ", mListeners.toArray().length));
        while (iterator.hasNext()) {
            try {
                iterator.next().onLogDownloadFailure(mPeripheral);
            } catch (RuntimeException e) {
                Log.e(TAG, "onLogDownloadFailure -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies to the user that the application has finish downloading the logged data.
     */
    private void onDownloadComplete() {
        final Iterator<LogDownloadListener> iterator = mListeners.iterator();
        Log.i(TAG, String.format("onDownloadComplete -> Notifying to the %d listeners. ", mListeners.toArray().length));
        while (iterator.hasNext()) {
            try {
                iterator.next().onLogDownloadCompleted(mPeripheral);
            } catch (RuntimeException e) {
                Log.e(TAG, "onDownloadComplete -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Deletes the data from the device.
     * NOTE: This method shouldn't be called from the UI thread. The user has to call it from another thread (or creating one)
     */
    public boolean deleteDataFromTheDevice() {
        final boolean initialLoggingState = mLoggingIsEnabled;
        if (mLoggingIsEnabled) {
            setGadgetLoggingEnabled(false);
        }
        setGadgetLoggingEnabled(true);
        if (initialLoggingState) {
            Log.i(TAG, String.format("deleteDataFromTheDevice -> In device %s data was deleted.", getDeviceAddress()));
            return true;
        }
        return setGadgetLoggingEnabled(false);
    }
}