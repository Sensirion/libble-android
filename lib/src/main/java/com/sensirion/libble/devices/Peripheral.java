package com.sensirion.libble.devices;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.AbstractBleService;
import com.sensirion.libble.services.BleServiceFactory;
import com.sensirion.libble.services.AbstractHistoryService;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a remote piece of Hardware that can have 1-N {@link com.sensirion.libble.services.AbstractBleService}
 */

public class Peripheral implements BleDevice, Comparable<Peripheral> {

    // Class TAG for debugging.
    private static final String TAG = Peripheral.class.getSimpleName();

    // Force reading attributes.
    private static final byte INTERVAL_BETWEEN_CHECKS_MILLISECONDS = 51;
    private final Queue<Object> mLastActionUsedQueue = new LinkedBlockingQueue<>();

    //Peripheral attributes
    @Nullable
    private final String mAdvertisedName;
    @NonNull
    private final BlePeripheralService mPeripheralService;
    @NonNull
    private final BluetoothDevice mBluetoothDevice;
    @NonNull
    private final String mAddress;

    //Listener list
    private final Set<NotificationListener> mNotificationListeners = Collections.synchronizedSet(new HashSet<NotificationListener>());

    //Services List
    private final Set<AbstractBleService> mServices = Collections.synchronizedSet(new HashSet<AbstractBleService>());
    private volatile boolean mForceOperationRunning = false;
    private boolean mIsConnected = false;
    private int mRSSI;

    //Gathering controller.
    private BluetoothGatt mBluetoothGatt;

    private final BleStackProtector mBleStackProtector = new BleStackProtector() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
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
                    mPeripheralService.onPeripheralConnectionChanged(Peripheral.this);
                    break;
                case BluetoothProfile.STATE_DISCONNECTING:
                    Log.d(TAG, "onConnectionStateChange() -> BluetoothProfile.STATE_DISCONNECTING: " + address);
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(TAG, "onConnectionStateChange() -> disconnected from GATT server: " + address);
                    mIsConnected = false;
                    mPeripheralService.onPeripheralConnectionChanged(Peripheral.this);
                    break;
                default:
                    throw new IllegalStateException("onConnectionStateChange() -> state not implemented: " + newState);
            }
        }

        @Override
        public void onCharacteristicRead(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (final AbstractBleService service : mServices) {
                    service.onCharacteristicUpdate(characteristic);
                }
                if (mForceOperationRunning) {
                    mLastActionUsedQueue.add(characteristic);
                }
            } else {
                Log.w(TAG, String.format("onCharacteristicRead -> Characteristic %s failed with the following status: %d", characteristic.getUuid(), status));
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            for (final AbstractBleService service : mServices) {
                service.onCharacteristicUpdate(characteristic);
            }
            mBleStackProtector.execute(mBluetoothGatt);
        }

        @Override
        public void onCharacteristicWrite(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, String.format("onCharacteristicWrite -> Received Characteristic %s with status %d", characteristic.getUuid(), status));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (final AbstractBleService service : mServices) {
                    service.onCharacteristicWrite(characteristic);
                }
                if (mForceOperationRunning) {
                    mLastActionUsedQueue.add(characteristic);
                }
            } else {
                Log.e(TAG, String.format("onCharacteristicWrite -> The device %s was unable to write in the characteristic %s.", getAddress(), characteristic.getUuid()));
            }
        }

        @Override
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                final List<BluetoothGattService> discoveredServices = gatt.getServices();
                Log.w(TAG, String.format(String.format("onServicesDiscovered -> Discovered %s services in the device %s.", discoveredServices.size(), getAddress())));
                for (final BluetoothGattService service : discoveredServices) {
                    final AbstractBleService knownService = BleServiceFactory.getInstance().createServiceFor(Peripheral.this, service);
                    if (knownService != null) {
                        mServices.add(knownService);
                        Log.d(TAG, String.format("onServiceDiscovered -> Added service %s to the service list.", knownService));
                    }
                }
                for (final NotificationListener listener : mNotificationListeners) {
                    registerDeviceListener(listener);
                }
                mPeripheralService.onPeripheralServiceDiscovery(Peripheral.this);
            } else {
                Log.w(TAG, String.format("onServicesDiscovered -> Failed with status: " + status));
            }
        }

        @Override
        public void onDescriptorRead(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattDescriptor descriptor, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (mForceOperationRunning) {
                    mLastActionUsedQueue.add(descriptor);
                }
                Log.i(TAG, String.format("onDescriptorRead -> Descriptor %s was read successfully.", descriptor.getUuid()));
                for (final AbstractBleService service : mServices) {
                    service.onDescriptorRead(descriptor);
                }
            } else {
                Log.i(TAG, String.format("onDescriptorRead -> Descriptor %s failed with status %d.", descriptor.getUuid(), status));
            }
        }

        @Override
        public void onDescriptorWrite(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattDescriptor descriptor, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, String.format("onDescriptorWrite -> Descriptor %s was written successfully.", descriptor.getUuid()));
                if (mForceOperationRunning) {
                    mLastActionUsedQueue.add(descriptor);
                }
                for (final AbstractBleService service : mServices) {
                    service.onDescriptorWrite(descriptor);
                }
            } else {
                Log.i(TAG, String.format("onDescriptorWrite -> Descriptor %s failed with status %d.", descriptor.getUuid(), status));
            }
        }
    };

    public Peripheral(@NonNull final BlePeripheralService parent, @NonNull final BluetoothDevice bluetoothDevice, final int rssi) {
        mPeripheralService = parent;
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = null;
        mAddress = bluetoothDevice.getAddress();
        if (bluetoothDevice.getName() == null) {
            Log.w(TAG, "Constructor -> The incoming device does not have a valid advertise name.");
            mAdvertisedName = null;
        } else {
            mAdvertisedName = bluetoothDevice.getName().trim();
        }
        mRSSI = rssi;
    }

    /**
     * Establish a connection between the application and the peripheral.
     *
     * @param context of the application that wants to connect with the device. Cannot be <code>null</code>
     */
    @Override
    public void connect(@NonNull final Context context) {
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothDevice.connectGatt(context, false, mBleStackProtector);
    }

    /**
     * Tries to establish a connection with a device that has been connected previously.
     *
     * @return <code>true</code> if the connection was recovered - <code>false</code> otherwise.
     */
    @Override
    public boolean reconnect() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "reconnect -> Bluetooth gatt it's not connected.");
            return false;
        }
        return mBluetoothGatt.connect();
    }

    /**
     * Closes a connection to a device.
     */
    @Override
    public void disconnect() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "disconnect -> Bluetooth gatt was already disconnected.");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * Obtains the physical address of the device.
     *
     * @return {@link java.lang.String} with the MAC-Address of the device.
     */
    @Override
    @NonNull
    public String getAddress() {
        return mAddress;
    }

    /**
     * Checks the signal strength of the device towards the external BleDevice.
     *
     * @return {@link java.lang.Integer} with the signal strength.
     */
    @Override
    public int getRSSI() {
        return mRSSI;
    }

    /**
     * Sets the signal strength of the device towards the external BleDevice.
     *
     * @param RSSI with the signal strength.
     */
    public void setRSSI(final int RSSI) {
        mRSSI = RSSI;
    }

    /**
     * Obtains the public name of the BleDevice.
     *
     * @return {@link java.lang.String} with the advertised name.
     */
    @Override
    public String getAdvertisedName() {
        return mAdvertisedName;
    }

    /**
     * Checks if a device is connected or not.
     *
     * @return <code>true</code> if the device is connected - <code>false</code> if the device is disconnected.
     */
    @Override
    public boolean isConnected() {
        return mIsConnected;
    }

    /**
     * Obtain a peripheral service in case the peripheral haves it.
     * NOTE: Returns the first service found of the given type.
     *
     * @param type service class that the user wants to obtain.
     * @param <T>  Class of the service.
     * @return {@link com.sensirion.libble.services.AbstractBleService} that corresponds to the given class.
     */
    @Override
    public <T extends AbstractBleService> T getDeviceService(@NonNull final Class<T> type) {
        for (final AbstractBleService service : mServices) {
            if (service.getClass().equals(type)) {
                return (T) service;
            }
        }
        return null;
    }

    /**
     * Asks for a service with a particular name.
     * NOTE: Returns the first service found of the given type.
     *
     * @param serviceName name of the service.
     * @return {@link com.sensirion.libble.services.AbstractBleService} that corresponds to the given name
     */
    @Override
    public AbstractBleService getDeviceService(@NonNull final String serviceName) {
        for (final AbstractBleService service : mServices) {
            if (service.isExplicitService(serviceName)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Obtains a list with the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} with the names of the discovered services.
     */
    @Override
    @NonNull
    public Iterable<AbstractBleService> getDiscoveredServices() {
        final List<AbstractBleService> discoveredBleServices = new LinkedList<>();
        for (final AbstractBleService service : mServices) {
            discoveredBleServices.add(service);
        }

        return discoveredBleServices;
    }

    /**
     * Obtains a list with the name of the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} with the names of the discovered services.
     */
    @Override
    @NonNull
    public Iterable<String> getDiscoveredServicesNames() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "getDiscoveredServiceNames() -> Bluetooth gatt is not initialized yet.");
            return new LinkedList<>();
        }
        final Set<String> discoveredServices = new HashSet<>();
        for (final AbstractBleService service : mServices) {
            discoveredServices.add(String.format("%s %s", service.getClass().getSimpleName(), service.getUUIDString()));
        }
        return discoveredServices;
    }

    /**
     * Retrieves the device history service in case it has one.
     * NOTE: In case the device has more that one history service it will only return the first one.
     *
     * @return {@link com.sensirion.libble.services.AbstractHistoryService} of the device - <code>null</code> if it doesn't haves one.
     */
    @Override
    public AbstractHistoryService getHistoryService() {
        for (final AbstractBleService service : mServices) {
            if (service instanceof AbstractHistoryService) {
                return (AbstractHistoryService) service;
            }
        }
        return null;
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
     * {@code BluetoothGattCallback#onCharacteristicUpdate(android.bluetooth.BluetoothGatt,
     *android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        mBleStackProtector.addReadCharacteristic(characteristic);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Method that send once a characteristic to the device, informing the user if the characteristic was retrieved.
     *
     * @param characteristic that is going to be readed. Cannot be <code>null</code>
     * @param maxWaitingTime acceptable time without receiving an answer from the peripheral.
     * @return <code>true</code> if the characteristic was read - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean readCharacteristicWithConfirmation(final BluetoothGattCharacteristic characteristic, final int maxWaitingTime) {
        return forceReadCharacteristic(characteristic, maxWaitingTime, 1);
    }

    /**
     * Convenience method for forcing a characteristic read, in case we want to be sure to read the characteristic.
     * It blocks the UI thread until it receives a response or a timeout is produced.
     *
     * @param characteristic       that is going to be readed. Cannot be <code>null</code>
     * @param timeoutMs            acceptable time without receiving an answer from the peripheral.
     * @param maxNumberConnections maximumNumberOfReads. It has to be a positive number.
     * @return <code>true</code> if the characteristic was read - <code>false</code> otherwise.
     */
    public boolean forceReadCharacteristic(final BluetoothGattCharacteristic characteristic, final int timeoutMs, final int maxNumberConnections) {
        return forceActionReadOrWrite(characteristic, timeoutMs, maxNumberConnections, true);
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}. The write
     * operation is done asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to overwrite.
     */
    public void writeCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic) {
        mBleStackProtector.addWriteCharacteristic(characteristic);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Method that send once a characteristic to the device, informing the user if the characteristic was retrieved.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param characteristic that is going to be readed. Cannot be <code>null</code>
     * @param maxWaitingTime acceptable time without receiving an answer from the peripheral.
     * @return <code>true</code> if the characteristic was read - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean writeCharacteristicWithConfirmation(final BluetoothGattCharacteristic characteristic, final int maxWaitingTime) {
        return forceWriteCharacteristic(characteristic, maxWaitingTime, 1);
    }

    /**
     * Convenience method for forcing a characteristic to write, in case we want to be sure to write the characteristic.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param characteristic  that is going to be wrote. Cannot be <code>null</code>
     * @param timeoutMs       acceptable time without receiving an answer from the peripheral.
     * @param maxRequestCount maximumNumberOfReads. It has to be a positive number.
     * @return <code>true</code> if the characteristic was written - <code>false</code> otherwise.
     */
    public boolean forceWriteCharacteristic(final BluetoothGattCharacteristic characteristic, final int timeoutMs, final int maxRequestCount) {
        return forceActionReadOrWrite(characteristic, timeoutMs, maxRequestCount, false);
    }


    /**
     * Reads a {@link android.bluetooth.BluetoothGattDescriptor} in the bluetooth gatherer.
     *
     * @param descriptor the descriptor we want to read.
     */
    public void readDescriptor(@NonNull final BluetoothGattDescriptor descriptor) {
        mBleStackProtector.addReadDescriptor(descriptor);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Method that send a read request of a descriptor to device, informing the user if the characteristic was retrieved.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param descriptor that is going to be readed. Cannot be <code>null</code>
     * @param timeoutMs  acceptable time without receiving an answer from the peripheral.
     * @return <code>true</code> if the characteristic was read - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean readDescriptorWithConfirmation(@NonNull final BluetoothGattDescriptor descriptor, final int timeoutMs) {
        return forceDescriptorRead(descriptor, timeoutMs, 1);
    }

    /**
     * Convenience method for forcing the read of a descritor.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param descriptor       that is going to be written. Cannot be <code>null</code>
     * @param timeoutMs        acceptable time without receiving an answer from the peripheral.
     * @param maxNumberRequest It needs to be a positive number.
     * @return <code>true</code> if the characteristic was written - <code>false</code> otherwise.
     */
    public boolean forceDescriptorRead(@NonNull final BluetoothGattDescriptor descriptor, final int timeoutMs, final int maxNumberRequest) {
        return forceActionReadOrWrite(descriptor, timeoutMs, maxNumberRequest, true);
    }

    /**
     * Writes a {@link android.bluetooth.BluetoothGattDescriptor} in the bluetooth gatherer.
     *
     * @param descriptor the descriptor we want to store.
     */
    public void writeDescriptor(final BluetoothGattDescriptor descriptor) {
        mBleStackProtector.addWriteDescriptor(descriptor);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Method that send a read request of a descriptor to device, informing the user if the characteristic was retrieved.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param descriptor that is going to be readed. Cannot be <code>null</code>
     * @param timeoutMs  acceptable time without receiving an answer from the peripheral.
     * @return <code>true</code> if the characteristic was written - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public boolean writeDescriptorWithConfirmation(@NonNull final BluetoothGattDescriptor descriptor, final int timeoutMs) {
        return forceDescriptorWrite(descriptor, timeoutMs, 1);
    }

    /**
     * Convenience method for forcing the write of a descritor.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param descriptor       that is going to be written. Cannot be <code>null</code>
     * @param timeoutMs        acceptable time without receiving an answer from the peripheral.
     * @param maxNumberRequest It needs to be a positive number.
     * @return <code>true</code> if the characteristic was written - <code>false</code> otherwise.
     */
    public boolean forceDescriptorWrite(@NonNull final BluetoothGattDescriptor descriptor, final int timeoutMs, final int maxNumberRequest) {
        return forceActionReadOrWrite(descriptor, timeoutMs, maxNumberRequest, false);
    }

    /**
     * Convenience method for forcing a characteristic read or write, in case we want to be sure to read the characteristic.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param action          that is going to be processed. Needs to be {@link android.bluetooth.BluetoothGattCharacteristic} or a {@link android.bluetooth.BluetoothGattDescriptor}. Cannot be <code>null</code>
     * @param timeoutMs       acceptable time without receiving an answer from the peripheral.
     * @param maxRequestCount maximumNumberOfReads. It has to be a positive number.
     * @return <code>true</code> if the characteristic was processed - <code>false</code> otherwise.
     */
    private synchronized boolean forceActionReadOrWrite(@NonNull final Object action, final int timeoutMs, final int maxRequestCount, final boolean isReadAction) {
        try {
            mForceOperationRunning = true;
            final long timeNow = System.currentTimeMillis();
            mBleStackProtector.cleanCharacteristicCache();
            int requestCounter = 0;
            while (isConnected()) {
                if (System.currentTimeMillis() - timeoutMs * requestCounter > timeNow) {
                    executeAction(action, isReadAction);
                    requestCounter++;
                }
                if (mLastActionUsedQueue.contains(action)) {
                    return true;
                }
                try {
                    Thread.sleep(INTERVAL_BETWEEN_CHECKS_MILLISECONDS);
                } catch (final InterruptedException ignored) {
                    Log.w(TAG, String.format("forceActionReadOrWrite -> Action %s produced an interruptedException.", action));
                }
                if (requestCounter >= maxRequestCount) {
                    break;
                }
            }
            return false;
        } finally {
            mLastActionUsedQueue.clear();
            mForceOperationRunning = false;
            mBleStackProtector.cleanCharacteristicCache();
        }
    }

    private void executeAction(@NonNull final Object action, final boolean isReadAction) {
        if (isReadAction) {
            if (action instanceof BluetoothGattCharacteristic) {
                readCharacteristic((BluetoothGattCharacteristic) action);
            } else if (action instanceof BluetoothGattDescriptor) {
                readDescriptor((BluetoothGattDescriptor) action);
            }
        } else {
            if (action instanceof BluetoothGattCharacteristic) {
                writeCharacteristic((BluetoothGattCharacteristic) action);
            } else if (action instanceof BluetoothGattDescriptor) {
                writeDescriptor((BluetoothGattDescriptor) action);
            }
        }
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If <code>true</code>, enable notification - <code>false</code> otherwise.
     */
    public void setCharacteristicNotification(@NonNull final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        mBleStackProtector.addCharacteristicNotification(characteristic, enabled);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Ask all the services to enable or disable all their notifications.
     *
     * @param enabled <code>true</code> if notifications wants to be enabled - <code>false</code> otherwise.
     */
    @Override
    public void setAllNotificationsEnabled(final boolean enabled) {
        for (final AbstractBleService service : mServices) {
            service.setNotificationsEnabled(enabled);
        }
    }

    /**
     * Register listener on all BleServices of all the devices. The device doesn't need to be connected.
     * Each service with notifications checks if the listener is able to read its data with interfaces.
     *
     * @param listener Activity from outside the library that
     *                 wants to listen for notifications.
     * @return <code>true</code> if a valid service was found, <code>false</code> otherwise.
     */
    @Override
    public boolean registerDeviceListener(@NonNull final NotificationListener listener) {
        mNotificationListeners.add(listener);
        boolean validServiceFound = false;
        for (final AbstractBleService service : mServices) {
            if (service.registerNotificationListener(listener)) {
                validServiceFound = true;
                service.setNotificationsEnabled(true);
            }
        }
        return validServiceFound;
    }

    /**
     * Unregister a listener from all the BleServices.
     * Each service with notifications removes it from
     * from it's list, in case the listener was listening it.
     *
     * @param listener from outside the library that doesn't
     *                 want to listen for notifications anymore.
     */
    @Override
    public void unregisterDeviceListener(@NonNull final NotificationListener listener) {
        for (final AbstractBleService service : mServices) {
            service.unregisterNotificationListener(listener);
        }
        mNotificationListeners.remove(listener);
    }

    /**
     * Counts the number of services.
     *
     * @return number of discovered services.
     */
    @Override
    public int getNumberServices() {
        return mServices.size();
    }

    /**
     * This method cleans the characteristic stack of the device.
     */
    public void cleanCharacteristicCache() {
        mBleStackProtector.cleanCharacteristicCache();
    }

    @Override
    @SuppressWarnings("SimplifiableIfStatement")
    public boolean equals(final Object otherPeripheral) {
        if (otherPeripheral instanceof Peripheral) {
            return mAddress.equals(((Peripheral) otherPeripheral).getAddress());
        }
        return false;
    }

    /**
     * Compares two discovered peripherals with the RSSI. Used for sorting.
     * In case the peripherals are connected it respects the order of insertion. (Return 0)
     * In case the peripherals are disconnected it works using the order of insertion.
     *
     * @param anotherPeripheral peripheral that has to be sorted.
     * @return positive number if this peripheral has bigger RSSI, negative otherwise. returns <code>0</code> if the device is connected.
     */
    @Override
    public int compareTo(@Nullable final Peripheral anotherPeripheral) {
        if (isConnected()) {
            return 0;
        }
        if (anotherPeripheral == null) {
            Log.e(TAG, "compareTo -> Received a null peripheral.");
            return 0; //A peripheral cannot be compared to a null peripheral.
        }
        return getRSSI() - anotherPeripheral.getRSSI();
    }
}