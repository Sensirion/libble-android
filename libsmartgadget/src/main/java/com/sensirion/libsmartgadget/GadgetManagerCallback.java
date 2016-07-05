package com.sensirion.libsmartgadget;

public interface GadgetManagerCallback {
    void onGadgetManagerInitialized();

    void onGadgetDiscovered(final Gadget gadget, final int rssi);
}
