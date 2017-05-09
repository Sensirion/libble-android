package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SHT3xHistoryService extends SmartGadgetHistoryService {
    private static final String TAG = SHT3xHistoryService.class.getSimpleName();

    public static final String SERVICE_UUID = "0000f234-b38d-4985-720e-0f993a68ee41";

    private static final String SYNC_TIME_CHARACTERISTIC_UUID = "0000f235-b38d-4985-720e-0f993a68ee41";
    private static final String READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID = "0000f236-b38d-4985-720e-0f993a68ee41";
    private static final String NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID = "0000f237-b38d-4985-720e-0f993a68ee41";
    private static final String START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID = "0000f238-b38d-4985-720e-0f993a68ee41";
    private static final String LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID = "0000f239-b38d-4985-720e-0f993a68ee41";

    private static final byte DATA_POINT_SIZE = 4;

    private DownloadState mDownloadState;
    private boolean mSlavesSubscribed; // TODO Handle that Humi and Temperature must be subscribed
    private long mNewestSampleTimeMs;
    private long mOldestSampleTimeMs;

    /**
     * {@inheritDoc}
     */
    public SHT3xHistoryService(@NonNull final ServiceListener serviceListener,
                               @NonNull final BleConnector bleConnector,
                               @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, new String[]{
                SERVICE_UUID,
                SYNC_TIME_CHARACTERISTIC_UUID,
                READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID,
                NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID,
                START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID,
                LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID,
                SHT3xHumidityService.NOTIFICATIONS_UUID,
                SHT3xTemperatureService.NOTIFICATIONS_UUID
        });
        mDownloadState = DownloadState.IDLE;
        mLoggerStateEnabled = true;
    }

    /*
        Implementation of {@link GadgetDownloadService}
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGadgetLoggingStateEditable() {
        return false; // Device logging is always enabled.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setGadgetLoggingEnabled(final boolean enabled) {
        if (isGadgetLoggingEnabled() != enabled) {
            throw new IllegalStateException("Logging State is not editable"); // Device logging is always enabled.
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setLoggerInterval(final int loggerIntervalMs) {
        return super.writeValueToCharacteristic(LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID, loggerIntervalMs, BluetoothGattCharacteristic.FORMAT_SINT32, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDownloading() {
        return !mDownloadState.equals(DownloadState.IDLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLoggerInterval() {
        return mLoggerIntervalMs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        readLoggerInterval();
        readNewestSampleTime();
        readReadBackToTime();
    }

    /*
        Implementation of {@link BleConnectorCallback}
     */

    @Override
    public void onFail(final String characteristicUuid, final byte[] data,
                       final boolean isWriteFailure) {
        if (!isUuidSupported(characteristicUuid)) {
            return;
        }

        if (isDownloading()) {
            onDownloadFailed();
            return;
        }

        if (isWriteFailure) {
            switch (characteristicUuid) {
                case LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID:
                    mServiceListener.onSetLoggerIntervalFailed(this);
                    break;
            }
        }
        // ignore read failures if not currently downloading
    }

    /*
        Private helper methods
     */

    @Override
    protected void handleDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isDownloadedData(characteristicUuid, rawData)) {
            Log.d(TAG, "Received downloaded data in raw form");
            handleDownloadedData(characteristicUuid, rawData);
            return;
        }

        switch (characteristicUuid) {
            case LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID:
                mLoggerIntervalMs = LittleEndianExtractor.extractInteger(rawData);
                mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), mLoggerIntervalMs, LOGGER_INTERVAL_UNIT)};
                continueDownloadProtocol();
                mServiceListener.onSetLoggerIntervalSuccess();
                break;
            case NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID:
                mNewestSampleTimeMs = LittleEndianExtractor.extractLong(rawData);
                continueDownloadProtocol();
                break;
            case READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID:
                mOldestSampleTimeMs = LittleEndianExtractor.extractLong(rawData);
                continueDownloadProtocol();
                break;
        }
    }

    @Override
    protected void handleDataWritten(final String characteristicUuid) {
        switch (characteristicUuid) {
            case LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID:
                readLoggerInterval();
                break;
            case SYNC_TIME_CHARACTERISTIC_UUID:
                continueDownloadProtocol();
                break;
        }
    }

    @Override
    protected boolean initiateDownloadProtocol() {
        final BluetoothGattCharacteristic syncCharacteristic =
                mBleConnector.getCharacteristics(mDeviceAddress,
                        Collections.singletonList(SYNC_TIME_CHARACTERISTIC_UUID))
                        .get(SYNC_TIME_CHARACTERISTIC_UUID);
        if (syncCharacteristic == null) return false;

        mDownloadState = DownloadState.SYNC;
        final byte[] timestampLittleEndianBuffer = LittleEndianExtractor.convertToByteArray(System.currentTimeMillis());
        syncCharacteristic.setValue(timestampLittleEndianBuffer);
        mBleConnector.writeCharacteristic(mDeviceAddress, syncCharacteristic);
        mDownloadProgress = 0;
        return true;
    }

    private void continueDownloadProtocol() {
        switch (mDownloadState) {
            case SYNC:
                mDownloadState = DownloadState.INTERVAL;
                readLoggerInterval();
                break;
            case INTERVAL:
                mDownloadState = DownloadState.NEWEST;
                readNewestSampleTime();
                break;
            case NEWEST:
                mDownloadState = DownloadState.READ_BACK;
                readReadBackToTime();
                break;
            case READ_BACK:
                mDownloadState = DownloadState.RUNNING;
                mNrOfElementsDownloaded = 0;
                mNrOfElementsToDownload = (int) Math.floor((mNewestSampleTimeMs - mOldestSampleTimeMs) / mLoggerIntervalMs);

                if (mNrOfElementsToDownload == 0) {
                    onNoDataAvailable();
                    return;
                }

                final BluetoothGattCharacteristic startDownloadCharacteristic =
                        mBleConnector.getCharacteristics(mDeviceAddress,
                                Collections.singletonList(START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID))
                                .get(START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID);

                if (startDownloadCharacteristic != null) {
                    startDownloadCharacteristic.setValue(1, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                    mBleConnector.setCharacteristicNotification(mDeviceAddress, startDownloadCharacteristic, null, true);
                    mBleConnector.writeCharacteristic(mDeviceAddress, startDownloadCharacteristic);
                } else {
                    onDownloadFailed();
                }
                break;
            case IDLE:
            case RUNNING:
            default:
                // No download running
                break;
        }
    }

    private void readReadBackToTime() {
        mBleConnector.readCharacteristic(mDeviceAddress, READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID);
    }

    private void readNewestSampleTime() {
        mBleConnector.readCharacteristic(mDeviceAddress, NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID);
    }

    private void readLoggerInterval() {
        mBleConnector.readCharacteristic(mDeviceAddress, LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID);
    }

    private void handleDownloadedData(final String characteristicUuid, final byte[] rawData) {
        if (rawData.length < 4 * 2 || rawData.length % 4 > 0) {
            Log.e(TAG, "parseHistoryValue -> Received History value does not have a valid length.");
            return;
        }

        final int sequenceNr = updateDownloadProgress(rawData);
        final List<GadgetValue> downloadedValues = parseDownloadedDate(characteristicUuid, rawData, sequenceNr);

        mServiceListener.onGadgetDownloadDataReceived(this,
                downloadedValues.toArray(new GadgetValue[downloadedValues.size()]),
                mDownloadProgress);

        if (mNrOfElementsDownloaded >= mNrOfElementsToDownload) {
            onDownloadComplete();
        }
    }

    @NonNull
    private List<GadgetValue> parseDownloadedDate(String characteristicUuid, byte[] rawData, int sequenceNr) {
        final String unit = getUnitFromUuid(characteristicUuid, UNKNOWN_UNIT);

        // get data points from raw data
        final List<GadgetValue> downloadedValues = new ArrayList<>();
        for (int offset = DATA_POINT_SIZE; offset < rawData.length; offset += DATA_POINT_SIZE) {
            final long timestamp = mNewestSampleTimeMs - (mLoggerIntervalMs * ((((offset / DATA_POINT_SIZE) - 1) + sequenceNr)));
            final float downloadedValue = LittleEndianExtractor.extractFloat(rawData, offset);
            downloadedValues.add(new SmartGadgetValue(new Date(timestamp), downloadedValue, unit));
        }
        return downloadedValues;
    }

    private String getUnitFromUuid(final String characteristicUuid, final String defaultUnit) {
        if (characteristicUuid.equals(SHT3xHumidityService.NOTIFICATIONS_UUID)) {
            return SHT3xHumidityService.UNIT;
        } else if (characteristicUuid.equals(SHT3xTemperatureService.NOTIFICATIONS_UUID)) {
            return SHT3xTemperatureService.UNIT;
        }
        return defaultUnit;
    }

    private int updateDownloadProgress(byte[] rawData) {
        final int sequenceNr = extractSequenceNumber(rawData);
        final int numberParsedElements = (rawData.length / 4) - 1;
        mNrOfElementsDownloaded = sequenceNr + numberParsedElements + 4;

        mDownloadProgress = (int) Math.ceil(100 * (mNrOfElementsDownloaded / (float) mNrOfElementsToDownload));
        if (mNrOfElementsDownloaded >= mNrOfElementsToDownload) {
            mDownloadProgress = 100;
        }
        return sequenceNr;
    }

    private boolean isDownloadedData(final String characteristicUuid, final byte[] rawData) {
        return (characteristicUuid.equals(SHT3xHumidityService.NOTIFICATIONS_UUID) ||
                characteristicUuid.equals(SHT3xTemperatureService.NOTIFICATIONS_UUID)) &&
                rawData.length > (2 * DATA_POINT_SIZE);
    }

    private int extractSequenceNumber(@NonNull final byte[] byteBuffer) {
        final int[] wrappedSequenceNumber = new int[1];
        final byte[] sequenceNumberBuffer = new byte[4];
        System.arraycopy(byteBuffer, 0, sequenceNumberBuffer, 0, 4);
        ByteBuffer.wrap(sequenceNumberBuffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(wrappedSequenceNumber);
        return wrappedSequenceNumber[0];
    }

    private void onDownloadComplete() {
        mDownloadProgress = 100;
        mDownloadState = DownloadState.IDLE;
        mServiceListener.onDownloadCompleted(this);
    }

    private void onNoDataAvailable() {
        mDownloadProgress = 0;
        mDownloadState = DownloadState.IDLE;
        mServiceListener.onDownloadNoData(this);
    }

    private void onDownloadFailed() {
        mDownloadState = DownloadState.IDLE;
        mDownloadProgress = -1;
        mServiceListener.onDownloadFailed(this);
    }

    enum DownloadState {
        IDLE, SYNC, INTERVAL, NEWEST, READ_BACK, RUNNING
    }
}
