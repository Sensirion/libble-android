package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * @see com.sensirion.libble.services.BleService
 * Manages automatically the services listeners represented with the generic <code>ListenerType extends NotificationListener</code>
 */
public abstract class AbstractBleService<ListenerType extends NotificationListener> implements BleService {

    //Characteristic descriptor UUIDs
    protected static final UUID USER_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    protected static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //Force action attributes
    private static final short WAIT_BETWEEN_NOTIFICATION_REGISTER_REQUEST = 210; //Ask the device every 4 connection intervals.
    private static final byte MAX_NUMBER_NOTIFICATION_REGISTER_REQUEST = 10;

    //Class attributes
    protected final String TAG = this.getClass().getSimpleName();
    @NonNull
    protected final Peripheral mPeripheral;

    //Listeners.
    @Nullable
    protected final Set<ListenerType> mListeners;

    //Parent service.
    @NonNull
    private final BluetoothGattService mBluetoothGattService;

    //Notification attributes.
    @NonNull
    private final Set<BluetoothGattCharacteristic> mNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());
    @NonNull
    private final Set<BluetoothGattCharacteristic> mRegisteredNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());
    @Nullable
    private final Class<ListenerType> mNotificationClassType;
    private boolean mIsRequestingNotifications = false;

    public AbstractBleService(@NonNull final Peripheral servicePeripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        mPeripheral = servicePeripheral;
        mBluetoothGattService = bluetoothGattService;
        mNotificationClassType = getListenerClassType();
        if (mNotificationClassType == null) {
            Log.d(TAG, String.format("BleService -> Service %s does not use the automatic listener system.", TAG));
            mListeners = null;
        } else {
            Log.d(TAG, String.format("BleService -> Service %s manages automatically listeners from type: %s", TAG, mNotificationClassType.getSimpleName()));
            mListeners = Collections.synchronizedSet(new HashSet<ListenerType>());
        }
    }

    /**
     * Obtain the class type of <code>ListenerType</code> in case it's set by the {@link AbstractBleService} implementation.
     *
     * @return <code>Class<ListenerType></code> in case the service wants the listeners to be managed automatically - <code>null</code> otherwise.
     */
    @Nullable
    private Class<ListenerType> getListenerClassType() {
        final Stack<Class<? extends AbstractBleService>> classInheritanceStack = new Stack<>();
        Class<? extends AbstractBleService> serviceClass = getClass();
        while (serviceClass.getSuperclass() != AbstractBleService.class) {
            classInheritanceStack.push(serviceClass);
            serviceClass = (Class<? extends AbstractBleService>) serviceClass.getSuperclass();
        }
        do {
            Type genericSuperclass = serviceClass.getGenericSuperclass();
            if (genericSuperclass instanceof Class) {
                return null; //The BleService implementation does not use Generics.
            }
            final Type[] actualTypeArguments = ((ParameterizedType) genericSuperclass).getActualTypeArguments();
            if (actualTypeArguments.length == 0) {
                return null; //The arguments don't have actual type parameters.
            }
            if (actualTypeArguments[0] instanceof Class) {
                return (Class<ListenerType>) actualTypeArguments[0];
            }
            serviceClass = classInheritanceStack.pop();
        } while (classInheritanceStack.size() >= 0);
        return null;
    }

    /**
     * Asks the bluetooth service for a characteristic.
     *
     * @param uuid {@link java.lang.String} from the characteristic requested by the user.
     * @return {@link android.bluetooth.BluetoothGattCharacteristic} requested by the user - <code>null</code> if no service is found.
     */
    @Nullable
    protected BluetoothGattCharacteristic getCharacteristic(@NonNull final String uuid) {
        return mBluetoothGattService.getCharacteristic(UUID.fromString(uuid.trim().toLowerCase()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        return mBluetoothGattService.getCharacteristics().contains(updatedCharacteristic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        return mBluetoothGattService.getCharacteristics().contains(characteristic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDescriptorRead(@NonNull final BluetoothGattDescriptor descriptor) {
        return false; // This method needs to be overridden in order to do something with the descriptor.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDescriptorWrite(@NonNull final BluetoothGattDescriptor descriptor) {
        if (mNotifyCharacteristics.contains(descriptor.getCharacteristic())) {
            mRegisteredNotifyCharacteristics.add(descriptor.getCharacteristic());
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getUUIDString() {
        return mBluetoothGattService.getUuid().toString();
    }

    /**
     * {@inheritDoc}
     */

    public boolean isExplicitService(@NonNull final String serviceDescription) {
        return this.getClass().getSimpleName().equals(serviceDescription);
    }

    /**
     * Obtains the device address of the device.
     *
     * @return {@link java.lang.String} with the device address.
     */
    @NonNull
    public String getDeviceAddress() {
        return mPeripheral.getAddress();
    }

    protected void registerNotification(@NonNull final BluetoothGattCharacteristic characteristic) {
        final int properties = characteristic.getProperties();

        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristics.add(characteristic);
            Log.i(TAG, String.format("registerNotification -> On device %s the notification %s was registered.", getDeviceAddress(), characteristic.getUuid()));
            setCharacteristicNotification(characteristic, true);
            return;
        }

        Log.w(TAG, String.format("registerNotification -> The application does not have permission to register for notifications in the characteristic %s.", characteristic.getUuid()));
        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristics.contains(characteristic)) {
                mNotifyCharacteristics.remove(characteristic);
                Log.i(TAG, String.format("registerNotification -> Cleared active notification of UUID %s in peripheral with address: %s", characteristic.getUuid(), getDeviceAddress()));
                setCharacteristicNotification(characteristic, false);
                if (mNotifyCharacteristics.isEmpty()) {
                    setNotificationsEnabled(false);
                }
            }
        }
    }

    /**
     * Enables or disables a notification.
     *
     * @param characteristic of notifications.
     * @param enabled        <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    private void setCharacteristicNotification(@NonNull final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        Log.i(TAG, String.format("setCharacteristicNotification -> Setting notification state to %b in characteristic with UUID %s on device %s.", enabled, characteristic.getUuid(), getDeviceAddress()));
        mPeripheral.setCharacteristicNotification(characteristic, enabled);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        if (mRegisteredNotifyCharacteristics.contains(characteristic)) {
            mPeripheral.readCharacteristic(characteristic);
        } else {
            Executors.newSingleThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    mPeripheral.forceDescriptorWrite(descriptor, WAIT_BETWEEN_NOTIFICATION_REGISTER_REQUEST, MAX_NUMBER_NOTIFICATION_REGISTER_REQUEST);
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNotificationsEnabled(final boolean enabled) {
        mIsRequestingNotifications = enabled;
        if (enabled) {
            registerDeviceCharacteristicNotifications();
        } else {
            disableNotifications();
        }
    }

    /**
     * Registers the notification characteristics in case it's needed.
     */
    public void registerDeviceCharacteristicNotifications() {
        //This service needs to be override in order to register for notifications
    }

    /**
     * Disables all the service notifications.
     */
    private void disableNotifications() {
        Log.d(TAG, String.format("disableNotifications -> Disabling notifications on device %s.", getDeviceAddress()));
        if (mNotifyCharacteristics.isEmpty()) {
            return;
        }
        for (final BluetoothGattCharacteristic characteristic : mNotifyCharacteristics) {
            setCharacteristicNotification(characteristic, false);
        }
        mNotifyCharacteristics.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean registerNotificationListener(@NonNull final NotificationListener listener) {
        if (mNotificationClassType == null || mListeners == null) {
            if (mIsRequestingNotifications) {
                setNotificationsEnabled(true);
            }
            return false; //This service does not implement a listener interface. It would not manage notifications automatically.
        }
        if (mNotificationClassType.isAssignableFrom(listener.getClass())) {
            final ListenerType validListener = (ListenerType) listener;
            if (mListeners.contains(validListener)) {
                Log.w(TAG, String.format("registerNotificationListener -> Listener %s was already registered in peripheral %s.", listener, getDeviceAddress()));
            } else {
                Log.d(TAG, String.format("registerNotificationListener -> Registered %s notification in peripheral %s.", listener.getClass().getSimpleName(), getDeviceAddress()));
                mListeners.add(validListener);
            }
            if (mIsRequestingNotifications) {
                setNotificationsEnabled(true);
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("SuspiciousMethodCalls") //It's checked with isAssignableFrom
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listener) {
        if (mListeners == null || mNotificationClassType == null) {
            return false; //This service does not manage listeners automatically.
        }
        if (mNotificationClassType.isAssignableFrom(listener.getClass())) {
            mListeners.remove(listener);
        }
        if (mListeners.isEmpty()) {
            setNotificationsEnabled(false);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(@Nullable final Object otherService) {
        if (otherService == null) {
            return false;
        } else if (otherService instanceof AbstractBleService) {
            return ((AbstractBleService) otherService).getClass().getSimpleName().equals(TAG);
        } else if (otherService instanceof String) {
            return isExplicitService((String) otherService);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("%s of the device: %s", getClass().getSimpleName(), getDeviceAddress());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@NonNull final BleService otherService) {
        return otherService.getServiceSynchronizationPriority().ordinal()
                - getServiceSynchronizationPriority().ordinal();
    }
}