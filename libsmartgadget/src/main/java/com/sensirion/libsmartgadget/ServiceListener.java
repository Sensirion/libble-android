package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

public interface ServiceListener {
    void onGadgetValuesReceived(@NonNull GadgetService service, @NonNull GadgetValue[] values);

    void onGadgetDownloadDataReceived(@NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress);

}
