package com.sensirion.libble;

import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class PeripheralServiceFactory {
    private static final String TAG = PeripheralServiceFactory.class.getSimpleName();
    private static PeripheralServiceFactory mInstance = new PeripheralServiceFactory();

    private Map<String, Class<? extends PeripheralService>> mServiceLookUp;

    public static PeripheralServiceFactory getInstance() {
        return mInstance;
    }

    private PeripheralServiceFactory() {
        mServiceLookUp = new HashMap<String, Class<? extends PeripheralService>>();
        mServiceLookUp.put(PeripheralServiceBattery.UUID_SERVICE, PeripheralServiceBattery.class);
    }

    /**
     * Wraps a given {@link android.bluetooth.BluetoothGattService} to
     * a {@link com.sensirion.libble.PeripheralService}
     *
     * @param service {@link android.bluetooth.BluetoothGattService} that should be wrapped
     * @return {@link com.sensirion.libble.PeripheralService}
     */
    public PeripheralService createServiceFor(Peripheral parent, BluetoothGattService service) {
        String uuid = service.getUuid().toString();
        if (mServiceLookUp.containsKey(uuid)) {
            Class c = mServiceLookUp.get(uuid);
            Log.i(TAG, "Create known service with uuid: " + uuid + " | from class: " + c.getSimpleName());
            try {
                Constructor<? extends PeripheralService> constructor = c.getDeclaredConstructor(Peripheral.class, BluetoothGattService.class);
                return constructor.newInstance(parent, service);
            } catch (NoSuchMethodException e) {
                Log.e(TAG, e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(TAG, e.getMessage());
            } catch (InstantiationException e) {
                Log.e(TAG, e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, e.getMessage());
            }
            throw new IllegalArgumentException("Unable to instantiate class type: " + c.toString());
        } else {
            Log.w(TAG, "createServiceFor() -> fallback to generic PeripheralService for UUID: " + uuid);
            return new PeripheralService(parent, service);
        }
    }

    /**
     * Let's you add your own specific service implementations that are created on app-level.
     *
     * @param newService
     */
    public void configureCustomServiceImplementation(String uuid, Class<? extends PeripheralService> newService) {
        mServiceLookUp.put(uuid, newService);
    }

}
