package com.sensirion.libble.bleservice.implementations.humigadget;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.NotificationListener;

public interface HumigadgetLogDownloadListener extends NotificationListener {
    /**
     * Sets the actual number of downloaded elements.
     *
     * @param downloadProgress number of downloaded elements.
     */
    void setDownloadProgress(int downloadProgress);

    /**
     * Sends to the user the last datapoint read.
     *
     * @param dataPoint readed by the user.
     */
    void onNewDatapointDownloaded(RHTDataPoint dataPoint);

    /**
     * Sets the total number of elements in a download.
     *
     * @param amount number of elements to download.
     */
    void setRequestedDatapointAmount(int amount);

    /**
     * Advices the listeners that an error was produced when downloading the log.
     */
    void onDownloadFailure(BleDevice device);

    /**
     * Advices the listeners that the service has finish downloading the file.
     */
    void onDownloadCompleted(BleDevice device);
}
