package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.BatteryListener;
import com.sensirion.libble.services.AbstractBleService;

import java.util.Iterator;

public class BatteryService extends AbstractBleService<BatteryListener> {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";

    //UUIDs
    private static final String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mBatteryLevelCharacteristic;

    //BATTERY_SERVICE LEVEL
    private Integer mBatteryLevel = null;

    public BatteryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mBatteryLevelCharacteristic = getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mBatteryLevelCharacteristic);
    }

    /**
     * Returns battery level as percentage. It asks for an updated battery level in a background thread.
     *
     * @return {@link java.lang.Integer} between 0 and 100 if the battery level could be read, <code>null</code> otherwise.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Integer getBatteryLevel() {
        mPeripheral.readCharacteristic(mBatteryLevelCharacteristic);
        return mBatteryLevel;
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param updatedCharacteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        if (mBatteryLevelCharacteristic.equals(updatedCharacteristic)) {
            mBatteryLevel = mBatteryLevelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.i(TAG, String.format("onCharacteristicUpdate -> Battery it's at %d%% in the device %s.", mBatteryLevel, getDeviceAddress()));
            notifyListeners();
            return true;
        }
        return super.onCharacteristicUpdate(updatedCharacteristic);
    }

    private void notifyListeners() {
        final Iterator<BatteryListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewBatteryLevel(mPeripheral, mBatteryLevel);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListeners -> An error was thrown when notifying the listeners -> ", e);
                iterator.remove();
            }
        }
    }

    @Override
    public boolean isServiceReady() {
        return mBatteryLevel != null;
    }

    @Override
    public void synchronizeService() {
        if (mBatteryLevel == null) {
            getBatteryLevel();
        }
    }

    /**
     * Registers the notification characteristics in case it's needed.
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        registerNotification(mBatteryLevelCharacteristic);
    }
}