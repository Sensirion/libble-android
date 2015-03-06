package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

/**
 * Represents a {@link android.bluetooth.BluetoothGattService} as defined in org.bluetooth.service.*
 * or a proprietary implementation.
 */
public abstract class BleService<ListenerType extends NotificationListener> {

    //Characteristic descriptor UUIDs
    protected static final UUID USER_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");
    protected static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //Force action attributes
    private static final short WAIT_BETWEEN_NOTIFICATION_REGISTER_REQUEST = 210; //Ask the device every 4 connection intervals.
    private static final byte MAX_NUMBER_NOTIFICATION_REGISTER_REQUEST = 10;
    protected final String TAG = this.getClass().getSimpleName();
    //Class attributes
    protected final Peripheral mPeripheral;
    //Listeners
    protected final Set<ListenerType> mListeners = Collections.synchronizedSet(new HashSet<ListenerType>());
    private final BluetoothGattService mBluetoothGattService;
    //Notifications
    private final Set<BluetoothGattCharacteristic> mNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());
    private final Set<BluetoothGattCharacteristic> mRegisteredNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());
    private boolean mNotificationsAreEnabled = false;
    private boolean mIsRequestingNotifications = false;

    public BleService(@NonNull final Peripheral servicePeripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        mPeripheral = servicePeripheral;
        mBluetoothGattService = bluetoothGattService;
    }

    /**
     * Asks the bluetooth service for a characteristic.
     *
     * @param uuid {@link java.lang.String} from the characteristic requested by the user.
     * @return {@link android.bluetooth.BluetoothGattCharacteristic} requested by the user.
     */
    protected BluetoothGattCharacteristic getCharacteristic(@NonNull final String uuid) {
        return mBluetoothGattService.getCharacteristic(UUID.fromString(uuid.trim().toLowerCase()));
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        return mBluetoothGattService.getCharacteristics().contains(updatedCharacteristic);
    }

    /**
     * This method is called when a characteristic was written in the device.
     *
     * @param characteristic that was written in the device with success.
     *                       return <code>true</code> if the service managed the given characteristic - <code>false</code> otherwise.
     */
    public boolean onCharacteristicWrite(@NonNull final BluetoothGattCharacteristic characteristic) {
        return mBluetoothGattService.getCharacteristics().contains(characteristic);
    }

    /**
     * Method called when a descriptor is read.
     *
     * @param descriptor that was read by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    public boolean onDescriptorRead(@NonNull final BluetoothGattDescriptor descriptor) {
        return false; // This method needs to be overridden in order to do something with the descriptor.
    }

    /**
     * Method called when a descriptor is written.
     *
     * @param descriptor that was written by the device.
     * @return <code>true</code> if the descriptor was processed - <code>false</code> otherwise.
     */
    public boolean onDescriptorWrite(@NonNull final BluetoothGattDescriptor descriptor) {
        if (mNotifyCharacteristics.contains(descriptor.getCharacteristic())) {
            mRegisteredNotifyCharacteristics.add(descriptor.getCharacteristic());
            return true;
        }
        return false;
    }

    /**
     * Gets the UUID of the service.
     *
     * @return {@link java.lang.String} of the UUID.
     */
    public String getUUIDString() {
        return mBluetoothGattService.getUuid().toString();
    }

    /**
     * Checks if the service implementation is from a given type.
     *
     * @param serviceDescription name or simple name of the class from the service.
     * @return <code>true</code> if the service implementation is from the given type - <code>false</code> otherwise.
     */
    public boolean isExplicitService(final String serviceDescription) {
        if (BleService.class.getName().endsWith(serviceDescription)) {
            Log.w(TAG, "isExplicitService -> A generic service can't be retrieved.");
            return false; //A generic service can't be retrieved.
        }
        return this.getClass().getName().endsWith(serviceDescription);
    }

    /**
     * Obtains the device address of the device.
     *
     * @return {@link java.lang.String} with the device address.
     */
    public String getDeviceAddress() {
        return mPeripheral.getAddress();
    }


    protected void registerNotification(@NonNull final BluetoothGattCharacteristic characteristic) {
        final int properties = characteristic.getProperties();

        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristics.add(characteristic);
            Log.i(TAG, String.format("registerNotification -> On device %s the notification %s was registered.", getDeviceAddress(), characteristic.getUuid()));
            setCharacteristicNotification(characteristic, true);
            mNotificationsAreEnabled = true;
        } else {
            Log.w(TAG, String.format("registerNotification -> The application does not have permission to register for notifications in the characteristic %s.", characteristic.getUuid()));
            if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristics.contains(characteristic)) {
                    mNotifyCharacteristics.remove(characteristic);
                    Log.i(TAG, String.format("registerNotification -> Cleared active notification of UUID %s in peripheral with address: %s", characteristic.getUuid(), getDeviceAddress()));
                    setCharacteristicNotification(characteristic, false);
                    if (mNotifyCharacteristics.isEmpty()) {
                        mNotificationsAreEnabled = false;
                    }
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
        Log.i(TAG, String.format("setCharacteristicNotification to %b in characteristic with UUID %s on device %s.", enabled, characteristic.getUuid(), getDeviceAddress()));
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
     * Enables or disables the characteristic notification of a service.
     *
     * @param enabled <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
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
     * This method should only be called by the {@link com.sensirion.libble.devices.Peripheral} registered with this instance.
     * This method adds a listener to the list in case it's from the type specified in the ListenerType generics.
     *
     * @param listener the new candidate object for being a listener of this class.
     * @return <code>true</code> in case it's a valid listener, <code>false</code> otherwise.
     */
    public boolean registerNotificationListener(@NonNull final NotificationListener listener) {
        try {
            final ListenerType validListener = (ListenerType) listener;
            if (mListeners.contains(validListener)) {
                Log.w(TAG, String.format("registerNotificationListener -> Listener %s was already registered in peripheral %s.", validListener, getDeviceAddress()));
            } else {
                Log.d(TAG, String.format("registerNotificationListener -> Registered %s notification in peripheral %s.", listener.getClass().getSimpleName(), getDeviceAddress()));
                mListeners.add(validListener);
            }
            return true;
        } catch (final ClassCastException e) {
            return false;
        } finally {
            if (mIsRequestingNotifications && mListeners.size() >= 1) {
                setNotificationsEnabled(true);
            }
        }
    }

    /**
     * This method should only be called by the {@link com.sensirion.libble.devices.Peripheral} registered with this instance.
     * This method unregister a listener from the list in case of having it.
     *
     * @param listener the listener that doesn't want to hear from a device anymore.
     */
    public boolean unregisterNotificationListener(@NonNull final NotificationListener listener) {
        try {
            mListeners.remove(listener);
            return true;
        } catch (final ClassCastException cce) {
            Log.e(TAG, "unregisterNotificationListener -> The following error was produced when trying to remove the notification listener -> ", cce);
            return false;
        } finally {
            if (mListeners.isEmpty() && mNotificationsAreEnabled) {
                setNotificationsEnabled(false);
            }
        }
    }

    @Override
    public boolean equals(@Nullable final Object otherService) {
        if (otherService == null) {
            return false;
        } else if (otherService instanceof BleService) {
            return ((BleService) otherService).getClass().getSimpleName().equals(TAG);
        } else if (otherService instanceof String) {
            return isExplicitService((String) otherService);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("%s of the device: %s", getClass().getSimpleName(), getDeviceAddress());
    }
}