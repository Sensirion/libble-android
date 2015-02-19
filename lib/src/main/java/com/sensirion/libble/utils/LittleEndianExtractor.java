package com.sensirion.libble.utils;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public abstract class LittleEndianExtractor {

    /**
     * Extracts a Little Endian byte array from a long value.
     *
     * @param longValue that is going to be converted into a byte array.
     * @return array of {@link java.lang.Byte} ordered using the little endian convention.
     */
    public static byte[] extractLittleEndianByteArrayFromLong(final long longValue) {
        final byte[] byteArray = longToByteArray(longValue);
        if (ByteOrder.nativeOrder() == LITTLE_ENDIAN) {
            return reverseByteArray(byteArray);
        }
        return byteArray;
    }

    private static byte[] longToByteArray(final long value) {
        return new byte[]{
                (byte) (value >> 56),
                (byte) (value >> 48),
                (byte) (value >> 40),
                (byte) (value >> 32),
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) value
        };
    }

    /**
     * Extracts a Little Endian byte array from an integer value.
     *
     * @param intValue that is going to be converted into a byte array.
     * @return array of {@link java.lang.Byte} ordered using the little endian convention.
     */
    public static byte[] extractLittleEndianByteArrayFromInteger(final int intValue) {
        final byte[] byteArray = intToByteArray(intValue);
        if (ByteOrder.nativeOrder() == LITTLE_ENDIAN) {
            return reverseByteArray(byteArray);
        }
        return byteArray;
    }

    private static byte[] intToByteArray(final int value) {
        return new byte[]{
                (byte) (value >> 24),
                (byte) (value >> 16),
                (byte) (value >> 8),
                (byte) (value)
        };
    }

    private static byte[] reverseByteArray(@NonNull final byte[] array) {
        final byte[] reversedArray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversedArray[i] = array[array.length - i - 1];
        }
        return reversedArray;
    }

    /**
     * Extracts a Little Endian Integer from a characteristic.
     * NOTE: Java does not java unsigned long values, data will be extracted in both cases into a signed integer attribute.
     *
     * @param characteristic that contains the a 32 bit integer.
     * @return {@link java.lang.Float} with the extracted value.
     */
    public static int extractLittleEndianIntegerFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic) {
        int[] extractedValue = new int[1];
        ByteBuffer.wrap(characteristic.getValue()).order(LITTLE_ENDIAN).asIntBuffer().get(extractedValue);
        return extractedValue[0];
    }

    /**
     * Extracts a Little Endian Long from a characteristic.
     * NOTE: Java does not java unsigned long values, data will be extracted in both cases into a signed long attribute.
     *
     * @param characteristic that contains the little 64 bit integer.
     * @return {@link java.lang.Float} with the extracted value.
     */
    public static long extractLittleEndianLongFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic) {
        long[] extractedValue = new long[1];
        ByteBuffer.wrap(characteristic.getValue()).order(LITTLE_ENDIAN).asLongBuffer().get(extractedValue);
        return extractedValue[0];
    }

    /**
     * Extracts a Little Endian Float from a characteristic.
     *
     * @param characteristic that contains the little endian Float.
     * @return {@link java.lang.Float} with the extracted value.
     */
    public static float extractLittleEndianFloatFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic) {
        return extractLittleEndianFloatFromCharacteristic(characteristic, 0);
    }

    /**
     * Extracts a Little Endian Float from a characteristic.
     *
     * @param characteristic that contains the little endian Float.
     * @param offset         of the float value inside the characteristic.
     * @return {@link java.lang.Float} with the extracted value.
     */
    public static float extractLittleEndianFloatFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic, final int offset) {
        final float[] wrappedFloatValue = new float[1];
        final byte[] valueBuffer = new byte[4];
        System.arraycopy(characteristic.getValue(), offset, valueBuffer, 0, 4);
        ByteBuffer.wrap(valueBuffer).order(LITTLE_ENDIAN).asFloatBuffer().get(wrappedFloatValue);
        return wrappedFloatValue[0];
    }

    /**
     * Extracts an unsigned 16 bit short from a bluetooth characteristic.
     *
     * @param characteristic that contains a 32 bit little endian integer.
     * @param offset         number of bytes of offset.
     * @return {@link java.lang.Integer} with the short unsigned value.
     */
    public static Integer extractUnsignedShortFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic, final int offset) {
        final int lowerByte = characteristic.getIntValue(FORMAT_UINT8, offset);
        final int upperByte = characteristic.getIntValue(FORMAT_UINT8, offset + 1); // Note: interpret MSB as unsigned.
        return (upperByte << 8) + lowerByte;
    }

    /**
     * Extracts a signed 16 bit short from a bluetooth characteristic.
     *
     * @param characteristic that contains a 32 bit little endian integer.
     * @param offset         number of bytes of offset.
     * @return {@link java.lang.Integer} with the short unsigned value.
     */
    public static Integer extractSignedShortFromCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic, final int offset) {
        final int lowerByte = characteristic.getIntValue(FORMAT_UINT8, offset);
        final int upperByte = characteristic.getIntValue(FORMAT_SINT8, offset + 1); // Note: interpret MSB as signed.
        return (upperByte << 8) + lowerByte;
    }
}