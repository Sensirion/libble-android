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
        // Generic
        mGadgetServiceRepository.put(BatteryService.SERVICE_UUID, BatteryService.class);
        // 3x Gadget
        mGadgetServiceRepository.put(SHT3xTemperatureService.SERVICE_UUID, SHT3xTemperatureService.class);
        mGadgetServiceRepository.put(SHT3xHumidityService.SERVICE_UUID, SHT3xHumidityService.class);
        mGadgetServiceRepository.put(SHT3xHistoryService.SERVICE_UUID, SHT3xHistoryService.class);
        // C1 Gadget
        mGadgetServiceRepository.put(SHTC1TemperatureAndHumidityService.SERVICE_UUID, SHTC1TemperatureAndHumidityService.class);
        mGadgetServiceRepository.put(SHTC1HistoryService.SERVICE_UUID, SHTC1HistoryService.class);
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
