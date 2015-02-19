package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.HistoryService;
import com.sensirion.libble.utils.LittleEndianExtractor;

public class SmartgadgetHistoryService extends HistoryService {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000f234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTIC UUID
    private static final String SYNC_TIME_UUID = "0000f235-b38d-4985-720e-0f993a68ee41";
    private static final String READ_BACK_TO_TIME_MS_UUID = "0000f236-b38d-4985-720e-0f993a68ee41";
    private static final String NEWEST_SAMPLE_TIME_MS_UUID = "0000f237-b38d-4985-720e-0f993a68ee41";
    private static final String START_LOGGER_DOWNLOAD_UUID = "0000f238-b38d-4985-720e-0f993a68ee41";
    private static final String LOGGER_INTERVAL_MS_UUID = "0000f239-b38d-4985-720e-0f993a68ee41";

    //FORCE READ/WRITE ATTRIBUTES
    private static final short MAX_WAITING_TIME_BETWEEN_REQUEST_MS = 400;
    private static final byte NUMBER_FORCE_READING_REQUEST = 3;

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mSyncTimeCharacteristic;
    private final BluetoothGattCharacteristic mOldestTimestampCharacteristic;
    private final BluetoothGattCharacteristic mNewestSampleTimestampMsCharacteristic;
    private final BluetoothGattCharacteristic mStartLoggerDownloadCharacteristic;
    private final BluetoothGattCharacteristic mLoggerIntervalMsCharacteristic;

    //CLASS VALUES
    private Long mNewestSampleTimestampMs = null;
    private Long mOldestTimestampToDownloadMs = null;
    private Integer mLoggerIntervalMs = null;
    private Long mUserOldestTimestampToDownloadMs = null;
    private volatile boolean mTryingToDownload = false;
    private volatile int mLastSequenceNumberDownloaded = 0;

    public SmartgadgetHistoryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mSyncTimeCharacteristic = super.getCharacteristic(SYNC_TIME_UUID);
        mOldestTimestampCharacteristic = super.getCharacteristic(READ_BACK_TO_TIME_MS_UUID);
        mNewestSampleTimestampMsCharacteristic = super.getCharacteristic(NEWEST_SAMPLE_TIME_MS_UUID);
        mStartLoggerDownloadCharacteristic = super.getCharacteristic(START_LOGGER_DOWNLOAD_UUID);
        mLoggerIntervalMsCharacteristic = super.getCharacteristic(LOGGER_INTERVAL_MS_UUID);
        parent.readCharacteristic(mLoggerIntervalMsCharacteristic);
        syncTimestamp();
    }

    @Override
    public void registerDeviceCharacteristicNotifications() {
        super.registerNotification(mStartLoggerDownloadCharacteristic);
    }

    /**
     * Checks if the history service has the updated information from the BleDevice.
     *
     * @return <code>true</code> if the service is synchronized. <code>false</code> otherwise.
     */
    @Override
    public boolean isServiceSynchronized() {
        if (mOldestTimestampToDownloadMs == null || mNewestSampleTimestampMs == null) {
            syncTimestamp();
        }
        if (mOldestTimestampToDownloadMs == null) {
            mPeripheral.forceReadCharacteristic(mOldestTimestampCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
            if (mOldestTimestampToDownloadMs == null) {
                return false;
            }
        }
        Log.d(TAG, String.format("isServiceSynchronized -> Oldest timestamp is %d.", mOldestTimestampToDownloadMs));
        if (mNewestSampleTimestampMs == null) {
            mPeripheral.forceReadCharacteristic(mNewestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
            if (mNewestSampleTimestampMs == null) {
                return false;
            }
        }
        Log.d(TAG, String.format("isServiceSynchronized -> Newest timestamp is %d.", mNewestSampleTimestampMs));
        if (mLoggerIntervalMs == null) {
            mPeripheral.forceReadCharacteristic(mNewestSampleTimestampMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
            if (mNewestSampleTimestampMs == null) {
                return false;
            }
        }
        Log.d(TAG, String.format("isServiceSynchronized -> Logger interval is %d.", mLoggerIntervalMs));
        return true;
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

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was read.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        if (mNewestSampleTimestampMsCharacteristic.equals(updatedCharacteristic)) {
            return onNewestSampleTimestampTimeMsRead(updatedCharacteristic);
        } else if (mOldestTimestampCharacteristic.equals(updatedCharacteristic)) {
            return onOldestTimestampRead(updatedCharacteristic);
        } else if (mLoggerIntervalMsCharacteristic.equals(updatedCharacteristic)) {
            mLoggerIntervalMs = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristic(updatedCharacteristic);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Logger interval in the device %s is %d milliseconds.", getDeviceAddress(), mLoggerIntervalMs));
            return true;
        } else if (mStartLoggerDownloadCharacteristic.equals(updatedCharacteristic)) {
            if (mStartLoggerDownloadCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 0) {
                mTryingToDownload = false;
                super.onDownloadComplete();
            }
        }
        return false;
    }

    private boolean onNewestSampleTimestampTimeMsRead(@NonNull final BluetoothGattCharacteristic newestTimestampCharacteristic) {
        final long newestTimestampToDownload = LittleEndianExtractor.extractLittleEndianLongFromCharacteristic(newestTimestampCharacteristic);
        if (newestTimestampToDownload == 0) {
            syncTimestamp();
            Log.w(TAG, "onNewestSampleTimestampTimeMsRead -> Time is not synced yet.");
            return false;
        }
        mNewestSampleTimestampMs = newestTimestampToDownload;
        Log.d(TAG, String.format("onNewestSampleTimestampTimeMsRead -> Newest timestamp from the device %s is %d. (%d seconds ago)", getDeviceAddress(), mNewestSampleTimestampMs, ((System.currentTimeMillis() - mNewestSampleTimestampMs) / 1000)));
        if (mOldestTimestampToDownloadMs == null) {
            mPeripheral.readCharacteristic(mOldestTimestampCharacteristic);
        } else if (mTryingToDownload) {
            enableHistoryDataNotifications();
        }
        return true;
    }

    private boolean onOldestTimestampRead(@NonNull final BluetoothGattCharacteristic oldestTimestampCharacteristic) {
        final long oldestTimestampToDownload = LittleEndianExtractor.extractLittleEndianLongFromCharacteristic(oldestTimestampCharacteristic);
        if (oldestTimestampToDownload == 0) {
            syncTimestamp();
            Log.w(TAG, "onNewestSampleTimestampTimeMsRead -> Time is not synced yet.");
            return false;
        }
        mOldestTimestampToDownloadMs = oldestTimestampToDownload;
        Log.d(TAG, String.format("onOldestTimestampRead -> Oldest timestamp from the device %s is %d. (%d seconds ago).", getDeviceAddress(), mOldestTimestampToDownloadMs, ((System.currentTimeMillis() - mOldestTimestampToDownloadMs) / 1000)));
        if (mNewestSampleTimestampMs == null) {
            mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
        } else if (mTryingToDownload) {
            enableHistoryDataNotifications();
        }
        return true;
    }

    private void enableHistoryDataNotifications() {
        if (mUserOldestTimestampToDownloadMs != null && mUserOldestTimestampToDownloadMs > mOldestTimestampToDownloadMs && mUserOldestTimestampToDownloadMs < mNewestSampleTimestampMs) {
            mPeripheral.forceWriteCharacteristic(mOldestTimestampCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        }
        mStartLoggerDownloadCharacteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        sleep(MAX_WAITING_TIME_BETWEEN_REQUEST_MS);
        mPeripheral.cleanCharacteristicCache();
        mPeripheral.forceWriteCharacteristic(mStartLoggerDownloadCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
    }

    private void notifyNumberElementsToDownload() {
        super.notifyTotalNumberElements(getNumberLoggedElements());
    }

    /**
     * This method is called when a characteristic was written in the device.
     *
     * @param characteristic that was written in the device with success.
     *                       return <code>true</code> if the service managed the given characteristic - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (characteristic.equals(mSyncTimeCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Time was synced successfully in the device %s.", getDeviceAddress()));
            mPeripheral.forceReadCharacteristic(mOldestTimestampCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
            return true;
        } else if (characteristic.equals(mOldestTimestampCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Read back to time characteristic was set correctly in device %s.", getDeviceAddress()));
            mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
            return true;
        } else if (characteristic.equals(mStartLoggerDownloadCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Download started successfully on device %s.", getDeviceAddress()));
            notifyNumberElementsToDownload();
            return true;
        } else if (characteristic.equals(mLoggerIntervalMsCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Download interval has been set correctly in device %s.", getDeviceAddress()));
            syncTimestamp();
            return true;
        }
        return false;
    }

    /**
     * Starts the data download from the device.
     *
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @Override
    public boolean startDataDownload() {
        if (mLastSequenceNumberDownloaded > 0 && mTryingToDownload) {
            Log.e(TAG, "startDataDownload -> The user can't download the data from the device because another download is in progress.");
            return false;
        }
        mTryingToDownload = true;
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
        mLoggerIntervalMsCharacteristic.setValue(loggerIntervalInMilliseconds, BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        mLoggerIntervalMs = null;
        mOldestTimestampToDownloadMs = null;
        mNewestSampleTimestampMs = null;
        return mPeripheral.forceWriteCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
    }

    /**
     * Gets the interval of the device in milliseconds.
     *
     * @return {@link java.lang.Integer} with the logger interval in milliseconds - <code>null</code> if it's not known
     */
    @Override
    public Integer getDownloadIntervalMs() {
        if (mLoggerIntervalMs == null) {
            mPeripheral.forceReadCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
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
        return setDownloadInterval(getDownloadIntervalMs());
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
     * Checks the number of elements a device have to download.
     *
     * @return <code>true</code> if the device has data - <code>false</code> otherwise.
     */
    @Override
    public Integer getNumberLoggedElements() {
        if (isServiceSynchronized()) {
            return (int) (mNewestSampleTimestampMs - mOldestTimestampToDownloadMs) / mLoggerIntervalMs;
        }
        return null;
    }


    private void syncTimestamp() {
        final byte[] timestampLittleEndianBuffer = LittleEndianExtractor.extractLittleEndianByteArrayFromLong(System.currentTimeMillis());
        mSyncTimeCharacteristic.setValue(timestampLittleEndianBuffer);
        mPeripheral.forceWriteCharacteristic(mSyncTimeCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        mOldestTimestampToDownloadMs = null;
        mNewestSampleTimestampMs = null;
    }

    /**
     * Checks if a logging download is in progress.
     *
     * @return <code>true</code> if a download is in progress - <code>false</code> otherwise.
     */
    @Override
    public boolean isDownloadInProgress() {
        return mTryingToDownload;
    }

    /**
     * @param sequenceNumber that has been downloaded by the user.
     *                       NOTE: This method should only be called by other smartgadget services.
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
    public Long getNewestTimestampMs() {
        return mNewestSampleTimestampMs;
    }
}