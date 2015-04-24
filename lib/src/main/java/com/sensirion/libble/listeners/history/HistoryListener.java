package com.sensirion.libble.listeners.history;

import android.support.annotation.NonNull;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.NotificationListener;

public interface HistoryListener extends NotificationListener {

    /**
     * Sets the actual number of downloaded elements.
     *
     * @param device           {@link BleDevice} that send the download progress.
     * @param downloadProgress {@link int} number of downloaded elements.
     */
    void setDownloadProgress(@NonNull BleDevice device, int downloadProgress);

    /**
     * Sets the total number of elements in a download.
     *
     * @param device {@link BleDevice} that send the number of elements to download.
     * @param amount {@link int} number of elements to download.
     */
    void setAmountElementsToDownload(@NonNull BleDevice device, int amount);

    /**
     * Advices the listeners that an error was produced when downloading the log.
     *
     * @param device {@link BleDevice} whose download finished prematurely.
     */
    void onLogDownloadFailure(@NonNull BleDevice device);

    /**
     * Advices the listeners that the service has finish downloading the file.
     *
     * @param device {@link BleDevice} whose download finished successfully.
     */
    void onLogDownloadCompleted(@NonNull BleDevice device);
}