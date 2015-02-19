package com.sensirion.libble.utils;

import android.support.annotation.NonNull;

public abstract class TemperatureConverter {

    /**
     * Convert a given temperature to Celsius.
     *
     * @param temperature that is going to be converted to Celsius.
     * @param unit        of the temperature. {@link com.sensirion.libble.utils.TemperatureUnit}
     * @return {@link java.lang.Float} with the temperature in Celsius.
     */
    public static float convertTemperatureToCelsius(final float temperature, @NonNull final TemperatureUnit unit) {
        switch (unit) {
            case CELSIUS:
                return temperature;
            case FAHRENHEIT:
                return TemperatureConverter.convertFahrenheitToCelsius(temperature);
            case KELVIN:
                return TemperatureConverter.convertKelvinToCelsius(temperature);
            default:
                throw new IllegalArgumentException(String.format("convertTemperatureToCelsius -> Value Unit %s is not implemented yet", unit));
        }
    }

    /**
     * Convert a given temperature to Fahrenheit.
     *
     * @param temperature that is going to be converted to Fahrenheit.
     * @param unit        of the temperature. {@link com.sensirion.libble.utils.TemperatureUnit}
     * @return {@link java.lang.Float} with the temperature in Fahrenheit.
     */
    public static float convertTemperatureToFahrenheit(final float temperature, @NonNull final TemperatureUnit unit) {
        switch (unit) {
            case CELSIUS:
                return convertCelsiusToFahrenheit(temperature);
            case FAHRENHEIT:
                return temperature;
            case KELVIN:
                return TemperatureConverter.convertKelvinToFahrenheit(temperature);
            default:
                throw new IllegalArgumentException(String.format("convertTemperatureToFahrenheit -> Value Unit %s is not implemented yet", unit));
        }
    }

    /**
     * Convert a given temperature to Kelvin.
     *
     * @param temperature that is going to be converted to Kelvin.
     * @param unit        of the temperature. {@link com.sensirion.libble.utils.TemperatureUnit}
     * @return {@link java.lang.Float} with the temperature in Kelvin.
     */
    public static float convertTemperatureToKelvin(final float temperature, @NonNull final TemperatureUnit unit) {
        switch (unit) {
            case CELSIUS:
                return convertCelsiusToKelvin(temperature);
            case FAHRENHEIT:
                return convertFahrenheitToKelvin(temperature);
            case KELVIN:
                return temperature;
            default:
                throw new IllegalArgumentException(String.format("convertTemperatureToKelvin -> Value Unit %s is not implemented yet", unit));
        }
    }

    /**
     * Convert a given Celsius temperature into Fahrenheit.
     *
     * @param temperatureInCelsius that will be converted into Fahrenheit.
     * @return {@link java.lang.Float} with the temperature in Fahrenheit.
     */
    public static float convertCelsiusToFahrenheit(final float temperatureInCelsius) {
        return (temperatureInCelsius * 9f / 5f + 32f);
    }

    /**
     * Convert a given Celsius temperature into Kelvin.
     *
     * @param temperatureInCelsius that will be converted into Kelvin.
     * @return {@link java.lang.Float} with the temperature in Kelvin.
     */
    public static float convertCelsiusToKelvin(final float temperatureInCelsius) {
        return temperatureInCelsius + 273.15f;
    }

    /**
     * Convert a given Fahrenheit temperature into Celsius.
     *
     * @param tempInFahrenheit that will be converted into Celsius.
     * @return {@link java.lang.Float} with the temperature in Celsius.
     */
    public static float convertFahrenheitToCelsius(final float tempInFahrenheit) {
        return (tempInFahrenheit - 32f) * 5f / 9f;
    }

    /**
     * Convert a given Fahrenheit temperature into Kelvin.
     *
     * @param tempInFahrenheit that will be converted into Kelvin.
     * @return {@link java.lang.Float} with the temperature in Kelvin.
     */
    public static float convertFahrenheitToKelvin(final float tempInFahrenheit) {
        return convertCelsiusToKelvin(convertFahrenheitToCelsius(tempInFahrenheit));
    }

    /**
     * Convert a given Kelvin temperature into Celsius.
     *
     * @param temperatureInKelvin that will be converted into Celsius.
     * @return {@link java.lang.Float} with the temperature in Celsius.
     */
    public static float convertKelvinToCelsius(final float temperatureInKelvin) {
        return temperatureInKelvin - 273.15f;
    }

    /**
     * Convert a given Kelvin temperature into Fahrenheit.
     *
     * @param temperatureInKelvin that will be converted into Fahrenheit.
     * @return {@link java.lang.Float} with the temperature in Fahrenheit.
     */
    public static float convertKelvinToFahrenheit(final float temperatureInKelvin) {
        return convertCelsiusToFahrenheit(convertKelvinToCelsius(temperatureInKelvin));
    }
}