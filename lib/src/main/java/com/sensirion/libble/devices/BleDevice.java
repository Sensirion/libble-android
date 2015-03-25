package com.sensirion.libble.devices;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.AbstractBleService;
import com.sensirion.libble.services.AbstractHistoryService;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

    /**
     * Checks if a device is connected or not.
     *
     * @return <code>true</code> if the device is connected - <code>false</code> if the device is disconnected.
     */
    @SuppressWarnings("unused")
    boolean isConnected();

    /**
     * Establish a connection between the application and the peripheral.
     *
     * @param context of the application that wants to connect with the device. Cannot be <code>null</code>
     */
    @SuppressWarnings("unused")
    void connect(@NonNull Context context);

    /**
     * Tries to establish a connection with a device that has been connected previously.
     *
     * @return <code>true</code> if the connection was recovered - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    boolean reconnect();

    /**
     * Closes a connection to a device.
     */
    @SuppressWarnings("unused")
    void disconnect();

    /**
     * Obtains the physical address of the device.
     *
     * @return {@link java.lang.String} with the MAC-Address of the device.
     */
    @NonNull
    String getAddress();

    /**
     * Obtains the public name of the BleDevice.
     *
     * @return {@link java.lang.String} with the advertised name.
     */
    @Nullable
    String getAdvertisedName();

    /**
     * Checks the signal strength of the device towards the external BleDevice.
     *
     * @return {@link java.lang.Integer} with the signal strength.
     */
    @SuppressWarnings("unused")
    int getRSSI();

    /**
     * NOTE: Returns the first {@link com.sensirion.libble.services.AbstractBleService} found for the given type.
     * Obtain a {@link com.sensirion.libble.services.AbstractBleService} in case the peripheral has one.
     *
     * @param type service class that the user wants to obtain.
     * @param <T>  Class of the service.
     * @return {@link com.sensirion.libble.services.AbstractBleService} that corresponds to the given class.
     */
    @Nullable
    <T extends AbstractBleService> T getDeviceService(@NonNull Class<T> type);

    /**
     * Obtains a {@link com.sensirion.libble.services.AbstractBleService} with a particular name.
     * NOTE: Returns the first service found with the given name.
     *
     * @param serviceName name of the service.
     * @return {@link com.sensirion.libble.services.AbstractBleService} that corresponds to the given name
     */
    @Nullable
    AbstractBleService getDeviceService(@NonNull String serviceName);

    /**
     * Obtains a list of the discovered {@link com.sensirion.libble.services.AbstractBleService}.
     *
     * @return Iterable with a list of the discovered services.
     */
    @SuppressWarnings("unused")
    @NonNull
    Iterable<AbstractBleService> getDiscoveredServices();

    /**
     * Obtains a list with the name of the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} of the names of the discovered services.
     */
    @NonNull
    Iterable<String> getDiscoveredServicesNames();

    /**
     * Counts the number of {@link com.sensirion.libble.services.AbstractBleService}.
     *
     * @return <code>int</code> with the number of {@link com.sensirion.libble.services.AbstractBleService}.
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
    boolean registerDeviceListener(@NonNull NotificationListener listener);

    /**
     * Unregister {@link com.sensirion.libble.listeners.NotificationListener} from all services.
     * Each service unregisters the {@param listener} from its notification list.
     *
     * @param listener to unsubscribe.
     */
    void unregisterDeviceListener(@NonNull NotificationListener listener);

    /**
     * Retrieves the device history service in case it has one.
     * NOTE: In case the device has more than one history service it will only return the first one.
     *
     * @return {@link com.sensirion.libble.services.AbstractHistoryService} of the device - <code>null</code> if it doesn't haves one.
     */
    @Nullable
    @SuppressWarnings("unused")
    AbstractHistoryService getHistoryService();


    /**
     * Checks if the peripheral has all its services synchronized.
     *
     * @return <code>true</code> if the services are synchronized - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    boolean areAllServicesReady();
}