# libble-smartgadget

Android Bluetooth library to communicate with Sensirion SmartGadgets

## Installation

### Gradle

Add the following snippet to your build.gradle file

```gradle
dependencies {
    compile 'com.sensirion:libble:2.1.1'
    compile 'com.sensirion:libsmartgadget:1.1.6'
}
```

### Non-gradle

Go to [this page](https://bintray.com/sensimobilesw/maven/libsmartgadget-android)
and download the latest version of LibSmartGadget.

Go to [this page](https://bintray.com/sensimobilesw/maven/libble-android)
and download the latest version of LibBle.

Then, import the libraries to the IDE of your choice.

## Using the library

### STEP 1: Implement the GadgetManagerCallback interface

```java
public class MyGadgetManagerCallback implements GadgetManagerCallback {

    /**
     * Called when the GadgetManager was successfully initialized.
     */
    void onGadgetManagerInitialized() {
        //
    }

    /**
     * Called when the GadgetManager initialization failed. Check your devices's BLE capabilities and
     * Bluetooth permissions. You need location permission for bluetooth scanning to work.
     * <p/>
     * <uses-feature
     * android:name="android.hardware.bluetooth_le"
     * android:required="true"/>
     * <p/>
     * <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
     */
    void onGadgetManagerInitializationFailed() {
        //
    }

    /**
     * Callback to report found gadgets after the {@link GadgetManager#startGadgetDiscovery(long)}
     * was called.
     *
     * @param gadget The discovered {@link Gadget} instance.
     * @param rssi   The received signal strength of the gadget.
     */
    void onGadgetDiscovered(final Gadget gadget, final int rssi) {
        //
    }

    /**
     * Callback when gadget discovery could not be started.
     */
    void onGadgetDiscoveryFailed(){
        //
    }

    /**
     * Called when the discovery has stopped after the predefined duration time or if
     * {@link GadgetManager#stopGadgetDiscovery()} was called.
     */
    void onGadgetDiscoveryFinished(){
        //
    }
}
```

### STEP 2: Create and initialize the GadgetManager

```java
public class MainActivity extends AppCompatActivity {
    private GadgetManagerCallback mGMCallback;
    private GadgetManager mGadgetManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGMCallback = new MyGadgetManagerCallback();
        mGadgetManager = GadgetManagerFactory.create(mGMCallback);
        mGadgetManager.initialize(this);
    }
}
```

### STEP 3: Check for scanning permissions

Android needs location permission to scan for BLE devices. Because of that, you have to request
permission if you don't have it.

```java
public class MainActivity extends AppCompatActivity {
    // ...

    private void startScanning() {
        if (!BLEUtility.hasScanningPermission(this)) {
            BLEUtility.requestScanningPermission(this, LOCATION_PERMISSION);
            return;
        }

        // ...
    }
}
```

After the user decides to either accept or deny the permission request,
```onRequestPermissionsResult()``` is called.

```java
public class MainActivity extends AppCompatActivity {
    // ...

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start scanning
                    onScanButtonClick(null);
                }
                // Otherwise: Permission denied, do nothing
                break;
        }
    }
}
```

### STEP 4: Start scanning

```java
public class MainActivity extends AppCompatActivity {
    private static final int SCANNING_DURATION_MS = 60_000;
    private static final String[] NAME_FILTER = {
        "SHTC1 smart gadget",
        "SHTC1 smart gadget\u0002",
        "Smart Humigadget",
        "SensorTag"
    };
    private static final String[] UUID_FILTER = {
        SHT3xTemperatureService.SERVICE_UUID,
        SHT3xHumidityService.SERVICE_UUID,
        SHTC1TemperatureAndHumidityService.SERVICE_UUID,
        SensorTagTemperatureAndHumidityService.SERVICE_UUID
    };

    // ...

    private void startScanning() {
        // ...

        if (!mGadgetManager.startGadgetDiscovery(SCAN_DURATION_MS, NAME_FILTER, UUID_FILTER)) {
            // Failed with starting a scan
            Log.e(TAG, "Could not start discovery");
        }

        // Successfully started scanning
        // Discovered Gadgets are reported back through the GadgetManagerCallback implementation
    }
}
```

If you wish to stop a scan early, simply call ```mGadgetManager.stopGadgetDiscovery();```

### STEP 5: Connecting to gadgets

To receive callbacks when the connection status of a Gadget changes or if there is some new data
available, you need to add an implementation of ```GadgetListener``` as a listener to the gadget

```java
public class MainActivity extends AppCompatActivity {
    private GadgetListener mGadgetListener;

    // ...

    private connectToGadget(Gadget gadget) {
        gadget.addListener(mGadgetListener);
        mGadgetManager.connect(gadget);

        // As soon as the gadget connection is established,
        // mGadgetListener.onGadgetConnected(gadget) gets called.
    }

    private disconnectFromGadget(Gadget gadget) {
        mGadgetListener.disconnect(gadget);

        // As soon as the gadget connection is terminated (or lost),
        // mGadgetListener.onGadgetDisconnect(gadget) gets called.

        // Remove the gadget listener to clean up
        gadget.removeListener(mGadgetListener);
    }
}
```

### STEP 6: Gadget services

There are multiple gadget services available in order to get status data from a gadget and also
download logged data on it. These are the currently supported services:

 * DeviceInformationService
 * GadgetDownloadService
   * SHT3xHistoryService
   * SHTC1HistoryService
 * GadgetNotificationService
   * BatteryService
   * SHT3xHumidityService
   * SHT3xTemperatureService
   * SHTC1TemperatureAndHumidityService
   * SensorTagTemperatureAndHumidityService

In order to get data from these services, you first have to check whether they are available

```java
public class MainActivity extends AppCompatActivity {
    // ...

    private boolean checkIfServiceSupported(Gadget gadget, Class<? extends GadgetService> gadgetServiceClass) {
        return gadget.supportsServiceOfType(gadgetServiceClass);
    }
}
```

Then, you can get the services and load the data

```java
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    // ...

    private List<GadgetService> getServicesWithType(Gadget gadget, Class<? extends GadgetService> gadgetServiceClass) {
        return gadget.getServicesOfType(gadgetServiceClass);
    }

    private GadgetService getServiceOfType(Gadget gadget, Class<? extends GadgetService> gadgetServiceClass) {
        final List<GadgetService> services = gadget.getServicesOfType(gadgetServiceClass);
        if (services.size() == 0) {
            return null;
        }

        if (services.size() > 1) {
            Log.w(TAG, String.format("Multiple services of type %s available - Application can only handle one", gadgetServiceClass));
        }

        return services.get(0);
    }

    private GadgetValue[] loadValues(GadgetService service) {
        return service.getLastValues();
    }
}
```

You can also get a list of all the available services by simply calling ```gadget.getServices();```.


#### Example 1: Get the battery level

There are two different ways to get the current battery level: Either by getting the last values of
the BatteryService or as live update

 * Getting last battery level

```java
public class MainActivity extends AppCompatActivity {
    private static final int UNKNOWN_BATTERY_LEVEL = -1;
    // ...

    private int getBatteryLevel() {
        final GadgetService batteryService = getServiceOfType(mGadget, BatteryService.class);
        if (batteryService == null) {
            return UNKNOWN_BATTERY_LEVEL;
        }
        final GadgetValue[] lastValues = batteryService.getLastValues();
        return (lastValues.length > 0) ? lastValues[0].getValue().intValue() : UNKNOWN_BATTERY_LEVEL;
    }
}
```

 * Live update

In your ```GadgetListener``` implementation, you can use the ```onGadgetValuesReceived()``` method
to get live data updates

```java
public class MyGadgetListener implements GadgetListener {
    // ...

    public void onGadgetValuesReceived(Gadget gadget, GadgetService service, GadgetValue[] values) {
        if (service instanceof BatteryService) {
            int level = (int) values[0].getValue();
        }
    }

    // ...
}
```

#### Example 2: Download logged data

```java
public class MainActivity extends AppCompatActivity {
    // ...

    private boolean downloadLoggedData() {
        final GadgetDownloadService downloadService = (GadgetDownloadService) getServiceOfType(mGadget, GadgetDownloadService.class);
        if (downloadService == null) {
            return false;
        }

        // Return true if a download was initiated
        // Data gets reported through callback GadgetListener.onGadgetDownloadDataReceived()
        return downloadService.download();
    }
}
```

## Documentation of the Interfaces

### GadgetManager Interface

```java
/**
 * The GadgetManager is the main interface to interact with Sensirion Smart Gadgets. It provides
 * functions to initialize the communication stack and find gadgets in range. See {@link Gadget} for
 * more information on how to connect to the found gadgets. Note that only Gadget instance received
 * via {@link GadgetManagerCallback#onGadgetDiscovered(Gadget, int)} can be used to establish a
 * connection.
 */
public interface GadgetManager {
    /**
     * Initialize the communication stack and register a {@link GadgetManagerCallback}.
     * You must call this method at least once to initialize the library. You will get notified
     * asynchronously as soon as the library has finished initializing via
     * {@link GadgetManagerCallback#onGadgetManagerInitialized()}.
     *
     * @param applicationContext the application context instance.
     */
    void initialize(@NonNull final Context applicationContext);

    /**
     * After the GadgetManager has been initialized, you can register/add your own GadgetServices
     * and let the library handle the detection and decoding using your GadgetService class.
     *
     * @param serviceUuid  The UUID of the service to be added.
     * @param serviceClass The Class of your GadgetService.
     */
    void registerCustomGadgetService(@NonNull final String serviceUuid,
                                     @NonNull final Class<? extends GadgetService> serviceClass);

    /**
     * Call this method if you don't plan to use the library anymore. This makes sure all resources
     * of the library are properly freed.
     *
     * @param applicationContext the application context instance.
     */
    void release(@NonNull final Context applicationContext);

    /**
     * Check if the library is ready and was successfully initialized.
     *
     * @return true if the library is ready to be used.
     */
    boolean isReady();

    /**
     * Starts a scan for Sensirion Smart Gadgets.
     *
     * @param durationMs                  The duration how long the library should scan for. Make sure not to scan
     *                                    for too long to prevent a large battery drain.
     * @param advertisedNameFilter        An Array of advertised gadget names to only deliver results for.
     *                                    Provide null or an empty array to discover all gadgets in range.
     * @param advertisedServiceUuidFilter An Array of advertised service UUIDs to also deliver results for.
     * @return true if the scan was successfully initiated.
     */
    boolean startGadgetDiscovery(final long durationMs, final String[] advertisedNameFilter,
                                 String[] advertisedServiceUuidFilter);

    /**
     * Stops an ongoing scan for Smart Gadgets. Nothing happens if there is no scan running.
     */
    void stopGadgetDiscovery();
}
```

### GadgetManagerCallback Interface

```java
public interface GadgetManagerCallback {
    /**
     * Called when the GadgetManager was successfully initialized.
     */
    void onGadgetManagerInitialized();

    /**
     * Called when the GadgetManager initialization failed. Check you devices's BLE capabilities and
     * Bluetooth permissions. You need location permission for bluetooth scanning to work.
     * <p/>
     * <uses-feature
     * android:name="android.hardware.bluetooth_le"
     * android:required="true"/>
     * <p/>
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
     */
    void onGadgetManagerInitializationFailed();

    /**
     * Callback to report found gadgets after the {@link GadgetManager#startGadgetDiscovery(long)}
     * was called.
     *
     * @param gadget The discovered {@link Gadget} instance.
     * @param rssi   The received signal strength of the gadget.
     */
    void onGadgetDiscovered(final Gadget gadget, final int rssi);

    /**
     * Callback when gadget discovery could not be started.
     */
    void onGadgetDiscoveryFailed();

    /**
     * Called when the discovery has stopped after the predefined duration time or if
     * {@link GadgetManager#stopGadgetDiscovery()} was called.
     */
    void onGadgetDiscoveryFinished();
}
```

### Gadget Interface

```java
/**
 * The interface all Gadget Objects have to implement providing each Gadget's basic functionality.
 */
public interface Gadget {

    /**
     * Returns the gadget's name advertised by the BLE device.
     *
     * @return The name of the gadget.
     */
    @NonNull
    String getName();

    /**
     * Returns the gadget hardware address.
     *
     * @return String representation of the gadget's hardware address.
     */
    @NonNull
    String getAddress();

    /**
     * Connects the given Gadget and try to discover all the supported services.
     *
     * @return true if the connect call was dispatched. The confirmation if the connection and
     * service discovery was successful will be delivered to the registered {@link GadgetListener}
     * instances via {@link GadgetListener#onGadgetConnected(Gadget)}.
     */
    boolean connect();

    /**
     * Disconnects the given Gadget.
     */
    void disconnect();

    /**
     * Check the connection state of the Gadget.
     *
     * @return true if the gadget is connected and its services are available, false otherwise.
     */
    boolean isConnected();

    /**
     * Register a {@link GadgetListener} to receive Gadget related state changes.
     *
     * @param callback the callback instance implementing {@link GadgetListener}.
     */
    void addListener(@NonNull GadgetListener callback);

    /**
     * Unregister a {@link GadgetListener} on which Gadget related state changes are received.
     *
     * @param callback the callback instance implementing {@link GadgetListener} you want to remove.
     */
    void removeListener(@NonNull GadgetListener callback);

    /**
     * Makes the gadget subscribe to all services of type {@link GadgetNotificationService}.
     */
    void subscribeAll();

    /**
     * Makes the gadget unsubscribe from all services of type {@link GadgetNotificationService}
     * it is subscribed to.
     */
    void unsubscribeAll();

    /**
     * Makes the gadget initiate an asynchronous internal values update of all its services of type
     * {@link GadgetService}.
     */
    void refresh();

    /**
     * Returns all services supported by this gadget.
     *
     * @return A list of all supported GadgetServices.
     */
    @NonNull
    List<GadgetService> getServices();

    /**
     * Checks if the Gadget provides at least one service of the given class. You can use
     * {@link GadgetNotificationService} or {@link GadgetDownloadService} or any other Service
     * interface to check for basic functionality of the Gadget's Services.
     *
     * @param gadgetServiceClass The class representing the desired service of a gadget.
     * @return true if the service is supported by the gadget, else false is returned.
     */
    boolean supportsServiceOfType(@NonNull Class<? extends GadgetService> gadgetServiceClass);

    /**
     * Returns all services that are instances of the given class. You can use
     * {@link GadgetNotificationService} or {@link GadgetDownloadService} or any other Service
     * interface to check for basic functionality of the Gadget's Services. All services are
     * returned If {@link GadgetService} is provided (then use {@link Gadget#getServices()}
     * instead).
     *
     * @param gadgetServiceClass The class representing the desired service of a gadget.
     * @return A list of the GadgetServices described by the provided parameter.
     */
    @NonNull
    List<GadgetService> getServicesOfType(@NonNull Class<? extends GadgetService> gadgetServiceClass);
}
```

### GadgetListener Interface

```java
public interface GadgetListener {

    /**
     * Callback reporting that the gadget connection and service discovery initiated by the
     * {@link Gadget#connect()} function was successful.
     *
     * @param gadget The gadget the connection was established to.
     */
    void onGadgetConnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that the gadget's connection got lost. This can happen even if
     * {@link Gadget#disconnect()} was not called.
     *
     * @param gadget The gadget to which the connection got lost.
     */
    void onGadgetDisconnected(@NonNull Gadget gadget);

    /**
     * Callback reporting that there were new values received for the given {@link GadgetService}.
     *
     * @param gadget  The gadget the values were sent from.
     * @param service The dedicated service.
     * @param values  the received values.
     */
    void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service,
                                @NonNull GadgetValue[] values);

    /**
     * Callback reporting that there were new values downloaded from the given
     * {@link GadgetDownloadService}.
     *
     * @param gadget   The gadgets from which the values are coming from.
     * @param service  The dedicated download service.
     * @param values   the received values.
     * @param progress the delivery progress in percent
     */
    void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service,
                                      @NonNull GadgetValue[] values, int progress);


    /**
     * Callback when the logging state change has failed.
     *
     * @param gadget  The gadget on which the service is running.
     * @param service The service used to change the logging feature state.
     */
    void onSetGadgetLoggingEnabledFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Called when the setting of the logger interval has failed.
     *
     * @param gadget  The gadget on which the download service is running.
     * @param service The service providing the interval change feature.
     */
    void onSetLoggerIntervalFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Called when the logger interval was updated.
     *
     * @param gadget   The gadget on which the logger interval was changed.
     */
    void onSetLoggerIntervalSuccess(@NonNull Gadget gadget);

    /**
     * Callback when the download has failed.
     *
     * @param gadget  The gadget from which data was downloaded.
     * @param service The service used to download the data.
     */
    void onDownloadFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Callback when the download is completed.
     *
     * @param gadget  The gadget from which data was downloaded.
     * @param service The service used to download the data.
     */
    void onDownloadCompleted(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

    /**
     * Callback when there is no data available for download.
     *
     * @param gadget  The gadget from which data download was tried.
     * @param service The service trying to download the data.
     */
    void onDownloadNoData(@NonNull Gadget gadget, @NonNull GadgetDownloadService service);

}
```

### GadgetValue Interface

```java
public interface GadgetValue {

    /**
     * Retrieve time when this value was created.
     *
     * @return the time when this value was created.
     */
    @NonNull
    Date getTimestamp();

    /**
     * Getter for the value.
     *
     * @return the value received from the gadget.
     */
    @NonNull
    Number getValue();

    /**
     * Getter for the unit of the value.
     *
     * @return the string representing the values's unit.
     */
    @NonNull
    String getUnit();

}
```

### GadgetService Interface

```java
public interface GadgetService {

    /**
     * Requests the Service to update its internal state. Only use this method if there is a problem
     * using the service, like failing data downloads or failing to change the logging interval.
     */
    void requestValueUpdate();

    /**
     * Retrieve the last received, cached GadgetValues. An empty array is returned, if there are no
     * previously received values yet.
     *
     * @return the GadgetValue array last received by the Smart Gadget.
     */
    GadgetValue[] getLastValues();
}
```

### GadgetDownloadService Interface

```java
public interface GadgetDownloadService extends GadgetService {
    /**
     * Returns whether the logging feature of the Gadget can be enabled or disabled.
     *
     * @return true if the state can be changed, false otherwise.
     */
    boolean isGadgetLoggingStateEditable();

    /**
     * Returns if the logging feature is currently enabled.
     *
     * @return true if it is enabled, false otherwise;
     */
    boolean isGadgetLoggingEnabled();

    /**
     * Enables or disables the Gadget's logging feature. Note that if you enable the logging
     * feature, all data stored on the device will be deleted.
     *
     * @param enabled true to enable the feature, false otherwise.
     */
    void setGadgetLoggingEnabled(final boolean enabled);

    /**
     * Sets the interval used by the gadget in milliseconds, in which data points should be
     * internally saved for later download. Note that if you set the logging interval, all data
     * stored on the device will be deleted.
     *
     * @param loggerIntervalMs The interval in milliseconds.
     * @return true if the write request was successfully dispatched, false otherwise.
     */
    boolean setLoggerInterval(final int loggerIntervalMs);

    /**
     * Gets the interval used by the gadget in milliseconds, in which data points should be
     * internally saved for later download.
     *
     * @return the set log interval in milliseconds.
     */
    int getLoggerInterval();

    /**
     * Initiates a download of all the data provided by this service.
     *
     * @return true if the call was successfully dispatched to the gadget.
     */
    boolean download();

    /**
     * To check if there is already an ongoing download running for the given gadget instance.
     *
     * @return true if there already was a download initiated using this particular gadget instance.
     */
    boolean isDownloading();

    /**
     * Returns the current download progress. The return value is not defined, if there is no
     * download running. Use {@link GadgetDownloadService#isDownloading()}
     * to check if there is a download running.
     *
     * @return the download progress.
     */
    int getDownloadProgress();
}
```

### GadgetNotificationService Interface

```java
public interface GadgetNotificationService extends GadgetService {
    /**
     * Subscribe for the notification defined by this service. Notifications will be received via
     * {@link GadgetListener#onGadgetValuesReceived(Gadget, GadgetService, GadgetValue[])}.
     */
    void subscribe();

    /**
     * Unsubscribe from the notification defined by this service.
     */
    void unsubscribe();

    /**
     * Checks if you're already subscribed for the given service notifications.
     *
     * @return true if already subscribed.
     */
    boolean isSubscribed();
}
```

### BLEUtility class

```java
public final class BLEUtility {
    /**
     * Checks if BLE connections are supported and if Bluetooth is enabled on the device.
     *
     * @return <code>true</code> if it's enabled - <code>false</code> otherwise.
     */
    public static boolean isBLEEnabled(@NonNull final Context context);

    /**
     * Runtime request for ACCESS_COARSE_LOCATION. This is required on Android 6.0 and higher in order
     * to perform BLE scans.
     *
     * @param requestingActivity The activity requesting the permission.
     * @param requestCode        The request code used to deliver the user feedback to the calling
     *                           activity.
     */
    public static void requestScanningPermission(@NonNull final Activity requestingActivity,
                                                 final int requestCode);

    /**
     * Checks if a context has scanning permission
     *
     * @param context The context of which the user wants to know if it has scanning permission
     * @return        <code>true</code> if the context has scanning permission - <code>false</code> otherwise.
     */
    public static boolean hasScanningPermission(@NonNull final Context context);

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context {@link android.content.Context} of the requesting activity.
     */
    public static void requestEnableBluetooth(@NonNull final Context context);
}
```
