package com.sensirion.libsmartgadget.smartgadget;

import android.support.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetService;
import com.sensirion.libsmartgadget.GadgetValue;

import java.util.HashSet;
import java.util.Set;

public class DeviceInformationService implements GadgetService, BleConnectorCallback {
    public static final String SERVICE_UUID = "0000180a-0000-1000-8000-00805f9b34fb";

    private static final String MANUFACTURER_NAME_CHARACTERISTIC_UUID = "00002a29-0000-1000-8000-00805f9b34fb";
    private static final String MODEL_NUMBER_CHARACTERISTIC_UUID = "00002a24-0000-1000-8000-00805f9b34fb";
    private static final String SERIAL_NUMBER_CHARACTERISTIC_UUID = "00002a25-0000-1000-8000-00805f9b34fb";
    private static final String HARDWARE_REVISION_CHARACTERISTIC_UUID = "00002a27-0000-1000-8000-00805f9b34fb";
    private static final String FIRMWARE_REVISION_CHARACTERISTIC_UUID = "00002a26-0000-1000-8000-00805f9b34fb";
    private static final String SOFTWARE_REVISION_CHARACTERISTIC_UUID = "00002a28-0000-1000-8000-00805f9b34fb";

    public static final String UNIT = "";
    public static final String UNKNOWN = "UNKNOWN";

    private final ServiceListener mServiceListener;
    private final BleConnector mBleConnector;
    private final String mDeviceAddress;

    private final Set<String> mSupportedUuids;
    private GadgetValue[] mLastValues;

    private String mManufacturerName = UNKNOWN;
    private String mModelNumber = UNKNOWN;
    private String mSerialNumber = UNKNOWN;
    private String mHardwareRevision = UNKNOWN;
    private String mFirmwareRevision = UNKNOWN;
    private String mSoftwareRevision = UNKNOWN;

    public DeviceInformationService(@NonNull final ServiceListener serviceListener,
                                    @NonNull final BleConnector bleConnector,
                                    @NonNull final String deviceAddress) {
        mDeviceAddress = deviceAddress;
        mBleConnector = bleConnector;
        mServiceListener = serviceListener;
        mLastValues = new GadgetValue[0];

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(SERVICE_UUID);
        mSupportedUuids.add(MANUFACTURER_NAME_CHARACTERISTIC_UUID);
        mSupportedUuids.add(MODEL_NUMBER_CHARACTERISTIC_UUID);
        mSupportedUuids.add(SERIAL_NUMBER_CHARACTERISTIC_UUID);
        mSupportedUuids.add(HARDWARE_REVISION_CHARACTERISTIC_UUID);
        mSupportedUuids.add(FIRMWARE_REVISION_CHARACTERISTIC_UUID);
        mSupportedUuids.add(SOFTWARE_REVISION_CHARACTERISTIC_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        mBleConnector.readCharacteristic(mDeviceAddress, MANUFACTURER_NAME_CHARACTERISTIC_UUID);
        mBleConnector.readCharacteristic(mDeviceAddress, MODEL_NUMBER_CHARACTERISTIC_UUID);
        mBleConnector.readCharacteristic(mDeviceAddress, SERIAL_NUMBER_CHARACTERISTIC_UUID);
        mBleConnector.readCharacteristic(mDeviceAddress, HARDWARE_REVISION_CHARACTERISTIC_UUID);
        mBleConnector.readCharacteristic(mDeviceAddress, FIRMWARE_REVISION_CHARACTERISTIC_UUID);
        mBleConnector.readCharacteristic(mDeviceAddress, SOFTWARE_REVISION_CHARACTERISTIC_UUID);
    }

    /**
     * Returns the software revision of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the software revision of the device.
     */
    public String getSoftwareRevision() {
        return mSoftwareRevision;
    }

    /**
     * Returns the firmware revision of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the firmware revision.
     */
    public String getFirmwareRevision() {
        return mFirmwareRevision;
    }

    /**
     * Returns the hardware revision of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the hardware revision.
     */
    public String getHardwareRevision() {
        return mHardwareRevision;
    }

    /**
     * Returns the serial number of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the serial number of the device.
     */
    public String getSerialNumber() {
        return mSerialNumber;
    }

    /**
     * Returns the model number of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the model number of the device.
     */
    public String getModelNumber() {
        return mModelNumber;
    }

    /**
     * Returns the manufacturer name of the device. If {@link DeviceInformationService#UNKNOWN} is
     * returned means that the data is not available yet. call
     * {@link DeviceInformationService#requestValueUpdate()} for the service to fetch the newest
     * values from the gadget.
     *
     * @return {@link java.lang.String} with the manufacturer name of the device.
     */
    public String getManufacturerName() {
        return mManufacturerName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GadgetValue[] getLastValues() {
        return mLastValues;
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        if (connected) {
            requestValueUpdate();
        }
    }

    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            final String readString = new String(rawData);
            switch (characteristicUuid) {
                case MANUFACTURER_NAME_CHARACTERISTIC_UUID:
                    mManufacturerName = readString;
                    break;
                case MODEL_NUMBER_CHARACTERISTIC_UUID:
                    mModelNumber = readString;
                    break;
                case SERIAL_NUMBER_CHARACTERISTIC_UUID:
                    mSerialNumber = readString;
                    break;
                case HARDWARE_REVISION_CHARACTERISTIC_UUID:
                    mHardwareRevision = readString;
                    break;
                case FIRMWARE_REVISION_CHARACTERISTIC_UUID:
                    mFirmwareRevision = readString;
                    break;
                case SOFTWARE_REVISION_CHARACTERISTIC_UUID:
                    mSoftwareRevision = readString;
                    break;
            }
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        // ignore ... no characteristic written in this service
    }

    // TODO: Think about limiting the retries
    @Override
    public void onFail(final String characteristicUuid, final byte[] data,
                       final boolean wasWriting) {
        if (isUuidSupported(characteristicUuid) && !wasWriting) {
            mBleConnector.readCharacteristic(mDeviceAddress, characteristicUuid);  // Try again
        }
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }
}
