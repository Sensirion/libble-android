package com.sensirion.libble.services.generic;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.services.AbstractBleService;

public class DeviceInformationService extends AbstractBleService {

    //SERVICE UUIDs
    public static final String SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";

    //UUIDs
    private static final String MANUFACTURER_NAME_CHARACTERISTIC_UUID = "00002A29-0000-1000-8000-00805f9b34fb";
    private static final String MODEL_NUMBER_CHARACTERISTIC_UUID = "00002A24-0000-1000-8000-00805f9b34fb";
    private static final String SERIAL_NUMBER_CHARACTERISTIC_UUID = "00002A25-0000-1000-8000-00805f9b34fb";
    private static final String HARDWARE_REVISION_CHARACTERISTIC_UUID = "00002A27-0000-1000-8000-00805f9b34fb";
    private static final String FIRMWARE_REVISION_CHARACTERISTIC_UUID = "00002A26-0000-1000-8000-00805f9b34fb";
    private static final String SOFTWARE_REVISION_CHARACTERISTIC_UUID = "00002A28-0000-1000-8000-00805f9b34fb";

    //SERVICE CHARACTERISTICS
    @Nullable
    private final BluetoothGattCharacteristic mManufacturerNameCharacteristic;
    @Nullable
    private final BluetoothGattCharacteristic mModelNumberCharacteristic;
    @Nullable
    private final BluetoothGattCharacteristic mSerialNumberCharacteristic;
    @Nullable
    private final BluetoothGattCharacteristic mHardwareRevisionCharacteristic;
    @Nullable
    private final BluetoothGattCharacteristic mFirmwareRevisionCharacteristic;
    @Nullable
    private final BluetoothGattCharacteristic mSoftwareRevisionCharacteristic;

    //CHARACTERISTIC VALUES
    @Nullable
    private String mManufacturerName;
    @Nullable
    private String mModelNumber;
    @Nullable
    private String mSerialNumber;
    @Nullable
    private String mHardwareRevision;
    @Nullable
    private String mFirmwareRevision;
    @Nullable
    private String mSoftwareRevision;

    public DeviceInformationService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        mManufacturerNameCharacteristic = getCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
        if (mManufacturerNameCharacteristic != null) {
            parent.readCharacteristic(mManufacturerNameCharacteristic);
        }
        mModelNumberCharacteristic = getCharacteristic(MODEL_NUMBER_CHARACTERISTIC_UUID);
        if (mModelNumberCharacteristic != null) {
            parent.readCharacteristic(mModelNumberCharacteristic);
        }
        mSerialNumberCharacteristic = getCharacteristic(SERIAL_NUMBER_CHARACTERISTIC_UUID);
        if (mSerialNumberCharacteristic != null) {
            parent.readCharacteristic(mSerialNumberCharacteristic);
        }
        mHardwareRevisionCharacteristic = getCharacteristic(HARDWARE_REVISION_CHARACTERISTIC_UUID);
        if (mHardwareRevisionCharacteristic != null) {
            parent.readCharacteristic(mHardwareRevisionCharacteristic);
        }
        mFirmwareRevisionCharacteristic = getCharacteristic(FIRMWARE_REVISION_CHARACTERISTIC_UUID);
        if (mFirmwareRevisionCharacteristic != null) {
            parent.readCharacteristic(mFirmwareRevisionCharacteristic);
        }
        mSoftwareRevisionCharacteristic = getCharacteristic(SOFTWARE_REVISION_CHARACTERISTIC_UUID);
        if (mSoftwareRevisionCharacteristic != null) {
            parent.readCharacteristic(mSoftwareRevisionCharacteristic);
        }
    }

    @Override
    public boolean isServiceReady() {
        return ((mManufacturerNameCharacteristic != null) == (mManufacturerName != null)) &&
                ((mModelNumberCharacteristic != null) == (mModelNumber != null)) &&
                ((mSerialNumberCharacteristic != null) == (mSerialNumber != null)) &&
                ((mHardwareRevisionCharacteristic != null) == (mHardwareRevision != null)) &&
                ((mFirmwareRevisionCharacteristic != null) == (mFirmwareRevision != null)) &&
                ((mSoftwareRevisionCharacteristic != null) == (mSoftwareRevision != null));
    }

    @Override
    public void synchronizeService() {
        if (mManufacturerNameCharacteristic != null) {
            if (getManufacturerName() == null) {
                Log.w(TAG, "synchronizeService -> Manufacturer name characteristic is not synchronized.");
            }
        }
        if (mModelNumberCharacteristic != null) {
            if (getModelNumber() == null) {
                Log.w(TAG, "synchronizeService -> Model name characteristic is not synchronized.");
            }
        }
        if (mSerialNumberCharacteristic != null) {
            if (getSerialNumber() == null) {
                Log.w(TAG, "synchronizeService -> Serial number characteristic is not synchronized.");
            }
        }
        if (mHardwareRevisionCharacteristic != null) {
            if (getHardwareRevision() == null) {
                Log.w(TAG, "synchronizeService -> Hardware revision characteristic is not synchronized.");
            }
        }
        if (mFirmwareRevisionCharacteristic != null) {
            if (getFirmwareRevision() != null) {
                Log.w(TAG, "synchronizeService -> Firmware revision characteristic is not synchronized.");
            }
        }
        if (mSoftwareRevisionCharacteristic != null) {
            if (getSoftwareRevision() != null) {
                Log.w(TAG, "synchronizeService -> Software revision characteristic is not synchronized.");
            }
        }
    }

    /**
     * Method called when a characteristic is read.
     *
     * @param characteristic that was updated.
     * @return <code>true</code> if the characteristic was read correctly - <code>false</code> otherwise.
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic characteristic) {
        if (mManufacturerNameCharacteristic != null && mManufacturerNameCharacteristic.equals(characteristic)) {
            mManufacturerName = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Manufacturer name is %s in device with address %s.", mManufacturerName, getDeviceAddress()));
            return true;
        } else if (mModelNumberCharacteristic != null && mModelNumberCharacteristic.equals(characteristic)) {
            mModelNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Model number is %s in device with address %s.", mModelNumber, getDeviceAddress()));
            return true;
        } else if (mSerialNumberCharacteristic != null && mSerialNumberCharacteristic.equals(characteristic)) {
            mSerialNumber = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Serial number is %s in device with address %s.", mSerialNumber, getDeviceAddress()));
            return true;
        } else if (mHardwareRevisionCharacteristic != null && mHardwareRevisionCharacteristic.equals(characteristic)) {
            mHardwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Hardware revision is %s in device with address %s.", mHardwareRevision, getDeviceAddress()));
            return true;
        } else if (mFirmwareRevisionCharacteristic != null && mFirmwareRevisionCharacteristic.equals(characteristic)) {
            mFirmwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Firmware revision is %s in device with address %s.", mFirmwareRevision, getDeviceAddress()));
            return true;
        } else if (mSoftwareRevisionCharacteristic != null && mSoftwareRevisionCharacteristic.equals(characteristic)) {
            mSoftwareRevision = characteristic.getStringValue(0);
            Log.d(TAG, String.format("onCharacteristicUpdate -> Software revision is %s in device with address %s.", mSoftwareRevision, getDeviceAddress()));
            return true;
        }
        return super.onCharacteristicUpdate(characteristic);
    }

    /**
     * Returns the manufacturer name of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the manufacturer name of the device - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getManufacturerName() {
        if (mManufacturerNameCharacteristic == null){
            Log.w(TAG, "getManufacturerName -> The device does not implement the manufacturer name characteristic.");
            return null;
        }
        if (mManufacturerName == null) {
            Log.w(TAG, "getManufacturerName -> Manufacturer Name is not available yet. Requesting it in a background thread");
            mPeripheral.readCharacteristic(mManufacturerNameCharacteristic);
        }
        return mManufacturerName;
    }

    /**
     * Returns the model number of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the model number of the device - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getModelNumber() {
        if (mModelNumberCharacteristic == null){
            Log.w(TAG, "getModelNumber -> The device does not implement the model number characteristic.");
            return null;
        }
        if (mModelNumber == null) {
            Log.w(TAG, "getModelNumber -> Model Number is not available yet. Requesting it in a background thread");
            mPeripheral.readCharacteristic(mModelNumberCharacteristic);
        }
        return mModelNumber;
    }

    /**
     * Returns the serial number of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the serial number of the device - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getSerialNumber() {
        if (mSerialNumberCharacteristic == null){
            Log.w(TAG, "getSerialNumber -> The device does not implement the serial number characteristic.");
            return null;
        }
        if (mSerialNumber == null) {
            mPeripheral.readCharacteristic(mSerialNumberCharacteristic);
            Log.w(TAG, "getSerialNumber -> Serial number is not available yet. Requesting it in a background thread");
        }
        return mSerialNumber;
    }

    /**
     * Returns the hardware revision of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the hardware revision - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getHardwareRevision() {
        if (mHardwareRevisionCharacteristic == null){
            Log.w(TAG, "getHardwareRevision -> The device does not implement the hardware revision characteristic.");
            return null;
        }
        if (mHardwareRevision == null) {
            mPeripheral.readCharacteristic(mHardwareRevisionCharacteristic);
            Log.w(TAG, "getHardwareRevision -> Hardware revision is not available yet. Requesting it in a background thread");
        }
        return mHardwareRevision;
    }

    /**
     * Returns the firmware revision of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the firmware revision - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getFirmwareRevision() {
        if (mFirmwareRevisionCharacteristic == null){
            Log.w(TAG, "getFirmwareRevision -> The device does not implement the firmware characteristic.");
            return null;
        }
        if (mFirmwareRevision == null) {
            mPeripheral.readCharacteristic(mFirmwareRevisionCharacteristic);
            Log.w(TAG, "getFirmwareRevision -> Firmware revision is not available yet. Requesting it in a background thread");
        }
        return mHardwareRevision;
    }

    /**
     * Returns the software revision of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the software revision of the device - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getSoftwareRevision() {
        if (mSoftwareRevisionCharacteristic == null){
            Log.w(TAG, "getSoftwareRevision -> The device does not implement the software revision characteristic.");
            return null;
        }
        if (mSoftwareRevision == null) {
            mPeripheral.readCharacteristic(mSoftwareRevisionCharacteristic);
            Log.w(TAG, "getSoftwareRevision -> Software revision is not available yet. Requesting it in a background thread");
        }
        return mSoftwareRevision;
    }
}