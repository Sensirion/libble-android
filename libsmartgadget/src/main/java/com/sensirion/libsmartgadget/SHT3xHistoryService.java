package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SHT3xHistoryService implements GadgetDownloadService, BleConnectorCallback {
    private static final String TAG = SHT3xHistoryService.class.getSimpleName();

    public static final String SERVICE_UUID = "0000f234-b38d-4985-720e-0f993a68ee41";

    private static final String SYNC_TIME_CHARACTERISTIC_UUID = "0000f235-b38d-4985-720e-0f993a68ee41";
    private static final String READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID = "0000f236-b38d-4985-720e-0f993a68ee41";
    private static final String NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID = "0000f237-b38d-4985-720e-0f993a68ee41";
    private static final String START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID = "0000f238-b38d-4985-720e-0f993a68ee41";
    private static final String LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID = "0000f239-b38d-4985-720e-0f993a68ee41";

    private static final String UNKNOWN_UNIT = "";
    private static final String LOGGER_INTERVAL_UNIT = "ms";
    private static final byte DATA_POINT_SIZE = 4;

    private final BleConnector mBleConnector;
    private final ServiceListener mServiceListener;
    private final String mDeviceAddress;

    private final Set<String> mSupportedUuids;

    private int mDownloadProgress;
    private DownloadState mDownloadState;

    private boolean mSlavesSubscribed; // TODO Handle that Humi and Temperature must be subscribed
    private int mLoggerIntervalMs;
    private long mNewestSampleTimeMs;
    private long mOldestSampleTimeMs;
    private int mNrOfElementsDownloaded;
    private float mNrOfElementsToDownload;
    private GadgetValue[] mLastValue;

    // TODO: Create abstract SmartGadgetHistoryService with shared functionality of SHTC1 and SHT3x
    public SHT3xHistoryService(@NonNull final ServiceListener serviceListener,
                               @NonNull final BleConnector bleConnector,
                               @NonNull final String deviceAddress) {
        mBleConnector = bleConnector;
        mServiceListener = serviceListener;
        mDeviceAddress = deviceAddress;

        mDownloadProgress = -1;
        mDownloadState = DownloadState.IDLE;
        mLastValue = new GadgetValue[]{new SmartGadgetValue(new Date(), -1, LOGGER_INTERVAL_UNIT)};

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(SERVICE_UUID);
        mSupportedUuids.add(SYNC_TIME_CHARACTERISTIC_UUID);
        mSupportedUuids.add(READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID);
        mSupportedUuids.add(NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID);
        mSupportedUuids.add(START_LOGGER_DOWNLOAD_CHARACTERISTIC_UUID);
        mSupportedUuids.add(LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID);
        mSupportedUuids.add(SHT3xHumidityService.NOTIFICATIONS_UUID);
        mSupportedUuids.add(SHT3xTemperatureService.NOTIFICATIONS_UUID);
    }

    /*
        Implementation of {@link GadgetDownloadService}
     */

    @Override
    public boolean download() {
        if (isDownloading()) {
            return false;
        }

        return initiateDownloadProtocol();
    }

    @Override
    public boolean isDownloading() {
        return !mDownloadState.equals(DownloadState.IDLE);
    }

    @Override
    public int getDownloadProgress() {
        return mDownloadProgress;
    }

    @Override
    public GadgetValue[] getLastValues() {
        return mLastValue;
    }

    @Override
    public boolean isGadgetLoggingStateEditable() {
        return false; // Device logging is always enabled.
    }

    @Override
    public boolean isGadgetLoggingEnabled() {
        return true; // Device logging is always enabled.
    }

    @Override
    public void setGadgetLoggingEnabled(final boolean enabled) {
        if (isGadgetLoggingEnabled() != enabled) {
            throw new IllegalStateException("Logging State is not editable"); // Device logging is always enabled.
        }
    }

    @Override
    public boolean setLoggerInterval(final int loggerIntervalMs) {
        final BluetoothGattCharacteristic characteristic =
                mBleConnector.getCharacteristics(mDeviceAddress,
                        Collections.singletonList(LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID))
                        .get(LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID);
        if (characteristic == null) return false;

        characteristic.setValue(LittleEndianExtractor.extractLittleEndianByteArrayFromInteger(loggerIntervalMs));
        mBleConnector.writeCharacteristic(mDeviceAddress, characteristic);
        return true;
    }

    @Override
    public int getLoggerInterval() {
        return mLoggerIntervalMs;
    }

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
    public void onConnectionStateChanged(final boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            if (isDownloadedData(characteristicUuid, rawData)) {
                Log.d(TAG, "Received downloaded data in raw form");
                handleDownloadedData(characteristicUuid, rawData);
                return;
            }

            switch (characteristicUuid) {
                case LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID:
                    mLoggerIntervalMs = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristicValue(rawData);
                    mLastValue[0] = new SmartGadgetValue(new Date(), mLoggerIntervalMs, LOGGER_INTERVAL_UNIT);
                    continueDownloadProtocol();
                    break;
                case NEWEST_SAMPLE_TIME_MS_CHARACTERISTIC_UUID:
                    mNewestSampleTimeMs = LittleEndianExtractor.extractLittleEndianLongFromCharacteristicValue(rawData);
                    continueDownloadProtocol();
                    break;
                case READ_BACK_TO_TIME_MS_CHARACTERISTIC_UUID:
                    mOldestSampleTimeMs = LittleEndianExtractor.extractLittleEndianLongFromCharacteristicValue(rawData);
                    continueDownloadProtocol();
                    break;
            }
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        switch (characteristicUuid) {
            case LOGGER_INTERVAL_MS_CHARACTERISTIC_UUID:
                readLoggerInterval();
                break;
            case SYNC_TIME_CHARACTERISTIC_UUID:
                continueDownloadProtocol();
                break;
        }
    }

    /*
        Private helper methods
     */

    private boolean initiateDownloadProtocol() {
        final BluetoothGattCharacteristic syncCharacteristic =
                mBleConnector.getCharacteristics(mDeviceAddress,
                        Collections.singletonList(SYNC_TIME_CHARACTERISTIC_UUID))
                        .get(SYNC_TIME_CHARACTERISTIC_UUID);
        if (syncCharacteristic == null) return false;

        mDownloadState = DownloadState.SYNC;
        final byte[] timestampLittleEndianBuffer = LittleEndianExtractor.extractLittleEndianByteArrayFromLong(System.currentTimeMillis());
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
                // TODO: Its strange to a have nr of elements to download as a float... think about
                // TODO      floor here and make int
                mNrOfElementsToDownload = (mNewestSampleTimeMs - mOldestSampleTimeMs) / mLoggerIntervalMs;

                if (mNrOfElementsToDownload == 0) {
                    onDownloadComplete();
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
                    // TODO: Report Error!
                    mDownloadState = DownloadState.IDLE;
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
    }

    @NonNull
    private List<GadgetValue> parseDownloadedDate(String characteristicUuid, byte[] rawData, int sequenceNr) {
        final String unit = getUnitFromUuid(characteristicUuid, UNKNOWN_UNIT);

        // get data points from raw data
        final List<GadgetValue> downloadedValues = new ArrayList<>();
        for (int offset = DATA_POINT_SIZE; offset < rawData.length; offset += DATA_POINT_SIZE) {
            final long timestamp = mNewestSampleTimeMs - (mLoggerIntervalMs * ((((offset / DATA_POINT_SIZE) - 1) + sequenceNr)));
            final float downloadedValue = LittleEndianExtractor.extractLittleEndianFloatFromCharacteristicValue(rawData, offset);
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

        mDownloadProgress = (int) Math.ceil(100 * (mNrOfElementsDownloaded / mNrOfElementsToDownload));
        if (mNrOfElementsDownloaded >= mNrOfElementsToDownload) {
            onDownloadComplete();
        }
        return sequenceNr;
    }

    private boolean isDownloadedData(final String characteristicUuid, final byte[] rawData) {
        return (characteristicUuid.equals(SHT3xHumidityService.NOTIFICATIONS_UUID) ||
                characteristicUuid.equals(SHT3xTemperatureService.NOTIFICATIONS_UUID)) &&
                rawData.length > (2 * DATA_POINT_SIZE);
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
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
    }

    enum DownloadState {
        IDLE, SYNC, INTERVAL, NEWEST, READ_BACK, RUNNING
    }
}
