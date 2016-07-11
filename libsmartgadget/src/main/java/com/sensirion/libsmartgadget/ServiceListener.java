package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.List;

public interface ServiceListener {
    void onGadgetValuesReceived(@NonNull GadgetService service, @NonNull GadgetValue[] values);

    void onGadgetDownloadDataReceived(@NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress);

}
