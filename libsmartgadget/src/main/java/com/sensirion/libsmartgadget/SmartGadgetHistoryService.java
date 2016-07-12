package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public abstract class SmartGadgetHistoryService implements GadgetDownloadService, BleConnectorCallback {
    private static final String TAG = SmartGadgetHistoryService.class.getSimpleName();

    protected static final String UNKNOWN_UNIT = "";
    protected static final String LOGGER_INTERVAL_UNIT = "ms";

    protected final BleConnector mBleConnector;
    protected final ServiceListener mServiceListener;
    protected final String mDeviceAddress;

    protected final Set<String> mSupportedUuids;

    protected int mLoggerIntervalMs;
    protected boolean mLoggerStateEnabled;
    protected int mDownloadProgress;
    protected int mNrOfElementsDownloaded;
    protected int mNrOfElementsToDownload;

    protected GadgetValue[] mLastValue;

    public SmartGadgetHistoryService(@NonNull final ServiceListener serviceListener,
                                     @NonNull final BleConnector bleConnector,
                                     @NonNull final String deviceAddress,
                                     @NonNull final String[] supportedUuids) {
        mBleConnector = bleConnector;
        mServiceListener = serviceListener;
        mDeviceAddress = deviceAddress;

        mDownloadProgress = -1;
        mLastValue = new GadgetValue[]{new SmartGadgetValue(new Date(), -1, LOGGER_INTERVAL_UNIT)};

        mSupportedUuids = new HashSet<>();
        Collections.addAll(mSupportedUuids, supportedUuids);
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
    public abstract boolean isDownloading();

    @Override
    public int getDownloadProgress() {
        return mDownloadProgress;
    }

    @Override
    public GadgetValue[] getLastValues() {
        return mLastValue;
    }

    @Override
    public abstract boolean isGadgetLoggingStateEditable();

    @Override
    public boolean isGadgetLoggingEnabled() {
        return mLoggerStateEnabled;
    }

    @Override
    public abstract void setGadgetLoggingEnabled(final boolean enabled);

    @Override
    public abstract boolean setLoggerInterval(final int loggerIntervalMs);

    @Override
    public abstract void requestValueUpdate();

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
            handleDataReceived(characteristicUuid, rawData);
        }
    }

    protected abstract void handleDataReceived(final String characteristicUuid, final byte[] rawData);

    @Override
    public void onDataWritten(final String characteristicUuid) {
        if (isUuidSupported(characteristicUuid)) {
            handleDataWritten(characteristicUuid);
        }
    }

    protected abstract void handleDataWritten(final String characteristicUuid);

    /*
        Private helper methods
     */

    protected abstract boolean initiateDownloadProtocol();

    protected boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }

    protected boolean writeValueToCharacteristic(final String characteristicUuid, final int value,
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
}
