package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetService;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class GadgetServiceFactory {
    private static final String TAG = GadgetServiceFactory.class.getSimpleName();
    private final BleConnector mBleConnector;
    private final Map<String, Class<? extends GadgetService>> mGadgetServiceRepository;

    public GadgetServiceFactory(final BleConnector bleConnector) {
        mBleConnector = bleConnector;
        mGadgetServiceRepository = new HashMap<>();

        registerSmartGadgetServices();
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

    public void registerSmartGadgetService(@NonNull final String serviceUuid,
                                           @NonNull final Class<? extends GadgetService> serviceClass) {
        mGadgetServiceRepository.put(serviceUuid, serviceClass);
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

    private void registerSmartGadgetServices() {
        // Generic
        registerSmartGadgetService(BatteryService.SERVICE_UUID, BatteryService.class);
        registerSmartGadgetService(DeviceInformationService.SERVICE_UUID, DeviceInformationService.class);
        // 3x Gadget
        registerSmartGadgetService(SHT3xTemperatureService.SERVICE_UUID, SHT3xTemperatureService.class);
        registerSmartGadgetService(SHT3xHumidityService.SERVICE_UUID, SHT3xHumidityService.class);
        registerSmartGadgetService(SHT3xHistoryService.SERVICE_UUID, SHT3xHistoryService.class);
        // C1 Gadget
        registerSmartGadgetService(SHTC1TemperatureAndHumidityService.SERVICE_UUID, SHTC1TemperatureAndHumidityService.class);
        registerSmartGadgetService(SHTC1HistoryService.SERVICE_UUID, SHTC1HistoryService.class);
        // TI Sensor Tag
        registerSmartGadgetService(SensorTagTemperatureAndHumidityService.SERVICE_UUID, SensorTagTemperatureAndHumidityService.class);
    }
}
