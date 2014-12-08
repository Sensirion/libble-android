package com.sensirion.libble.services;

import com.sensirion.libble.services.sensirion.shtc1.HumigadgetRHTNotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public enum KnownDevices {
    /**
     * Humigadget device.
     * Has notifications: YES
     * Notification interface: {@link com.sensirion.libble.services.sensirion.shtc1.HumigadgetRHTNotificationService}
     */
    HUMIGADGET {
        @Override
        public UUID[] getDescriptorUUIDs() {
            final UUID descriptorUUID = UUID.fromString(HumigadgetRHTNotificationService.RHT_DESCRIPTOR_UUID);
            return new UUID[]{descriptorUUID};
        }

        @Override
        public List<String> getAdvertisedNames() {
            List<String> deviceNames = new ArrayList<>(2);
            deviceNames.add("SHTC1 smart gadget");
            deviceNames.add("SHT31 Smart Gadget");
            return deviceNames;
        }

        @Override
        public String toString() {
            return "HUMIGADGET";
        }
    };

    /**
     * Returns the descriptor UUIDs from the gadget.
     * In spite of we usually we only need one descriptor UUID, the
     * method LeScan in Android Bluetooth library ask for an array.
     *
     * @return {@link java.util.UUID} array.
     */
    public abstract UUID[] getDescriptorUUIDs();

    /**
     * Returns a {@link java.util.List} of {@link java.lang.String} of the valid advertised names of the gadget.
     *
     * @return {@link java.util.List}
     */
    public abstract List<String> getAdvertisedNames();

    @Override
    public abstract String toString();
}