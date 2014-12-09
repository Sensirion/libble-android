package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.peripherals.Peripheral;
import com.sensirion.libble.services.generic.BatteryPeripheralService;
import com.sensirion.libble.services.sensirion.shtc1.HumigadgetConnectionSpeedService;
import com.sensirion.libble.services.sensirion.shtc1.HumigadgetLoggingService;
import com.sensirion.libble.services.sensirion.shtc1.HumigadgetRHTNotificationService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PeripheralServiceFactory {

    private static final String TAG = PeripheralServiceFactory.class.getSimpleName();

    private static PeripheralServiceFactory mInstance = new PeripheralServiceFactory();

    private final Map<String, Class<? extends PeripheralService>> mServiceLookUp = Collections.synchronizedMap(new HashMap<String, Class<? extends PeripheralService>>());

    private PeripheralServiceFactory() {
        registerServiceImplementation(BatteryPeripheralService.SERVICE_UUID, BatteryPeripheralService.class);
        registerServiceImplementation(HumigadgetRHTNotificationService.SERVICE_UUID, HumigadgetRHTNotificationService.class);
        registerServiceImplementation(HumigadgetLoggingService.SERVICE_UUID, HumigadgetLoggingService.class);
        registerServiceImplementation(HumigadgetConnectionSpeedService.SERVICE_UUID, HumigadgetConnectionSpeedService.class);
    }

    public static PeripheralServiceFactory getInstance() {
        return mInstance;
    }

    /**
     * Wraps a given {@link android.bluetooth.BluetoothGattService} to a {@link PeripheralService}
     *
     * @param parent  {@link com.sensirion.libble.peripherals.Peripheral} that discovered the service.
     * @param service {@link android.bluetooth.BluetoothGattService} that should be wrapped.
     * @return {@link PeripheralService} with the service class with the same lookup UUID as the BluetoothGattService.
     */
    public PeripheralService createServiceFor(@NonNull final Peripheral parent, @NonNull final BluetoothGattService service) {
        final String uuid = service.getUuid().toString();
        final Class c = mServiceLookUp.get(uuid);

        if (c == null){
            Log.w(TAG, String.format("createServiceFor() -> Create generic service with uuid: %s", uuid));
            return new PeripheralService(parent, service);
        }

        Log.i(TAG, String.format("createServiceFor -> Create known service with uuid %s from class %s", uuid, c.getSimpleName()));
        try {
            Constructor<? extends PeripheralService> constructor = c.getDeclaredConstructor(Peripheral.class, BluetoothGattService.class);
            return constructor.newInstance(parent, service);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Log.e(TAG, "createServiceFor -> During the creation of a service the following exception was thrown -> ", e);
            throw new RuntimeException(String.format("%s: createServiceFor -> During the creation of a service the following exception was thrown ->", TAG),  e);
        }
    }

    /**
     * Let's you add your own specific service implementations that are created on app-level.
     * Make sure that these classes extend {@link com.sensirion.libble.services.PeripheralService}.
     *
     * @param newService for being checked.
     */
    public void registerServiceImplementation(@NonNull final String uuid, @NonNull final Class<? extends PeripheralService> newService) {
        if (mServiceLookUp.containsKey(uuid)) {
            Log.w(TAG, String.format("registerServiceImplementation -> The service with UUID %s was replaced by another service version.", uuid));
            mServiceLookUp.remove(uuid);
        } else {
            Log.i(TAG, String.format("registerServiceImplementation -> The service with UUID %s was added to the library lookup services.", uuid));
        }
        mServiceLookUp.put(uuid, newService);
    }
}