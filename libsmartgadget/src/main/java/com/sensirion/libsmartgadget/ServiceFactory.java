package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class ServiceFactory {
    @NonNull
    public static List<GadgetService> createServicesFor(List<BluetoothGattService> services) {
        final List<GadgetService> serviceList = new ArrayList<>();

        // TODO impl.

        return serviceList;
    }
}
