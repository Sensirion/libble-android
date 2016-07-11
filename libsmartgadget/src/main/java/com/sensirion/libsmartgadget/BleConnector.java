package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.List;
import java.util.Map;

interface BleConnector {

    boolean connect(SmartGadget gadget);

    boolean disconnect(SmartGadget gadget);

    @NonNull
    List<BluetoothGattService> getServices(SmartGadget gadget);

    @NonNull
    public Map<String, BluetoothGattCharacteristic> getCharacteristics(@NonNull final String deviceAddress,
                                                                       final List<String> uuids);

    void readCharacteristic(@NonNull final String deviceAddress, final String characteristicUuid);

    void readCharacteristic(@NonNull final String deviceAddress,
                            final BluetoothGattCharacteristic characteristic);

    void writeCharacteristic(@NonNull final String deviceAddress,
                             final BluetoothGattCharacteristic characteristic);

    void setCharacteristicNotification(@NonNull final String deviceAddress,
                                       final BluetoothGattCharacteristic characteristic,
                                       final BluetoothGattDescriptor descriptor,
                                       final boolean enabled);
}
