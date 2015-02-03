package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.BleService;

public class DeviceInformationService extends BleService<String> {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";

    //FORCE READING CONSTANTS
    private static final int WAITING_TIME_BETWEEN_READS_MS = 75;
    private static final int MAX_READ_TRIES = 3000 / WAITING_TIME_BETWEEN_READS_MS;

    //CLASS PREFIX
    private static final String PREFIX = DeviceInformationService.class.getName();

    //NAMED CHARACTERISTIC NAMES
    public static final String READ_MANUFACTER_NAME_CHARACTERISTIC = String.format("%s.getManufacterName", PREFIX);
    public static final String READ_MODEL_NUMBER_CHARACTERISTIC = String.format("%s.getModelNumber", PREFIX);
    public static final String READ_SERIAL_NUMBER_CHARACTERISTIC = String.format("%s.getSerialNumber", PREFIX);
    public static final String READ_HARDWARE_REVISION_CHARACTERISTIC = String.format("%s.getHardwareRevision", PREFIX);
    public static final String READ_FIRMWARE_REVISION_CHARACTERISTIC = String.format("%s.getFirmwareRevision", PREFIX);
    public static final String READ_SOFTWARE_REVISION_CHARACTERISTIC = String.format("%s.getSoftwareRevision", PREFIX);

    //UUIDs
    private static final String MANUFACTER_NAME_CHARACTERISTIC_UUID = "00002A29-0000-1000-8000-00805f9b34fb";
    private static final String MODEL_NUMBER_CHARACTERISTIC_UUID = "00002A24-0000-1000-8000-00805f9b34fb";
    private static final String SERIAL_NUMBER_CHARACTERISTIC_UUID = "00002A25-0000-1000-8000-00805f9b34fb";
    private static final String HARDWARE_REVISION_CHARACTERISTIC_UUID = "00002A27-0000-1000-8000-00805f9b34fb";
    private static final String FIRMWARE_REVISION_CHARACTERISTIC_UUID = "00002A26-0000-1000-8000-00805f9b34fb";
    private static final String SOFTWARE_REVISION_CHARACTERISTIC_UUID = "00002A28-0000-1000-8000-00805f9b34fb";

    //SERVICE CHARACTERISTICS
    private final BluetoothGattCharacteristic mManufacterNameCharacteristic;
    private final BluetoothGattCharacteristic mModelNumberCharacteristic;
    private final BluetoothGattCharacteristic mSerialNumberCharacteristic;
    private final BluetoothGattCharacteristic mHardwareRevisionCharacteristic;
    private final BluetoothGattCharacteristic mFirmwareRevisionCharacteristic;
    private final BluetoothGattCharacteristic mSoftwareRevisionCharacteristic;

    //CHARACTERISTIC VALUES
    private String mManufacterName;
    private String mModelNumber;
    private String mSerialNumber;
    private String mHardwareRevision;
    private String mFirmwareRevision;
    private String mSoftwareRevision;

    public DeviceInformationService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);

        mManufacterNameCharacteristic = getCharacteristic(MANUFACTER_NAME_CHARACTERISTIC_UUID);
        parent.readCharacteristic(mManufacterNameCharacteristic);

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

    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (mManufacterNameCharacteristic.equals(characteristic)) {
            mManufacterName = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Manufacter name is %s in device with address %s.", mManufacterName, getAddress()));
            return true;
        } else if (mModelNumberCharacteristic.equals(characteristic)) {
            mModelNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Model number is %s in device with address %s.", mModelNumber, getAddress()));
            return true;
        } else if (mSerialNumberCharacteristic.equals(characteristic)) {
            mSerialNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Serial number is %s in device with address %s.", mSerialNumber, getAddress()));
            return true;
        } else if (mHardwareRevisionCharacteristic.equals(characteristic)) {
            mHardwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Hardware revision is %s in device with address %s.", mHardwareRevision, getAddress()));
            return true;
        } else if (mFirmwareRevisionCharacteristic.equals(characteristic)) {
            mFirmwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Firmware revision is %s in device with address %s.", mFirmwareRevision, getAddress()));
            return true;
        } else if (mSoftwareRevisionCharacteristic.equals(characteristic)) {
            mSoftwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Software revision is %s in device with address %s.", mSoftwareRevision, getAddress()));
            return true;
        }
        return super.onCharacteristicUpdate(characteristic);
    }

    @Override
    public String getCharacteristicValue(final String characteristicName) {
        if (READ_MANUFACTER_NAME_CHARACTERISTIC.equals(characteristicName)) {
            return getManufacterName();
        } else if (READ_MODEL_NUMBER_CHARACTERISTIC.equals(characteristicName)) {
            return getModelNumber();
        } else if (READ_SERIAL_NUMBER_CHARACTERISTIC.equals(characteristicName)) {
            return getSerialNumber();
        } else if (READ_HARDWARE_REVISION_CHARACTERISTIC.equals(characteristicName)) {
            return getHardwareRevision();
        } else if (READ_FIRMWARE_REVISION_CHARACTERISTIC.equals(characteristicName)) {
            return getFirmwareRevision();
        } else if (READ_SOFTWARE_REVISION_CHARACTERISTIC.equals(characteristicName)) {
            return getSoftwareRevision();
        }
        return null;
    }

    /**
     * Returns the manufacter name of the device.
     *
     * @return {@link java.lang.String} with the manufacter name of the device.
     */
    public String getManufacterName() {
        if (mManufacterName == null) {
            mPeripheral.forceReadCharacteristic(mManufacterNameCharacteristic, WAITING_TIME_BETWEEN_READS_MS, MAX_READ_TRIES);
            if (mManufacterName == null) {
                Log.e(TAG, "getManufacterName -> Manufacter Name is not known yet.");
                return null;
            }
        }
        return mManufacterName;
    }

    /**
     * Returns the model number of the device.
     *
     * @return {@link java.lang.String} with the model number of the device.
     */
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