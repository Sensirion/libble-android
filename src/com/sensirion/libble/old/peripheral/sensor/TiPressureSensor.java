package com.sensirion.libble.old.peripheral.sensor;

import android.bluetooth.BluetoothGattCharacteristic;

import com.sensirion.libble.BluetoothGattExecutor;

import static java.lang.Math.pow;

public class TiPressureSensor extends AbstractSensor<Double> {

    private static final String TAG = TiPressureSensor.class.getSimpleName();

    private static final String UUID_SERVICE = "f000aa40-0451-4000-b000-000000000000";
    private static final String UUID_DATA = "f000aa41-0451-4000-b000-000000000000";
    private static final String UUID_CONFIG = "f000aa42-0451-4000-b000-000000000000";
    private static final String UUID_CALIBRATION = "f000aa43-0451-4000-b000-000000000000";

    private static final byte[] CALIBRATION_DATA = new byte[]{
            2
    };

    private final int[] calibration = new int[8];

    TiPressureSensor() {
        super();
    }

    @Override
    public String getName() {
        return TAG;
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
    public String getCharacteristicName(String uuid) {
        if (UUID_CALIBRATION.equals(uuid))
            return getName() + " Calibration";
        return super.getCharacteristicName(uuid);
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGattCharacteristic c) {
        super.onCharacteristicRead(c);

        if (!c.getUuid().toString().equals(UUID_CALIBRATION))
            return false;

        for (int i = 0; i < 4; ++i) {
            calibration[i] = TiSensorUtils.shortUnsignedAtOffset(c, i * 2);
            calibration[i + 4] = TiSensorUtils.shortSignedAtOffset(c, 8 + i * 2);
        }

        return true;
    }

    @Override
    public String getDataString() {
        final double data = getData();
        return "" + data;
    }

    @Override
    public BluetoothGattExecutor.ServiceAction[] enable(boolean enable) {
        if (enable) {
            return new BluetoothGattExecutor.ServiceAction[]{
                    write(UUID_CONFIG, CALIBRATION_DATA),
                    read(UUID_CALIBRATION),
                    write(getConfigUUID(), getConfigValues(enable)),
                    notify(enable)
            };
        } else {
            return super.enable(enable);
        }
    }

    @Override
    public Double parse(BluetoothGattCharacteristic c) {
        // c holds the calibration coefficients

        /* Temperature raw value from sensor */
        final Integer t_r = TiSensorUtils.shortSignedAtOffset(c, 0);
        
        /* Pressure raw value from sensor */
        final Integer p_r = TiSensorUtils.shortUnsignedAtOffset(c, 2);
        
        /* Temperature actual value in unit centi degrees celsius */
//        final Double t_a = (100 * (calibration[0] * t_r / pow(2, 8) + calibration[1] * pow(2, 6)))
//                / pow(2, 16);

        /* Interim value in calculation */
        final Double S = calibration[2] + calibration[3] * t_r / pow(2, 17)
                + ((calibration[4] * t_r / pow(2, 15)) * t_r) / pow(2, 19);

        /* Interim value in calculation */
        final Double O = calibration[5] * pow(2, 14) + calibration[6] * t_r / pow(2, 3)
                + ((calibration[7] * t_r / pow(2, 15)) * t_r) / pow(2, 4);

        /* Pressure actual value in unit Pascal */
        final Double p_a = (S * p_r + O) / pow(2, 14);

        return p_a;
    }

}
