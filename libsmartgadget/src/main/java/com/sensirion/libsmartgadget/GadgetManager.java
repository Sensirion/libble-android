package com.sensirion.libsmartgadget;

import android.os.Handler;
import android.support.annotation.NonNull;

import java.util.Locale;
import java.util.Random;

/**
 * The GadgetManager is the main interface to interact with Sensirion Smart Gadgets. It provides
 * functions to initialize the communication stack and find gadgets in range. See {@link Gadget} for
 * more information on how to connect to the found gadgets. Note that only Gadget instance received
 * via {@link GadgetManagerCallback#onGadgetDiscovered(Gadget, int)} can be used to establish a
 * connection.
 */
public class GadgetManager {
    private boolean mReady;
    private GadgetManagerCallback mCallback;

    public GadgetManager() {
        mReady = false;
    }

    /**
     * Initialize the communication stack and register a {@link GadgetManagerCallback}.
     * You must call this method at least once to initialize the library. You will get notified
     * asynchronously as soon as the library has finished initializing via
     * {@link GadgetManagerCallback#onGadgetManagerInitialized()}.
     *
     * @param callback The callback instance to be registered for state change notifications.
     */
    public void initialize(@NonNull final GadgetManagerCallback callback) {
        mCallback = callback;

        // Simulate
        mSimulator.initialize(callback);
    }

    /**
     * Call this method if you don't plan to use the library anymore. This makes sure all resources
     * of the library are properly freed.
     */
    public void release() {
        mReady = false;
        mCallback = null;
    }

    /**
     * Check if the library is ready and was successfully initialized.
     *
     * @return true if the library is ready to be used.
     */
    public boolean isReady() {
        return mReady;
    }

    /**
     * Starts a scan for Sensirion Smart Gadgets.
     *
     * @param durationMs The duration how long the library should scan for. Make sure not to scan
     *                   for too long to prevent a large battery drain.
     * @return true if the scan was successfully initiated.
     */
    public boolean startGadgetDiscovery(final long durationMs) { // TODO: ADD FILTERING CAPABILITIES
        mSimulator.findGadgets();
        return true;
    }

    /**
     * Stops an ongoing scan for Smart Gadgets. Nothing happens if there is no scan running.
     */
    public void stopGadgetDiscovery() { // TODO: ADD FILTERING CAPABILITIES
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////// SIMULATOR //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /* Behavior Simulator */
    private LibrarySimulator mSimulator = new LibrarySimulator();

    private class LibrarySimulator {
        private final static long INITIALIZATION_TIME_MS = 2000;
        private final static long SCANNING_TIME_MS = 1000;
        private final Handler mEventHandler = new Handler();

        void initialize(final GadgetManagerCallback callback) {
            mEventHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    GadgetManager.this.mReady = true;
                    callback.onGadgetManagerInitialized();
                }
            }, INITIALIZATION_TIME_MS);
        }

        void findGadgets() {
            mEventHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        final String gadgetName = String.format(Locale.GERMANY, "Sample Gadget %d",
                                (new Random().nextInt(20) + 1));

                        final Gadget foundGadget = new SampleGadget(gadgetName);
                        final int rssi = -45;

                        GadgetManager.this.mCallback.onGadgetDiscovered(foundGadget, rssi);
                    }
                }
            }, SCANNING_TIME_MS);
        }
    }
}
