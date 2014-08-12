package com.sensirion.libble;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public enum KnownDevices {
    HUMIGADGET {
        @Override
        public UUID [] getDescriptorUUIDs (){
            final UUID descriptorUUID = UUID.fromString("0000aa20-0000-1000-8000-00805f9b34fb");
            return new UUID[] {descriptorUUID};
        }

        @Override
        public List<String> getAdvertisedNames() {
            List<String> stringList = new LinkedList<String>();
            stringList.add("SHTC1 smart gadget");
            stringList.add("SHT31 Smart Gadget");
            return stringList;
        }

        @Override
        public String toString (){
            return "HUMIGADGET";
        }
    };

    /**
     * Returns the descriptor UUIDs from the gadget.
     *
     * @return {@link java.util.UUID} array.
     *
     * In spite of we usually we only need one descriptor UUID, the method LeScan
     * in Android Bluetooth library ask for an array.
     */
    public abstract UUID [] getDescriptorUUIDs ();

    /**
     * Returns a list of string of the valid advertised names of the gadget.
     *
     * @return {@link java.util.List}
     */
    public abstract List<String> getAdvertisedNames();
    @Override
    public abstract String toString();
}