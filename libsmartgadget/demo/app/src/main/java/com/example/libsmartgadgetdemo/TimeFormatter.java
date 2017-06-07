package com.example.libsmartgadgetdemo;

import java.util.Locale;


class TimeFormatter {
    private final static byte MINUTE = 60; // seconds
    private final static short HOUR = MINUTE * 60; // seconds

    private final int mHours;
    private final byte mMinutes;
    private final byte mSeconds;
    private final int mTotalSeconds;

    TimeFormatter(final int numberOfSeconds) {
        mTotalSeconds = numberOfSeconds;
        mHours = numberOfSeconds / HOUR;
        final int rest = numberOfSeconds % HOUR;
        mMinutes = (byte) (rest / MINUTE);
        mSeconds = (byte) (rest % MINUTE);
    }

    /**
     * Gets the time String using one single unit. (Hours, minutes or seconds)
     * Example 1: 7200s --> "2 hours"
     * Example 2: 5453s --> "5453 seconds"
     * Example 3: 3600s --> "1 hour"
     * Example 4: 2532s --> "2532 seconds"
     * Example 5: 180s --> "3 minutes"
     * Example 6: 60s --> "1 minute"
     * Example 7: 45s --> "45 seconds"
     * Example 8: 1s --> "1 second"
     *
     * @return {@link java.lang.String} with the short time representation.
     */
    String getShortTime() {

        if (mTotalSeconds % HOUR == 0) {
            if (mTotalSeconds == HOUR) {
                return "1 hour";
            }
            return String.format(Locale.ENGLISH, "%d hours", mHours);
        }
        if (mTotalSeconds % MINUTE == 0) {
            if (mTotalSeconds == MINUTE) {
                return "1 minute";
            }
            return String.format(Locale.ENGLISH, "%d minutes", mMinutes);
        }
        if (mTotalSeconds == 1) {
            return "1 second";
        }
        return String.format(Locale.ENGLISH, "%d seconds", mSeconds);
    }
}