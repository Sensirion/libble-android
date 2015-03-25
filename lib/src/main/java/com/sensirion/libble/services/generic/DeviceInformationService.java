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
    private final BluetoothGattCharacteristic mManufacturerNameCharacteristic;
    private final BluetoothGattCharacteristic mModelNumberCharacteristic;
    private final BluetoothGattCharacteristic mSerialNumberCharacteristic;
    private final BluetoothGattCharacteristic mHardwareRevisionCharacteristic;
    private final BluetoothGattCharacteristic mFirmwareRevisionCharacteristic;
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

    @Override
    public boolean isServiceReady() {
        return getManufacturerName() != null & getModelNumber() != null & getSerialNumber() != null
                & getHardwareRevision() != null & getFirmwareRevision() != null & getSoftwareRevision() != null;
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
     * Returns the manufacturer name of the device. If its not available it request it in a background thread.
     *
     * @return {@link java.lang.String} with the manufacturer name of the device - <code>null</code> if its not available yet.
     */
    @Nullable
    public String getManufacturerName() {
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
        if (mSoftwareRevision == null) {
            mPeripheral.readCharacteristic(mSoftwareRevisionCharacteristic);
            Log.w(TAG, "getSoftwareRevision -> Software revision is not available yet. Requesting it in a background thread");
        }
        return mSoftwareRevision;
    }
}