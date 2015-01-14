package com.sensirion.libble.peripherals;

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

import com.sensirion.libble.services.NotificationListener;
import com.sensirion.libble.services.NotificationService;
import com.sensirion.libble.services.PeripheralService;
import com.sensirion.libble.services.PeripheralServiceFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a remote piece of HW (SmartGadget) that can have 1-N {@link com.sensirion.libble.services.PeripheralService}
 */

public class Peripheral implements BleDevice, Comparable<Peripheral> {

    // Class TAG for debugging.
    private static final String TAG = Peripheral.class.getSimpleName();

    // Force reading attributes.
    private static final byte INTERVAL_BETWEEN_CHECKS_MILLISECONDS = 25;
    private final Queue<BluetoothGattCharacteristic> mLastCharacteristicsUsedQueue = new LinkedBlockingQueue<>();

    //Peripheral attributes
    private final BlePeripheralService mParent;
    private final BluetoothDevice mBluetoothDevice;
    private final String mAdvertisedName;
    private final String mAddress;

    //Listener list
    private final Set<PeripheralService> mServices = Collections.synchronizedSet(new HashSet<PeripheralService>());
    private volatile boolean mConfirmationOperationRunning = false;
    private BluetoothGatt mBluetoothGatt;
    private int mReceivedSignalStrengthIndication;
    private boolean mIsConnected = false;
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
        public void onServicesDiscovered(@NonNull final BluetoothGatt gatt, final int status) {
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
        public void onCharacteristicRead(@NonNull final BluetoothGatt gatt, @NonNull final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (PeripheralService service : mServices) {
                    service.onCharacteristicRead(characteristic);
                }
                if (mConfirmationOperationRunning) {
                    mLastCharacteristicsUsedQueue.add(characteristic);
                }
            } else {
                Log.w(TAG, "onCharacteristicRead of " + characteristic.getUuid() + " failed with a status of: " + status);
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
            mBleStackProtector.execute(mBluetoothGatt);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, String.format("On characteristic write %s with value %d with status %d", characteristic.getUuid(), characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0), status));
            if (mConfirmationOperationRunning) {
                mLastCharacteristicsUsedQueue.add(characteristic);
            }
        }
    };

    public Peripheral(@NonNull final BlePeripheralService parent, @NonNull final BluetoothDevice bluetoothDevice, final int rssi) {
        mParent = parent;
        mBluetoothDevice = bluetoothDevice;
        mBluetoothGatt = null;
        mAddress = bluetoothDevice.getAddress();
        mAdvertisedName = bluetoothDevice.getName().trim();
        mReceivedSignalStrengthIndication = rssi;
    }

    /**
     * Checks if a characteristic is inside an iterable.
     *
     * @param characteristic to be checked
     * @param iterable       with the elements we want to compare with the characteristic.
     * @return <code>true</code> if the iterable contains the element - <code>false</code> otherwise.
     */
    public static boolean containsCharacteristic(final BluetoothGattCharacteristic characteristic, final Iterable<BluetoothGattCharacteristic> iterable) {
        for (final BluetoothGattCharacteristic lastCharacteristic : iterable) {
            if (lastCharacteristic.getUuid().toString().equals(characteristic.getUuid().toString())) {
                return true;
            }
        }
        return false;
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

    @SuppressWarnings("unused")
    public <T extends PeripheralService> T getPeripheralService(final Class<T> type) {
        for (PeripheralService service : mServices) {
            if (service.getClass().equals(type)) {
                return (T) service;
            }
        }
        return null;
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param serviceName name of the service.
     * @return {@link com.sensirion.libble.services.PeripheralService}
     */
    public PeripheralService getPeripheralService(final String serviceName) {
        for (PeripheralService service : mServices) {
            if (service.isExplicitService(serviceName)) {
                return service;
            }
        }
        return null;
    }

    /**
     * Obtains the list of the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} with the names of the discovered services.
     */
    @SuppressWarnings("unused")
    public Iterable<String> getDiscoveredPeripheralServices() {
        final Set<String> discoveredServices = new HashSet<>();
        for (PeripheralService service : mServices) {
            discoveredServices.add(String.format("%s %s", service.getClass().getSimpleName(), service.getUUIDString()));
        }
        return discoveredServices;
    }

    public void connect(@NonNull final Context ctx) {
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothDevice.connectGatt(ctx, false, mBleStackProtector);
    }

    public boolean reconnect() {
        if (mBluetoothGatt == null) {
            Log.e(TAG, "reconnect -> Bluetooth gatt it's not connected.");
            return false;
        }
        return mBluetoothGatt.connect();
    }

    public void disconnect() {
        if (mBluetoothGatt == null) {
            Log.w(TAG, "disconnect -> Bluetooth gatt was already disconnected.");
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
        return forceCharacteristicReadOrWrite(characteristic, timeoutMs, maxNumberConnections, true);
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
        return forceCharacteristicReadOrWrite(characteristic, timeoutMs, maxRequestCount, false);
    }

    /**
     * Convenience method for forcing a characteristic read or write, in case we want to be sure to read the characteristic.
     * It blocks the UI thread until it receives a response or a timeout is produced. It should be called by other thread.
     *
     * @param characteristic  that is going to be processed. Cannot be <code>null</code>
     * @param timeoutMs       acceptable time without receiving an answer from the peripheral.
     * @param maxRequestCount maximumNumberOfReads. It has to be a positive number.
     * @return <code>true</code> if the characteristic was processed - <code>false</code> otherwise.
     */
    private synchronized boolean forceCharacteristicReadOrWrite(final BluetoothGattCharacteristic characteristic, final int timeoutMs, final int maxRequestCount, final boolean isReading) {
        mConfirmationOperationRunning = true;
        final long timeNow = System.currentTimeMillis();
        mBleStackProtector.cleanCharacteristicCache();
        int requestCounter = 0;
        while (isConnected()) {
            if (System.currentTimeMillis() - timeoutMs * requestCounter > timeNow) {
                if (requestCounter >= maxRequestCount) {
                    break;
                }
                if (isReading) {
                    readCharacteristic(characteristic);
                } else {
                    writeCharacteristic(characteristic);
                }
                requestCounter++;
            }

            if (containsCharacteristic(characteristic, mLastCharacteristicsUsedQueue)) {
                break;
            }

            try {
                Thread.sleep(INTERVAL_BETWEEN_CHECKS_MILLISECONDS);
            } catch (final InterruptedException ignored) {
                Log.w(TAG, String.format("forceCharacteristicReadOrWrite -> Characteristic %s produced an interruptedException.", characteristic.getUuid().toString()));
            }
        }
        mLastCharacteristicsUsedQueue.clear();
        mConfirmationOperationRunning = false;
        mBleStackProtector.cleanCharacteristicCache();
        return false;
    }

    /**
     * Ask for the a characteristic of a service
     *
     * @param characteristicName name of the characteristic.
     * @return {@link java.lang.Object} with the characteristic parsed by the service
     * <code>null</code> if no service was able to parse it.
     */
    public Object getCharacteristicValue(final String characteristicName) {
        for (PeripheralService<?> service : mServices) {
            final Object value = service.getCharacteristicValue(characteristicName);
            if (value == null) {
                continue;
            }
            return value;
        }
        return null;
    }

    /**
     * Request a write to a given {@code BluetoothGattCharacteristic}. The write
     * operation is done asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicWrite(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
     *
     * @param characteristic The characteristic to overwrite.
     */
    public void writeCharacteristic(final BluetoothGattCharacteristic characteristic) {
        mBleStackProtector.addWriteCharacteristic(characteristic);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Enables or disables notification on a given characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If <code>true</code>, enable notification - <code>false</code> otherwise.
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        mBleStackProtector.addCharacteristicNotification(characteristic, enabled);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Writes a {@link android.bluetooth.BluetoothGattDescriptor} in the bluetooth gatherer.
     *
     * @param descriptor the descriptor we want to store.
     */
    public void writeDescriptor(final BluetoothGattDescriptor descriptor) {
        mBleStackProtector.addDescriptorCharacteristic(descriptor);
        mBleStackProtector.execute(mBluetoothGatt);
    }

    /**
     * Ask all the services to enable or disable all their notifications.
     *
     * @param enabled <code>true</code> if notifications wants to be enabled - <code>false</code> otherwise.
     */
    public void setAllNotificationsEnabled(final boolean enabled) {
        for (PeripheralService service : mServices) {
            if (service instanceof NotificationService) {
                ((NotificationService) service).setNotificationsEnabled(enabled);
            }
        }
    }

    /**
     * Ask every service for being listened.
     * Each service with notifications checks if the listener is able to read it's data with interfaces.
     *
     * @param listener Activity from outside the library that
     *                 wants to listen for notifications.
     * @return <code>true</code> if a valid service was found, <code>false</code> otherwise.
     */
    public boolean registerPeripheralListener(final NotificationListener listener) {
        return registerPeripheralListener(listener, null);
    }

    /**
     * Registers a notification listener in service that are able to inform to the incoming listener.
     * Each service with notifications checks if the listener is able to read it's data with interfaces.
     *
     * @param listener    Activity from outside the library that
     *                    wants to listen for notifications.
     * @param serviceName <code>null</code> in case we want to ask to every service.
     *                    name of the service in case we want to listen to a particular service.
     * @return <code>true</code> if a valid service was found, <code>false</code> otherwise.
     */
    public boolean registerPeripheralListener(final NotificationListener listener, final String serviceName) {
        boolean validServiceFound = false;
        for (PeripheralService service : mServices) {
            if (service instanceof NotificationService) {
                if (serviceName == null) {
                    ((NotificationService) service).registerNotificationListener(listener);
                    validServiceFound = true;
                } else if (service.isExplicitService(serviceName)) {
                    ((NotificationService) service).registerNotificationListener(listener);
                    return true;
                }
            }
        }
        return validServiceFound;
    }

    /**
     * Ask every service for not being listened by a listener.
     * Each service with notifications removes it from
     * from it's list, in case the listener was listening it.
     *
     * @param listener from outside the library that doesn't
     *                 want to listen for notifications anymore.
     */
    public void unregisterPeripheralListener(final NotificationListener listener) {
        for (PeripheralService service : mServices) {
            if (service instanceof NotificationService) {
                ((NotificationService) service).unregisterNotificationListener(listener);
            }
        }
    }

    /**
     * Counts the number of services.
     *
     * @return number of discovered services.
     */
    public int getNumberOfDiscoveredServices() {
        return mServices.size();
    }

    /**
     * Obtains the names of each discovered service.
     *
     * @return {@link java.util.List} with the services names. (Simple names)
     */
    public List<String> getDiscoveredServicesNames() {
        final List<String> serviceNames = new LinkedList<>();
        for (PeripheralService service : mServices) {
            serviceNames.add(service.getClass().getSimpleName());
        }
        return serviceNames;
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
     * In case the peripherals are disconnected it worrks using the order of insertion.
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