package com.sensirion.libble.old.peripheral.sensor;

import java.util.HashMap;

public class TiSensors {

    private static HashMap<String, AbstractSensor<?>> SENSORS = new HashMap<String, AbstractSensor<?>>();

    static {
        final TiAccelerometerSensor accelerometerSensor = new TiAccelerometerSensor();
        final TiGyroscopeSensor gyroscopeSensor = new TiGyroscopeSensor();
        final TiHumiditySensor humiditySensor = new TiHumiditySensor();
        final TiKeysSensor keysSensor = new TiKeysSensor();
        final TiMagnetometerSensor magnetometerSensor = new TiMagnetometerSensor();
        final TiPressureSensor pressureSensor = new TiPressureSensor();
        final TiTemperatureSensor temperatureSensor = new TiTemperatureSensor();
        final TiTestSensor testSensor = new TiTestSensor();

        SENSORS.put(accelerometerSensor.getServiceUUID(), accelerometerSensor);
        SENSORS.put(gyroscopeSensor.getServiceUUID(), gyroscopeSensor);
        SENSORS.put(humiditySensor.getServiceUUID(), humiditySensor);
        SENSORS.put(keysSensor.getServiceUUID(), keysSensor);
        SENSORS.put(magnetometerSensor.getServiceUUID(), magnetometerSensor);
        SENSORS.put(pressureSensor.getServiceUUID(), pressureSensor);
        SENSORS.put(temperatureSensor.getServiceUUID(), temperatureSensor);
        SENSORS.put(testSensor.getServiceUUID(), testSensor);
    }

    public static AbstractSensor<?> getSensor(String uuid) {
        return SENSORS.get(uuid);
    }
}
