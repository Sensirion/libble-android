package com.sensirion.libsmartgadget.smartgadget;

import android.support.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetValue;

import java.util.Date;

public class SmartGadgetValue implements GadgetValue {
    private final Date mTimestamp;
    private final Number mValue;
    private final String mUnit;

    public SmartGadgetValue(@NonNull final Date timestamp, @NonNull final Number value,
                            @NonNull final String unit) {
        mTimestamp = timestamp;
        mValue = value;
        mUnit = unit;
    }

    @Override
    @NonNull
    public Date getTimestamp() {
        return mTimestamp;
    }

    @Override
    @NonNull
    public Number getValue() {
        return mValue;
    }

    @Override
    @NonNull
    public String getUnit() {
        return mUnit;
    }
}
