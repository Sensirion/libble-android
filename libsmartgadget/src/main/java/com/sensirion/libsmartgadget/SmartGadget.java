package com.sensirion.libsmartgadget;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SmartGadget implements Gadget, BleConnectorCallback, ServiceListener {
    private final BleConnector mBleConnector;
    private final GadgetServiceFactory mGadgetServiceFactory;
    private final String mName;
    private final String mAddress;
    private final List<GadgetService> mGadgetServiceList;
    private final Set<GadgetListener> mListeners;

    private boolean mConnected;

    public SmartGadget(@NonNull final BleConnector bleConnector,
                       @NonNull final GadgetServiceFactory gadgetServiceFactory,
                       @NonNull final String name, @NonNull final String address) {
        mBleConnector = bleConnector;
        mGadgetServiceFactory = gadgetServiceFactory;
        mName = name;
        mAddress = address;
        mConnected = false;
        mGadgetServiceList = Collections.synchronizedList(new ArrayList<GadgetService>());
        mListeners = Collections.synchronizedSet(new HashSet<GadgetListener>());
    }

    /*
        Implementation of {@link Gadget} interface
     */
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
        synchronized (mGadgetServiceList) {
            return new ArrayList<>(mGadgetServiceList);
        }
    }

    @Override
    public boolean supportsServiceOfType(@NonNull Class<?> gadgetServiceClass) {
        return !getServicesOfType(gadgetServiceClass).isEmpty();
    }

    @NonNull
    @Override
    public List<GadgetService> getServicesOfType(@NonNull Class<?> gadgetServiceClass) {
        final ArrayList<GadgetService> resultList = new ArrayList<>();
        synchronized (mGadgetServiceList) {
            for (final GadgetService service : mGadgetServiceList) {
                if (gadgetServiceClass.isInstance(service)) {
                    resultList.add(service);
                }
            }
        }
        return resultList;
    }

    /*
        Implementation of {@link BleConnectorCallback} interface
     */
    @Override
    public void onConnectionStateChanged(boolean connected) {
        mConnected = connected;

        if (connected) {
            mGadgetServiceList.addAll(mGadgetServiceFactory.createServicesFor(this, mAddress,
                    mBleConnector.getServices(this)));
            notifyServicesOnConnectionStateChanged(connected);

            synchronized (mListeners) {
                for (GadgetListener listener : mListeners) {
                    listener.onGadgetConnected(this);
                }
            }
        } else {
            notifyServicesOnConnectionStateChanged(connected);
            mGadgetServiceList.clear();
            synchronized (mListeners) {
                for (GadgetListener listener : mListeners) {
                    listener.onGadgetDisconnected(this);
                }
            }
        }
    }

    private void notifyServicesOnConnectionStateChanged(boolean connected) {
        synchronized (mGadgetServiceList) {
            for (GadgetService service : mGadgetServiceList) {
                if (service instanceof BleConnectorCallback) {
                    ((BleConnectorCallback) service).onConnectionStateChanged(connected);
                }
            }
        }
    }

    @Override
    public void onDataReceived(final String characteristicUuid, final byte[] rawData) {
        synchronized (mGadgetServiceList) {
            for (GadgetService service : mGadgetServiceList) {
                if (service instanceof BleConnectorCallback) {
                    ((BleConnectorCallback) service).onDataReceived(characteristicUuid, rawData);
                }
            }
        }
    }

    @Override
    public void onDataWritten(String characteristicUuid) {
        synchronized (mGadgetServiceList) {
            for (GadgetService service : mGadgetServiceList) {
                if (service instanceof BleConnectorCallback) {
                    ((BleConnectorCallback) service).onDataWritten(characteristicUuid);
                }
            }
        }
    }

    /*
        Implementation of {@link ParentGadget} interface
     */
    @Override
    public void onGadgetValuesReceived(@NonNull GadgetService service, @NonNull GadgetValue[] values) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onGadgetValuesReceived(this, service, values);
            }
        }
    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onGadgetDownloadDataReceived(this, service, values, progress);
            }
        }
    }

    /*
        Others
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SmartGadget)) return false;

        SmartGadget that = (SmartGadget) o;

        return mAddress.equals(that.mAddress);

    }

    @Override
    public int hashCode() {
        return mAddress.hashCode();
    }

}
