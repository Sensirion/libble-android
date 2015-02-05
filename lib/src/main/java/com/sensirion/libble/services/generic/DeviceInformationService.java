package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.BleService;

public class DeviceInformationService extends BleService {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";

    //FORCE READING CONSTANTS
    private static final int WAITING_TIME_BETWEEN_READS_MS = 75;
    private static final int MAX_READ_TRIES = 3000 / WAITING_TIME_BETWEEN_READS_MS;

    //UUIDs
    private static final String MANUFACTURER_NAME_CHARACTERISTIC_UUID = "00002A29-0000-1000-8000-00805f9b34fb";
    private static final String MODEL_NUMBER_CHARACTERISTIC_UUID = "00002A24-0000-1000-8000-00805f9b34fb";
    private static final String SERIAL_NUMBER_CHARACTERISTIC_UUID = "00002A25-0000-1000-8000-00805f9b34fb";
    private static final String HARDWARE_REVISION_CHARACTERISTIC_UUID = "00002A27-0000-1000-8000-00805f9b34fb";
    private static final String FIRMWARE_REVISION_CHARACTERISTIC_UUID = "00002A26-0000-1000-8000-00805f9b34fb";
    private static final String SOFTWARE_REVISION_CHARACTERISTIC_UUID = "00002A28-0000-1000-8000-00805f9b34fb";

    //SERVICE CHARACTERISTICS
    private final BluetoothGattCharacteristic mManufacturerNameCharacteristic;
    private final BluetoothGattCharacteristic mModelNumberCharacteristic;
    private final BluetoothGattCharacteristic mSerialNumberCharacteristic;
    private final BluetoothGattCharacteristic mHardwareRevisionCharacteristic;
    private final BluetoothGattCharacteristic mFirmwareRevisionCharacteristic;
    private final BluetoothGattCharacteristic mSoftwareRevisionCharacteristic;

    //CHARACTERISTIC VALUES
    private String mManufacturerName;
    private String mModelNumber;
    private String mSerialNumber;
    private String mHardwareRevision;
    private String mFirmwareRevision;
    private String mSoftwareRevision;

    public DeviceInformationService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);

        mManufacturerNameCharacteristic = getCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mManufacturerNameCharacteristic);

        mModelNumberCharacteristic = getCharacteristic(MODEL_NUMBER_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mModelNumberCharacteristic);

        mSerialNumberCharacteristic = getCharacteristic(SERIAL_NUMBER_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mSerialNumberCharacteristic);

        mHardwareRevisionCharacteristic = getCharacteristic(HARDWARE_REVISION_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mHardwareRevisionCharacteristic);

        mFirmwareRevisionCharacteristic = getCharacteristic(FIRMWARE_REVISION_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mFirmwareRevisionCharacteristic);

        mSoftwareRevisionCharacteristic = getCharacteristic(SOFTWARE_REVISION_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mSoftwareRevisionCharacteristic);
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param characteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (mManufacturerNameCharacteristic.equals(characteristic)) {
            mManufacturerName = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Manufacturer name is %s in device with address %s.", mManufacturerName, getDeviceAddress()));
            return true;
        } else if (mModelNumberCharacteristic.equals(characteristic)) {
            mModelNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Model number is %s in device with address %s.", mModelNumber, getDeviceAddress()));
            return true;
        } else if (mSerialNumberCharacteristic.equals(characteristic)) {
            mSerialNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Serial number is %s in device with address %s.", mSerialNumber, getDeviceAddress()));
            return true;
        } else if (mHardwareRevisionCharacteristic.equals(characteristic)) {
            mHardwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Hardware revision is %s in device with address %s.", mHardwareRevision, getDeviceAddress()));
            return true;
        } else if (mFirmwareRevisionCharacteristic.equals(characteristic)) {
            mFirmwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Firmware revision is %s in device with address %s.", mFirmwareRevision, getDeviceAddress()));
            return true;
        } else if (mSoftwareRevisionCharacteristic.equals(characteristic)) {
            mSoftwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Software revision is %s in device with address %s.", mSoftwareRevision, getDeviceAddress()));
            return true;
        }
        return super.onCharacteristicUpdate(characteristic);
    }

    /**
     * Returns the manufacter name of the device.
     *
     * @return {@link java.lang.String} with the manufacter name of the device.
     */
    @SuppressWarnings("unused")
    public String getManufacterName() {
        if (mManufacturerName == null) {
            mPeripheral.forceReadCharacteristic(mManufacturerNameCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mManufacturerName == null) {
                Log.e(TAG, "getManufacterName -> Manufacter Name is not known yet.");
                return null;
            }
        }
        return mManufacturerName;
    }

    /**
     * Returns the model number of the device.
     *
     * @return {@link java.lang.String} with the model number of the device.
     */
    @SuppressWarnings("unused")
    public String getModelNumber() {
        if (mModelNumber == null) {
            mPeripheral.forceReadCharacteristic(mModelNumberCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mModelNumber == null) {
                Log.e(TAG, "getModelNumber -> Model Number is not known yet.");
                return null;
            }
        }
        return mModelNumber;
    }

    /**
     * Returns the serial number of the device.
     *
     * @return {@link java.lang.String} with the serial number of the device.
     */
    @SuppressWarnings("unused")
    public String getSerialNumber() {
        if (mSerialNumber == null) {
            mPeripheral.forceReadCharacteristic(mSerialNumberCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mSerialNumber == null) {
                Log.e(TAG, "getSerialNumber -> Serial Number is not known yet.");
                return null;
            }
        }
        return mSerialNumber;
    }

    /**
     * Returns the hardware revision of the device.
     *
     * @return {@link java.lang.String} with the hardware revision.
     */
    @SuppressWarnings("unused")
    public String getHardwareRevision() {
        if (mHardwareRevision == null) {
            mPeripheral.forceReadCharacteristic(mHardwareRevisionCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mHardwareRevision == null) {
                Log.e(TAG, "getHardwareRevision -> Hardware Revision is not known yet.");
                return null;
            }
        }
        return mHardwareRevision;
    }

    /**
     * Returns the firmware revision of the device.
     *
     * @return {@link java.lang.String} with the firmware revision.
     */
    @SuppressWarnings("unused")
    public String getFirmwareRevision() {
        if (mFirmwareRevision == null) {
            mPeripheral.forceReadCharacteristic(mFirmwareRevisionCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mFirmwareRevision == null) {
                Log.e(TAG, "getFirmwareRevision -> Firmware Revision is not known yet.");
                return null;
            }
        }
        return mHardwareRevision;
    }

    /**
     * Returns the software revision of the device.
     *
     * @return {@link java.lang.String} with the software revision of the device.
     */
    @SuppressWarnings("unused")
    public String getSoftwareRevision() {
        if (mSoftwareRevision == null) {
            mPeripheral.forceReadCharacteristic(mSoftwareRevisionCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mSoftwareRevision == null) {
                Log.e(TAG, "getSoftwareRevision -> Software Revision is not known yet.");
                return null;
            }
        }
        return mSoftwareRevision;
    }
}