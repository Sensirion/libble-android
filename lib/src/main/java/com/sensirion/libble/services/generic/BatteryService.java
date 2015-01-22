package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.BleService;

public class BatteryService extends BleService<Integer> {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180f-0000-1000-8000-00805f9b34fb";
    //NAMED CHARACTERISTIC NAME
    public static final String READ_BATTERY_CHARACTERISTIC_VALUE_NAME = BatteryService.class.getName() + ".getBattery";
    //FORCE READING CONSTANTS
    private static final int MAX_WAITING_TIME_BETWEEN_READS_MS = 75;
    private static final int MAX_READ_TRIES = 3000 / MAX_WAITING_TIME_BETWEEN_READS_MS;
    //CLASS TAG
    private static final String TAG = BatteryService.class.getSimpleName();
    //UUIDs
    private static final String UUID_BATTERY_LEVEL_CHARACTERISTIC = "00002a19-0000-1000-8000-00805f9b34fb";
    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mBatteryLevelCharacteristic;
    //BATTERY_SERVICE LEVEL
    private Integer mBatteryLevel = null;

    public BatteryService(Peripheral parent, BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mBatteryLevelCharacteristic = getCharacteristicFor(UUID_BATTERY_LEVEL_CHARACTERISTIC);
        parent.readCharacteristic(mBatteryLevelCharacteristic);
    }

    /**
     * Returns battery level as percentage
     *
     * @return {@link java.lang.Integer} between 0 and 100 if the battery level could be read, <code>null</code> otherwise.
     */
    public Integer getBatteryLevel() {
        if (mBatteryLevel == null) {
            return getCharacteristicValue(READ_BATTERY_CHARACTERISTIC_VALUE_NAME);
        }
        mPeripheral.readCharacteristic(mBatteryLevelCharacteristic);
        return mBatteryLevel;
    }

    @Override
    public boolean onCharacteristicRead(final BluetoothGattCharacteristic characteristic) {
        if (mBatteryLevelCharacteristic.equals(characteristic)) {
            mBatteryLevel = mBatteryLevelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        }
        Log.i(TAG, String.format("Battery it's at %d%s in the device %s.", mBatteryLevel, "%", mPeripheral.getAddress()));
        return super.onCharacteristicRead(characteristic);
    }

    /**
     * Return the battery level in case it's known.
     *
     * @param characteristicName name of the characteristic.
     * @return an {@link java.lang.Integer} object with the battery level in case we know it - <code>null</code> otherwise.
     */
    @Override
    public Integer getCharacteristicValue(final String characteristicName) {
        Log.d(TAG, String.format("Requested battery level in peripheral %s.", mPeripheral.getAddress()));
        if (characteristicName.equals(READ_BATTERY_CHARACTERISTIC_VALUE_NAME)) {
            //If it doesn't have the battery level.
            if (mBatteryLevel == null) {
                mPeripheral.forceReadCharacteristic(mBatteryLevelCharacteristic, MAX_WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
                mPeripheral.cleanCharacteristicCache();
                return mBatteryLevel;
            } else {
                mPeripheral.readCharacteristic(mBatteryLevelCharacteristic);
                return mBatteryLevel;
            }
        }
        return null;
    }
}