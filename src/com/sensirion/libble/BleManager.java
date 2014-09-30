package com.sensirion.libble;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.sensirion.libble.bleservice.PeripheralService;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class BleManager {

    private static final String TAG = BleManager.class.getSimpleName();

    private static BleManager mInstance;

    private boolean mShouldStartScanning;

    private BlePeripheralService mBlePeripheralService;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected() -> connected to BlePeripheralService");
            mBlePeripheralService = ((BlePeripheralService.LocalBinder) service).getService();

            if (mShouldStartScanning) {
                Log.i(TAG, "onServiceConnected() -> re-trigger startScanning()");
                startScanning();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected() -> disconnected from BlePeripheralService");
            mBlePeripheralService = null;
        }
    };

    private BleManager() {
    }

    public synchronized static BleManager getInstance() {
        if (mInstance == null) {
            mInstance = new BleManager();
        }
        return mInstance;
    }

    /**
     * This method should be called immediately just after the first getInstance()
     * call in the application that wants to use the library.
     *
     * @param appContext of the implementing application.
     */
    public void init(final Context appContext) {
        if (appContext instanceof Application) {
            Log.i(TAG, "init() -> binding to BlePeripheralService");
            Intent intent = new Intent(appContext, BlePeripheralService.class);
            if (appContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)) {
                Log.i(TAG, "init() -> successfully bound to BlePeripheralService!");
            } else {
                throw new IllegalStateException("init() -> unable to bind to BlePeripheralService!");
            }
        } else {
            throw new IllegalArgumentException("init() -> BleManager has to be initialized with an application context");
        }
    }

    /**
     * This method should be called at the end of the execution of the implementing application.
     *
     * @param appContext of the implementing application.
     */
    public void release(final Context appContext) {
        if (appContext instanceof Application) {
            try {
                Log.w(TAG, "release() -> unbinding mServiceConnection");
                appContext.unbindService(mServiceConnection);
            } catch (Exception e) {
                Log.e(TAG, "An exception was produced when when trying to unbind from it.", e);
            }
        } else {
            throw new IllegalArgumentException("release() -> BleManager only can be released with the application context.");
        }
    }

    /**
     * Starts to scan for all bluetooth devices in range.
     *
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning() {
        return startScanning(null);
    }

    /**
     * Start scanning devices in range using provided UUIDs.
     *
     * @param deviceUUIDs deviceUUIDs that we want want to use,
     *                    <code>null</code> if all devices have to be retrieved.
     * @return <code>true</code> if it's scanning, <code>false</code> otherwise.
     */
    public boolean startScanning(final UUID[] deviceUUIDs) {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "startScanning() -> not yet connected to BlePeripheralService; try to re-trigger scanning when connected.");
            mShouldStartScanning = true;
            return false;
        }
        if (mBlePeripheralService.isScanning()) {
            Log.w(TAG, "startScanning() -> already scanning; ignoring this request.");
            return true;
        }
        if (deviceUUIDs == null) {
            Log.d(TAG, "startScanning() -> mBlePeripheralService.startLeScan()");
            return mBlePeripheralService.startLeScan();
        }
        Log.d(TAG, "startScanning() -> mBlePeripheralService.startLeScan(UUIDs)");
        return mBlePeripheralService.startLeScan(deviceUUIDs);
    }

    /**
     * Stops the scan of new devices.
     */
    public void stopScanning() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "stopScanning() -> not connected to BlePeripheralService");
            return;
        }

        if (mBlePeripheralService.isScanning()) {
            Log.d(TAG, "stopScanning() -> mBlePeripheralService.stopLeScan()");
            mBlePeripheralService.stopLeScan();
        }
    }

    /**
     * Ask if the peripheral service is scanning.
     *
     * @return <code>true</code> if it's scanning - <code>false</code> otherwise.
     */
    public boolean isScanning() {
        if (mBlePeripheralService == null) {
            Log.w(TAG, "isScanning() -> not connected to BlePeripheralService");
            return false;
        }
        return mBlePeripheralService.isScanning();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice}.
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices() {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals();
    }

    /**
     * Get all discovered {@link com.sensirion.libble.BleDevice} with valid names for the application.
     *
     * @param validDeviceNames {@link java.util.List} of devices names.
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getDiscoveredBleDevices(final List<String> validDeviceNames) {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getDiscoveredPeripherals(validDeviceNames);
    }

    /**
     * Returns the number of connected devices.
     *
     * @return <code>int</code> with the number of devices.
     */
    public int getConnectedBleDeviceCount() {
        return mBlePeripheralService.getConnectedBleDeviceCount();
    }

    /**
     * Get all connected {@link com.sensirion.libble.BleDevice}
     *
     * @return Iterable
     */
    public Iterable<? extends BleDevice> getConnectedBleDevices() {
        if (mBlePeripheralService == null) {
            return new ArrayList<BleDevice>();
        }
        return mBlePeripheralService.getConnectedPeripherals();
    }

    /**
     * Returns the {@link com.sensirion.libble.BleDevice} belonging to the given address
     *
     * @param address MAC-Address of the desired {@link com.sensirion.libble.BleDevice}
     * @return Connected device as {@link com.sensirion.libble.BleDevice} or <code>null</code> if the device is not connected
     */
    public BleDevice getConnectedDevice(final String address) {
        if (mBlePeripheralService == null) {
            return null;
        }
        return mBlePeripheralService.getConnectedDevice(address);
    }

    /**
     * Tries to establish a connection to a selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be connected
     */
    public boolean connectPeripheral(final String address) {
        if (mBlePeripheralService == null) {
            Log.e(TAG, BlePeripheralService.class.getSimpleName() + " is null -> could not connect peripheral!");
        } else {
            Log.d(TAG, "connectPeripheral() -> stopScanning()");
            stopScanning();
            return mBlePeripheralService.connect(address);
        }
        return false;
    }

    /**
     * Tries to disconnect a selected peripheral (by address)
     *
     * @param address MAC-Address of the peripheral that should be disconnected
     */
    public void disconnectPeripheral(final String address) {
        if (mBlePeripheralService != null) {
            mBlePeripheralService.disconnect(address);
        } else {
            Log.e(TAG, "Service not ready!");
        }
    }

    /**
     * Looks for a connected peripheral.
     *
     * @param deviceAddress of the peripheral.
     * @return the {@link com.sensirion.libble.Peripheral} with the given address
     */
    public Peripheral getConnectedPeripheral(final String deviceAddress) {
        Log.i(TAG, "getConnectedPeripheral -> Requested peripheral with address: " + deviceAddress);
        final Iterator<? extends BleDevice> iterator = getConnectedBleDevices().iterator();
        while (iterator.hasNext()) {
            final BleDevice device = iterator.next();
            try {
                if (deviceAddress.equals(device.getAddress())) {
                    Log.i(TAG, String.format("getConnectedPeripheral -> Peripheral with address %s was found.", deviceAddress));
                    return (Peripheral) device;
                }
            } catch (NullPointerException e) {
                iterator.remove();
                Log.w(TAG, String.format("getConnectedPeripheral -> Device with address %s was disconnected.", deviceAddress));
            }
        }
        return null;
    }

    /**
     * Asks for a service with a particular name.
     *
     * @param deviceAddress of the peripheral.
     * @param serviceName   name of the service.
     * @return {@link com.sensirion.libble.bleservice.PeripheralService}
     */
    public PeripheralService getServiceWithName(final String deviceAddress, final String serviceName) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getServiceWithName -> Device with address %s not found.", deviceAddress));
            return null;
        }
        return device.getPeripheralService(serviceName);
    }

    /**
     * Ask for the a characteristic of a service
     *
     * @param characteristicName name of the characteristic.
     * @return {@link java.lang.Object} with the characteristic parsed by the service - <code>null</code> if no service was able to parse it.
     */
    public Object getCharacteristicValue(final String deviceAddress, final String characteristicName) {
        final Peripheral device = getConnectedPeripheral(deviceAddress);
        if (device == null) {
            Log.e(TAG, String.format("getCharacteristicValue -> Device with address %s did not found the characteristic with name: %s", deviceAddress, characteristicName));
            return null;
        }
        return device.getCharacteristicValue(characteristicName);
    }

    /**
     * Register a listener in all connected peripherals.ho
     *
     * @param listener pretending to listen for notifications in all peripherals.
     */
    public void registerPeripheralListenerToAllConnected(final NotificationListener listener) {
        registerPeripheralListener(listener, null);
    }


    /**
     * Registers a listener in a connected peripheral.
     *
     * @param address  address of the peripheral we want to listen to,
     *                 <code>null</code> if we want to register a listener to all connected devices.
     * @param listener pretending to listen for notifications of a peripheral.
     */
    public void registerPeripheralListener(final NotificationListener listener, final String address) {
        for (BleDevice device : getConnectedBleDevices()) {
            if (address == null || device.getAddress().equals(address)) {
                ((Peripheral) device).registerPeripheralListener(listener);
                if (address == null) {
                    continue;
                }
                break;
            }
        }
    }

    /**
     * Unregister a listener from all connected peripherals.
     *
     * @param listener that does not want to get notifications any more.
     */
    public void unregisterPeripheralListenerFromAllConnected(final NotificationListener listener) {
        unregisterPeripheralListener(listener, null);
    }

    /**
     * Unregister a listener from a connected peripheral.
     *
     * @param listener that wants to unregister from the notifications of a peripheral.
     * @param address  of the peripheral you don't want to get notifications from anymore.
     */
    public void unregisterPeripheralListener(final NotificationListener listener, final String address) {
        final Peripheral device = getConnectedPeripheral(address);
        if (device == null) {
            Log.e(TAG, "unregisterPeripheralListener -> Device with address " + address + " not found.");
        } else {
            device.unregisterPeripheralListener(listener);
        }
    }

    /**
     * Checks if bluetooth connection is enabled on the device.
     *
     * @return <code>true</code> if it's enabled - <code>false</code> otherwise.
     */
    public boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter was not found.");
            return false;
        }
        return bluetoothAdapter.isEnabled();
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context of the requesting activity.
     */
    public void requestEnableBluetooth(final Context context) {
        if (isBluetoothEnabled()) {
            Log.d(TAG, "Bluetooth is enabled");
        } else {
            Log.d(TAG, "Bluetooth is disabled");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
        }
    }

    /**
     * Checks if a device is connected.
     *
     * @param deviceAddress of the device.
     * @return <code>true</code> if connected - <code>false</code> otherwise.
     */
    public boolean isDeviceConnected(final String deviceAddress) {
        for (BleDevice device : mBlePeripheralService.getConnectedPeripherals()) {
            if (device.getAddress().equals(deviceAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enables/disables notifications for all connected devices.
     *
     * @param enabled <code>true</code> for enabling notifications - <code>false</code> otherwise.
     */
    public void setAllNotificationsEnabled(final boolean enabled) {
        Log.i(TAG, String.format("%s all the notifications in all the devices.", (enabled) ? "Enabled" : "Disabled"));
        for (BleDevice device : getConnectedBleDevices()) {
            ((Peripheral) device).setAllNotificationsEnabled(enabled);
        }
    }

    /**
     * Counts the number of services.
     *
     * @param address of the peripheral.
     * @return number of discovered services.
     */
    public int getNumberOfDiscoveredServices(final String address) {
        final Peripheral device = getConnectedPeripheral(address);
        if (device == null) {
            Log.e(TAG, "getNumberOfDiscoveredServices -> Device with address " + address + " not found.");
            return -1;
        }
        return device.getNumberOfDiscoveredServices();
    }

    /**
     * Obtains the names of each discovered service.
     *
     * @param address of the peripheral.
     * @return {@link java.util.List} with the services names.
     */
    public List<String> getDiscoveredServicesNames(final String address) {
        final Peripheral device = getConnectedPeripheral(address);
        if (device == null) {
            Log.e(TAG, "getDiscoveredServicesNames -> Device with address " + address + " not found.");
            return null;
        }
        return device.getDiscoveredServicesNames();
    }
}
