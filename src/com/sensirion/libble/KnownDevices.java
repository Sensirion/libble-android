package com.sensirion.libble;

import java.util.UUID;

public enum KnownDevices {
    HUMIGADGET {
        private final UUID descriptorUUID = UUID.fromString("0000aa20-0000-1000-8000-00805f9b34fb");

        @Override
        public UUID [] getDescriptorUUIDs (){
            return new UUID[] {descriptorUUID};
        }

        @Override
        public String toString (){
            return "HUMIGADGET";
        }
    };

    /**
     * This method has to be implemented by all the types of known devices.
     *
     * @return array with descriptor UUIDs. In spite of we usually we only need
     * one descriptor UUID, the method LeScan in Android Bluetooth library ask
     * for an array.
     */
    public abstract UUID [] getDescriptorUUIDs ();
    @Override
    public abstract String toString();
}