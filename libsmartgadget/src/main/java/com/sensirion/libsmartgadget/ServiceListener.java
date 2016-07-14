package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

interface ServiceListener {
    void onGadgetValuesReceived(@NonNull GadgetService service, @NonNull GadgetValue[] values);

    void onGadgetDownloadDataReceived(@NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress);

    void onDownloadFailed(@NonNull GadgetDownloadService service);

    void onSetGadgetLoggingEnabledFailed(@NonNull GadgetDownloadService service);

    void onSetLoggerIntervalFailed(@NonNull GadgetDownloadService service);

}
