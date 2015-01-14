package com.sensirion.libble.peripherals;

/**
 * This listener tells to the user when scan is turned on or off.
 */
public interface BleScanListener {
    /**
     * This method tells to the user when scan is turned on or off.
     *
     * @param enabled <code>true</code> if scan starts - <code>false</code> if scan stops.
     * NOTE: When scan is (re)enabled the library clears it's internal list for discovered devices.
     */
    void onScanStateChanged(boolean enabled);
}
