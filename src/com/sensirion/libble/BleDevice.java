package com.sensirion.libble;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    String getAddress();

    String getAdvertisedName();

    int getRSSI();

    boolean isConnected();

    <T extends PeripheralService> T getPeripheralService(Class<T> type);

}
