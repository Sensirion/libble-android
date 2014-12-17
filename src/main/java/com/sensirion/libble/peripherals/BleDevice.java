package com.sensirion.libble.peripherals;

import com.sensirion.libble.services.PeripheralService;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    String getAddress();

    String getAdvertisedName();

    int getRSSI();

    boolean isConnected();
}