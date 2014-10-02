package com.sensirion.libble.bleservice.implementations.humigadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.sensirion.libble.Peripheral;
import com.sensirion.libble.bleservice.PeripheralService;

import java.util.Arrays;
import java.util.List;

public class HumigadgetConnectionSpeedService extends PeripheralService<Boolean> {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000fa10-0000-1000-8000-00805f9b34fb";

    //CLASS TAGS
    private static final String TAG = HumigadgetConnectionSpeedService.class.getSimpleName();
    private static final String PREFIX = HumigadgetConnectionSpeedService.class.getName();

    //CHARACTERISTICS value NAMES
    public static final String READ_NOTIFICATION_SPEED_NAME = PREFIX + ".getNotificationSpeed";
    public static final String WRITE_NOTIFICATION_LOW_SPEED_NAME = PREFIX + ".setNotificationSpeedSlow";
    public static final String WRITE_NOTIFICATION_HIGH_SPEED_NAME = PREFIX + ".setNotificationSpeedFast";
    public static final String WRITE_NOTIFICATION_DEFAULT_SPEED_NAME = PREFIX + ".setNotificationSpeedToDefault";
    public static final List<String> CHARACTERISTICS_VALUE_LIST =
            Arrays.asList(READ_NOTIFICATION_SPEED_NAME,
                    WRITE_NOTIFICATION_LOW_SPEED_NAME,
                    WRITE_NOTIFICATION_HIGH_SPEED_NAME,
                    WRITE_NOTIFICATION_DEFAULT_SPEED_NAME);

    //FORCE READING CONSTANTS
    private static final int MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES = 75;
    private static final int MAX_TIME_FOR_READ_OR_WRITE = 1800; //1.8 seconds
    private static final int MAX_READ_TRIES = MAX_TIME_FOR_READ_OR_WRITE / MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES;

    //UUIDs
    private static final String NOTIFICATION_CHARACTERISTIC_UUID = "0000fa11-0000-1000-8000-00805f9b34fb";

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mNotificationSpeedCharacteristic;

    //NOTIFICATION SPEED LEVEL
    private Byte mNotificationSpeedLevel = null;

    public HumigadgetConnectionSpeedService(final Peripheral peripheral, final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService);
        mNotificationSpeedCharacteristic = getCharacteristicFor(NOTIFICATION_CHARACTERISTIC_UUID);
    }

    /**
     * Returns the characteristic value.
     *
     * @param characteristicName of the characteristic wanted.
     * @return <code>true</code> if the speed is high - <code>false</code> if the speed is slow - <code>null</code> if it's not known.
     */
    @Override
    public Boolean getCharacteristicValue(final String characteristicName) {
        Log.i(TAG, String.format("Requested battery level in peripheral %s.", mPeripheral.getAddress()));
        if (characteristicName.equals(READ_NOTIFICATION_SPEED_NAME)) {
            return isNotificationSpeedValueSetHigh();
        }
        if (characteristicName.equals(WRITE_NOTIFICATION_LOW_SPEED_NAME)) {
            return setConnectionSpeed(false);
        }
        if (characteristicName.equals(WRITE_NOTIFICATION_HIGH_SPEED_NAME)
                || characteristicName.equals(WRITE_NOTIFICATION_DEFAULT_SPEED_NAME)) {
            return setConnectionSpeed(true);
        }
        return null;
    }

    /**
     * Reads the notification speed value.
     *
     * @return <code>true</code> if the notification speed is high. <code>false</code> if it's slow. <code>null</code> if it's not known.
     */
    public Boolean isNotificationSpeedValueSetHigh() {
        //If it doesn't have the speed value it asks for it.
        if (mNotificationSpeedLevel == null) {
            mPeripheral.forceReadCharacteristic(mNotificationSpeedCharacteristic, MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES, MAX_READ_TRIES);
            mPeripheral.cleanCharacteristicCache();
            if (mNotificationSpeedLevel == null) {
                return null;
            }
            return CONNECTION_SPEED.HIGH.value == mNotificationSpeedLevel;
        } else {
            mPeripheral.readCharacteristic(mNotificationSpeedCharacteristic);
            return CONNECTION_SPEED.HIGH.value == mNotificationSpeedLevel;
        }
    }

    /**
     * Sets the notification speed using {@link HumigadgetConnectionSpeedService.CONNECTION_SPEED}.
     *
     * @param connectionSpeed that wants to be set in the device.
     * @return <code>true</code> if the connection speed was set in the device - <code>false</code> otherwise.
     */
    public boolean setNotificationSpeed(final CONNECTION_SPEED connectionSpeed) {
        switch (connectionSpeed) {
            case HIGH:
                return setConnectionSpeed(true);
            case LOW:
                return setConnectionSpeed(false);
            case DEFAULT:
                return setConnectionSpeed(true);
        }
        return false;
    }

    /**
     * Sets the connection speed in the device
     *
     * @param connectionSpeed <code>true</code> for high notification speed - <code>false</code> for low notification speed.
     * @return <code>true</code> if the notification speed was set correctly - <code>false</code> otherwise.
     */
    public boolean setConnectionSpeed(final boolean connectionSpeed) {
        if (isConnectionSpeedKnown()) {
            final byte notificationSpeedRequested = (connectionSpeed) ? CONNECTION_SPEED.HIGH.value : CONNECTION_SPEED.LOW.value;
            if (notificationSpeedRequested == mNotificationSpeedLevel) {
                Log.w(TAG, String.format("In device %s the notification speed level was already set to %s speed.", mPeripheral.getAddress(), ((connectionSpeed) ? "HIGH" : "LOW")));
                return true;
            }
            mNotificationSpeedCharacteristic.setValue(notificationSpeedRequested, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            if (mPeripheral.forceWriteCharacteristic(mNotificationSpeedCharacteristic, MAX_TIME_FOR_READ_OR_WRITE, MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES)) {
                if (mPeripheral.forceReadCharacteristic(mNotificationSpeedCharacteristic, MAX_TIME_FOR_READ_OR_WRITE, MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES)) {
                    return mNotificationSpeedLevel.equals(notificationSpeedRequested);
                }
            }
            return false;
        }
        return false;
    }

    private boolean isConnectionSpeedKnown() {
        if (mNotificationSpeedLevel == null) {
            if (isNotificationSpeedValueSetHigh() == null) {
                return false;
            }
        }
        return true;
    }

    //CONNECTION SPEED SETTINGS
    public static enum CONNECTION_SPEED {
        LOW((byte) 1),
        HIGH((byte) 0),
        DEFAULT(HIGH.value);

        private final byte value;

        private CONNECTION_SPEED(final byte value) {
            this.value = value;
        }
    }
}