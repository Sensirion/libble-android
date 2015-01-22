package com.sensirion.libble.utils;

import android.support.annotation.NonNull;

/**
 * Convenience class for storing the obtained humidity and temperature.
 */
public class RHTDataPoint implements Comparable<RHTDataPoint> {

    public static final int CELSIUS = 0;
    public static final int FAHRENHEIT = 1;
    public static final int KELVIN = 2;

    private static final String TAG = RHTDataPoint.class.getSimpleName();

    private final float mTemperatureInCelsius;
    private final float mRelativeHumidity;
    private final long mTimestamp;

    /**
     * Constructor for the datapoint in case we are using CELSIUS as temperature unit..
     *
     * @param temperatureInCelsius in Celsius.
     * @param relativeHumidity     of the datapoint.
     * @param timestamp            where this datapoint was obtained in milliseconds UTC.
     */
    public RHTDataPoint(final float temperatureInCelsius, final float relativeHumidity, final long timestamp) {
        this(temperatureInCelsius, relativeHumidity, timestamp, CELSIUS);
    }

    /**
     * Constructor for the datapoint.
     *
     * @param temperature      in the selected unit in the @param temperatureUnit.
     * @param relativeHumidity of the datapoint.
     * @param timestamp        where this datapoint was obtained in milliseconds UTC.
     * @param temperatureUnit  can be RHTDataPoint.CELSIUS or RHTDataPoint.FAHRENHEIT or RHTDataPoint.KELVIN.
     */
    public RHTDataPoint(final float temperature, final float relativeHumidity, final long timestamp, final int temperatureUnit) {
        switch (temperatureUnit) {
            case CELSIUS:
                mTemperatureInCelsius = temperature;
                break;
            case FAHRENHEIT:
                mTemperatureInCelsius = convertFahrenheitToCelsius(temperature);
                break;
            case KELVIN:
                mTemperatureInCelsius = convertKelvinToCelsius(temperature);
                break;
            default:
                throw new IllegalArgumentException(String.format("%s: Constructor: Temperature unit has to be %s.CELSIUS or %s.FAHRENHEIT or %s.KELVIN", TAG, TAG, TAG, TAG));
        }

        mRelativeHumidity = relativeHumidity;
        mTimestamp = timestamp;
    }

    private static float convertCelsiusToFahrenheit(final float temperatureInCelsius) {
        return (temperatureInCelsius * 9f / 5f + 32f);
    }

    private static float convertCelsiusToKelvin(final float temperatureInCelsius) {
        return temperatureInCelsius + 273.15f;
    }

    private static float convertFahrenheitToCelsius(final float tempInFahrenheit) {
        return (tempInFahrenheit - 32f) * 5f / 9f;
    }

    private static float convertKelvinToCelsius(final float temperatureInKelvin) {
        return temperatureInKelvin - 273.15f;
    }

    /**
     * Obtains the relative humidity of the datapoint.
     *
     * @return {@link java.lang.Float} with the relative humidity.
     */
    public float getRelativeHumidity() {
        return mRelativeHumidity;
    }

    /**
     * Obtains the temperature of the datapoint in Celsius.
     *
     * @return {@link java.lang.Float} with the datapoint in Celsius.
     */
    public float getTemperatureCelsius() {
        return mTemperatureInCelsius;
    }

    /**
     * Obtains the temperature of the datapoint in Fahrenheit.
     *
     * @return {@link java.lang.Float} with the datapoint in Fahrenheit.
     */
    @SuppressWarnings("unused")
    public float getTemperatureFahrenheit() {
        return convertCelsiusToFahrenheit(mTemperatureInCelsius);
    }

    /**
     * Obtains the temperature of the datapoint in Kelvin
     *
     * @return {@link java.lang.Float} with the datapoint in Kelvin.
     */
    @SuppressWarnings("unused")
    public float getTemperatureKelvin() {
        return convertCelsiusToKelvin(mTemperatureInCelsius);
    }

    /**
     * This method returns the dew point of the datapoint in Celsius.
     *
     * @return {@link java.lang.Float} with the datapoint in Celsius.
     */
    @SuppressWarnings("unused")
    public float getDewPointCelsius() {
        float h = (float) (Math.log((mRelativeHumidity / 100.0)) + (17.62 * mTemperatureInCelsius) / (243.12 + mTemperatureInCelsius));
        return (float) (243.12 * h / (17.62 - h));
    }

    /**
     * This method returns the dew point of the datapoint in Fahrenheit.
     *
     * @return {@link java.lang.Float} with the datapoint in Fahrenheit.
     */
    @SuppressWarnings("unused")
    public float getDewPointFahrenheit() {
        return convertCelsiusToFahrenheit(getDewPointCelsius());
    }

    /**
     * This method returns the dew point of the datapoint in Kelvin.
     *
     * @return {@link java.lang.Float} with the datapoint in Kelvin.
     */
    @SuppressWarnings("unused")
    public float getDewPointKelvin() {
        return convertCelsiusToKelvin(getDewPointCelsius());
    }

    /**
     * This method obtains the heat index of a temperature and humidity
     * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
     * comes from Stull, Richard (2000). Meteorology for Scientists and
     * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
     *
     * @return {@link java.lang.Float} with the Heat Index in Celsius.
     */
    @SuppressWarnings("unused")
    public float getHeatIndexCelsius() {
        return convertFahrenheitToCelsius(getHeatIndexFahrenheit());
    }

    /**
     * This method obtains the heat index of a temperature and humidity
     * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
     * comes from Stull, Richard (2000). Meteorology for Scientists and
     * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
     *
     * @return {@link java.lang.Float} with the Heat Index in Fahrenheit.
     */
    public float getHeatIndexFahrenheit() {
        final float temperatureInFahrenheit = convertCelsiusToFahrenheit(mTemperatureInCelsius);
        return HeatIndexCalculator.calcHeatIndexInFahrenheit(temperatureInFahrenheit, mRelativeHumidity);
    }

    /**
     * This method obtains the heat index of a temperature and humidity
     * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
     * comes from Stull, Richard (2000). Meteorology for Scientists and
     * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
     *
     * @return {@link java.lang.Float} with the Heat Index in Kelvin.
     */
    @SuppressWarnings("unused")
    public float getHeatIndexKelvin() {
        return convertCelsiusToKelvin(getHeatIndexCelsius());
    }

    /**
     * Returns the moment when it was obtained the data point in milliseconds.
     *
     * @return the timestamp in milliseconds.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    @Override
    @SuppressWarnings("StringBufferReplaceableByString")
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Temperature in Celsius: ").append(getTemperatureCelsius());
        sb.append(" Relative Humidity: ").append(getRelativeHumidity());
        sb.append(" TimestampMs: ").append(getTimestamp());
        sb.append(" Seconds from now: ").append((int) ((System.currentTimeMillis() - getTimestamp()) / 1000l)).append(" second(s).");
        return sb.toString();
    }

    @Override
    public int compareTo(@NonNull final RHTDataPoint anotherDatapoint) {
        if (anotherDatapoint.getTimestamp() - mTimestamp > 0) {
            return -1;
        }
        return 1;
    }

    private static class HeatIndexCalculator {

        /**
         * Heat formula coefficients.
         */
        private final static float c1 = 16.923f;
        private final static float c2 = 0.185212f;
        private final static float c3 = 5.37941f;
        private final static float c4 = -0.100254f;
        private final static float c5 = 9.41695E-3f;
        private final static float c6 = 7.28898E-3f;
        private final static float c7 = 3.45372E-4f;
        private final static float c8 = -8.14971E-4f;
        private final static float c9 = 1.02102E-5f;
        private final static float c10 = -3.8646E-5f;
        private final static float c11 = 2.91583E-5f;
        private final static float c12 = 1.42721E-6f;
        private final static float c13 = 1.97483E-7f;
        private final static float c14 = -2.18429E-8f;
        private final static float c15 = 8.43296E-10f;
        private final static float c16 = -4.81975E-11f;

        /**
         * Heat formula boundaries.
         */
        private final static float LOW_BOUNDARY_FORMULA_FAHRENHEIT = 70f;
        private final static float UPPER_BOUNDARY_FORMULA_FAHRENHEIT = 115f;

        /**
         * This method obtains the heat index of a temperature and humidity
         * using the formula from: http://en.wikipedia.org/wiki/Heat_index that
         * comes from Stull, Richard (2000). Meteorology for Scientists and
         * Engineers, Second Edition. Brooks/Cole. p. 60. ISBN 9780534372149.
         *
         * @param h relative humidity
         * @param t ambient temperature in Fahrenheit.
         * @return {@link java.lang.Float} with the Heat Index in Fahrenheit.
         */
        private static float calcHeatIndexInFahrenheit(final float t, final float h) {

            //Checks if the temperature and the humidity makes sense.
            if (t < LOW_BOUNDARY_FORMULA_FAHRENHEIT || t > UPPER_BOUNDARY_FORMULA_FAHRENHEIT || h < 0 || h > 100) {
                return Float.NaN;
            }

            //Prepares values for improving the readability of the method.
            final float t2 = t * t;
            final float t3 = t2 * t;
            final float h2 = h * h;
            final float h3 = h2 * h;

            return c1
                    + c2 * t
                    + c3 * h
                    + c4 * t * h
                    + c5 * t2
                    + c6 * h2
                    + c7 * t2 * h
                    + c8 * t * h2
                    + c9 * t2 * h2
                    + c10 * t3
                    + c11 * h3
                    + c12 * t3 * h
                    + c13 * t * h3
                    + c14 * t3 * h2
                    + c15 * t2 * h3
                    + c16 * t3 * h3;
        }
    }
}