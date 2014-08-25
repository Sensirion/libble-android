package com.sensirion.libble.bleservice;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.NotificationListener;
import com.sensirion.libble.Peripheral;

public abstract class NotificationService extends PeripheralService {

    private static final String TAG = NotificationService.class.getSimpleName();
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    protected NotificationService (Peripheral parent, BluetoothGattService bluetoothGattService){
        super(parent, bluetoothGattService);
    }

    protected void registerNotification(BluetoothGattCharacteristic gattCharacteristic) {
        final int charaProp = gattCharacteristic.getProperties();

        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            // If there is an active notification on a characteristic, clear
            // it first so it doesn't update the data field on
            // the user interface.
            if (mNotifyCharacteristic != null) {
                mNotifyCharacteristic = null;
                Log.i(TAG, "Cleared active notification of: " + gattCharacteristic.getUuid() + " in " + mParent.getAddress());
                setCharacteristicNotification(mNotifyCharacteristic, false);
            }
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mNotifyCharacteristic = gattCharacteristic;
            Log.i(TAG, "Register notification for: " + gattCharacteristic.getUuid() + " in " + mParent.getAddress());
            setCharacteristicNotification(gattCharacteristic, true);
        }
    }

    public abstract void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled);
    public abstract boolean onChangeNotification(BluetoothGattCharacteristic updatedCharacteristic);
    public abstract boolean registerNotificationListener(NotificationListener newListener);
    public abstract void unregisterNotificationListener(NotificationListener listenerForRemove);
}