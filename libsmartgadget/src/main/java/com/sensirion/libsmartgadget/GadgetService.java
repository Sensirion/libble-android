package com.sensirion.libsmartgadget;

public interface GadgetService {

    /**
     * Retrieve the last received, cached GadgetValues.
     *
     * @return the GadgetValue array last received by the Smart Gadget.
     */
    GadgetValue[] getLastValues();
}
