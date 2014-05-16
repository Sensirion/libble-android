package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

public class TiSensorUtils {

    private TiSensorUtils() {
    }

    /**
     * Gyroscope, Magnetometer, Barometer, IR temperature all store 16 bit two's
     * complement values in the awkward format LSB MSB, which cannot be directly
     * parsed as getIntValue(FORMAT_SINT16, offset) because the bytes are stored
     * in the "wrong" direction. This function extracts these 16 bit two's
     * complement values.
     */
    public static Integer shortSignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(FORMAT_UINT8, offset);
        if (lowerByte == null)
            return 0;
        // Note: interpret MSB as signed.
        Integer upperByte = c.getIntValue(FORMAT_SINT8, offset + 1);
        if (upperByte == null)
            return 0;

        return (upperByte << 8) + lowerByte;
    }

    public static Integer shortUnsignedAtOffset(BluetoothGattCharacteristic c, int offset) {
        Integer lowerByte = c.getIntValue(FORMAT_UINT8, offset);
        if (lowerByte == null)
            return 0;
        // Note: interpret MSB as unsigned.
        Integer upperByte = c.getIntValue(FORMAT_UINT8, offset + 1);
        if (upperByte == null)
            return 0;

        return (upperByte << 8) + lowerByte;
    }
}
