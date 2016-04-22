package com.sensirion.libble.services;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BleServiceTest extends AndroidTestCase {

    /**
     * Test {@link BleService#compareTo(BleService)}
     */
    @SmallTest
    public void testCompareTo() {
        // Creates mock services from different priorities.
        final BleService testServiceLowPriority =
                new MockBleService(BleServiceSynchronizationPriority.LOW_PRIORITY);
        final BleService testServiceNormalPriority =
                new MockBleService(BleServiceSynchronizationPriority.NORMAL_PRIORITY);
        final BleService testServiceHighPriority =
                new MockBleService(BleServiceSynchronizationPriority.HIGH_PRIORITY);
        // Sorts an unordered Service list using the comparator.
        final List<BleService> serviceList = Arrays.asList(
                testServiceNormalPriority,
                testServiceHighPriority,
                testServiceLowPriority
        );
        Collections.sort(serviceList);
        // Checks if the services are ordered from high to high priority.
        assertEquals(testServiceHighPriority, serviceList.get(0));
        assertEquals(testServiceNormalPriority, serviceList.get(1));
        assertEquals(testServiceLowPriority, serviceList.get(2));
    }
}
