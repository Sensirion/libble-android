package com.sensirion.libble.bleservice.implementations.humigadget;

public interface HumigadgetLogListener extends HumigadgetRHTListener {
    /**
     * Sets the actual number of downloaded elements.
     *
     * @param downloadProgress number of downloaded elements.
     */
    public void setDownloadProgress(final int downloadProgress);

    /**
     * Sets the total number of elements in a download.
     *
     * @param amount number of elements to download.
     */
    public void setRequestedDatapointAmount(final int amount);

    /**
     * Advices the listeners that an error was produced when downloading the log.
     */
    public void onDownloadFailure();

    /**
     * Advices the listeners that the service has finish downloading the file.
     */
    public void onDownloadCompleted();
}
