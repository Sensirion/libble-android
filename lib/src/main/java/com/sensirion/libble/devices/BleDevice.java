package com.sensirion.libble.devices;

import android.content.Context;
import android.support.annotation.NonNull;

import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.BleService;
import com.sensirion.libble.services.HistoryService;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    /**
     * Checks if a device is connected or not.
     *
     * @return <code>true</code> if the device is connected - <code>false</code> if the device is disconnected.
     */
    boolean isConnected();

    /**
     * Establish a connection between the application and the peripheral.
     *
     * @param context of the application that wants to connect with the device. Cannot be <code>null</code>
     */
    void connect(@NonNull Context context);

    /**
     * Tries to establish a connection with a device that has been connected previously.
     *
     * @return <code>true</code> if the connection was recovered - <code>false</code> otherwise.
     */
    boolean reconnect();

    /**
     * Closes a connection to a device.
     */
    public void disconnect();

    /**
     * Obtains the physical address of the device.
     *
     * @return {@link java.lang.String} with the MAC-Address of the device.
     */
    String getAddress();

    /**
     * Obtains the public name of the BleDevice.
     *
     * @return {@link java.lang.String} with the advertised name.
     */
    String getAdvertisedName();

    /**
     * Checks the signal strength of the device towards the external BleDevice.
     *
     * @return {@link java.lang.Integer} with the signal strength.
     */
    int getRSSI();


    /**
     * NOTE: Returns the first {@link com.sensirion.libble.services.BleService} found for the given type.
     * Obtain a {@link com.sensirion.libble.services.BleService} in case the peripheral has one.
     *
     * @param type service class that the user wants to obtain.
     * @param <T>  Class of the service.
     * @return {@link com.sensirion.libble.services.BleService} that corresponds to the given class.
     */
    <T extends BleService> T getDeviceService(@NonNull Class<T> type);

    /**
     * Obtains a {@link com.sensirion.libble.services.BleService} with a particular name.
     * NOTE: Returns the first service found with the given name.
     *
     * @param serviceName name of the service.
     * @return {@link com.sensirion.libble.services.BleService} that corresponds to the given name
     */
    BleService getDeviceService(@NonNull String serviceName);

    /**
     * Obtains a list of the discovered {@link com.sensirion.libble.services.BleService}.
     *
     * @return Iterable with a list of the discovered services.
     */
    @SuppressWarnings("unused")
    Iterable<BleService> getDiscoveredServices();

    /**
     * Obtains a list with the name of the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} of the names of the discovered services.
     */
    Iterable<String> getDiscoveredServicesNames();

    /**
     * Counts the number of {@link com.sensirion.libble.services.BleService}.
     *
     * @return <code>int</code> with the number of {@link com.sensirion.libble.services.BleService}.
     */
    int getNumberServices();

    /**
     * Ask all the services to enable or disable all their notifications.
     *
     * @param enabled <code>true</code> if notifications are to be enabled - <code>false</code> otherwise.
     */
    void setAllNotificationsEnabled(boolean enabled);

    /**
     * Ask every service for being listened by the incoming {@link com.sensirion.libble.listeners.NotificationListener}.
     * Each service with notifications checks if the listener is able to read its data with interfaces.
     *
     * @param listener Activity from outside the library that wants to listen for notifications.
     * @return <code>true</code> if a valid service was found, <code>false</code> otherwise.
     */
    public boolean registerDeviceListener(@NonNull NotificationListener listener);

    /**
     * Unregister {@link com.sensirion.libble.listeners.NotificationListener} from all services.
     * Each service unregisters the {@param listener} from its notification list.
     *
     * @param listener to unsubscribe.
     */
    public void unregisterDeviceListener(@NonNull NotificationListener listener);

    /**
     * Retrieves the device history service in case it has one.
     * NOTE: In case the device has more than one history service it will only return the first one.
     *
     * @return {@link com.sensirion.libble.services.HistoryService} of the device - <code>null</code> if it doesn't haves one.
     */
    public HistoryService getHistoryService();
}