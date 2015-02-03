package com.sensirion.libble.devices;

import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.services.BleService;
import com.sensirion.libble.services.HistoryService;

/**
 * Interface for any device that supports Bluetooth Low Energy (BLE)
 */
public interface BleDevice {

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
     * Checks if a device is connected or not.
     *
     * @return <code>true</code> if the device is connected - <code>false</code> if the device is disconnected.
     */
    boolean isConnected();

    /**
     * NOTE: Returns the first service it founds with of the given type.
     * Obtain a peripheral service in case the peripheral haves it.
     *
     * @param type service class that the user wants to obtain.
     * @param <T>  Class of the service.
     * @return {@link com.sensirion.libble.services.BleService} that corresponds to the given class.
     */
    <T extends BleService> T getDeviceService(Class<T> type);

    /**
     * Asks for a service with a particular name.
     * NOTE: Returns the first service it founds with the given name.
     *
     * @param serviceName name of the service.
     * @return {@link com.sensirion.libble.services.BleService} that corresponds to the given name
     */
    BleService getDeviceService(String serviceName);

    /**
     * Obtains a list with the discovered services.
     *
     * @return Iterable with a list of the discovered services.
     */
    Iterable<BleService> getDiscoveredServices();

    /**
     * Obtains a list with the name of the discovered services.
     *
     * @return Iterable with a list of {@link java.lang.String} with the names of the discovered services.
     */
    Iterable<String> getDiscoveredServicesNames();

    /**
     * Counts the number of discovered services.
     *
     * @return number of discovered services.
     */
    int getNumberOfDiscoveredServices();

    /**
     * Ask for the a characteristic of a service
     * NOTE: It returns the first characteristic it founds.
     *
     * @param characteristicName name of the characteristic.
     * @return {@link java.lang.Object} with the characteristic parsed by the service - <code>null</code> if no service was able to parse it.
     */
    Object getCharacteristicValue(String characteristicName);

    /**
     * Ask all the services to enable or disable all their notifications.
     *
     * @param enabled <code>true</code> if notifications wants to be enabled - <code>false</code> otherwise.
     */
    void setAllNotificationsEnabled(boolean enabled);


    /**
     * Ask every service for being listened.
     * Each service with notifications checks if the listener is able to read it's data with interfaces.
     *
     * @param listener Activity from outside the library that
     *                 wants to listen for notifications.
     * @return <code>true</code> if a valid service was found, <code>false</code> otherwise.
     */
    public boolean registerDeviceListener(final NotificationListener listener);

    /**
     * Ask every service for not being listened by a listener.
     * Each service with notifications removes it from
     * from it's list, in case the listener was listening it.
     *
     * @param listener from outside the library that doesn't
     *                 want to listen for notifications anymore.
     */
    public void unregisterDeviceListener(final NotificationListener listener);

    /**
     * Retrieves the device history service in case it haves one.
     * NOTE: In case the device haves more that one history service it will only return the first one.
     *
     * @return {@link com.sensirion.libble.services.HistoryService} of the device - <code>null</code> if it doesn't haves one.
     */
    public HistoryService getHistoryService();
}