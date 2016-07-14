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
    public void addListener(@NonNull final GadgetListener callback) {
        mListeners.add(callback);
    }

    @Override
    public void removeListener(@NonNull final GadgetListener callback) {
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
    public boolean supportsServiceOfType(@NonNull final Class<?> gadgetServiceClass) {
        return !getServicesOfType(gadgetServiceClass).isEmpty();
    }

    @NonNull
    @Override
    public List<GadgetService> getServicesOfType(@NonNull final Class<?> gadgetServiceClass) {
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
    public void onConnectionStateChanged(final boolean connected) {
        mConnected = connected;

        if (connected) {
            mGadgetServiceList.addAll(mGadgetServiceFactory.createServicesFor(this, mAddress,
                    mBleConnector.getServices(this)));
        }

        notifyServicesOnConnectionStateChanged(connected);

        if (connected) {
            synchronized (mListeners) {
                for (GadgetListener listener : mListeners) {
                    listener.onGadgetConnected(this);
                }
            }
        } else {
            mGadgetServiceList.clear();
            synchronized (mListeners) {
                for (GadgetListener listener : mListeners) {
                    listener.onGadgetDisconnected(this);
                }
            }
        }
    }

    private void notifyServicesOnConnectionStateChanged(final boolean connected) {
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
    public void onDataWritten(final String characteristicUuid) {
        synchronized (mGadgetServiceList) {
            for (GadgetService service : mGadgetServiceList) {
                if (service instanceof BleConnectorCallback) {
                    ((BleConnectorCallback) service).onDataWritten(characteristicUuid);
                }
            }
        }
    }

    @Override
    public void onFail(final String characteristicUuid, final byte[] data,
                       final boolean isWriteFailure) {
        synchronized (mGadgetServiceList) {
            for (GadgetService service : mGadgetServiceList) {
                if (service instanceof BleConnectorCallback) {
                    ((BleConnectorCallback) service).onFail(characteristicUuid, data, isWriteFailure);
                }
            }
        }
    }

    /*
            Implementation of {@link ServiceListener} interface
         */
    @Override
    public void onGadgetValuesReceived(@NonNull final GadgetService service,
                                       @NonNull final GadgetValue[] values) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onGadgetValuesReceived(this, service, values);
            }
        }
    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull final GadgetDownloadService service,
                                             @NonNull final GadgetValue[] values,
                                             final int progress) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onGadgetDownloadDataReceived(this, service, values, progress);
            }
        }
    }

    @Override
    public void onDownloadFailed(@NonNull final GadgetDownloadService service) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onDownloadFailed(this, service);
            }
        }
    }

    @Override
    public void onSetGadgetLoggingEnabledFailed(@NonNull final GadgetDownloadService service) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onSetGadgetLoggingEnabledFailed(this, service);
            }
        }
    }

    @Override
    public void onSetLoggerIntervalFailed(@NonNull final GadgetDownloadService service) {
        synchronized (mListeners) {
            for (GadgetListener listener : mListeners) {
                listener.onSetLoggerIntervalFailed(this, service);
            }
        }
    }

    /*
        Others
     */
    @Override
    public boolean equals(final Object o) {
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
