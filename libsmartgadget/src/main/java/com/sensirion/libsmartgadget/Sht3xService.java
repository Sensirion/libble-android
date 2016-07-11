package com.sensirion.libsmartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libsmartgadget.utils.LittleEndianExtractor;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class Sht3xService implements GadgetNotificationService, BleConnectorCallback {
    private static final String TAG = Sht3xService.class.getSimpleName();
    private static final String NOTIFICATION_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    private final BleConnector mBleConnector;
    private final ServiceListener mServiceListener;
    private final String mDeviceAddress;

    private final String mNotificationsUuis;
    private final String mUnit;

    private final Set<String> mSupportedUuids;
    private GadgetValue[] mLastValues;
    private boolean mSubscribed;

    public Sht3xService(@NonNull final ServiceListener serviceListener,
                        @NonNull final BleConnector bleConnector,
                        @NonNull final String deviceAddress,
                        @NonNull final String serviceUuid,
                        @NonNull final String notificationsUuid,
                        @NonNull final String unit) {
        mServiceListener = serviceListener;
        mBleConnector = bleConnector;
        mDeviceAddress = deviceAddress;
        mNotificationsUuis = notificationsUuid;
        mUnit = unit;
        mLastValues = new GadgetValue[0];
        mSubscribed = false;

        mSupportedUuids = new HashSet<>();
        mSupportedUuids.add(serviceUuid);
        mSupportedUuids.add(mNotificationsUuis);
        mSupportedUuids.add(NOTIFICATION_DESCRIPTOR_UUID);
    }

    @Override
    public void subscribe() {
        if (isSubscribed()) {
            return;
        }

        final Map<String, BluetoothGattCharacteristic> characteristics =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuis));

        subscribeNotifications(mDeviceAddress, characteristics.get(mNotificationsUuis), true);
        mSubscribed = true;
    }

    @Override
    public void unsubscribe() {
        if (!isSubscribed()) {
            return;
        }

        final Map<String, BluetoothGattCharacteristic> characteristics =
                mBleConnector.getCharacteristics(mDeviceAddress, Collections.singletonList(mNotificationsUuis));

        subscribeNotifications(mDeviceAddress, characteristics.get(mNotificationsUuis), false);
        mSubscribed = false;
    }

    @Override
    public boolean isSubscribed() {
        return mSubscribed;
    }

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
    public void onDataWritten(String characteristicUuid) {
        // ignore ... nothing written here
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
        // ignore ... nothing to do here for the service
    }

    /*
        Private Helper Methods
    */

    private void subscribeNotifications(@NonNull final String deviceAddress,
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

    private void handleLiveValue(final byte[] rawData) {
        final float value = LittleEndianExtractor.extractLittleEndianFloatFromCharacteristicValue(rawData, 0);
        mLastValues = new GadgetValue[]{new SmartGadgetValue(new Date(), value, mUnit)};
        mServiceListener.onGadgetValuesReceived(this, mLastValues);
    }

    private boolean isUuidSupported(final String characteristicUuid) {
        return mSupportedUuids.contains(characteristicUuid);
    }

}
