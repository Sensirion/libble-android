package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.TemperatureListener;
import com.sensirion.libble.utils.TemperatureUnit;

import java.util.Iterator;

import static com.sensirion.libble.utils.TemperatureConverter.convertCelsiusToFahrenheit;
import static com.sensirion.libble.utils.TemperatureConverter.convertCelsiusToKelvin;
import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToCelsius;
import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToFahrenheit;
import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToKelvin;

public class SmartgadgetTemperatureService extends AbstractSmartgadgetRHTService<TemperatureListener> {

    //SERVICE UUID.
    public static final String SERVICE_UUID = "00002234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTICS UUID.
    private static final String TEMPERATURE_NOTIFICATIONS_UUID = "00002235-b38d-4985-720e-0f993a68ee41";

    // Temperature unit that this service will use when notifying all it's listeners
    @Nullable
    private TemperatureUnit mValueUnit = null;

    public SmartgadgetTemperatureService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService, TEMPERATURE_NOTIFICATIONS_UUID);
    }

    /**
     * Converts a temperature value to an specific format
     *
     * @param temperatureInCelsius that will be converted to the format specified on @param temperatureUnit
     * @param temperatureUnit      that will be output
     * @return <code>float</code> with the converted temperature
     */
    private static float prepareTemperatureValueToFormat(final float temperatureInCelsius,
                                                         @NonNull final TemperatureUnit temperatureUnit) {
        switch (temperatureUnit) {
            case FAHRENHEIT:
                return convertCelsiusToFahrenheit(temperatureInCelsius);
            case KELVIN:
                return convertCelsiusToKelvin(temperatureInCelsius);
            default:
                return temperatureInCelsius;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void notifyListenersNewLiveValue() {
        final Iterator<TemperatureListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewTemperature(
                        mPeripheral,
                        prepareTemperatureValueToFormat(mLastValue, mValueUnit),
                        mSensorName,
                        mValueUnit
                );
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
        Log.d(TAG,
                String.format(
                        "%s -> Notifying historical temperature value: %f%s from sensor %s.",
                        "notifyListenersNewLiveValue",
                        mLastValue,
                        mValueUnit,
                        mSensorName
                )
        );
        final Iterator<TemperatureListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHistoricalTemperature(
                        mPeripheral,
                        prepareTemperatureValueToFormat(value, mValueUnit),
                        timestamp,
                        mSensorName,
                        mValueUnit
                );
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
        if (valueUnit.endsWith("C")) {
            mValueUnit = TemperatureUnit.CELSIUS;
        } else if (valueUnit.endsWith("F")) {
            mValueUnit = TemperatureUnit.FAHRENHEIT;
        } else if (valueUnit.endsWith("K")) {
            mValueUnit = TemperatureUnit.KELVIN;
        } else {
            Log.e(TAG, String.format("setValueUnit -> The service does not know how to manage value unit %s.", valueUnit));
        }
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
     * {@inheritDoc}
     */
    @Override
    public boolean isServiceReady() {
        return mLastValue != null && mValueUnit != null && mSensorName != null;
    }

    /**
     * Obtains the latest temperature in Celsius.
     *
     * @return {@link java.lang.Float} with the temperature in Celsius - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInCelsius() {
        if (isServiceReady()) {
            return convertTemperatureToCelsius(mLastValue, mValueUnit);
        }
        Log.e(TAG, "getTemperatureInCelsius -> Service is not synchronized yet. (HINT -> call synchronizeService first).");
        return null;
    }

    /**
     * Obtains the latest temperature in Fahrenheit.
     *
     * @return {@link java.lang.Float} with the temperature in Fahrenheit - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInFahrenheit() {
        if (isServiceReady()) {
            return convertTemperatureToFahrenheit(mLastValue, mValueUnit);
        }
        Log.e(TAG, "getTemperatureInFahrenheit -> Service is not synchronized yet. (HINT -> call synchronizeService first).");
        return null;
    }

    /**
     * Obtains the latest temperature in Kelvin.
     *
     * @return {@link java.lang.Float} with the temperature in Fahrenheit - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInKelvin() {
        if (isServiceReady()) {
            return convertTemperatureToKelvin(mLastValue, mValueUnit);
        }
        Log.e(TAG, "getTemperatureInKelvin -> Service is not synchronized yet. (HINT -> call synchronizeService first).");
        return null;
    }
}