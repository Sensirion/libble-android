package com.sensirion.libble.services;

import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.history.HistoryListener;

import java.util.Iterator;
import java.util.concurrent.FutureTask;

/**
 * This service can be used to control data download. The user can obtain this service directly from the
 * {@link com.sensirion.libble.devices.BleDevice} and can use it without needing to know how it is
 * implemented or which type of data it use.
 * <p/>
 * This service should be implemented by all the services that retrieve historical data.
 */
public abstract class AbstractHistoryService extends AbstractBleService<HistoryListener> {

    private static final short ONE_SECOND_MS = 1000;
    private static final short THREE_SECONDS_MS = 3 * ONE_SECOND_MS;
    private static final short DOWNLOAD_PROGRESS_TIMEOUT = THREE_SECONDS_MS;

    private static final float MIN_DOWNLOAD_SUCCESS_PROGRESS = 0.95f; // 95%
    @NonNull
    private final Handler mDownloadProgressTimeoutHandler;
    @Nullable
    private Integer mNumberElementsToDownload;
    @Nullable
    private Integer mLastDownloadProgress;
    @NonNull
    private Runnable mOnDownloadTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (mNumberElementsToDownload == null || mLastDownloadProgress == null) {
                Log.e(TAG, "DownloadTimeoutTimer.run() -> Can not obtain the download state.");
            } else if (mLastDownloadProgress > mNumberElementsToDownload * MIN_DOWNLOAD_SUCCESS_PROGRESS) {
                onDownloadComplete();
            }
            onDownloadFailure();
        }
    };

    public AbstractHistoryService(@NonNull final Peripheral parent, @NonNull final BluetoothGattService bluetoothGattService) {
        super(parent, bluetoothGattService);
        Looper.prepare();
        mDownloadProgressTimeoutHandler = new Handler();
    }

    /**
     * Notifies the download progress of a download.
     *
     * @param downloadProgress with the number of downloaded elements.
     */
    protected void notifyDownloadProgress(final int downloadProgress) {
        mLastDownloadProgress = downloadProgress;
        resetDownloadTimer();
        final Iterator<HistoryListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                final HistoryListener listener = iterator.next();
                listener.setDownloadProgress(mPeripheral, downloadProgress);
            } catch (RuntimeException e) {
                Log.e(TAG, "onDatapointRead() -> Listener was removed from the list because the following exception was thrown -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notify the service about the number of elements to download.
     *
     * @param numberElementsToDownload number of elements to download.
     */
    protected void notifyTotalNumberElements(final int numberElementsToDownload) {
        mNumberElementsToDownload = numberElementsToDownload;
        final Iterator<HistoryListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().setAmountElementsToDownload(mPeripheral, numberElementsToDownload);
            } catch (RuntimeException e) {
                Log.e(TAG, "notifyTotalNumberElements -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * This will notify all listeners of the download failure by calling their onLogDownloadFailure method.
     */
    protected void onDownloadFailure() {
        mDownloadProgressTimeoutHandler.removeCallbacks(mOnDownloadTimeoutRunnable);
        final Iterator<HistoryListener> iterator = mListeners.iterator();
        Log.i(TAG, String.format("onLogDownloadFailure -> Notifying to the %d listeners. ", mListeners.size()));
        while (iterator.hasNext()) {
            try {
                iterator.next().onLogDownloadFailure(mPeripheral);
            } catch (RuntimeException e) {
                Log.e(TAG, "onLogDownloadFailure -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies to the user that the application has finish downloading the logged data.
     */
    protected void onDownloadComplete() {
        mDownloadProgressTimeoutHandler.removeCallbacks(mOnDownloadTimeoutRunnable);
        final Iterator<HistoryListener> iterator = mListeners.iterator();
        Log.i(TAG, String.format("onDownloadComplete -> Notifying to the %d listeners. ", mListeners.size()));
        while (iterator.hasNext()) {
            try {
                iterator.next().onLogDownloadCompleted(mPeripheral);
            } catch (final RuntimeException e) {
                Log.e(TAG, "onDownloadComplete -> The following exception was produced when notifying the listeners: ", e);
                iterator.remove();
            }
        }
    }

    private synchronized void resetDownloadTimer() {
        mDownloadProgressTimeoutHandler.removeCallbacks(mOnDownloadTimeoutRunnable);
        mDownloadProgressTimeoutHandler.postDelayed(mOnDownloadTimeoutRunnable, DOWNLOAD_PROGRESS_TIMEOUT);
    }

    /**
     * Starts the data download from the device.
     *
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Boolean> startDataDownload();

    /**
     * Starts to download data from a given timestamp.
     *
     * @param oldestTimestampToDownload the oldest timestamp that the device will download.
     * @return <code>true</code> if the data download started correctly. <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Boolean> startDataDownload(long oldestTimestampToDownload);

    /**
     * Change the download interval of a device.
     *
     * @param loggerIntervalInMilliseconds that the device will use for logging.
     * @return <code>true</code> if the download interval was set - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Boolean> setDownloadInterval(int loggerIntervalInMilliseconds);

    /**
     * Gets the interval between history data points on the device in milliseconds.
     *
     * @return {@link java.lang.Integer} with the logger interval in milliseconds - <code>null</code> if it's not known.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Integer> getLoggingIntervalMs();

    /**
     * Deletes all the data from the device.
     *
     * @return <code>true</code> if the data was deleted - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Boolean> resetDeviceData();

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
    @NonNull
    public abstract FutureTask<Boolean> setLoggingState(boolean enabled);

    /**
     * Checks the number of logged elements that the user can download.
     *
     * @return {@link java.lang.Integer} with the number of logged elements. <code>null</code> if it's unknown.
     */
    @SuppressWarnings("unused")
    @NonNull
    public abstract FutureTask<Integer> getNumberLoggedElements();

    /**
     * Checks if a logging download is in progress.
     *
     * @return <code>true</code> if a download is in progress - <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")
    public abstract boolean isDownloadInProgress();
}