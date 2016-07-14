package com.sensirion.libsmartgadget.utils;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public final class LittleEndianExtractor {
    private LittleEndianExtractor() {
    }

    /**
     * Converts a long value into a Little Endian byte array.
     *
     * @param longValue that is going to be converted into a byte array.
     * @return array of {@link Byte} ordered using the little endian convention.
     */
    public static byte[] convertToByteArray(final long longValue) {
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

    private static byte[] reverseByteArray(@NonNull final byte[] array) {
        final byte[] reversedArray = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            reversedArray[i] = array[array.length - i - 1];
        }
        return reversedArray;
    }

    /**
     * Extracts an integer from a byte array.
     *
     * @param value the byte array from which to extract the integer
     * @return the extracted integer
     */
    public static int extractInteger(@NonNull final byte[] value) {
        int[] extractedValue = new int[1];
        ByteBuffer.wrap(value).order(LITTLE_ENDIAN).asIntBuffer().get(extractedValue);
        return extractedValue[0];
    }

    /**
     * Extracts a long from a byte array.
     *
     * @param value the byte array from which to extract the long.
     * @return the extracted long.
     */
    public static long extractLong(@NonNull final byte[] value) {
        long[] extractedValue = new long[1];
        ByteBuffer.wrap(value).order(LITTLE_ENDIAN).asLongBuffer().get(extractedValue);
        return extractedValue[0];
    }

    /**
     * Extracts a Little Endian Float from a byte array.
     *
     * @param value  value that contains the little endian Float.
     * @param offset of the float value inside the byte array.
     * @return {@link Float} with the extracted value.
     */
    public static float extractFloat(@NonNull final byte[] value, final int offset) {
        final float[] wrappedFloatValue = new float[1];
        final byte[] valueBuffer = new byte[4];
        System.arraycopy(value, offset, valueBuffer, 0, 4);
        ByteBuffer.wrap(valueBuffer).order(LITTLE_ENDIAN).asFloatBuffer().get(wrappedFloatValue);
        return wrappedFloatValue[0];
    }

    /**
     * Extracts a short from a byte array.
     *
     * @param value the byte array from which to extract the short.
     * @return the extracted short.
     */
    public static int extractShort(@NonNull final byte[] value) {
        short[] extractedValue = new short[1];
        ByteBuffer.wrap(value).order(LITTLE_ENDIAN).asShortBuffer().get(extractedValue);
        return extractedValue[0];
    }
}