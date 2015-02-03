package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.history.HistoryListener;

/**
 * This listener needs to be implemented by all the history services.
 */
public abstract class HistoryService extends NotificationService<Boolean, HistoryListener> {

    public HistoryService(@NonNull Peripheral parent, @NonNull BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
    }

    /**
     * Starts the data download from the device.
     *
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean startDataDownload();

    /**
     * Starts to download data from a given timestamp
     *
     * @param oldestTimestampToDownload the oldest timestamp that the device will download.
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean startDataDownload(long oldestTimestampToDownload);

    /**
     * Change the download interval of a device.
     *
     * @param loggerIntervalInMilliseconds that the device will use for logging.
     * @return <code>true</code> if the download interval was set - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean setDownloadInterval(int loggerIntervalInMilliseconds);

    /**
     * Gets the interval of the device in milliseconds.
     *
     * @return {@link java.lang.Integer} with the logger interval in milliseconds - <code>null</code> if it's not known
     */
    @SuppressWarnings("unused")
    public abstract Integer getDownloadIntervalMs();

    /**
     * Deletes all the data from the device.
     *
     * @return <code>true</code> if the data was deleted - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean resetDeviceData();

    /**
     * Checks if device is logging historical values.
     *
     * @return <code>true</code> if logging is enabled - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean isGadgetLoggingEnabled();

    /**
     * Checks is the user can modify the logging state.
     *
     * @return <code>true</code> if the user can enable or disable logging - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean isLoggingStateEditable();

    /**
     * User can set the logging state in case it's editable.
     *
     * @param enabled <code>true</code> if the user wants to enable logging - <code>false</code> otherwise.
     * @return <code>true</code> if the state was changed - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean setLoggingState(boolean enabled);

    /**
     * Checks the number of elements a device have to download.
     *
     * @return <code>true</code> if the device has data - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract int getNumberLoggedElements();

    /**
     * Checks if a logging download is in progress.
     *
     * @return <code>true</code> if a download is in progress - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean isDownloadInProgress();
}