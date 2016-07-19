package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.List;

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
     * interface to check for basic functionalities of the Gadget's Services.
     *
     * @param gadgetServiceClass The class representing the desired service of a gadget.
     * @return true if the service is supported by the gadget, else false is returned.
     */
    boolean supportsServiceOfType(@NonNull Class<? extends GadgetService> gadgetServiceClass);

    /**
     * Returns all services that are instances of the given class. You can use
     * {@link GadgetNotificationService} or {@link GadgetDownloadService} or any other Service
     * interface to check for basic functionalities of the Gadget's Services. All services are
     * returned If {@link GadgetService} is provided (then use {@link Gadget#getServices()}
     * instead).
     *
     * @param gadgetServiceClass The class representing the desired service of a gadget.
     * @return A list of the GadgetServices described by the provided parameter.
     */
    @NonNull
    List<GadgetService> getServicesOfType(@NonNull Class<? extends GadgetService> gadgetServiceClass);
}
