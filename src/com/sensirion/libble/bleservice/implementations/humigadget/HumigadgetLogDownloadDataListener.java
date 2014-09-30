package com.sensirion.libble.bleservice.implementations.humigadget;

import com.sensirion.libble.NotificationListener;

public interface HumigadgetLogDownloadDataListener extends NotificationListener {
    public void onNewDatapointDownloaded(final RHTDataPoint dataPoint);

    public void onDownloadEnd(final String address);
}