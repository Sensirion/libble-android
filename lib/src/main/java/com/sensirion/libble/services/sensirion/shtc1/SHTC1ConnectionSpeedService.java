package com.sensirion.libble.services.sensirion.shtc1;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractBleService;

public class SHTC1ConnectionSpeedService extends AbstractBleService {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000fa10-0000-1000-8000-00805f9b34fb";

    //FORCE READING CONSTANTS
    private static final int MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES = 75;
    private static final int TIME_BETWEEN_WRITE_REQUESTS = 1800; //1.8 seconds
    private static final int MAX_WRITE_TRIES = TIME_BETWEEN_WRITE_REQUESTS / MAX_WAIT_TIME_BETWEEN_READ_OR_WRITE_TRIES;

    //UUIDs
    private static final String NOTIFICATION_CHARACTERISTIC_UUID = "0000fa11-0000-1000-8000-00805f9b34fb";

    //CHARACTERISTICS
    private final BluetoothGattCharacteristic mNotificationSpeedCharacteristic;

    //NOTIFICATION SPEED LEVEL
    private Byte mNotificationSpeedLevel = null;

    public SHTC1ConnectionSpeedService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService);
        mNotificationSpeedCharacteristic = getCharacteristic(NOTIFICATION_CHARACTERISTIC_UUID);
    }

    /**
     * Reads the notification speed value.
     *
     * @return <code>true</code> if the notification speed is high. <code>false</code> if it's slow. <code>null</code> if it's not known.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Boolean isNotificationSpeedValueSetHigh() {
        //If it doesn't have the speed value it asks for it.
        if (mNotificationSpeedLevel == null) {
            mPeripheral.readCharacteristic(mNotificationSpeedCharacteristic);
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
     * Sets the notification speed using {@link SHTC1ConnectionSpeedService.CONNECTION_SPEED}.
     *
     * @param connectionSpeed that wants to be set in the device.
     * @return <code>true</code> if the connection speed was set in the device - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean setNotificationSpeed(@NonNull final CONNECTION_SPEED connectionSpeed) {
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
        if (!isServiceReady()) {
            Log.e(TAG, "setConnectionSpeed -> Service is not synchronized. (HINT -> Use synchronize()).");
            return false;
        }
        final byte notificationSpeedRequested = (connectionSpeed) ? CONNECTION_SPEED.HIGH.value : CONNECTION_SPEED.LOW.value;
        if (notificationSpeedRequested == mNotificationSpeedLevel) {
            Log.w(TAG, String.format("In device %s the notification speed level was already set to %s speed.", getDeviceAddress(), ((connectionSpeed) ? "HIGH" : "LOW")));
            return true;
        }
        mNotificationSpeedCharacteristic.setValue(notificationSpeedRequested, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (mPeripheral.forceWriteCharacteristic(mNotificationSpeedCharacteristic, TIME_BETWEEN_WRITE_REQUESTS, MAX_WRITE_TRIES)) {
            if (mPeripheral.forceReadCharacteristic(mNotificationSpeedCharacteristic, TIME_BETWEEN_WRITE_REQUESTS, MAX_WRITE_TRIES)) {
                return mNotificationSpeedLevel.equals(notificationSpeedRequested);
            }
        }
        return false;
    }

    @Override
    public boolean isServiceReady() {
        return mNotificationSpeedLevel != null;
    }

    @Override
    public void synchronizeService() {
        if (mNotificationSpeedLevel == null) {
            mPeripheral.readCharacteristic(mNotificationSpeedCharacteristic);
            Log.w(TAG, "synchronizeService -> Service is not ready, asking for the remaining values in the background.");
        }
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