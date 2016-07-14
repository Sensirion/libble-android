package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.Date;

public interface GadgetValue {

    /**
     * Retrieve time when this value was created.
     *
     * @return the time when this value was created.
     */
    @NonNull
    Date getTimestamp();

    /**
     * Getter for the value.
     *
     * @return the value received from the gadget.
     */
    @NonNull
    Number getValue();

    /**
     * Getter for the unit of the value.
     *
     * @return the string representing the values's unit.
     */
    @NonNull
    String getUnit();

}
