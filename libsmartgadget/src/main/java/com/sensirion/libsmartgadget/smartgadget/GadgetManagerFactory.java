package com.sensirion.libsmartgadget.smartgadget;


import android.support.annotation.NonNull;

import com.sensirion.libsmartgadget.GadgetManager;
import com.sensirion.libsmartgadget.GadgetManagerCallback;

public final class GadgetManagerFactory {
    private GadgetManagerFactory() {
    }

    public static GadgetManager create(@NonNull final GadgetManagerCallback callback) {
        return (GadgetManager) new SmartGadgetManager(callback);
    }
}
