package com.sensirion.libble.peripherals;

/**
 * This listener tells to the user when scan is turned on or off.
 */
public interface BleScanListener {
    /**
     * NOTE: When scan is (re)enabled the library clears it's internal list for discovered devices.
     * This method tells to the user when scan is turned on or off.
     *
     * @param isScanEnabled <code>true</code> if scan is turned on - <code>false</code> if scan is turned off.
     */
    void onScanStateChanged(boolean isScanEnabled);
}
