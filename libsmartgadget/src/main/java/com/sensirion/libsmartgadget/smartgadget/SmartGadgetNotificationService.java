package com.sensirion.libsmartgadget.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.GadgetNotificationService;
import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

abstract class SmartGadgetNotificationService implements GadgetNotificationService, BleConnectorCallback {
    private static final String TAG = SmartGadgetNotificationService.class.getSimpleName();
    protected static final String NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    protected final BleConnector mBleConnector;
    protected final ServiceListener mServiceListener;
    protected final String mDeviceAddress;

    protected final String mNotificationsUuid;
    protected final String mUnit;

    protected final Set<String> mSupportedUuids;
    protected GadgetValue[] mLastValues;
    protected boolean mSubscribed;

    /**
     * {@inheritDoc}
     */
    public SmartGadgetNotificationService(@NonNull final ServiceListener serviceListener,
                                          @NonNull final BleConnector bleConnector,
                                          @NonNull final String deviceAddress,
                                          @NonNull final String serviceUuid,
                                          @NonNull final String notificationsUuid,
                                          @NonNull final String unit) {
        mServiceListener = serviceListener;
        mBleConnector = bleConnector;
        mDeviceAddress = deviceAddress;
        mNotificationsUuid = notificationsUuid;
        mUnit = unit;
        mLastValues = new GadgetValue[0];
        mSubscribed = false;

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(serviceUuid);
        mSupportedUuids.add(mNotificationsUuid);
        mSupportedUuids.add(NOTIFICATION_DESCRIPTOR_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe() {
        final Map<String, BluetoothGattCharacteristic> characteristics =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid));

        subscribeNotifications(mDeviceAddress, characteristics.get(mNotificationsUuid), true);
        mSubscribed = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe() {
        final Map<String, BluetoothGattCharacteristic> characteristics =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuid));

        subscribeNotifications(mDeviceAddress, characteristics.get(mNotificationsUuid), false);
        mSubscribed = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscribed() {
        return mSubscribed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestValueUpdate() {
        mBleConnector.readCharacteristic(mDeviceAddress, mNotificationsUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GadgetValue[] getLastValues() {
        return mLastValues;
    }

    /*
        Implementation of {@link BleConnectorCallback}
     */
    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        if (isUuidSupported(characteristicUuid)) {
            if (rawData.length <= 8) {
                handleLiveValue(rawData);
            }
        }
    }

    @Override
    public void onDataWritten(final String characteristicUuid) {
        // ignore ... nothing written here
    }

    @Override
    public void onConnectionStateChanged(final boolean connected) {
        // ignore ... nothing to do here for the service
    }

    @Override
    public void onFail(final String characteristicUuid, final byte[] data, final boolean isWriteFailure) {
        if (!isUuidSupported(characteristicUuid)) {
            return;
        }

        // TODO think about limiting the retires
        if (characteristicUuid.equals(mNotificationsUuid)) {
            if (isSubscribed()) {
                subscribe(); // failed to subscribe... retry
            } else {
                unsubscribe(); // failed to unsubscribe... retry
            }
        }
    }

    /*
        Private Helper Methods
    */

    private synchronized void subscribeNotifications(@NonNull final String deviceAddress,
                                                     @NonNull final BluetoothGattCharacteristic characteristic,
                                                     final boolean enable) {
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                UUID.fromString(NOTIFICATION_DESCRIPTOR_UUID));
        if (descriptor == null) {
            Log.w(TAG, "Null Descriptor when subscribing gadget " + deviceAddress);
            return;
        }
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleConnector.setCharacteristicNotification(deviceAddress, characteristic, descriptor, enable);
    }

    protected void handleLiveValue(final byte[] rawData) {
        final float value = LittleEndianExtractor.extractFloat(rawData, 0);
        mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), value, mUnit)};
        mServiceListener.onGadgetValuesReceived(this, mLastValues);
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }

}
