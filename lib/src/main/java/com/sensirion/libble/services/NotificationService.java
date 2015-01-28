package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class NotificationService<CharacteristicValueType, ListenerType extends NotificationListener> extends BleService<CharacteristicValueType> {

    private static final String TAG = NotificationService.class.getSimpleName();

    protected final List<ListenerType> mListeners = Collections.synchronizedList(new LinkedList<ListenerType>());

    protected boolean mNotificationsAreEnabled = false;
    protected boolean mIsRequestingNotifications = false;

    protected Set<BluetoothGattCharacteristic> mNotifyCharacteristics = Collections.synchronizedSet(new HashSet<BluetoothGattCharacteristic>());

    protected NotificationService(final Peripheral parent, final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
    }

    protected void registerNotification(final BluetoothGattCharacteristic gattCharacteristic) {
        if (mListeners.isEmpty()) {
            return;
        }
        final int charaProp = gattCharacteristic.getProperties();

        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on the user interface.
            if (mNotifyCharacteristics.contains(gattCharacteristic)) {
                mNotifyCharacteristics.remove(gattCharacteristic);
                Log.i(TAG, String.format("registerNotification -> Cleared active notification of UUID %s in peripheral with address: %s", gattCharacteristic.getUuid(), mPeripheral.getAddress()));
                setCharacteristicNotification(gattCharacteristic, false);
                if (mNotifyCharacteristics.size() == 0) {
                    mNotificationsAreEnabled = false;
                }
            }
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristics.add(gattCharacteristic);
            Log.i(TAG, String.format("registerNotification -> In device %s the notification %s was registered.", getDeviceAddress(), gattCharacteristic.getUuid()));
            setCharacteristicNotification(gattCharacteristic, true);
            mNotificationsAreEnabled = true;
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
            registerNotificationCharacteristics();
        } else {
            disableNotifications();
        }
    }

    private void disableNotifications() {
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
     * This method should be called only from peripheral.
     * This method unregister a listener from the list in case of having it.
     *
     * @param listener the listener that doesn't want to hear from a device anymore.
     */
    public void unregisterNotificationListener(@NonNull final NotificationListener listener) {
        try {
            mListeners.remove(listener);
        } catch (final ClassCastException cce) {
            Log.e(TAG, "unregisterNotificationListener -> The following error was produced when trying to remove the notification listener -> ", cce);
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
    public abstract void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled);

    /**
     * This method checks if this service is able to handle the characteristic.
     * In case it's able to manage the characteristic it reads it and advice to this service listeners.
     *
     * @param updatedCharacteristic characteristic with new values coming from Peripheral.
     * @return <code>true</code> in case it managed correctly the new data - <code>false</code> otherwise.
     */
    public abstract boolean onChangeNotification(final BluetoothGattCharacteristic updatedCharacteristic);

    /**
     * Registers the notification characteristics in case it's needed.
     */
    public abstract void registerNotificationCharacteristics();
}