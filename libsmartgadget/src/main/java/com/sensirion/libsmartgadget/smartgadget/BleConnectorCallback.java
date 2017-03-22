package com.sensirion.libsmartgadget.smartgadget;

public interface BleConnectorCallback {
    void onConnectionStateChanged(boolean connected);

    void onDataReceived(final String characteristicUuid, final byte[] rawData);

    void onDataWritten(final String characteristicUuid);

    void onFail(final String characteristicUuid, byte[] data, final boolean isWriteFailure);
}
