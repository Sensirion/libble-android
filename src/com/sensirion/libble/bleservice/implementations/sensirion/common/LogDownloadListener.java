package com.sensirion.libble.bleservice.implementations.sensirion.common;

import com.sensirion.libble.NotificationListener;

public interface LogDownloadListener extends NotificationListener {
    /**
     * Sets the actual number of downloaded elements.
     *
     * @param downloadProgress number of downloaded elements.
     */
    void setDownloadProgress(String deviceAddress, int downloadProgress);

    /**
     * Sends to the user the last datapoint read.
     *
     * @param dataPoint downloaded.
     */
    void onNewDatapointDownloaded(RHTDataPoint dataPoint);

    /**
     * Sets the total number of elements in a download.
     *
     * @param amount number of elements to download.
     */
    void setRequestedDatapointAmount(String deviceAddress, int amount);

    /**
     * Advices the listeners that an error was produced when downloading the log.
     */
    void onLogDownloadFailure(String deviceAddress);

    /**
     * Advices the listeners that the service has finish downloading the file.
     */
    void onLogDownloadCompleted(String deviceAddress);
}
