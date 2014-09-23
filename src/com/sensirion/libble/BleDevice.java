package com.sensirion.libble;

import com.sensirion.libble.bleservice.PeripheralService;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    String getAddress();

    String getAdvertisedName();

    int getRSSI();

    boolean isConnected();

    <T extends PeripheralService> T getPeripheralService(Class<T> type);

    Iterable<String> getDiscoveredPeripheralServices();
}