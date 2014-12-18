package com.sensirion.libble.peripherals;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    String getAddress();

    String getAdvertisedName();

    int getRSSI();

    boolean isConnected();
}