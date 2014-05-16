package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

public class TiGyroscopeSensor extends AbstractSensor<float[]> {

    private static final String TAG = TiGyroscopeSensor.class.getSimpleName();

    TiGyroscopeSensor() {
        super();
    }

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public String getServiceUUID() {
        return "f000aa50-0451-4000-b000-000000000000";
    }

    @Override
    public String getDataUUID() {
        return "f000aa51-0451-4000-b000-000000000000";
    }

    @Override
    public String getConfigUUID() {
        return "f000aa52-0451-4000-b000-000000000000";
    }

    // TODO: there is period service

    @Override
    public String getDataString() {
        final float[] data = getData();
        return "x=" + data[0] + "\ny=" + data[1] + "\nz=" + data[2];
    }

    @Override
    protected byte[] getConfigValues(boolean enable) {
        return new byte[]{
                (byte) (enable ? 7 : 0)
        };
    }

    @Override
    public float[] parse(BluetoothGattCharacteristic c) {
        // NB: x,y,z has a weird order.
        float y = TiSensorUtils.shortSignedAtOffset(c, 0) * (500f / 65536f) * -1;
        float x = TiSensorUtils.shortSignedAtOffset(c, 2) * (500f / 65536f);
        float z = TiSensorUtils.shortSignedAtOffset(c, 4) * (500f / 65536f);

        return new float[]{
                x, y, z
        };
    }
}
