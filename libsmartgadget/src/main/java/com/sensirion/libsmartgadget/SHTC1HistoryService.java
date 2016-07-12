package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
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

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

// TODO implement error handling and receiving failed actions in each service and taking appropriate
// TODO     actions.

// TODO important to handle error happening here, specially ACTION_FAILED events which occurred
// TODO     quite often!
public class SHTC1HistoryService implements GadgetDownloadService, BleConnectorCallback {
    public static final String SERVICE_UUID = "0000fa20-0000-1000-8000-00805f9b34fb";
    private static final String TAG = SHTC1HistoryService.class.getSimpleName();
    private static final String START_STOP_CHARACTERISTIC_UUID = "0000fa21-0000-1000-8000-00805f9b34fb";
    private static final String LOGGING_INTERVAL_S_CHARACTERISTIC_UUID = "0000fa22-0000-1000-8000-00805f9b34fb";
    private static final String CURRENT_POINTER_CHARACTERISTIC_UUID = "0000fa23-0000-1000-8000-00805f9b34fb";
    private static final String START_POINTER_CHARACTERISTIC_UUID = "0000fa24-0000-1000-8000-00805f9b34fb";
    private static final String END_POINTER_CHARACTERISTIC_UUID = "0000fa25-0000-1000-8000-00805f9b34fb";
    private static final String LOGGED_DATA_CHARACTERISTIC_UUID = "0000fa26-0000-1000-8000-00805f9b34fb";
    private static final String USER_DATA_CHARACTERISTIC_UUID = "0000fa27-0000-1000-8000-00805f9b34fb";

    private static final String LOGGER_INTERVAL_UNIT = "ms";
    private static final int GADGET_RING_BUFFER_SIZE = 16384;
    private static final int DATA_POINT_SIZE = 4;
    private static final long SHTC1_SPECIFIC_READ_AFTER_WRITE_DELAY_MS = 1000;

    private final BleConnector mBleConnector;
    private final ServiceListener mServiceListener;
    private final String mDeviceAddress;

    private final Set<String> mSupportedUuids;

    private DownloadState mDownloadState;
    private Date mDownloadStartTimestamp;
    private boolean mLoggerStateBeforeDownload;
    private int mNrOfElementsToDownload;
    private int mNrOfElementsDownloaded;
    private int mDownloadProgress;

    private boolean mLoggerStateEnabled;
    private int mLoggerIntervalMs;
    private int mCurrentPointer;
    private int mStartPointer;
    private int mEndPointer;
    private int mLoggingEnabledTimestamp;

    private GadgetValue[] mLastValue;

    public SHTC1HistoryService(@NonNull final ServiceListener serviceListener,
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
        mSupportedUuids.add(START_STOP_CHARACTERISTIC_UUID);
        mSupportedUuids.add(LOGGING_INTERVAL_S_CHARACTERISTIC_UUID);
        mSupportedUuids.add(CURRENT_POINTER_CHARACTERISTIC_UUID);
        mSupportedUuids.add(START_POINTER_CHARACTERISTIC_UUID);
        mSupportedUuids.add(END_POINTER_CHARACTERISTIC_UUID);
        mSupportedUuids.add(LOGGED_DATA_CHARACTERISTIC_UUID);
        mSupportedUuids.add(USER_DATA_CHARACTERISTIC_UUID);
    }

    /*
        Implementation of {@link GadgetDownloadService}
     */

    @Override
    public boolean download() {
        if (isDownloading()) return false;
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
        return true;
    }

    @Override
    public boolean isGadgetLoggingEnabled() {
        return mLoggerStateEnabled;
    }

    @Override
    public void setGadgetLoggingEnabled(final boolean enabled) {
        if (enabled) {
            writeLoggingStartTimestamp();
        }
        writeValueToCharacteristic(START_STOP_CHARACTERISTIC_UUID, (byte) ((enabled) ? 1 : 0), FORMAT_UINT8, 0);
    }

    @Override
    public boolean setLoggerInterval(final int loggerIntervalMs) {
        final int loggerIntervalS = loggerIntervalMs / 1000;
        return writeValueToCharacteristic(LOGGING_INTERVAL_S_CHARACTERISTIC_UUID, loggerIntervalS, FORMAT_UINT16, 0);
    }

    @Override
    public int getLoggerInterval() {
        return mLoggerIntervalMs;
    }

    @Override
    public void requestValueUpdate() {
        readLoggerInterval();
        readLoggingState();
        readCurrentPointer();
        readStartPointer();
        readEndPointer();
        readUserData();
    }

    /*
        Implementation of {@link BleConnectorCallback}
     */

    @Override
    public void onConnectionStateChanged(boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    @Override
    public void onDataReceived(String characteristicUuid, byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            switch (characteristicUuid) {
                case LOGGING_INTERVAL_S_CHARACTERISTIC_UUID:
                    mLoggerIntervalMs = 1000 * LittleEndianExtractor.extractLittleEndianShortFromCharacteristicValue(rawData);
                    mLastValue[0] = new SmartGadgetValue(new Date(), mLoggerIntervalMs, LOGGER_INTERVAL_UNIT);
                    break;
                case START_STOP_CHARACTERISTIC_UUID:
                    mLoggerStateEnabled = ((int) rawData[0] > 0);
                    Log.d(TAG, "Received START_STOP_CHARACTERISTIC_UUID data: " + mLoggerStateEnabled);
                    continueDownloadProtocol();
                    break;
                case CURRENT_POINTER_CHARACTERISTIC_UUID:
                    mCurrentPointer = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristicValue(rawData);
                    Log.d(TAG, "Received CURRENT_POINTER_CHARACTERISTIC_UUID data: " + mCurrentPointer);
                    continueDownloadProtocol();
                    break;
                case START_POINTER_CHARACTERISTIC_UUID:
                    mStartPointer = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristicValue(rawData);
                    Log.d(TAG, "Received START_POINTER_CHARACTERISTIC_UUID data: " + mStartPointer);
                    continueDownloadProtocol();
                    break;
                case END_POINTER_CHARACTERISTIC_UUID:
                    mEndPointer = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristicValue(rawData);
                    Log.d(TAG, "Received END_POINTER_CHARACTERISTIC_UUID data: " + mEndPointer);
                    continueDownloadProtocol();
                    break;
                case USER_DATA_CHARACTERISTIC_UUID:
                    mLoggingEnabledTimestamp = LittleEndianExtractor.extractLittleEndianIntegerFromCharacteristicValue(rawData);
                    Log.d(TAG, "Received USER_DATA_CHARACTERISTIC_UUID data: " + mLoggingEnabledTimestamp);
                    break;
                case LOGGED_DATA_CHARACTERISTIC_UUID:
                    Log.d(TAG, "Received LOGGED_DATA_CHARACTERISTIC_UUID data");
                    handleDownloadedData(rawData);
                    if (mDownloadState.equals(DownloadState.RUNNING)) {
                        mBleConnector.readCharacteristic(mDeviceAddress, LOGGED_DATA_CHARACTERISTIC_UUID);
                    }
                    break;
            }
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (characteristicUuid) {
                    case LOGGING_INTERVAL_S_CHARACTERISTIC_UUID:
                        readLoggerInterval();
                        break;
                    case START_STOP_CHARACTERISTIC_UUID:
                        readLoggingState();
                        break;
                    case START_POINTER_CHARACTERISTIC_UUID:
                        readStartPointer();
                        break;
                    case END_POINTER_CHARACTERISTIC_UUID:
                        readEndPointer();
                        break;
                    case USER_DATA_CHARACTERISTIC_UUID:
                        readUserData();
                        break;
                }
            }
        }, SHTC1_SPECIFIC_READ_AFTER_WRITE_DELAY_MS);
    }

    /*
        Private Methods
     */
    private boolean initiateDownloadProtocol() {
        mDownloadState = DownloadState.INIT;
        mLoggerStateBeforeDownload = isGadgetLoggingEnabled();
        mDownloadStartTimestamp = new Date();
        setGadgetLoggingEnabled(false);
        return true; // TODO really?
    }

    private void continueDownloadProtocol() {
        switch (mDownloadState) {
            case INIT:
                mDownloadState = DownloadState.CURRENT_POINTER;
                readCurrentPointer();
                break;
            case CURRENT_POINTER:
                mDownloadState = DownloadState.START_POINTER;
                writeStartPointer();
                break;
            case START_POINTER:
                mDownloadState = DownloadState.END_POINTER;
                writeEndPointer();
                break;
            case END_POINTER:
                mDownloadState = DownloadState.RUNNING;
                mNrOfElementsDownloaded = 0;
                mDownloadProgress = 0;
                mNrOfElementsToDownload = calculateValuesToDownload();
                mBleConnector.readCharacteristic(mDeviceAddress, LOGGED_DATA_CHARACTERISTIC_UUID);
                break;
            case RUNNING:
            case IDLE:
            default:
                // No download running
                break;
        }
    }

    private int calculateValuesToDownload() {
        final int totalNumberOfValues = mCurrentPointer - mStartPointer;
        return (totalNumberOfValues > GADGET_RING_BUFFER_SIZE) ? GADGET_RING_BUFFER_SIZE : totalNumberOfValues;
    }

    private void handleDownloadedData(final byte[] rawData) {
        if (rawData.length == 0) {
            onDownloadComplete();
            return;
        }

        final List<GadgetValue> downloadedValues = parseDownloadData(rawData);

        mServiceListener.onGadgetDownloadDataReceived(this,
                downloadedValues.toArray(new GadgetValue[downloadedValues.size()]),
                mDownloadProgress);
    }

    @NonNull
    private List<GadgetValue> parseDownloadData(final byte[] rawData) {
        final List<GadgetValue> downloadedValues = new ArrayList<>();
        for (int i = 0; i < rawData.length; i += DATA_POINT_SIZE) {
            final byte[] dataPoint = new byte[DATA_POINT_SIZE];
            final short[] humidityAndTemperature = new short[DATA_POINT_SIZE / 2];
            System.arraycopy(rawData, i, dataPoint, 0, DATA_POINT_SIZE);
            ByteBuffer.wrap(dataPoint).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(humidityAndTemperature);

            final float temperature = ((float) humidityAndTemperature[0]) / 100f;
            final float humidity = ((float) humidityAndTemperature[1]) / 100f;
            final long timestamp = mLoggingEnabledTimestamp * 1000l + (mStartPointer + mNrOfElementsDownloaded) * (long) mLoggerIntervalMs;

            // TODO: The altTimestamp would enable us to do log downloads without the requirement
            // TODO:    to write the log enabled timestamp to user data. But it would require us to
            // TODO:    only be able to download data if logging is enabled. Hence, downloading
            // TODO:    would only be possible once, and on a disconnect, the data would be lost.
            final long altTimestamp = mDownloadStartTimestamp.getTime() - (mLoggerIntervalMs * (mNrOfElementsToDownload - mNrOfElementsDownloaded));
            Log.i(TAG, "DOWNLOADING DATA: timestamp comparison: legecy: " + timestamp + " vs. alternative: " + altTimestamp);

            downloadedValues.add(new SmartGadgetValue(new Date(timestamp), temperature, SHTC1TemperatureAndHumidityService.UNIT_T));
            downloadedValues.add(new SmartGadgetValue(new Date(timestamp), humidity, SHTC1TemperatureAndHumidityService.UNIT_RH));

            updateDownloadProgress();
        }
        return downloadedValues;
    }

    private void updateDownloadProgress() {
        mNrOfElementsDownloaded++;
        mDownloadProgress = (int) Math.ceil(100 * (mNrOfElementsDownloaded / (float) mNrOfElementsToDownload));
        if (mNrOfElementsDownloaded >= mNrOfElementsToDownload) {
            mDownloadProgress = 100;
        }
    }

    private void onDownloadComplete() {
        Log.i(TAG, "On Download Complete");
        mDownloadProgress = 100;
        mDownloadState = DownloadState.IDLE;
        setGadgetLoggingEnabled(mLoggerStateBeforeDownload);
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }

    private void readLoggerInterval() {
        mBleConnector.readCharacteristic(mDeviceAddress, LOGGING_INTERVAL_S_CHARACTERISTIC_UUID);
    }

    private void readLoggingState() {
        mBleConnector.readCharacteristic(mDeviceAddress, START_STOP_CHARACTERISTIC_UUID);
    }

    private void readCurrentPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, CURRENT_POINTER_CHARACTERISTIC_UUID);
    }

    private void readStartPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, START_POINTER_CHARACTERISTIC_UUID);
    }

    // TODO shouldn't we make use of the return value, like if it fails?
    private void writeStartPointer() {
        writeValueToCharacteristic(START_POINTER_CHARACTERISTIC_UUID, calculateMinimumStartPoint(), FORMAT_UINT32, 0);
    }

    private void readEndPointer() {
        mBleConnector.readCharacteristic(mDeviceAddress, END_POINTER_CHARACTERISTIC_UUID);
    }

    private void writeEndPointer() {
        writeValueToCharacteristic(END_POINTER_CHARACTERISTIC_UUID, mCurrentPointer, FORMAT_UINT32, 0);
    }

    private void readUserData() {
        mBleConnector.readCharacteristic(mDeviceAddress, USER_DATA_CHARACTERISTIC_UUID);
    }

    /*
     * The User data is used to store the time stamp when logging was enabled. This enables us to
     * recreate the timestamps of downloaded samples by multiplying the current pointer with the
     * logger interval, giving us the timestamp of the first downloaded sample.
     * The C1 gadget sets the current pointer to 0 when re-enabling the logger state. Hence the, as
     * we here disable the logging before downloading the data, we can only download the data once.
     * When it fails, the data is lost.
     */
    // TODO: This fails too much ... on fail, check if uuid == user_data_uuid, and then retry!
    private void writeLoggingStartTimestamp() {
        writeValueToCharacteristic(USER_DATA_CHARACTERISTIC_UUID, (int) (System.currentTimeMillis() / 1000l), FORMAT_UINT32, 0);
    }

    private int calculateMinimumStartPoint() {
        if (mCurrentPointer > GADGET_RING_BUFFER_SIZE) {
            return mCurrentPointer - GADGET_RING_BUFFER_SIZE;
        }
        return 1;
    }

    private boolean writeValueToCharacteristic(final String characteristicUuid, final int value,
                                               final int formatType, final int offset) {
        final BluetoothGattCharacteristic characteristic =
                mBleConnector.getCharacteristics(mDeviceAddress,
                        Collections.singletonList(characteristicUuid))
                        .get(characteristicUuid);
        if (characteristic == null) return false;

        characteristic.setValue(value, formatType, offset);
        mBleConnector.writeCharacteristic(mDeviceAddress, characteristic);
        return true;
    }

    enum DownloadState {
        IDLE, INIT, CURRENT_POINTER, START_POINTER, END_POINTER, RUNNING
    }
}
