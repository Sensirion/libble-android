package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.AbstractBleService;
import com.sensirion.libble.services.AbstractHistoryService;
import com.sensirion.libble.utils.LittleEndianExtractor;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

abstract class AbstractSmartgadgetService<ListenerType extends NotificationListener> extends AbstractBleService<ListenerType> {

    static final byte DATAPOINT_SIZE = 4;

    private final String VALUE_NOTIFICATIONS_UUID; //4 Byte Float Little Endian - 20 byte [1 little endian integer (Sequence number), 4 Byte Float Little Endian]

    @NonNull
    private final BluetoothGattCharacteristic mValueCharacteristic;
    @Nullable
    protected String mSensorName = null;
    @Nullable
    protected Float mLastValue = null;

    protected AbstractSmartgadgetService(@NonNull final Peripheral peripheral,
                                         @NonNull final BluetoothGattService gatt,
                                         @NonNull final String liveValueCharacteristicUUID) throws InstantiationException {
        super(peripheral, gatt);
        VALUE_NOTIFICATIONS_UUID = liveValueCharacteristicUUID;
        final BluetoothGattCharacteristic characteristic = super.getCharacteristic(VALUE_NOTIFICATIONS_UUID);
        if (characteristic == null) {
            throw new InstantiationException(String.format("%s: %s -> Can not found the value characteristic.", TAG, TAG));
        }
        mValueCharacteristic = characteristic;
        peripheral.readCharacteristic(mValueCharacteristic);
        gatt.addCharacteristic(mValueCharacteristic);
        mPeripheral.readDescriptor(mValueCharacteristic.getDescriptor(USER_CHARACTERISTIC_DESCRIPTOR_UUID));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerDeviceCharacteristicNotifications() {
        Log.d(TAG, "registerDeviceCharacteristicNotifications -> Requesting characteristic notifications.");
        registerNotification(mValueCharacteristic);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onDescriptorRead(@NonNull final BluetoothGattDescriptor descriptor) {
        if (descriptor.getUuid().equals(USER_CHARACTERISTIC_DESCRIPTOR_UUID)) {
            if (descriptor.getCharacteristic().getUuid().equals(mValueCharacteristic.getUuid())) {
                if (descriptor.getCharacteristic().getInstanceId() == mValueCharacteristic.getInstanceId()) {
                    Log.d(TAG, String.format("onDescriptorRead -> Reading descriptor %s from characteristic %s in device %s.", descriptor.getUuid(), descriptor.getCharacteristic().getUuid(), getDeviceAddress()));
                    try {
                        final String userDescriptor = new String(descriptor.getValue(), "UTF-8");
                        final String[] userValue = userDescriptor.split(" ");
                        Log.i(TAG, "onDescriptorRead -> " + Arrays.toString(userValue));
                        setValueUnit(userValue[3].trim());
                        mSensorName = userValue[0].trim();
                    } catch (final UnsupportedEncodingException e) {
                        Log.e(TAG, "onDescriptorRead -> The following exception was produced when trying to read the user description -> ", e);
                    }
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCharacteristicUpdate(@NonNull final BluetoothGattCharacteristic updatedCharacteristic) {
        final String characteristicUUID = updatedCharacteristic.getUuid().toString();
        if (characteristicUUID.equalsIgnoreCase(VALUE_NOTIFICATIONS_UUID)) {
            if (updatedCharacteristic.getValue().length <= 8) {
                Log.d(TAG, "onCharacteristicUpdate -> Parsing live value.");
                return parseLiveValue(updatedCharacteristic);
            } else {
                Log.d(TAG, "onCharacteristicUpdate -> Parsing historical value.");
                return parseHistoryValue(updatedCharacteristic);
            }
        }
        return false;
    }

    private boolean parseLiveValue(@NonNull final BluetoothGattCharacteristic updatedValue) {
        if (mSensorName == null) {
            mPeripheral.readDescriptor(updatedValue.getDescriptor(USER_CHARACTERISTIC_DESCRIPTOR_UUID));
            return false;
        }
        mLastValue = LittleEndianExtractor.extractLittleEndianFloatFromCharacteristic(updatedValue);
        notifyListenersNewLiveValue();
        return true;
    }

    private boolean parseHistoryValue(@NonNull final BluetoothGattCharacteristic updatedValue) {
        final byte[] historyValueBuffer = updatedValue.getValue();
        if (historyValueBuffer.length < DATAPOINT_SIZE * 2 || historyValueBuffer.length % DATAPOINT_SIZE > 0) {
            Log.e(TAG, "parseHistoryValue -> Received History value does not have a valid length.");
            return false;
        }

        final SmartgadgetHistoryService historyService;

        if (mPeripheral.getHistoryService() == null) {
            Log.e(TAG, "parseHistoryValue -> The device does not have a valid history service.");
            return false;
        } else if (mPeripheral.getHistoryService() instanceof SmartgadgetHistoryService) {
            historyService = (SmartgadgetHistoryService) mPeripheral.getHistoryService();
        } else {
            Log.e(TAG, String.format("parseHistoryValue -> The history service %s does not inherit %s.", mPeripheral.getHistoryService(), AbstractHistoryService.class));
            return false;
        }

        final int sequenceNumber = extractSequenceNumber(historyValueBuffer);
        final Integer historyInterval;
        try {
            historyInterval = historyService.getLoggingIntervalMs().get();
        } catch (final InterruptedException | ExecutionException e) {
            Log.e(TAG, "parseHistoryValue -> The following exception was thrown -> ", e);
            return false;
        }

        if (historyInterval == null) {
            Log.e(TAG, "parseHistoryValue -> History interval can't be null during data download.");
            return false;
        }

        final Long newestTimestamp = historyService.getNewestTimestampMs();
        if (newestTimestamp == null) {
            throw new IllegalArgumentException(String.format("%s: parseHistoryValue -> Cannot obtain the newest timestamp from the history service in device %s.", TAG, getDeviceAddress()));
        }

        for (int offset = 4; offset < historyValueBuffer.length; offset += 4) {
            final long timestamp = newestTimestamp - (historyInterval * ((((offset / DATAPOINT_SIZE) - 1) + sequenceNumber)));
            final float historyValue = LittleEndianExtractor.extractLittleEndianFloatFromCharacteristic(updatedValue, offset);
            Log.i(TAG, String.format("parseHistoryValue -> Obtained a value of %f in device %s (%s seconds ago).", historyValue, getDeviceAddress(), ((System.currentTimeMillis() - timestamp) / 1000)));
            notifyListenersNewHistoricalValue(historyValue, timestamp);
        }

        final int numberParsedElements = (historyValueBuffer.length / 4) - 1;
        historyService.setLastSequenceNumberDownloaded(sequenceNumber + numberParsedElements);

        return true;
    }

    private int extractSequenceNumber(@NonNull final byte[] byteBuffer) {
        final int[] wrappedSequenceNumber = new int[1];
        final byte[] sequenceNumberBuffer = new byte[4];
        System.arraycopy(byteBuffer, 0, sequenceNumberBuffer, 0, 4);
        ByteBuffer.wrap(sequenceNumberBuffer).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(wrappedSequenceNumber);
        return wrappedSequenceNumber[0];
    }

    /**
     * Obtains the sensor name of the service.
     *
     * @return {@link java.lang.String} with the sensor name - <code>null</code> if the sensor name is not known yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public String getSensorName() {
        if (mSensorName == null) {
            mPeripheral.readDescriptor(mValueCharacteristic.getDescriptor(USER_CHARACTERISTIC_DESCRIPTOR_UUID));
        }
        return mSensorName;
    }

    /**
     * Notifies the service listeners of the reading of a new live value.
     */
    abstract void notifyListenersNewLiveValue();

    /**
     * Notifies the service listeners of the reading of a new historical value.
     */
    abstract void notifyListenersNewHistoricalValue(final float value, final long timestamp);

    /**
     * Sends to the service implementation the extracted value unit.
     *
     * @param valueUnit {@link java.lang.String} with the value unit specified in the device.
     */
    abstract void setValueUnit(@NonNull final String valueUnit);
}