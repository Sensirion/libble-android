package com.sensirion.libble.listeners.services;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;
import com.sensirion.libble.utils.RHTDataPoint;

public interface RHTLogDownloadListener extends NotificationListener {
    /**
     * Sets the actual number of downloaded elements.
     *
     * @param downloadProgress number of downloaded elements.
     */
    void setDownloadProgress(BleDevice device, int downloadProgress);

    /**
     * Sends to the user the last datapoint read.
     *
     * @param dataPoint downloaded.
     */
    void onNewDatapointDownloaded(BleDevice device, RHTDataPoint dataPoint);

    /**
     * Sets the total number of elements in a download.
     *
     * @param amount number of elements to download.
     */
    void setRequestedDatapointAmount(BleDevice device, int amount);

    /**
     * Advices the listeners that an error was produced when downloading the log.
     */
    void onLogDownloadFailure(BleDevice device);

    /**
     * Advices the listeners that the service has finish downloading the file.
     */
    void onLogDownloadCompleted(BleDevice device);
}