package com.sensirion.libsmartgadget;

public interface GadgetManagerCallback {
    /**
     * Called when the GadgetManager was successfully initialized.
     */
    void onGadgetManagerInitialized();

    /**
     * Callback to report found gadgets after the {@link GadgetManager#startGadgetDiscovery(long)}
     * was called.
     *
     * @param gadget The discovered {@link Gadget} instance.
     * @param rssi   The received signal strength of the gadget.
     */
    void onGadgetDiscovered(final Gadget gadget, final int rssi);

    /**
     * Called when the GadgetManager initialization failed. Check you devices's BLE capabilities and
     * Bluetooth permissions. You need several permissions for this library to work.
     * <p/>
     * <uses-feature
     * android:name="android.hardware.bluetooth_le"
     * android:required="true"/>
     * <p/>
     * <uses-permission android:name="android.permission.BLUETOOTH"/>
     * <uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
     * <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
     */
    void onGadgetManagerInitializationFailed();

    /**
     * Callback when gadget search could not be started.
     */
    void onGadgetSearchFailed();

    /**
     * Called when the search has stopped after the predefined duration time or if
     * {@link GadgetManager#stopGadgetDiscovery()} was called.
     */
    void onGadgetSearchFinished();
}
