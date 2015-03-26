package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractHistoryService;
import com.sensirion.libble.utils.LittleEndianExtractor;

import java.util.concurrent.Executors;

public class SmartgadgetHistoryService extends AbstractHistoryService {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000f234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTIC UUID
    private static final String SYNC_TIME_UUID = "0000f235-b38d-4985-720e-0f993a68ee41";
    private static final String READ_BACK_TO_TIME_MS_UUID = "0000f236-b38d-4985-720e-0f993a68ee41";
    private static final String NEWEST_SAMPLE_TIME_MS_UUID = "0000f237-b38d-4985-720e-0f993a68ee41";
    private static final String START_LOGGER_DOWNLOAD_UUID = "0000f238-b38d-4985-720e-0f993a68ee41";
    private static final String LOGGER_INTERVAL_MS_UUID = "0000f239-b38d-4985-720e-0f993a68ee41";

    //INTERVALS
    private static final short ONE_SECOND_MS = 1000;
    private static final int ONE_MINUTE_MS = 60 * ONE_SECOND_MS;
    private static final int DOWNLOAD_TIMEOUT = ONE_MINUTE_MS;

    //FORCE READ/WRITE ATTRIBUTES
    private static final short MAX_WAITING_TIME_BETWEEN_REQUEST_MS = 101;
    private static final byte NUMBER_FORCE_READING_REQUEST = 10;

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mSyncTimeCharacteristic;
    private final BluetoothGattCharacteristic mOldestSampleTimestampMsCharacteristic;
    private final BluetoothGattCharacteristic mNewestSampleTimestampMsCharacteristic;
    private final BluetoothGattCharacteristic mStartLoggerDownloadCharacteristic;
    private final BluetoothGattCharacteristic mLoggerIntervalMsCharacteristic;

    //DEVICE VALUES
    @Nullable
    private Long mNewestSampleTimestampMs = null;
    @Nullable
    private Long mOldestTimestampToDownloadMs = null;
    @Nullable
    private Integer mLoggerIntervalMs = null;
    @Nullable
    private Long mUserOldestTimestampToDownloadMs = null;

    //SERVICE STATE VALUES
    private volatile Long mTryingToDownload = null;
    private volatile int mLastSequenceNumberDownloaded = 0;
    private volatile Integer mUserInterval = null;

    public SmartgadgetHistoryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mSyncTimeCharacteristic = super.getCharacteristic(SYNC_TIME_UUID);
        mOldestSampleTimestampMsCharacteristic = super.getCharacteristic(READ_BACK_TO_TIME_MS_UUID);
        mNewestSampleTimestampMsCharacteristic = super.getCharacteristic(NEWEST_SAMPLE_TIME_MS_UUID);
        mStartLoggerDownloadCharacteristic = super.getCharacteristic(START_LOGGER_DOWNLOAD_UUID);
        mLoggerIntervalMsCharacteristic = super.getCharacteristic(LOGGER_INTERVAL_MS_UUID);
        parent.readCharacteristic(mLoggerIntervalMsCharacteristic);
        syncTimestamps();
    }

    private static long calcSecondsSince(final long timestamp) {
        return (System.currentTimeMillis() - timestamp) / 1000;
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was read.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        if (mNewestSampleTimestampMsCharacteristic.equals(updatedCharacteristic)) {
            return onNewestTimestampRead(updatedCharacteristic);
        } else if (mOldestSampleTimestampMsCharacteristic.equals(updatedCharacteristic)) {
            return onOldestTimestampRead(updatedCharacteristic);
        } else if (mLoggerIntervalMsCharacteristic.equals(updatedCharacteristic)) {
            mLoggerIntervalMs = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristic(updatedCharacteristic);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Logger interval in the device %s is %d milliseconds.", getDeviceAddress(), mLoggerIntervalMs));
            return true;
        } else if (mStartLoggerDownloadCharacteristic.equals(updatedCharacteristic)) {
            if (mStartLoggerDownloadCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 0) {
                mTryingToDownload = null;
                onDownloadComplete();
            }
        }
        return false;
    }

    /**
     * This method is called when a characteristic was written in the device.
     *
     * @param characteristic that was written in the device with success.
     * @return <code>true</code> if the service managed the given characteristic - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (characteristic.equals(mSyncTimeCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Time was synced successfully in the device %s.", getDeviceAddress()));
            mPeripheral.forceReadCharacteristic(mOldestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
            return true;
        } else if (characteristic.equals(mOldestSampleTimestampMsCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Read back to time characteristic was set correctly in device %s.", getDeviceAddress()));
            mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
            return true;
        } else if (characteristic.equals(mStartLoggerDownloadCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Download started successfully on device %s.", getDeviceAddress()));
            notifyNumberElementsToDownload();
            return true;
        } else if (characteristic.equals(mLoggerIntervalMsCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Download interval has been set correctly in device %s.", getDeviceAddress()));
            mLoggerIntervalMs = mUserInterval;
            mOldestTimestampToDownloadMs = null;
            mNewestSampleTimestampMs = null;
            syncTimestamps();
            return true;
        }
        return false;
    }

    /**
     * Registers the notification characteristics in case it's needed.
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        super.registerNotification(mStartLoggerDownloadCharacteristic);
    }

    @Override
    public boolean isServiceReady() {
        return mLoggerIntervalMs != null && mOldestTimestampToDownloadMs != null && mNewestSampleTimestampMs != null
                && mOldestTimestampToDownloadMs != 0 && mNewestSampleTimestampMs != 0;
    }

    @Override
    public void synchronizeService() {
        if (mLoggerIntervalMs == null || mOldestTimestampToDownloadMs == null || mNewestSampleTimestampMs == null) {
            forceCharacteristicRequest();
        }
        if (mOldestTimestampToDownloadMs == 0 || mNewestSampleTimestampMs == 0) {
            readTimestamps();
        }
    }

    private void sleep(final long millis) {
        while (true) {
            try {
                Thread.sleep(millis);
                mPeripheral.cleanCharacteristicCache();
                break;
            } catch (InterruptedException ignored) {
            }
        }
    }

    private boolean onNewestTimestampRead(@NonNull final BluetoothGattCharacteristic newestTimestampCharacteristic) {
        final long newestTimestampToDownload = LittleEndianExtractor.extractLittleEndianLongFromCharacteristic(newestTimestampCharacteristic);
        if (newestTimestampToDownload == 0) {
            syncTimestamps();
            Log.w(TAG, "onNewestTimestampRead -> Time is not synced yet.");
            return false;
        }
        mNewestSampleTimestampMs = newestTimestampToDownload;
        Log.d(TAG, String.format("onNewestTimestampRead -> Newest timestamp from the device %s is from %d seconds ago", getDeviceAddress(), calcSecondsSince(mNewestSampleTimestampMs)));
        if (isReadyToDownload()) {
            notifyNumberElementsToDownload();
            if (canStartDownload() && !isDownloadInProgress()) {
                enableHistoryDataNotifications();
            }
        }
        return true;
    }

    private boolean onOldestTimestampRead(@NonNull final BluetoothGattCharacteristic oldestTimestampCharacteristic) {
        final long oldestTimestampToDownload = LittleEndianExtractor.extractLittleEndianLongFromCharacteristic(oldestTimestampCharacteristic);
        if (oldestTimestampToDownload == 0) {
            syncTimestamps();
            Log.w(TAG, "onNewestTimestampRead -> Time is not synced yet.");
            return false;
        }
        mOldestTimestampToDownloadMs = oldestTimestampToDownload;
        Log.d(TAG, String.format("onOldestTimestampRead -> Oldest timestamp from the device %s is from %d seconds ago.", getDeviceAddress(), calcSecondsSince(mOldestTimestampToDownloadMs)));
        if (isReadyToDownload()) {
            notifyNumberElementsToDownload();
            if (canStartDownload() && !isDownloadInProgress()) {
                enableHistoryDataNotifications();
            }
        }
        return true;
    }

    private boolean canStartDownload() {
        if (mTryingToDownload == null) {
            Log.w(TAG, "canStartDownload -> User is not trying to download.");
            return false;
        }
        return mTryingToDownload + ONE_MINUTE_MS > System.currentTimeMillis();
    }

    private boolean isReadyToDownload() {
        if (mNewestSampleTimestampMs == null || mLoggerIntervalMs == null || mOldestTimestampToDownloadMs == null) {
            lazyCharacteristicRequest();
            return false;
        }
        return true;
    }

    private void enableHistoryDataNotifications() {
        if (isServiceReady()) {
            updateOldestTimestamp();
            mStartLoggerDownloadCharacteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            sleep(MAX_WAITING_TIME_BETWEEN_REQUEST_MS);
            mPeripheral.cleanCharacteristicCache();
            mPeripheral.forceWriteCharacteristic(mStartLoggerDownloadCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        }
    }

    private void updateOldestTimestamp() {
        if (mOldestTimestampToDownloadMs == null || mNewestSampleTimestampMs == null) {
            Log.e(TAG, "updateOldestTimestamp -> Service needs to be synchronized in order to set the oldest timestamp to download.");
        } else if (mUserOldestTimestampToDownloadMs == null) {
            Log.i(TAG, "updateOldestTimestamp -> User wants to download all the data from the device. No need to update the oldest timestamp.");
        } else if (mUserOldestTimestampToDownloadMs > mOldestTimestampToDownloadMs && mUserOldestTimestampToDownloadMs < mNewestSampleTimestampMs) {
            mPeripheral.forceWriteCharacteristic(mOldestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        }
    }

    private void notifyNumberElementsToDownload() {
        final Integer numberLoggedElements = getNumberLoggedElements();
        if (numberLoggedElements == null) {
            Log.e(TAG, String.format("notifyNumberElementsToDownload -> Device %s tried to notify a null total number of elements.", getDeviceAddress()));
        } else {
            super.notifyTotalNumberElements(numberLoggedElements);
        }
    }

    /**
     * Starts the data download from the device.
     *
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @Override
    public boolean startDataDownload() {
        if (isDownloadInProgress()) {
            Log.e(TAG, "startDataDownload -> The user can't download the data from the device because another download is in progress.");
            return false;
        }
        mTryingToDownload = System.currentTimeMillis();
        mLastSequenceNumberDownloaded = 0;
        enableHistoryDataNotifications();
        return true;
    }

    /**
     * Starts to download data from a given timestamp
     *
     * @param oldestTimestampToDownload the oldest timestamp that the device will download.
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @Override
    public boolean startDataDownload(final long oldestTimestampToDownload) {
        mUserOldestTimestampToDownloadMs = oldestTimestampToDownload;
        startDataDownload();
        return true;
    }

    /**
     * Change the download interval of a device.
     *
     * @param loggerIntervalInMilliseconds that the device will use for logging.
     * @return <code>true</code> if the download interval was set - <code>false</code> otherwise.
     */
    @Override
    public boolean setDownloadInterval(final int loggerIntervalInMilliseconds) {
        Log.d(TAG, String.format("setDownloadInterval -> Setting the download interval to %d.", loggerIntervalInMilliseconds));
        mLoggerIntervalMsCharacteristic.setValue(LittleEndianExtractor.extractLittleEndianByteArrayFromInteger(loggerIntervalInMilliseconds));
        mUserInterval = loggerIntervalInMilliseconds;
        return mPeripheral.forceWriteCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
    }

    /**
     * Gets the interval between history data points on the device in milliseconds.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return {@link java.lang.Integer} with the logger interval in milliseconds - <code>null</code> if it's not known.
     */
    @Override
    @Nullable
    public Integer getLoggingIntervalMs() {
        if (mLoggerIntervalMs == null) {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    mPeripheral.forceReadCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
                }
            });
        }
        return mLoggerIntervalMs;
    }

    /**
     * Deletes all the data from the device.
     *
     * @return <code>true</code> if the data was deleted - <code>false</code> otherwise.
     */
    @Override
    public boolean resetDeviceData() {
        final Integer downloadIntervalMs = getLoggingIntervalMs();
        if (downloadIntervalMs == null) {
            Log.e(TAG, "resetDeviceData -> Device needs to be synchronized in order to reset it.");
            return false;
        }
        return setDownloadInterval(downloadIntervalMs);
    }

    /**
     * Checks if device is logging historical values.
     *
     * @return <code>true</code> if logging is enabled - <code>false</code> otherwise.
     */
    @Override
    public boolean isGadgetLoggingEnabled() {
        return true; //Device logging is always enabled.
    }

    /**
     * Checks is the user can modify the logging state.
     *
     * @return <code>true</code> if the user can enable or disable logging - <code>false</code> otherwise.
     */
    @Override
    public boolean isLoggingStateEditable() {
        return false; //Device logging is always enabled.
    }

    /**
     * User can set the logging state in case it's editable.
     *
     * @param enabled <code>true</code> if the user wants to enable logging - <code>false</code> otherwise.
     * @return <code>true</code> if the state was changed - <code>false</code> otherwise.
     */
    @Override
    public boolean setLoggingState(final boolean enabled) {
        Log.e(TAG, String.format("setLoggingState -> In the device %s logging can never ve enabled or disabled.", getDeviceAddress()));
        return false;
    }

    /**
     * Checks the number of logged elements that the user can download.
     * <p/>
     * In case the value is unknown it will ask for it in a background thread.
     * If the user calls this method again after some time it can return a different value.
     *
     * @return <code>true</code> if the device has data - <code>false</code> otherwise.
     */
    @Override
    @Nullable
    public Integer getNumberLoggedElements() {
        if (mNewestSampleTimestampMs != null && mOldestTimestampToDownloadMs != null && mLoggerIntervalMs != null) {
            return (int) (mNewestSampleTimestampMs - mOldestTimestampToDownloadMs) / mLoggerIntervalMs;
        }
        lazyCharacteristicRequest();
        return null;
    }

    private void lazyCharacteristicRequest() {
        mPeripheral.cleanCharacteristicCache();
        if (mNewestSampleTimestampMs == null) {
            mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
        }
        if (mOldestTimestampToDownloadMs == null) {
            mPeripheral.readCharacteristic(mOldestSampleTimestampMsCharacteristic);
        }
        if (mLoggerIntervalMs == null) {
            mPeripheral.readCharacteristic(mLoggerIntervalMsCharacteristic);
        }
    }

    private void forceCharacteristicRequest() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mNewestSampleTimestampMs == null) {
                    mPeripheral.forceReadCharacteristic(mNewestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
                }
                mPeripheral.cleanCharacteristicCache();
                if (mOldestTimestampToDownloadMs == null) {
                    mPeripheral.forceReadCharacteristic(mOldestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
                }
                mPeripheral.cleanCharacteristicCache();
                if (mLoggerIntervalMs == null) {
                    mPeripheral.forceReadCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
                }
                mPeripheral.cleanCharacteristicCache();
            }
        });
    }

    private void readTimestamps() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                syncTimestamps();
            }
        }).start();
    }

    private void syncTimestamps() {
        final byte[] timestampLittleEndianBuffer = LittleEndianExtractor.extractLittleEndianByteArrayFromLong(System.currentTimeMillis());
        mSyncTimeCharacteristic.setValue(timestampLittleEndianBuffer);
        mPeripheral.forceWriteCharacteristic(mSyncTimeCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        mOldestTimestampToDownloadMs = null;
        mNewestSampleTimestampMs = null;
        mPeripheral.readCharacteristic(mOldestSampleTimestampMsCharacteristic);
        mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
    }

    /**
     * Checks if a logging download is in progress.
     *
     * @return <code>true</code> if a download is in progress - <code>false</code> otherwise.
     */
    @Override
    public boolean isDownloadInProgress() {
        if (mTryingToDownload == null || mLastSequenceNumberDownloaded == 0) {
            return false;
        }
        Log.d(TAG, "isDownloadInProgress -> User is downloading data from the device.");
        return mTryingToDownload + DOWNLOAD_TIMEOUT > System.currentTimeMillis();
    }

    /**
     * Informs the history service of the last sequence number downloaded.
     *
     * @param sequenceNumber that has been downloaded by the user.
     */
    void setLastSequenceNumberDownloaded(final int sequenceNumber) {
        if (sequenceNumber > mLastSequenceNumberDownloaded) {
            mLastSequenceNumberDownloaded = sequenceNumber;
            notifyDownloadProgress(sequenceNumber);
        }
    }

    /**
     * Gets the newest timestamp of the data that is going to be downloaded.
     *
     * @return {@link java.lang.Long} with the newest sample timestamp - <code>null</code> if it's unknown.
     */
    @Nullable
    public Long getNewestTimestampMs() {
        return mNewestSampleTimestampMs;
    }
}