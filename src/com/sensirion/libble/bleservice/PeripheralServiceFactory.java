package com.sensirion.libble.bleservice;

import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.implementations.generic_services.BatteryPeripheralService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetConnectionSpeedService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetLoggingService;
import com.sensirion.libble.bleservice.implementations.sensirion.shtc1_smartgadget.HumigadgetRHTNotificationService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class PeripheralServiceFactory {
    private static final String TAG = PeripheralServiceFactory.class.getSimpleName();
    private static PeripheralServiceFactory mInstance = new PeripheralServiceFactory();

    private Map<String, Class<? extends PeripheralService>> mServiceLookUp;

    private PeripheralServiceFactory() {
        mServiceLookUp = new HashMap<String, Class<? extends PeripheralService>>();
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
     * @param parent  {@link com.sensirion.libble.Peripheral} that discovered the service.
     * @param service {@link android.bluetooth.BluetoothGattService} that should be wrapped
     * @return {@link PeripheralService}
     */
    public PeripheralService createServiceFor(final Peripheral parent, final BluetoothGattService service) {
        final String uuid = service.getUuid().toString();
        if (mServiceLookUp.containsKey(uuid)) {
            final Class c = mServiceLookUp.get(uuid);
            Log.i(TAG, "Create known service with uuid: " + uuid + " | from class: " + c.getSimpleName());
            try {
                Constructor<? extends PeripheralService> constructor = c.getDeclaredConstructor(Peripheral.class, BluetoothGattService.class);
                return constructor.newInstance(parent, service);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, "Exception when creating a new service", e);
            } catch (InvocationTargetException e) {
                Log.e(TAG, "Exception when creating a new service", e);
            } catch (InstantiationException e) {
                Log.e(TAG, "Exception when creating a new service", e);
            } catch (IllegalAccessException e) {
                Log.e(TAG, "Exception when creating a new service", e);
            }
            throw new IllegalArgumentException("Unable to instantiate class type: " + c.toString());
        } else {
            Log.w(TAG, "createServiceFor() -> fallback to generic PeripheralService for UUID: " + uuid);
            return new PeripheralService(parent, service);
        }
    }

    /**
     * Let's you add your own specific service implementations that are created on app-level.
     * Make sure that these classes extend {@link com.sensirion.libble.bleservice.PeripheralService}.
     *
     * @param newService for being checked.
     */
    public void registerServiceImplementation(final String uuid, final Class<? extends PeripheralService> newService) {
        if (mServiceLookUp.containsKey(uuid)) {
            Log.w(TAG, "The service " + uuid + " was replaced by another service version.");
            mServiceLookUp.remove(uuid);
        }
        mServiceLookUp.put(uuid, newService);
    }
}