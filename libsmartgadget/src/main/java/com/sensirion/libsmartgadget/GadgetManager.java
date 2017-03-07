package com.sensirion.libsmartgadget;

import android.content.Context;
import android.support.annotation.NonNull;

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
