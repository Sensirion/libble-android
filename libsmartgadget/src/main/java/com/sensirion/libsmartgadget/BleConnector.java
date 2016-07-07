package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.List;

interface BleConnector {

    boolean connect(SmartGadget gadget);

    boolean disconnect(SmartGadget gadget);

    @NonNull
    List<BluetoothGattService> getServices(SmartGadget gadget);

}
