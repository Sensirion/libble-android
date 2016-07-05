package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SampleGadget implements Gadget, GadgetListener {
    private final List<GadgetListener> mListeners = new ArrayList<>();
    private final List<GadgetService> mGadgetServiceList = new ArrayList<>();
    private final String mName;
    private boolean mConnected;

    public SampleGadget(final String name) {
        mName = name;
    }

    @NonNull
    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    @Override
    public String getAddress() {
        return "AB:CD:DE:FG:H" + mName.substring(mName.length() - 2);
    }

    @Override
    public boolean connect() {
        mGadgetServiceList.add(new SampleGadgetService(this));
        mConnected = true;

        for (GadgetListener listener : mListeners) {
            listener.onGadgetConnected(this);
        }
        return true;
    }

    @Override
    public boolean disconnect() {
        mConnected = false;
        mGadgetServiceList.clear();
        for (GadgetListener listener : mListeners) {
            listener.onGadgetDisconnected(this);
        }
        return true;
    }

    @Override
    public boolean isConnected() {
        return mConnected;
    }

    @Override
    public void addListener(@NonNull GadgetListener callback) {
        mListeners.add(callback);
    }

    @Override
    public void removeListener(@NonNull GadgetListener callback) {
        mListeners.remove(callback);
    }

    @NonNull
    @Override
    public List<GadgetService> getServices() {
        return new ArrayList<>(mGadgetServiceList);
    }

    @NonNull
    @Override
    public List<GadgetService> getServicesOfType(@NonNull Class<?> gadgetServiceClass) {
        final ArrayList<GadgetService> resultList = new ArrayList<>();
        for (GadgetService service : mGadgetServiceList) {
            if (gadgetServiceClass.isInstance(service)) {
                resultList.add(service);
            }
        }
        return resultList;
    }

    @Override
    public boolean supportsServiceOfType(@NonNull Class<?> gadgetServiceClass) {
        return !getServicesOfType(gadgetServiceClass).isEmpty();
    }

    @Override
    public void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service, @NonNull GadgetValue[] values) {
        for (GadgetListener listener : mListeners) {
            listener.onGadgetValuesReceived(this, service, values);
        }
    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress) {
        for (GadgetListener listener : mListeners) {
            listener.onGadgetDownloadDataReceived(gadget, service, values, progress);
        }
    }

    @Override
    public void onGadgetConnected(@NonNull Gadget gadget) {
        // UNUSED
    }

    @Override
    public void onGadgetDisconnected(@NonNull Gadget gadget) {
        // UNUSED
    }
}
