package com.sensirion.libble.services.ti;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractRHTService;
import com.sensirion.libble.services.BleServiceSynchronizationPriority;
import com.sensirion.libble.utils.LittleEndianExtractor;
import com.sensirion.libble.utils.RHTDataPoint;

public class SensorTagRHTService extends AbstractRHTService {

    //SERVICE UUID
    public static final String SERVICE_UUID = "f000aa20-0451-4000-b000-000000000000";

    //SENSOR NAME
    private static final String SENSOR_NAME = "Sensirion SHT21";

    //CHARACTERISTIC UUID
    private static final String RHT_CHARACTERISTIC_UUID = "f000aa21-0451-4000-b000-000000000000";
    private static final String CONFIG_CHARACTERISTIC_UUID = "f000aa22-0451-4000-b000-000000000000";

    //FORCE WRITING ATTRIBUTES
    private static final int TIME_BETWEEN_REQUEST_MS = 1000;
    private static final int MAX_NUMBER_REQUEST = 5;

    //CHARACTERISTIC UUID
    private final BluetoothGattCharacteristic mRHTCharacteristic;
    private final BluetoothGattCharacteristic mConfigurationCharacteristic;

    //CLASS ATTRIBUTES
    @Nullable
    private RHTDataPoint mLastDatapoint = null;

    public SensorTagRHTService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService);
        mRHTCharacteristic = getCharacteristic(RHT_CHARACTERISTIC_UUID);
        bluetoothGattService.addCharacteristic(mRHTCharacteristic);
        mConfigurationCharacteristic = getCharacteristic(CONFIG_CHARACTERISTIC_UUID);
        bluetoothGattService.addCharacteristic(mConfigurationCharacteristic);
    }

    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (characteristic.equals(mRHTCharacteristic)) {
            Log.d(TAG, String.format("onCharacteristicUpdate -> Received characteristic with UUID: %s.", characteristic.getUuid()));
            final float temperature = extractTemperatureData(characteristic);
            final float humidity = extractHumidityData(characteristic);
            Log.i(TAG, String.format("onCharacteristicUpdate -> Received new RHT data from %s. Humidity = %f  Temperature = %f.", getDeviceAddress(), humidity, temperature));
            mLastDatapoint = new RHTDataPoint(temperature, humidity, System.currentTimeMillis());
            notifyRHTDatapoint(mLastDatapoint, false);
            return true;
        }
        return false;
    }

    private float extractTemperatureData(@NonNull final BluetoothGattCharacteristic characteristic) {
        int a = LittleEndianExtractor.extractSignedShortFromCharacteristic(characteristic, 0);
        a = a - (a % 4);
        return 175.72f / 65536f * a - 46.85f;
    }

    private float extractHumidityData(@NonNull final BluetoothGattCharacteristic characteristic) {
        int a = LittleEndianExtractor.extractUnsignedShortFromCharacteristic(characteristic, 2);
        a = a - (a % 4);
        return 125f * (a / 65535f) - 6f;
    }

    /**
     * Enables the characteristic notifications of the service.
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        mConfigurationCharacteristic.setValue(new byte[]{1});
        mPeripheral.forceWriteCharacteristic(mConfigurationCharacteristic, TIME_BETWEEN_REQUEST_MS, MAX_NUMBER_REQUEST);
        registerNotification(mRHTCharacteristic);
    }

    @Override
    public boolean isServiceReady() {
        return mLastDatapoint != null;
    }

    @Override
    public void synchronizeService() {
        if (mLastDatapoint == null) {
            registerDeviceCharacteristicNotifications();
        }
    }

    /**
     * Obtains the sensor name of the device.
     *
     * @return {@link java.lang.String} with the sensor name.
     */
    @Override
    @NonNull
    public String getSensorName() {
        return SENSOR_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public BleServiceSynchronizationPriority getServiceSynchronizationPriority(){
        return BleServiceSynchronizationPriority.LOW_PRIORITY;
    }
}