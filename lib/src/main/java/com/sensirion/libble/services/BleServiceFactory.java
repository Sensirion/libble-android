package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.generic.BatteryService;
import com.sensirion.libble.services.generic.DeviceInformationService;
import com.sensirion.libble.services.sensirion.shtc1.SHTC1ConnectionSpeedService;
import com.sensirion.libble.services.sensirion.shtc1.SHTC1HistoryService;
import com.sensirion.libble.services.sensirion.shtc1.SHTC1RHTService;
import com.sensirion.libble.services.sensirion.smartgadget.SmartgadgetHistoryService;
import com.sensirion.libble.services.sensirion.smartgadget.SmartgadgetHumidityService;
import com.sensirion.libble.services.sensirion.smartgadget.SmartgadgetTemperatureService;
import com.sensirion.libble.services.ti.SensorTagRHTService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class BleServiceFactory {

    private static final String TAG = BleServiceFactory.class.getSimpleName();

    private static BleServiceFactory mInstance = new BleServiceFactory();

    private final Map<String, Class<? extends BleService>> mServiceLookUp = Collections.synchronizedMap(new HashMap<String, Class<? extends BleService>>());

    private BleServiceFactory() {
        registerGenericServices();
        registerSHTC1Services();
        registerSmartgadgetServices();
        registerTexasInstrumentsServices();
    }

    public static BleServiceFactory getInstance() {
        return mInstance;
    }

    private void registerGenericServices() {
        registerServiceImplementation(BatteryService.SERVICE_UUID, BatteryService.class);
        registerServiceImplementation(DeviceInformationService.SERVICE_UUID, DeviceInformationService.class);
    }

    private void registerSHTC1Services() {
        registerServiceImplementation(SHTC1RHTService.SERVICE_UUID, SHTC1RHTService.class);
        registerServiceImplementation(SHTC1HistoryService.SERVICE_UUID, SHTC1HistoryService.class);
        registerServiceImplementation(SHTC1ConnectionSpeedService.SERVICE_UUID, SHTC1ConnectionSpeedService.class);
    }

    private void registerSmartgadgetServices() {
        registerServiceImplementation(SmartgadgetTemperatureService.SERVICE_UUID, SmartgadgetTemperatureService.class);
        registerServiceImplementation(SmartgadgetHumidityService.SERVICE_UUID, SmartgadgetHumidityService.class);
        registerServiceImplementation(SmartgadgetHistoryService.SERVICE_UUID, SmartgadgetHistoryService.class);
    }

    private void registerTexasInstrumentsServices() {
        registerServiceImplementation(SensorTagRHTService.SERVICE_UUID, SensorTagRHTService.class);
    }

    /**
     * Wraps a given {@link android.bluetooth.BluetoothGattService} to a {@link BleService}
     *
     * @param parent  {@link com.sensirion.libble.devices.Peripheral} that discovered the service.
     * @param service {@link android.bluetooth.BluetoothGattService} that should be wrapped.
     * @return {@link BleService} with the service class with the same lookup UUID as the BluetoothGattService. <code>null</code> if the class is not a valid class.
     */
    public BleService createServiceFor(@NonNull final Peripheral parent, @NonNull final BluetoothGattService service) {
        final String uuid = service.getUuid().toString();
        final Class serviceClass = mServiceLookUp.get(uuid);

        if (serviceClass == null) {
            Log.w(TAG, String.format("createServiceFor() -> Service %s does not have an assigned BleService.", uuid));
            return null;
        }

        Log.i(TAG, String.format("createServiceFor -> Create known service with uuid %s from class %s", uuid, serviceClass.getSimpleName()));
        try {
            final Constructor<? extends BleService> constructor = serviceClass.getDeclaredConstructor(Peripheral.class, BluetoothGattService.class);
            return constructor.newInstance(parent, service);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "createServiceFor -> During the creation of a service the following exception was thrown -> ", e);
            return null;
        }
    }

    /**
     * Lets you add your own specific service implementations that are created on app-level.
     * Make sure that these classes extend {@link BleService}.
     *
     * @param uuid       of the service.
     * @param newService class that is going to be instantiate.
     */
    public void registerServiceImplementation(@NonNull final String uuid, @NonNull final Class<? extends BleService> newService) {
        if (mServiceLookUp.containsKey(uuid)) {
            Log.w(TAG, String.format("registerServiceImplementation -> The service with UUID %s was replaced by another service version.", uuid));
            mServiceLookUp.remove(uuid);
        } else {
            Log.i(TAG, String.format("registerServiceImplementation -> The service with UUID %s was added to the library lookup services.", uuid));
        }
        mServiceLookUp.put(uuid, newService);
    }
}