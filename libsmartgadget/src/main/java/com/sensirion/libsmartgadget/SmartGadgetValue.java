package com.sensirion.libsmartgadget;

import java.util.Date;

public class SmartGadgetValue implements GadgetValue {
    private final Date mTimestamp;
    private final Number mValue;
    private final String mUnit;

    public SmartGadgetValue(final Date timestamp, final Number value, final String unit) {
        mTimestamp = timestamp;
        mValue = value;
        mUnit = unit;
    }

    @Override
    public Date getTimestamp() {
        return mTimestamp;
    }

    @Override
    public Number getValue() {
        return mValue;
    }

    @Override
    public String getUnit() {
        return mUnit;
    }
}
