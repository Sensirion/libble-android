package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

public class HumidityService extends Sht3xService {
    private static final String TAG = HumidityService.class.getSimpleName();

    public static final String UNIT = "%";

    public static final String SERVICE_UUID = "00001234-b38d-4985-720e-0f993a68ee41";

    public static final String NOTIFICATIONS_UUID = "00001235-b38d-4985-720e-0f993a68ee41";

    public HumidityService(@NonNull final ServiceListener serviceListener,
                           @NonNull final BleConnector bleConnector,
                           @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, NOTIFICATIONS_UUID, UNIT);
    }
}
