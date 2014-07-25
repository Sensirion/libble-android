package com.sensirion.libble.bleservice.impl;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.PeripheralService;

public class BatteryPeripheralService extends PeripheralService {

    private static final String TAG = BatteryPeripheralService.class.getSimpleName();

    public static final String UUID_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb";
    private static final String UUID_BATTERY_LEVEL_CHARACTERISTIC = "00002a19-0000-1000-8000-00805f9b34fb";

    private final BluetoothGattCharacteristic mBatteryLevelCharacteristic;

    private int mBatteryLevel = -1;

    public BatteryPeripheralService(Peripheral parent, BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mBatteryLevelCharacteristic = getCharacteristicFor(UUID_BATTERY_LEVEL_CHARACTERISTIC);
        parent.readCharacteristic(mBatteryLevelCharacteristic);
    }

    /**
     * Returns battery level as percentage
     *
     * @return Integer between 0 and 100 if the battery level could be read, -1 else
     */
    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGattCharacteristic characteristic) {
        if (mBatteryLevelCharacteristic.equals(characteristic)) {
            mBatteryLevel = mBatteryLevelCharacteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        }
        Log.d(TAG, "onCharacteristicRead() -> mBatteryLevel = " + mBatteryLevel);
        return super.onCharacteristicRead(characteristic);
    }

}
