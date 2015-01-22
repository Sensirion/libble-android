package com.sensirion.libble.devices;

import java.util.ArrayList;
import java.util.List;

public enum KnownDevices {

    /**
     * Devices that returns Humidity and Temperature data.
     * Notification interface: {@link com.sensirion.libble.listeners.services.RHTListener}
     */
    RHT_GADGETS {
        @Override
        public List<String> getAdvertisedNames() {
            final List<String> deviceNames = new ArrayList<>(3);
            deviceNames.add("SHTC1 smart gadget");
            deviceNames.add("SHT31 Smart Gadget");
            deviceNames.add("Smart Humigadget");
            return deviceNames;
        }
    };

    /**
     * Returns a {@link java.util.List} of {@link java.lang.String} of the valid advertised names of the gadget.
     *
     * @return {@link java.util.List} with the advertised names of compatible devices.
     */
    public abstract List<String> getAdvertisedNames();
}