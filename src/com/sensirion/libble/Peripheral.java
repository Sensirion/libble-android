package com.sensirion.libble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import com.sensirion.libble.bleservice.NotificationService;
import com.sensirion.libble.bleservice.PeripheralService;
import com.sensirion.libble.bleservice.PeripheralServiceFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a remote piece of HW (SmartGadget) that can have 1-N {@link com.sensirion.libble.bleservice.PeripheralService}
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

    //TODO: implement a queue to protect the BT-Stack from multiple read/write/notification requests in parallel since this is not supported (Android 4.4)
    //maybe BluetoothGattExecutor shows a working mechanism - check that and apply here (ev. delegate to new Executor-class)
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

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
                    mParent.onPeripheralConnectionChanged(Peripheral.this);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange() -> BluetoothProfile.STATE_DISCONNECTING: " + address);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "onConnectionStateChange() -> disconnected from GATT server: " + address);
                    mIsConnected = false;
                    mParent.onPeripheralConnectionChanged(Peripheral.this);
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
                Log.w(TAG, "onServicesDiscovered failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (PeripheralService service : mServices) {
                    if (service.onCharacteristicRead(characteristic)) {
                        break;
                    }
                }
            } else {
                Log.w(TAG, "onCharacteristicRead failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            for (PeripheralService service : mServices) {
                if (service instanceof NotificationService) {
                    ((NotificationService) service).onChangeNotification(characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            //TODO: implement
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

    @Override
    public Iterable<String> getDiscoveredPeripheralServices() {
        HashSet<String> discoveredServices = new HashSet<String>();

        for (PeripheralService service : mServices) {
            discoveredServices.add(service.getClass().getSimpleName()
                    + " "
                    + service.getUUIDString());
        }

        return discoveredServices;
    }

    public void connect(Context context) {
        // We want to directly connect to the device, so we are setting the
        // autoConnect parameter to false.
        mBluetoothDevice.connectGatt(context, false, mGattCallback);
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

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt,
     *android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}. The write
     * operation is done asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt,
     *android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to overwrite.
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    /**
     * Writes a {@link android.bluetooth.BluetoothGattDescriptor} in the bluetooth gatherer.
     *
     * @param descriptor the descriptor we want to store.
     */
    public void writeDescriptor(BluetoothGattDescriptor descriptor) {
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * Ask every service for being listened.
     * Each service with notifications checks if the listener
     * is able to read it's data with interfaces.
     *
     * @param listener Activity from outside the library that
     *                 wants to listen for notifications.
     * @return true if a valid service was found, false otherwise.
     */
    public boolean registerPeripheralListener(NotificationListener listener) {
        boolean interestedServiceFound = false;
        for (PeripheralService service : mServices) {
            if (service instanceof NotificationService) {
                if (((NotificationService) service).registerNotificationListener(listener)) {
                    interestedServiceFound = true;
                }
            }
        }
        return interestedServiceFound;
    }

    /**
     * Ask every service for not being listened by a listener.
     * <p/>
     * Each service with notifications removes it from
     * from it's list, in case the listener was listening it.
     *
     * @param listener from outside the library that doesn't
     *                 want to listen for notifications anymore.
     */
    public void unregisterPeripheralListener(NotificationListener listener) {
        for (PeripheralService service : mServices) {
            if (service instanceof NotificationService) {
                ((NotificationService) service).unregisterNotificationListener(listener);
            }
        }
    }
}