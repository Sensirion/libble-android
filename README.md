libble
======

Android Bluetooth library to communicate with Bluetooth low energy (BLE).

* The library abstracts Bluetooth services from the library implementation.

* The library provides listener interfaces to listen for device notifications.

* The library is compatible by default with Sensirion Smart Gadgets.

* The library can be extended from outside with custom listeners and custom
services.

## Installation

#### Android Studio

* Add mavenCentral or jcenter repositories
* Add the following snippet to the build.gradle file:

```gradle
dependencies {
   compile project ('com.sensirion:libble')
}
```
#### Eclipse

* Place the library file in the libs library folder and compile the dependencies.

```
Properties > Java Build Path > Tab: Libraries > Add JARs...
```

## Using the library

### STEP 1: Initialize the library

* The library needs to be initialized before the user can use it.

```java
BleManager.getInstance().init(context);
```

*The library is automatically initialized, if the activities
com.sensirion.libble.BleActivity or
com.sensirion.libble.BleSupportV4FragmentActivity
are extended.*

### STEP 2: Register for device state modification changes

#### STEP 2.1: Register for scan state notifications

* Implement the interface com.sensirion.libble.listeners.devices.ScanListener
to receive notifications when the device enables or disables scanning for
new Bluetooth devices.

#### STEP 2.2: Register for device state modification notifications

* Implement the interface
com.sensirion.libble.listeners.devices.DeviceStateListener to receive
notifications when a new device is discovered or when a device is
connected or disconnected.

#### STEP 2.3: Listener registration

* Call the method registerListener(NotificationListener)
(from BleActivity, BleSupportV4FragmentActivity, or BleManager)
to register the listeners from steps 2.1 and 2.2.

##### Example:
```java
import com.sensirion.libble.listeners.devices.BleDeviceStateListener;
import com.sensirion.libble.listeners.devices.BleScanListener;
import com.sensirion.libble.devices.AbstractBleDevice;
import com.sensirion.libble.BleManager;

public class DeviceStateFragment extends Fragment
                                 implements BleDeviceStateListener, BleScanListener {

  @Override
  public void onResume(){
     super.onResume();
     BleManager.getInstance().registerListener(this);
     //Do something else.
  }

  @Override
  public void onPause(){
     super.onPause();
     BleManager.getInstance().unregisterListener(this);
     //Do something else.
  }

 /**
  * This method tells to the user when scan is turned on or off.
  *
  * @param isScanEnabled sets the scan state on/off (true/false)
  */
  @Override
  public void onScanStateChanged(final boolean isScanEnabled){
      //Do something.
  }

  /**
   * NOTE: The services and characteristics of this device are not connected yet.
   * NOTE: The connected device is removed from the library internal discovered list.
   * This method is called when a device is connected.
   *
   * @param device that was connected.
   */
  @Override
  public void onDeviceConnected(@NonNull final AbstractBleDevice device){
      //Do something.
  }

  /**
   * This method is called when a device becomes disconnected.
   *
   * @param device that was disconnected.
   */
  @Override
  public void onDeviceDisconnected(@NonNull final AbstractBleDevice device){
      //Do something.
  }

  /**
   * This method is called when the library discovers a new device.
   *
   * @param device that was discovered.
   */
  @Override
  public void onDeviceDiscovered(@NonNull final AbstractBleDevice device){
      //Do something.
  }

  /**
   * This method is called when all the device services are discovered.
   */
  @Override
  public void onDeviceAllServicesDiscovered(@NonNull final AbstractBleDevice device){
      //Do something.
  }
}
```

### STEP 3: Connect to a BLE device (*AbstractBleDevice*)

##### STEP 3.1: Scan for *AbstractBleDevices*

###### OPTION 1: Scan for all devices
```java
BleManager.getInstance().startScanning()
```

###### OPTION 2: Scan for devices with specific UUIDs
```java
private void scanForBleDevicesUsingUUIDs (@NonNull final UUID [] validDeviceUUIDs) {
  BleManager.getInstance().startScanning(validDeviceUUIDs)
}
```

##### STEP 3.2: Retrieve discovered devices

###### OPTION 1: Retrieve all discovered devices
```java
@NonNull
public Iterable <? extends AbstractBleDevice> getDiscoveredBleDevices() {
    return BleManager.getInstance().getDiscoveredBleDevices();
}
```

###### OPTION 2: Retrieve discovered devices with specific advertise names
```java
@NonNull
public Iterable<? extends AbstractBleDevice> getDiscoveredBleDevices(){
   final List acceptedAdvertiseNames = new LinkedList();
   acceptedAdvertiseNames.add("SHTC1 SmartGadget");
   acceptedAdvertiseNames.add("Smart Humigadget");
   return BleManager.getInstance().getDiscoveredBleDevices(acceptedAdvertiseNames);
}
```

##### STEP 3.3: Connect to the device

###### OPTION 1: Connect using the *AbstractBleDevice* object
```java
public static void connectDevice (@NonNull AbstractBleDevice device){
   device.connect();
}
```

###### OPTION 2: Connect using the device address
```java
BleManager.getInstance().connectDevice(deviceAddress);
```

### STEP 4: Register service listeners

The library abstracts all BLE services. Hence, only the corresponding
listeners need to be implemented and called from the BleManager's
registerListener(NotificationListener) or
registerDeviceListener(deviceAddress, NotificationListener) methods.

###### Example:
```java
import com.sensirion.libble.BleManager;
import com.sensirion.libble.listeners.services.HumidityListener;

/**
 * Example fragment that listens for humidity notifications.
 */
public class HumidityListenerFragment extends Fragment implements HumidityListener {

 @Override
 public onResume(){
    super.onResume();
    //Registers this humidity listener in all the peripherals.
    BleManager.getInstance().registerListener(this);
 }

 @Override
 public onPause(){
   super.onPause();
   //Registers this humidity listener in all the peripherals.
   BleManager.getInstance().unregisterListener(this);
 }

 /**
  * Advices the listeners that a new humidity value was obtained.
  *
  * @param device     {@link AbstractBleDevice} that send the humidity data.
  * @param humidity   {@link float} with the humidity value.
  * @param sensorName {@link String} with the sensor name.
  */
  public void onNewHumidity(@NonNull final AbstractBleDevice device,
                            final float humidity,
                            @NonNull final String sensorName,
                            @NonNull final String unit){
    //Do something
 }

  /**
  * Sends to the user the latest historical humidity.
  *
  * @param device           that send the humidity historical value.
  * @param relativeHumidity from a moment in the past.
  * @param timestamp        in milliseconds that determine when the humidity was obtained.
  * @param sensorName       that send the humidity.
  */
  public void onNewHistoricalHumidity(@NonNull final AbstractBleDevice device,
                                      final float relativeHumidity,
                                      final long timestamp,
                                      @NonNull final String sensorName,
                                      @NonNull final String unit){
     //Do something
  }
}
```

### STEP 5: Releasing resources

* For releasing the library resources the following method
must be called at the end of the application execution:

```java
BleManager.getInstance().release(context);
```
## Add new services to the library:

* Create for every new service a new interface in order
to be able to listen for notifications of the new service.
The new interface must extend
com.sensirion.libble.listeners.NotificationListener.

###### Notification interface example:
```java
import com.sensirion.libble.services.NotificationListener;

public interface StepListener extends NotificationListener {
  void onNewStep(@NonNull AbstractBleDevice device, int numberSteps);
}
```
After the interface is created, a new service can be built whose
objective is to send notifications to the user. The service needs
to extend com.sensirion.libble.services.AbstractBleService. It is possible
to generify the AbstractBleService to manage the listener registration
automatically.

###### Service registration:

```java
/**
 * The following command needs to be called before the device connection.
 * If a device with this service is connected, the factory will create
 * instances of this service class automatically.
 */
 BleServiceFactory.registerServiceImplementation(SERVICE_UUID, StepService.class);
```
###### Service example:

```java
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import com.sensirion.libble.devices.Peripheral;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT32;

public class StepService extends AbstractBleService<StepListener> {

 //SERVICE UUIDs
 public static final String SERVICE_UUID = "0000352f-0000-1000-8000-00805f9b34fb";
 private static final String CHARACTERISTIC_UUID = "00002539-0000-1000-8000-00805f9b34fb";
 private final BluetoothGattCharacteristic mStepCharacteristic;

 public StepService(@NonNull final Peripheral parent,
                    @NonNull final BluetoothGattService bluetoothGattService) {
    super(parent, bluetoothGattService);
    mStepCharacteristic = getCharacteristic(CHARACTERISTIC_UUID);
    parent.readCharacteristic(mStepCharacteristic);
 }

 /**
  * Registers the notification characteristics in case it's needed.
  */
  @Override
  public void registerDeviceCharacteristicNotifications() {
     registerNotification(mStepCharacteristic);
  }

 /**
  * Method called when a characteristic is read.
  *
  * @param characteristic that was updated.
  * @return <code>true</code> if the characteristic was read correctly.
  */
  @Override
  public boolean onCharacteristicUpdate(@NonNull BluetoothGattCharacteristic characteristic) {
     if (mStepCharacteristic.equals(updatedCharacteristic)) {
        final int numberSteps = characteristic.getIntValue(FORMAT_UINT32, 0);
        notifyListeners(numberSteps);
        return true;
     }
     return super.onCharacteristicUpdate(updatedCharacteristic);
  }

  private void notifyListeners(final int numberSteps) {
     final Iterator<StepListener> iterator = mListeners.iterator();
     while (iterator.hasNext()) {
        try {
           iterator.next().onNewStep(mPeripheral, numberSteps);
        } catch (final Exception e) {
           Log.e(TAG, "notifyListeners -> Exception thrown -> ", e);
           iterator.remove();
        }
     }
  }

 /**
  * Checks if a service is ready to use.
  *
  * @return <code>true</code> if the service is synchronized.
  */
  @Override
  public boolean isServiceReady() {
     return true; //This service is always ready.
  }

 /**
  * Tries to synchronize a service in case some of its data is missing.
  */
  @Override
  public abstract void synchronizeService() {
      // If notifications are registered, it won't do anything.
      registerDeviceCharacteristicNotifications();
  }
}
```