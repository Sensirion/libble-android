package com.sensirion.libsmartgadget;

public interface GadgetService {

    /**
     * Requests the Service to update its internal state. Only use this method if there is a problem
     * using the service, like failing data downloads or failing to change the logging interval.
     */
    void requestValueUpdate();

    /**
     * Retrieve the last received, cached GadgetValues. An empty array is returned, if there are no
     * previously received values yet.
     *
     * @return the GadgetValue array last received by the Smart Gadget.
     */
    GadgetValue[] getLastValues();
}
