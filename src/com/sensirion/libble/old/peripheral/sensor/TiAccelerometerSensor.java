package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.sensirion.libble.BluetoothGattExecutor;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_SINT8;

public class TiAccelerometerSensor extends AbstractSensor<float[]> implements PeriodicalSensorI {

    private static final String TAG = TiAccelerometerSensor.class.getSimpleName();

    private static final String UUID_SERVICE = "f000aa10-0451-4000-b000-000000000000";
    private static final String UUID_DATA = "f000aa11-0451-4000-b000-000000000000";
    private static final String UUID_CONFIG = "f000aa12-0451-4000-b000-000000000000";
    private static final String UUID_PERIOD = "f000aa13-0451-4000-b000-000000000000";

    private static final int PERIOD_MIN = 10;
    private static final int PERIOD_MAX = 255;

    private int mPeriod = 100;

    TiAccelerometerSensor() {
        super();
    }

    @Override
    public int getMinPeriod() {
        return PERIOD_MIN;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public int getMaxPeriod() {
        return PERIOD_MAX;
    }

    @Override
    public String getServiceUUID() {
        return UUID_SERVICE;
    }

    @Override
    public String getDataUUID() {
        return UUID_DATA;
    }

    @Override
    public String getConfigUUID() {
        return UUID_CONFIG;
    }

    @Override
    public boolean isConfigUUID(String uuid) {
        if (uuid.equals(UUID_PERIOD))
            return true;
        return super.isConfigUUID(uuid);
    }

    @Override
    public String getCharacteristicName(String uuid) {
        if (UUID_PERIOD.equals(uuid))
            return getName() + " Period";
        return super.getCharacteristicName(uuid);
    }

    @Override
    public String getDataString() {
        final float[] data = getData();
        return "x=" + data[0] + "\ny=" + data[1] + "\nz=" + data[2];
    }

    @Override
    public int getPeriod() {
        return mPeriod;
    }

    @Override
    public void setPeriod(int period) {
        this.mPeriod = period;
    }

    @Override
    public BluetoothGattExecutor.ServiceAction update() {
        return write(UUID_PERIOD, new byte[]{
                (byte) mPeriod
        });
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGattCharacteristic c) {
        super.onCharacteristicRead(c);

        if (!c.getUuid().toString().equals(UUID_PERIOD))
            return false;

        mPeriod = TiSensorUtils.shortUnsignedAtOffset(c, 0);
        return true;
    }

    @Override
    public float[] parse(final BluetoothGattCharacteristic c) {
        /*
         * The accelerometer has the range [-2g, 2g] with unit (1/64)g. To
         * convert from unit (1/64)g to unit g we divide by 64. (g = 9.81 m/s^2)
         * The z value is multiplied with -1 to coincide with how we have
         * arbitrarily defined the positive y direction. (illustrated by the
         * apps accelerometer image)
         */

        Integer x = c.getIntValue(FORMAT_SINT8, 0);
        Integer y = c.getIntValue(FORMAT_SINT8, 1);
        Integer z = c.getIntValue(FORMAT_SINT8, 2) * -1;

        double scaledX = x / 64.0;
        double scaledY = y / 64.0;
        double scaledZ = z / 64.0;

        return new float[]{
                (float) scaledX, (float) scaledY, (float) scaledZ
        };
    }
}
