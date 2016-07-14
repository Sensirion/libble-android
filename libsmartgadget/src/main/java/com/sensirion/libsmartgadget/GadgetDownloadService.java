package com.sensirion.libsmartgadget;

public interface GadgetDownloadService extends GadgetService {
    /**
     * Returns whether the logging feature of the Gadget can be enabled or disabled.
     *
     * @return true if the state can be changed, false otherwise.
     */
    boolean isGadgetLoggingStateEditable();

    /**
     * Returns if the logging feature is currently enabled.
     *
     * @return true if it is enabled, false otherwise;
     */
    boolean isGadgetLoggingEnabled();

    /**
     * Enables or disables the Gadget's logging feature. Note that if you enable the logging
     * feature, all data stored on the device will be deleted.
     *
     * @param enabled true to enable the feature, false otherwise.
     */
    void setGadgetLoggingEnabled(final boolean enabled);

    /**
     * Sets the interval used by the gadget in milliseconds, in which data points should be
     * internally saved for later download. Note that if you set the logging interval, all data
     * stored on the device will be deleted.
     *
     * @param loggerIntervalMs The interval in milliseconds.
     * @return true if the write request was successfully dispatched, false otherwise.
     */
    boolean setLoggerInterval(final int loggerIntervalMs);

    /**
     * Gets the interval used by the gadget in milliseconds, in which data points should be
     * internally saved for later download.
     *
     * @return the set log interval in milliseconds.
     */
    int getLoggerInterval();

    /**
     * Initiates a download of all the data provided by this service.
     *
     * @return true if the call was successfully dispatched to the gadget.
     */
    boolean download();

    /**
     * To check if there is already an ongoing download running for the given gadget instance.
     *
     * @return true if there already was a download initiated using this particular gadget instance.
     */
    boolean isDownloading();

    /**
     * Returns the current download progress. The return value is not defined, if there is no
     * download running. Use {@link GadgetDownloadService#isDownloading()}
     * to check if there is a download running.
     *
     * @return the download progress.
     */
    int getDownloadProgress();
}
