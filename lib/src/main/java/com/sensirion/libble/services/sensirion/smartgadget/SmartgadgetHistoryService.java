package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractHistoryService;
import com.sensirion.libble.utils.LittleEndianExtractor;
import com.sensirion.libble.utils.MockFutureTask;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import static com.sensirion.libble.services.sensirion.smartgadget.AbstractSmartgadgetService.DATAPOINT_SIZE;

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
    private static final short TEN_SECONDS_MS = 10 * ONE_SECOND_MS;
    private static final short DOWNLOAD_START_TIMEOUT_MS = TEN_SECONDS_MS;

    //FORCE READ/WRITE ATTRIBUTES
    private static final short MAX_WAITING_TIME_BETWEEN_REQUEST_MS = 101; //Two connection intervals.
    private static final byte NUMBER_FORCE_READING_REQUEST = 25;

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
    private boolean mAreDeviceTimestampsSynchronized = false;
    @Nullable
    private Long mLastDownloadUpdate = null;
    private int mLastSequenceNumberDownloaded = 0;
    @Nullable
    private Integer mUserInterval = null;

    public SmartgadgetHistoryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) throws InstantiationException {
        super(parent, bluetoothGattService);
        mSyncTimeCharacteristic = super.getCharacteristic(SYNC_TIME_UUID);
        mOldestSampleTimestampMsCharacteristic = super.getCharacteristic(READ_BACK_TO_TIME_MS_UUID);
        mNewestSampleTimestampMsCharacteristic = super.getCharacteristic(NEWEST_SAMPLE_TIME_MS_UUID);
        mStartLoggerDownloadCharacteristic = super.getCharacteristic(START_LOGGER_DOWNLOAD_UUID);
        mLoggerIntervalMsCharacteristic = super.getCharacteristic(LOGGER_INTERVAL_MS_UUID);
        if (mLoggerIntervalMsCharacteristic == null) {
            throw new InstantiationException(String.format("%s: %s -> Can not found the logger interval characteristic.", TAG, TAG));
        }
        parent.readCharacteristic(mLoggerIntervalMsCharacteristic);
        syncTimestamps();
    }

    private static long calcSecondsSince(final long timestamp) {
        return (System.currentTimeMillis() - timestamp) / ONE_SECOND_MS;
    }

    /**
     * {@inheritDoc}
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
                mLastDownloadUpdate = null;
                onDownloadComplete();
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (characteristic.equals(mSyncTimeCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicWrite -> Time was synced successfully in the device %s.", getDeviceAddress()));
            mAreDeviceTimestampsSynchronized = true;
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
            mAreDeviceTimestampsSynchronized = false;
            syncTimestamps();
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        super.registerNotification(mStartLoggerDownloadCharacteristic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isServiceReady() {
        return mLoggerIntervalMs != null && mOldestTimestampToDownloadMs != null && mNewestSampleTimestampMs != null
                && mOldestTimestampToDownloadMs != 0 && mNewestSampleTimestampMs != 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronizeService() {
        Log.d(TAG, "synchronizeService -> Synchronizing service...");
        if (mAreDeviceTimestampsSynchronized) {
            if (mLoggerIntervalMs == null || mOldestTimestampToDownloadMs == null || mNewestSampleTimestampMs == null) {
                forceCharacteristicRequest();
            } else if (mOldestTimestampToDownloadMs == 0 || mNewestSampleTimestampMs == 0) {
                readTimestamps();
            }
        } else {
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
        if (mLastDownloadUpdate == null) {
            Log.w(TAG, "canStartDownload -> User is not trying to download.");
            return false;
        }
        return mLastDownloadUpdate + DOWNLOAD_START_TIMEOUT_MS < System.currentTimeMillis();
    }

    private boolean isReadyToDownload() {
        if (mNewestSampleTimestampMs == null || mLoggerIntervalMs == null || mOldestTimestampToDownloadMs == null) {
            lazyCharacteristicRequest();
            return false;
        }
        return true;
    }

    private synchronized void enableHistoryDataNotifications() {
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
        final Integer numberLoggedElements;
        try {
            numberLoggedElements = getNumberLoggedElements().get();
        } catch (final InterruptedException | ExecutionException e) {
            Log.e(TAG, "notifyNumberElementsToDownload -> The following exception was thrown while obtaining the number of elements -> ", e);
            return;
        }
        if (numberLoggedElements == null) {
            Log.e(TAG, String.format("notifyNumberElementsToDownload -> Device %s tried to notify a null total number of elements.", getDeviceAddress()));
        } else {
            super.notifyTotalNumberElements(numberLoggedElements);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Boolean> startDataDownload() {
        if (isDownloadInProgress()) {
            Log.e(TAG, "startDataDownload -> The user can't download the data from the device because another download is in progress.");
            return new MockFutureTask<>(false);
        }
        mLastDownloadUpdate = System.currentTimeMillis();
        mLastSequenceNumberDownloaded = 0;
        enableHistoryDataNotifications();
        return new MockFutureTask<>(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Boolean> startDataDownload(final long oldestTimestampToDownload) {
        mUserOldestTimestampToDownloadMs = oldestTimestampToDownload;
        startDataDownload();
        return new MockFutureTask<>(true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Boolean> setDownloadInterval(final int loggerIntervalInMilliseconds) {
        Log.d(TAG, String.format("setDownloadInterval -> Setting the download interval to %d.", loggerIntervalInMilliseconds));
        mLoggerIntervalMsCharacteristic.setValue(LittleEndianExtractor.extractLittleEndianByteArrayFromInteger(loggerIntervalInMilliseconds));
        mUserInterval = loggerIntervalInMilliseconds;
        return mPeripheral.forceWriteCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Integer> getLoggingIntervalMs() {
        return new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (mLoggerIntervalMs == null) {
                    mPeripheral.forceReadCharacteristic(mLoggerIntervalMsCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST).get();
                }
                return mLoggerIntervalMs;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Boolean> resetDeviceData() {
        return new FutureTask<>(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final Integer downloadIntervalMs = getLoggingIntervalMs().get();
                if (downloadIntervalMs == null) {
                    Log.e(TAG, "resetDeviceData -> Device needs to be synchronized in order to reset it.");
                    return false;
                }
                return setDownloadInterval(downloadIntervalMs).get();
            }
        });

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGadgetLoggingEnabled() {
        return true; //Device logging is always enabled.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLoggingStateEditable() {
        return false; //Device logging is always enabled.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Boolean> setLoggingState(final boolean enabled) {
        Log.e(TAG, String.format("setLoggingState -> In the device %s logging can never be enabled or disabled.", getDeviceAddress()));
        return new MockFutureTask<>(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public FutureTask<Integer> getNumberLoggedElements() {
        return new FutureTask<>(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                if (mNewestSampleTimestampMs == null || mOldestTimestampToDownloadMs == null || mLoggerIntervalMs == null) {
                    forceCharacteristicRequest();
                    return null;
                }
                final int numberLoggedElements = (int) (mNewestSampleTimestampMs - mOldestTimestampToDownloadMs) / mLoggerIntervalMs;
                Log.i(TAG, String.format("getNumberLoggedElements -> Device %s has %d logged elements", getDeviceAddress(), numberLoggedElements));
                return numberLoggedElements;
            }
        });
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
                mPeripheral.cleanCharacteristicCache();
                if (mLoggerIntervalMs == null) {
                    Log.d(TAG, "forceCharacteristicRequest -> Log interval is not known yet.");
                    mPeripheral.forceReadCharacteristic(mLoggerIntervalMsCharacteristic);
                } else if (mNewestSampleTimestampMs == null) {
                    Log.d(TAG, "forceCharacteristicRequest -> Newest sample timestamp is not known yet.");
                    mPeripheral.forceReadCharacteristic(mNewestSampleTimestampMsCharacteristic);
                } else if (mOldestTimestampToDownloadMs == null) {
                    Log.d(TAG, "forceCharacteristicRequest -> Oldest sample timestamp is not known yet.");
                    mPeripheral.forceReadCharacteristic(mOldestSampleTimestampMsCharacteristic);
                }

            }
        });
    }

    private void readTimestamps() {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                syncTimestamps();
            }
        });
    }

    private void syncTimestamps() {
        Log.d(TAG, String.format("syncTimestamps() -> Syncing timestamps of device: %s", getDeviceAddress()));
        final byte[] timestampLittleEndianBuffer = LittleEndianExtractor.extractLittleEndianByteArrayFromLong(System.currentTimeMillis());
        mSyncTimeCharacteristic.setValue(timestampLittleEndianBuffer);
        mPeripheral.forceWriteCharacteristic(mSyncTimeCharacteristic, MAX_WAITING_TIME_BETWEEN_REQUEST_MS, NUMBER_FORCE_READING_REQUEST);
        mOldestTimestampToDownloadMs = null;
        mNewestSampleTimestampMs = null;
        mPeripheral.readCharacteristic(mOldestSampleTimestampMsCharacteristic);
        mPeripheral.readCharacteristic(mNewestSampleTimestampMsCharacteristic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDownloadInProgress() {
        if (mLastDownloadUpdate == null || mLastSequenceNumberDownloaded == 0) {
            return false;
        }
        if (mLastDownloadUpdate + DOWNLOAD_START_TIMEOUT_MS > System.currentTimeMillis()) {
            Log.d(TAG, "isDownloadInProgress -> User is downloading data from the device.");
            return true;
        }
        return false;
    }

    /**
     * Informs the history service of the last sequence number downloaded.
     *
     * @param sequenceNumber that has been downloaded by the user.
     */
    void setLastSequenceNumberDownloaded(final int sequenceNumber) {
        Log.d(TAG, String.format("setLastSequenceNumberDownloaded -> Received sequence number %d in device %s.", sequenceNumber, getDeviceAddress()));
        if (sequenceNumber > mLastSequenceNumberDownloaded) {
            mLastSequenceNumberDownloaded = sequenceNumber;
            final int downloadedElements = sequenceNumber + DATAPOINT_SIZE;
            notifyDownloadProgress(downloadedElements);
            final Integer numberDownloadElements;
            try {
                numberDownloadElements = getNumberLoggedElements().get();
            } catch (final ExecutionException | InterruptedException e) {
                Log.e(TAG, "setLastSequenceNumberDownloaded -> The following exception was thrown -> ", e);
                return;
            }
            if (numberDownloadElements != null && downloadedElements >= numberDownloadElements) {
                onDownloadComplete();
                mLastDownloadUpdate = null;
            } else {
                mLastDownloadUpdate = System.currentTimeMillis();
            }
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