package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.BleService;

public class BatteryService extends BleService<Integer> {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";

    //NAMED CHARACTERISTIC NAME
    public static final String READ_BATTERY_CHARACTERISTIC = BatteryService.class.getName() + ".getBattery";

    //FORCE READING CONSTANTS
    private static final int WAITING_TIME_BETWEEN_READS = 75;
    private static final int MAX_READ_TRIES = 3000 / WAITING_TIME_BETWEEN_READS;

    //UUIDs
    private static final String BATTERY_LEVEL_CHARACTERISTIC_UUID = "00002a19-0000-1000-8000-00805f9b34fb";

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mBatteryLevelCharacteristic;

    //BATTERY_SERVICE LEVEL
    private Integer mBatteryLevel = null;

    public BatteryService(Peripheral parent, BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mBatteryLevelCharacteristic = getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mBatteryLevelCharacteristic);
    }

    /**
     * Returns battery level as percentage
     *
     * @return {@link java.lang.Integer} between 0 and 100 if the battery level could be read, <code>null</code> otherwise.
     */
    public Integer getBatteryLevel() {
        if (mBatteryLevel == null) {
            mPeripheral.forceReadCharacteristic(mBatteryLevelCharacteristic, WAITING_TIME_BETWEEN_READS, MAX_READ_TRIES);
            return mBatteryLevel;
        }
        mPeripheral.readCharacteristic(mBatteryLevelCharacteristic);
        return mBatteryLevel;
    }

    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (mBatteryLevelCharacteristic.equals(characteristic)) {
            mBatteryLevel = mBatteryLevelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            Log.i(TAG, String.format("onCharacteristicUpdate -> Battery it's at %d%% in the device %s.", mBatteryLevel, getAddress()));
            return true;
        }
        return super.onCharacteristicUpdate(characteristic);
    }

    /**
     * Return the battery level in case it's known.
     *
     * @param characteristicName name of the characteristic.
     * @return an {@link java.lang.Integer} object with the battery level in case we know it - <code>null</code> otherwise.
     */
    @Override
    public Integer getCharacteristicValue(final String characteristicName) {
        if (characteristicName.equals(READ_BATTERY_CHARACTERISTIC)) {
            Log.d(TAG, String.format("getCharacteristicValue -> Requested battery level in peripheral %s.", mPeripheral.getAddress()));
            return getBatteryLevel();
        }
        return null;
    }
}