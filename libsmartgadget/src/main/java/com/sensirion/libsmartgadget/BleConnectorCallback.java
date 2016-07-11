package com.sensirion.libsmartgadget;

public interface BleConnectorCallback {
    void onConnectionStateChanged(boolean connected);

    void onDataReceived(final String characteristicUuid, final byte[] rawData);

    void onDataWritten(final String characteristicUuid);
}
