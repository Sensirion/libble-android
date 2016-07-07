package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmartGadget implements Gadget {
    private final BleConnector mBleConnector;
    private final String mName;
    private final String mAddress;
    private final List<GadgetService> mGadgetServiceList;
    private final Set<GadgetListener> mListeners;

    private boolean mConnected;

    public SmartGadget(@NonNull BleConnector bleConnector, @NonNull final String name,
                       @NonNull final String address) {
        mBleConnector = bleConnector;
        mName = name;
        mAddress = address;
        mConnected = false;
        mGadgetServiceList = new ArrayList<>();
        mListeners = new HashSet<>();
    }

    @NonNull
    @Override
    public String getName() {
        return mName;
    }

    @NonNull
    @Override
    public String getAddress() {
        return mAddress;
    }

    @Override
    public boolean connect() {
        return mBleConnector.connect(this);
    }

    @Override
    public boolean disconnect() {
        return mBleConnector.disconnect(this);
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

    @Override
    public boolean supportsServiceOfType(@NonNull Class<?> gadgetServiceClass) {
        return !getServicesOfType(gadgetServiceClass).isEmpty();
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

    /*
    BLE Feedback Channel
     */
    // TODO Synchronize all Listener access!
    void onConnectionStateChanged(boolean connected) {
        mConnected = connected;
        if (connected) {
            mGadgetServiceList.addAll(ServiceFactory.createServicesFor(mBleConnector.getServices(this)));
            for (GadgetListener listener : mListeners) {
                listener.onGadgetConnected(this);
            }
        } else {
            mGadgetServiceList.clear();
            for (GadgetListener listener : mListeners) {
                listener.onGadgetDisconnected(this);
            }
        }
    }
}
