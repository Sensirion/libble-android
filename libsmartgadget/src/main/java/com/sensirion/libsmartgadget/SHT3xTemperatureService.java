package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

public class SHT3xTemperatureService extends SmartGadgetNotificationService {
    private static final String TAG = SHT3xTemperatureService.class.getSimpleName();

    public static final String UNIT = "°C";

    public static final String SERVICE_UUID = "00002234-b38d-4985-720e-0f993a68ee41";

    public static final String NOTIFICATIONS_UUID = "00002235-b38d-4985-720e-0f993a68ee41";

    public SHT3xTemperatureService(@NonNull final ServiceListener serviceListener,
                                   @NonNull final BleConnector bleConnector,
                                   @NonNull final String deviceAddress) {
        super(serviceListener, bleConnector, deviceAddress, SERVICE_UUID, NOTIFICATIONS_UUID, UNIT);
    }
}
