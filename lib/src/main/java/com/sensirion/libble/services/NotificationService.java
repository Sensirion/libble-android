package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.peripherals.Peripheral;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class NotificationService<CharacteristicValueType, ListenerType extends NotificationListener> extends PeripheralService<CharacteristicValueType> {

    private static final String TAG = NotificationService.class.getSimpleName();

    protected final List<ListenerType> mListeners = Collections.synchronizedList(new LinkedList<ListenerType>());
    protected boolean mNotificationsAreEnabled = false;
    protected boolean mIsRequestingNotifications = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

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
            if (mNotifyCharacteristic != null) {
                mNotifyCharacteristic = null;
                Log.i(TAG, String.format("Cleared active notification of UUID %s in peripheral with address: %s", gattCharacteristic.getUuid(), mPeripheral.getAddress()));
                setCharacteristicNotification(mNotifyCharacteristic, false);
                mNotificationsAreEnabled = false;
            }
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            Log.i(TAG, "Register notification for: " + gattCharacteristic.getUuid() + " in " + mPeripheral.getAddress());
            setCharacteristicNotification(gattCharacteristic, true);
            mNotificationsAreEnabled = true;
        }
    }

    /**
     * Enables or disables the characteristic notification of a service.
     *
     * @param enabled <<code>true</code> if notifications have to be enabled - <code>false</code> otherwise.
     */
    public void setNotificationsEnabled(final boolean enabled) {
        if (enabled) {
            mIsRequestingNotifications = true;
            registerNotification(readNotificationCharacteristic());
        } else {
            disableNotifications();
        }
    }

    private void disableNotifications() {
        if (mNotifyCharacteristic == null) {
            return;
        }
        mIsRequestingNotifications = false;
        setCharacteristicNotification(mNotifyCharacteristic, false);
        mNotifyCharacteristic = null;
        mNotificationsAreEnabled = false;
    }

    /**
     * This method should be called only from peripheral.
     * This method adds a listener to the list in case it's a HumigadgetListener.
     *
     * @param listener the new candidate object for being a listener of this class.
     * @return <code>true</code> in case it's a valid listener, <code>false</code> otherwise.
     */
    public boolean registerNotificationListener(final NotificationListener listener) {
        final ListenerType validListener;
        try {
            validListener = (ListenerType) listener;
        } catch (ClassCastException e) {
            return false;
        }
        mListeners.add(validListener);
        Log.d(TAG, String.format("Registered %s notification in peripheral %s.", listener.getClass().getSimpleName(), mPeripheral.getAddress()));

        if (mIsRequestingNotifications && mListeners.size() == 1) {
            setNotificationsEnabled(true);
        }
        return true;
    }


    /**
     * This method should be called only from peripheral.
     * This method unregister a listener from the list in case of having it.
     *
     * @param listener the listener that doesn't want to hear from a device anymore.
     */
    public void unregisterNotificationListener(final NotificationListener listener) {
        if (listener == null) {
            Log.e(TAG, "Received a null listener in " + this.getClass().getSimpleName());
            return;
        }
        mListeners.remove(listener);
        if (mListeners.isEmpty() && mNotificationsAreEnabled) {
            setNotificationsEnabled(false);
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
     * Gets the notification characteristic of the method.
     *
     * @return {@link android.bluetooth.BluetoothGattCharacteristic} of the notification.
     */
    public abstract BluetoothGattCharacteristic readNotificationCharacteristic();
}