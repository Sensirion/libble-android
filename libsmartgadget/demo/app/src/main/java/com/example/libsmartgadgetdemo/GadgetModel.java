package com.example.libsmartgadgetdemo;

import com.sensirion.libsmartgadget.Gadget;

public class GadgetModel {
    private final Gadget gadget;
    private int RSSI;

    GadgetModel(Gadget gadget, int RSSI) {
        this.gadget = gadget;
        this.RSSI = RSSI;
    }

    public int getRssi() {
        return RSSI;
    }

    public void setRssi(int rssi) {
        RSSI = rssi;
    }

    public Gadget getGadget() {
        return gadget;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof GadgetModel && gadget.getAddress().equals(((GadgetModel) other).getGadget().getAddress());
    }
}
