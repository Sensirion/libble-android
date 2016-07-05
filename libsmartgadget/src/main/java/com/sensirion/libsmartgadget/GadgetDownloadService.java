package com.sensirion.libsmartgadget;

public interface GadgetDownloadService extends GadgetService {
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
