package com.sensirion.libble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a remote piece of HW (SmartGadget) that can have 1-N {@link com.sensirion.libble.PeripheralService}
 */
public class Peripheral implements BleDevice {

    private static final String TAG = Peripheral.class.getSimpleName();

    private final BlePeripheralHandler mParent;
    private final BluetoothDevice mBluetoothDevice;
    private final String mAdvertisedName;
    private final String mAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mReceivedSignalStrengthIndication;
    private boolean mIsConnected = false;

    private Set<PeripheralService> mServices = new HashSet<PeripheralService>();

    private final BluetoothGattExecutor mExecutor = new BluetoothGattExecutor() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            final String address = gatt.getDevice().getAddress();

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTING:
                    Log.d(TAG, "onConnectionStateChange() -> BluetoothProfile.STATE_CONNECTING: " + address);
                    break;
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(TAG, "onConnectionStateChange() -> connected to GATT server: " + address);
                    mIsConnected = true;
                    mBluetoothGatt = gatt;
                    Log.d(TAG, "onConnectionStateChange() -> mBluetoothGatt.discoverServices(): " + mBluetoothGatt.discoverServices());
                    mParent.onPeripheralConnectionStateChanged(Peripheral.this);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange() -> BluetoothProfile.STATE_DISCONNECTING: " + address);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "onConnectionStateChange() -> disconnected from GATT server: " + address);
                    mIsConnected = false;
                    mParent.onPeripheralConnectionStateChanged(Peripheral.this);
                    break;
                default:
                    throw new IllegalStateException("onConnectionStateChange() -> state not implemented: " + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService service : gatt.getServices()) {
                    mServices.add(PeripheralServiceFactory.getInstance().createServiceFor(Peripheral.this, service));
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {

                for (PeripheralService service : mServices) {
                    if (service.onCharacteristicRead(characteristic)) {
                        break;
                    }
                }

            } else {
                Log.w(TAG, "onCharacteristicRead received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

//            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    public Peripheral(BlePeripheralHandler parent, BluetoothDevice bluetoothDevice, int rssi) {
        mParent = parent;
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = null;
        mAddress = bluetoothDevice.getAddress();
        mAdvertisedName = bluetoothDevice.getName().trim();
        mReceivedSignalStrengthIndication = rssi;
    }

    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public int getRSSI() {
        return mReceivedSignalStrengthIndication;
    }

    public void setReceivedSignalStrengthIndication(int receivedSignalStrengthIndication) {
        mReceivedSignalStrengthIndication = receivedSignalStrengthIndication;
    }

    @Override
    public String getAdvertisedName() {
        return mAdvertisedName;
    }

    public boolean isConnected() {
        return mIsConnected;
    }

    @Override
    public <T extends PeripheralService> T getPeripheralService(Class<T> type) {
        for (PeripheralService service : mServices) {
            if (service.getClass().equals(type)) {
                return (T) service;
            }
        }

        return null;
    }

    public void connect(Context context) {
        // We want to directly connect to the device, so we are setting the
        // autoConnect parameter to false.
        mBluetoothDevice.connectGatt(context, false, mExecutor);
    }

    public boolean reconnect() {
        if (mBluetoothGatt == null) {
            return false;
        }
        return mBluetoothGatt.connect();
    }

    public void disconnect() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * Close the corresponding BluetoothGatt so that resources are cleaned up properly.
     */
    public void close() {
        mBluetoothGatt.close();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.readCharacteristic(characteristic);
    }
}
