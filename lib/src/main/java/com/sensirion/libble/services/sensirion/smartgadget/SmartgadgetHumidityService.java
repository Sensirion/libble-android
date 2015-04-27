package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.HumidityListener;
import com.sensirion.libble.utils.HumidityUnit;

import java.util.Iterator;

public class SmartgadgetHumidityService extends AbstractSmartgadgetRHTService<HumidityListener> {

    //SERVICE UUID.
    public static final String SERVICE_UUID = "00001234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTICS UUID.
    private static final String HUMIDITY_NOTIFICATIONS_UUID = "00001235-b38d-4985-720e-0f993a68ee41";

    //CLASS ATTRIBUTES.
    @Nullable
    private HumidityUnit mValueUnit;

    public SmartgadgetHumidityService(@NonNull final Peripheral peripheral,
                                      @NonNull final BluetoothGattService bluetoothGattService) throws InstantiationException {
        super(peripheral, bluetoothGattService, HUMIDITY_NOTIFICATIONS_UUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void notifyListenersNewLiveValue() {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying humidity value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<HumidityListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHumidity(mPeripheral, mLastValue, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void notifyListenersNewHistoricalValue(final float value, final long timestamp) {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying humidity value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<HumidityListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHistoricalHumidity(mPeripheral, value, timestamp, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void setValueUnit(@NonNull final String valueUnit) {
        if (valueUnit.endsWith("RH")) {
            mValueUnit = HumidityUnit.RELATIVE_HUMIDITY;
        } else {
            Log.e(TAG, String.format("setValueUnit -> Value unit %s is unknown.", valueUnit));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isServiceReady() {
        return mLastValue != null && mValueUnit != null && mSensorName != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronizeService() {
        if (mLastValue == null) {
            registerDeviceCharacteristicNotifications();
        }
    }

    /**
     * Obtains the latest relative humidity.
     *
     * @return {@link java.lang.Float} with the relative humidity - <code>null</code> if the relative humidity is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getRelativeHumidity() {
        if (isServiceReady()) {
            return mLastValue;
        }
        Log.e(TAG, "getRelativeHumidity -> Service is not synchronize yet. (HINT -> Call synchronizeService first.)");
        return null;
    }
}