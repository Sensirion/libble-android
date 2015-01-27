package com.sensirion.libble.utils;

public abstract class TemperatureConverter {

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