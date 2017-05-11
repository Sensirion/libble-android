# libble-android

Android Bluetooth library to communicate with Bluetooth low energy (BLE) devices

This library is a wrapper around the [BLE stack of Android](https://developer.android.com/guide/topics/connectivity/bluetooth-le.html).
It fixes common pitfalls and shortcomings of the Android libraries.

## Installation

### Gradle

Add the following snippet to your build.gradle file

```gradle
dependencies {
    compile 'com.sensirion:libble:2.1.0'
}
```

Then, sync your project with the updated gradle file

### Non-gradle

Go to [this page](https://bintray.com/sensimobilesw/maven/libble-android)
and download the latest version. Then, import it to the IDE of your choice.

## Using the library

### STEP 1: Bind a BleService to a context

 * Create a class that implements the ```ServiceConnection``` interface and its functions
   ```onServiceConnected(ComponentName name, IBinder service)``` and
   ```onServiceDisonnected(ComponentName name)```.
 * Create an intent with BleService.class as second argument, such as
   ```intent = new Intent(context, BleService.class);```
 * Bind the service: ```context.bindService(intent, new LibBleServiceConnection(), Context.BIND_AUTO_CREATE);```

Once a connection to the BleService is established, ```onServiceConnected()``` is called.
If the connection to the BleService is lost, ```onServiceDisconnected()``` is called.

You can use the following code snippet from our demo app for guidance:

```java
class LibBleServiceConnection implements ServiceConnection {
    private final MainActivity mCallback;

    LibBleServiceConnection(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        if (service instanceof BleService.LocalBinder) {
            BleService bleService = ((BleService.LocalBinder) service).getService();
            if (bleService.initialize()) {
                mCallback.onServiceConnected(bleService);
            } else {
                throw new IllegalStateException("BLE service was not successfully initialized");
            }
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mCallback.onServiceDisconnected();
    }
}
```

### STEP 2: Create a BroadcastReceiver and register an intent filter

To receive the asynchronously reported connection intents, you need to create a class that extends
Android's ```BroadcastReceiver``` class and its abstract function ```onReceive()```:

```java
class LibBleBroadcastReceiver extends BroadcastReceiver {

    private final MainActivity mCallback;

    LibBleBroadcastReceiver(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        final String device_address = intent.getStringExtra(BleService.EXTRA_DEVICE_ADDRESS);
        final String characteristicUUID = intent.getStringExtra(BleService.EXTRA_CHARACTERISTIC_UUID);
        final String data = intent.getStringExtra(BleService.EXTRA_DATA);
        switch (action) {
            case BleService.ACTION_GATT_CONNECTED:
                // A new device connected
                mCallback.onConnection(device_address);
                break;
            case BleService.ACTION_GATT_DISCONNECTED:
                // A device disconnected
                mCallback.onDisconnection(device_address);
                break;
            case BleService.ACTION_GATT_SERVICES_DISCOVERED:
                // A new service has been discovered
                break;
            case BleService.ACTION_DATA_AVAILABLE:
                // There is some data available
                break;
            case BleService.ACTION_DID_WRITE_CHARACTERISTIC:
                // Characteristics have been written
                break;
            case BleService.ACTION_DID_FAIL:
                // Something failed
                break;
        }
    }
}
```

Then, register an intent filter for it. You can use this code snippet for this:

```java
BroadcastReceiver receiver = new LibBleBroadcastReceiver();
IntentFilter intentFilter = new IntentFilter();
intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
intentFilter.addAction(BleService.ACTION_DID_WRITE_CHARACTERISTIC);
intentFilter.addAction(BleService.ACTION_DID_FAIL);
registerReceiver(receiver, intentFilter);
```

### STEP 3: Extend the BleScanCallback class to receive scanning callbacks

Create your own class which extends the ```BleScanCallback``` class. It has some useful callback functions:

 * onScanResult(int callbackType, ScanResult result)
 * onBatchScanResults(List&lt;ScanResults&gt; results)
 * onScanStopped()
 * onScanFailed(int errorCode)

You can use the following code snippet from our demo app for guidance:

```java
class LibBleScanCallback extends BleScanCallback {
    private final MainActivity mCallback;

    LibBleScanCallback(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        //
        mCallback.onScanResult(result);
    }

    @Override
    public void onBatchScanResult(List<ScanResult> results) {
        for (ScanResult result : results) {
            mCallback.onScanResult(result);
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        // errorCode is one of the BleScanCallback.SCAN_FAILED_* constants (see documation below)
        mCallback.onScanFailed(errorCode);
    }

    @Override
    public void onScanStopped() {
        mCallback.onScanStopped();
    }
}
```

### STEP 4: Request location permission

Android needs location permission to scan for BLE devices. Because of that, you have to request
permission if you don't have it.

```java
if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
}
```

You can use Androids ```OnRequestPermissionsResultCallback``` interface to get easily notified when
the user accepted the permission request.

### STEP 5: Put everything together

```java
public class MainActivity extends AppCompatActivity {
    private BleService mBleService;
    private BleScanCallback mScanCallback;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Instantiate your BroadcastReceiver
        mBroadcastReceiver = new LibBleBroadcastReceiver(this);

        // Instantiate your BleScanCallback
        mScanCallback = new LibBleScanCallback(this);

        // Create an intent for binding to the ServiceConnection
        Intent intent = new Intent(this, BleService.class);

        // Instantiate your ServiceConnection
        ServiceConnection connection = new LibBleServiceConnection(this);

        // Bind to the service
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Create an IntentFilter to filter for the BleService intents
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_DID_WRITE_CHARACTERISTIC);
        intentFilter.addAction(BleService.ACTION_DID_FAIL);

        // Register the BroadcastReceiver with the defined IntentFilter to receive the intents
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister the BroadcastReceiver. It is not needed anymore when the activity gets paused
        unregisterReceiver(mBroadcastReceiver);
    }


    /*
     * Service connection callbacks
     */

    public void onServiceConnected(BleService service) {
        // The BLE servie is ready to start scanning and interacting with BLE devices
        mBleService = service;
    }

    public void onServiceDisconnected() {
        // The BLE service disconnected. Scans are not possible anymore
        mBleService = null;
    }

    /*
     * Scan callbacks
     */

    public void onScanResult(ScanResult result) {
        // There is a new or updated scan result
        String device_address = result.getDevice().getAddress();
        String device_name = result.getDevice().getName();
        int signal_strength = result.getRssi();
    }

    public void onScanStopped() {
        // The scan finished
    }

    public void onScanFailed(int errorCode) {
        // The scan failed for some reason. Use the errorCode to determine what happened
        switch(errorCode) {
            case LibBleScanCallback.SCAN_FAILED_ALREADY_STARTED:
                // Scan was not started as another BLE scan with the same settings has already been
                // started by the app
                break;
            case LibBleScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                // Scan was not started as app cannot be registered
                break;
            case LibBleScanCallback.SCAN_FAILED_INTERNAL_ERROR:
                // There was an internal error
                break;
            case LibBleScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED:
                // Fails to start power optimized scan as this feature is not supported
                break;
            case LibBleScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                // Scan not started as it is out of hardware resources.
                break;
            default:
                throw new IllegalArgumentException("Unknown error code!");
        }
    }
}
```

HINT: You can have a look at the used classes in the source code of our demo app.

### STEP 6: Start scanning and connecting

```java
public class MainActivity extends AppCompatActivity {
    // ...

    private static final int SCAN_DURATION_MS = 60000;

    /*
     * Set these filter variables to filter for specific device names or UUIDs
     *
     * If null, no filter will be applied while scanning
     */
    private static final String[] DEVICE_NAME_FILTER = null;
    private static final String[] UUID_FILTER = null;

    /*
     * Scanning button implementations
     */

    public void startDiscovery(View view) {
        String location_permission = Manifest.permission.ACCESS_COARSE_LOCATION;
        if (ContextCompat.checkSelfPermission(this, location_permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{location_permission}, 0);
            return;
        }
        mBleService.startScan(mScanCallback, SCAN_DURATION_MS, DEVICE_NAME_FILTER, UUID_FILTER);
    }

    public void stopDiscovery(View view) {
        mBleService.stopScan(mScanCallback);
    }

    /*
     * Connection functions
     *
     * The functions do not have a return value, the connection status is asynchronously reported to
     * the BroadcastReceiver described above
     */
    public void connect(String device_address) {
        mBleService.connect(device_address);
    }

    public void disconnect(String device_address) {
        mBleService.disconnect(device_address);
    }
}
```

## Manifest and user permissions

If you are using the LibBLE library, you need three different permissions

 * Bluetooth
 * Bloetooth Admin
 * Location (coarse or fine)

The first one is quite self-explanatory. For the last two, let's have a look at the documentation
provided by Android. The LibBLE library uses the
[BluetoothLeScanner.startScan()](https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner.html#startScan(java.util.List%3Candroid.bluetooth.le.ScanFilter%3E,%20android.bluetooth.le.ScanSettings,%20android.bluetooth.le.ScanCallback))
function to scan for BLE devices. There, it says:

"Requires BLUETOOTH_ADMIN permission. An app must hold ACCESS_COARSE_LOCATION or
ACCESS_FINE_LOCATION permission in order to get results."

The two bluetooth permissions are already requested by LibBLE. So you just have to add the location
permission to your manifest file AndroidManifest.xml. Use this example manifest for guidance:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest
    xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.libbledemo">

    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true"/>

    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity android:name="com.example.libbledemo.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

To request location permission, have a look at step 4 above.

## Documentation

### BleService

```java
class BleService extends Service {
    /**
     * Initializes the BleService.
     *
     * @return Return true if the initialization is successful or if it has already been initialized,
     * false otherwise. Reasons for a failed initialization can be if BLE is not supported or if the
     * permissions are not given.
     */
    public boolean initialize();

    /**
     * Starts a BLE Scan. Discovered devices are reported via the delivered callback.
     *
     * @param callback                    An instance of the BleScanCallback, used to receive scan results.
     * @param durationMs                  The duration in milliseconds, how long the scan should last. This parameter
     *                                    must be greater or equal to 1000 ms.
     * @param deviceNameFilter            A array of device names to filter for. Only BLE devices with these
     *                                    names are reported to the callback.
     * @param advertisedServiceUuidFilter An Array of advertised service UUIDs to also deliver results for.
     * @return true if a scan was triggered and false, if it was not possible to trigger a scan or
     * if there is already an ongoing scan running.
     */
    public boolean startScan(@NonNull final BleScanCallback callback, final long durationMs,
                             final String[] deviceNameFilter, final String[] advertisedServiceUuidFilter);

    /**
     * Stops an ongoing BLE Scan for the given callback.
     *
     * @param callback An instance of the BleScanCallback, used to start the scan.
     */
    public void stopScan(@NonNull final BleScanCallback callback);

    /**
     * Retrieve a list of device addresses of all the connected devices.
     *
     * @return List of all device addresses of the BLE devices currently connected.
     */
    public List<String> getConnectedDevices();

    /**
     * Retrieves a Map of Characteristic UUID Descriptor to BluetoothGattCharacteristic supported
     * by this device.
     *
     * @param deviceAddress The device address identifying this device.
     * @param uuids         A list of descriptor UUIDs of the desired characteristics.
     * @return A Mapping with the characteristic UUIDs provided as uuids parameter and the corresponding characteristics.
     */
    @NonNull
    public Map<String, BluetoothGattCharacteristic> getCharacteristics(@NonNull final String deviceAddress,
                                                                       final List<String> uuids);

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param deviceAddress The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the intent action {@code ACTION_GATT_CONNECTED}
     */
    public boolean connect(@NonNull final String deviceAddress);

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the intent action {@code ACTION_GATT_DISCONNECTED}
     *
     * @param deviceAddress The device address of the destination device.
     */
    public void disconnect(@NonNull final String deviceAddress);

    /**
     * Convenience method to request a read on a given {@code BluetoothGattCharacteristic}. See
     * {@code readCharacteristic} for more details.
     * The read result is reported asynchronously through the intent action {@code ACTION_DATA_AVAILABLE}.
     *
     * @param deviceAddress      The device address of the destination device.
     * @param characteristicUuid The uuid of the characteristic to read from.
     */
    public void readCharacteristic(@NonNull final String deviceAddress,
                                   final String characteristicUuid);

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the intent action {@code ACTION_DATA_AVAILABLE}.
     *
     * @param deviceAddress  The device address of the destination device.
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(@NonNull final String deviceAddress,
                                   final BluetoothGattCharacteristic characteristic);

    /**
     * Request a write on a given {@code BluetoothGattCharacteristic}.
     *
     * @param deviceAddress  The device address of the destination device.
     * @param characteristic The characteristic to write to.
     */
    public void writeCharacteristic(@NonNull final String deviceAddress,
                                    final BluetoothGattCharacteristic characteristic);
}
```

### BleScanCallback

```java
class BleScanCallback extends ScanCallback {
    /**
     * Fails to start scan as BLE scan with the same settings is already started by the app.
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;

    /**
     * Fails to start scan as app cannot be registered.
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;

    /**
     * Fails to start scan due an internal error
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;

    /**
     * Fails to start power optimized scan as this feature is not supported.
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;

    /**
     * Fails to start scan as it is out of hardware resources.
     * @hide
     */
    public static final int SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5;

    /**
     * Callback when a BLE advertisement has been found.
     *
     * @param callbackType Determines how this callback was triggered. Could be one of
     *            {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
     *            {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
     *            {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
     * @param result A Bluetooth LE scan result.
     */
    public void onScanResult(int callbackType, ScanResult result);

    /**
     * Callback when batch results are delivered.
     *
     * @param results List of scan results that are previously scanned.
     */
    public void onBatchScanResults(List<ScanResult> results);

    /**
     * Callback when scan could not be started.
     *
     * @param errorCode Error code (one of SCAN_FAILED_*) for scan failure.
     */
    public void onScanFailed(int errorCode);

    /**
     * Callback when scan has stopped.
     */
    public void onScanStopped();
}
```
