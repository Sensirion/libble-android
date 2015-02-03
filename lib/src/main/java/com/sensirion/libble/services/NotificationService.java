package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

public abstract class NotificationService<CharacteristicValueType, ListenerType> extends BleService<CharacteristicValueType> {

    //Notification write descriptor.
    private static final UUID NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    //Force action attributes.
    private static final short WAIT_BETWEEN_NOTIFICATION_REGISTER_REQUEST = 160;
    private static final byte MAX_NUMBER_NOTIFICATION_REGISTER_REQUEST = 10;
    //Listener list.
    protected final Set<ListenerType> mListeners = Collections.synchronizedSet(new HashSet<ListenerType>());
    //Characteristics that send notifications from the BleDevice to the user.
    private final Set<BluetoothGattCharacteristic> mNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());
    //Notification state attributes.
    private boolean mNotificationsAreEnabled = false;
    private boolean mIsRequestingNotifications = false;

    protected NotificationService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
    }

    protected void registerNotification(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (mListeners.isEmpty()) {
            Log.w(TAG, "registerNotification -> No listeners to notify.");
            return;
        }

        final int properties = characteristic.getProperties();

        if ((properties & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristics.contains(characteristic)) {
                mNotifyCharacteristics.remove(characteristic);
                Log.i(TAG, String.format("registerNotification -> Cleared active notification of UUID %s in peripheral with address: %s", characteristic.getUuid(), mPeripheral.getAddress()));
                setCharacteristicNotification(characteristic, false);
                if (mNotifyCharacteristics.isEmpty()) {
                    mNotificationsAreEnabled = false;
                }
            }
        }
        if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristics.add(characteristic);
            Log.i(TAG, String.format("registerNotification -> In device %s the notification %s was registered.", getAddress(), characteristic.getUuid()));
            setCharacteristicNotification(characteristic, true);
            mNotificationsAreEnabled = true;
        } else {
            Log.w(TAG, String.format("registerNotification -> The application does not have permission to register for notifications in the characteristic %s.", characteristic.getUuid()));
        }
    }

    /**
     * Enables or disables the characteristic notification of a service.
     *
     * @param enabled <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    public void setNotificationsEnabled(final boolean enabled) {
        if (enabled) {
            mIsRequestingNotifications = true;
            registerDeviceCharacteristicNotifications();
        } else {
            disableNotifications();
        }
    }

    private void disableNotifications() {
        Log.d(TAG, String.format("disableNotifications -> Disabling notifications in device %s.", mPeripheral.getAddress()));
        if (mNotifyCharacteristics.isEmpty()) {
            return;
        }
        mIsRequestingNotifications = false;
        for (final BluetoothGattCharacteristic characteristic : mNotifyCharacteristics) {
            setCharacteristicNotification(characteristic, false);
        }
        mNotifyCharacteristics.clear();
        mNotificationsAreEnabled = false;
    }

    /**
     * This method should be called only from peripheral.
     * This method adds a listener to the list in case it's from the type specified in the ListenerType generics.
     *
     * @param listener the new candidate object for being a listener of this class.
     * @return <code>true</code> in case it's a valid listener, <code>false</code> otherwise.
     */
    public boolean registerNotificationListener(@NonNull final NotificationListener listener) {
        try {
            final ListenerType validListener = (ListenerType) listener;
            if (mListeners.contains(validListener)) {
                Log.w(TAG, String.format("registerNotificationListener -> Listener %s was already registered in peripheral %s.", validListener, mPeripheral.getAddress()));
            } else {
                Log.d(TAG, String.format("registerNotificationListener -> Registered %s notification in peripheral %s.", listener.getClass().getSimpleName(), mPeripheral.getAddress()));
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
     * This method should be called only from peripheral.
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

    /**
     * This method will normally only be called from {@link com.sensirion.libble.services.NotificationService}
     *
     * @param characteristic of notifications.
     * @param enabled        <code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    private void setCharacteristicNotification(@NonNull final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        Log.i(TAG, String.format("setCharacteristicNotification to %b in characteristic with UUID %s in device %s.", enabled, characteristic.getUuid(), getAddress()));
        mPeripheral.setCharacteristicNotification(characteristic, enabled);

        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(NOTIFICATION_DESCRIPTOR_UUID);
        if (enabled) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        }
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                mPeripheral.forceDescriptorWrite(descriptor, WAIT_BETWEEN_NOTIFICATION_REGISTER_REQUEST, MAX_NUMBER_NOTIFICATION_REGISTER_REQUEST);
            }
        });
    }

    /**
     * Registers the notification characteristics in case it's needed.
     */
    public abstract void registerDeviceCharacteristicNotifications();
}