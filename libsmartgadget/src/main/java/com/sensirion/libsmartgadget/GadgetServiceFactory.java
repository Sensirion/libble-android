package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GadgetServiceFactory {
    private static final String TAG = GadgetServiceFactory.class.getSimpleName();
    private final BleConnector mBleConnector;
    private Map<String, Class<? extends GadgetService>> mGadgetServiceRepository;

    public GadgetServiceFactory(final BleConnector bleConnector) {
        mBleConnector = bleConnector;
        mGadgetServiceRepository = new HashMap<>();

        // TODO move somewhere else
        mGadgetServiceRepository.put(BatteryService.SERVICE_UUID, BatteryService.class);
        mGadgetServiceRepository.put(TemperatureService.SERVICE_UUID, TemperatureService.class);
        mGadgetServiceRepository.put(HumidityService.SERVICE_UUID, HumidityService.class);
        mGadgetServiceRepository.put(Sht3xHistoryService.SERVICE_UUID, Sht3xHistoryService.class);
    }

    @NonNull
    public List<GadgetService> createServicesFor(@NonNull final ServiceListener serviceListener,
                                                 @NonNull final String deviceAddress,
                                                 @NonNull final List<BluetoothGattService> services) {
        final List<GadgetService> serviceList = new ArrayList<>();

        for (final BluetoothGattService service : services) {
            final Class<? extends GadgetService> gadgetServiceClass = mGadgetServiceRepository.get(service.getUuid().toString());
            if (gadgetServiceClass == null) {
                // Unknown Service ...
                continue;
            }
            final GadgetService gadgetService = createServicesFor(serviceListener, deviceAddress, mBleConnector, gadgetServiceClass);
            if (gadgetService == null) {
                // Failed to create service ...
                continue;
            }
            serviceList.add(gadgetService);
        }

        return serviceList;
    }

    private GadgetService createServicesFor(@NonNull final ServiceListener serviceListener,
                                            @NonNull final String deviceAddress,
                                            @NonNull final BleConnector bleConnector,
                                            @NonNull final Class<? extends GadgetService> gadgetServiceClass) {
        try {
            final Constructor<? extends GadgetService> constructor = gadgetServiceClass.getDeclaredConstructor(ServiceListener.class,
                    BleConnector.class, String.class);
            return constructor.newInstance(serviceListener, bleConnector, deviceAddress);
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to create service of type %s", gadgetServiceClass.toString()), e);
            return null;
        }
    }
}
