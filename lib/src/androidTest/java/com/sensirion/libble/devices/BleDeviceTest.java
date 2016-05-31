package com.sensirion.libble.devices;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.sensirion.libble.services.BleService;
import com.sensirion.libble.services.BleServiceSynchronizationPriority;
import com.sensirion.libble.services.MockBleService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BleDeviceTest extends AndroidTestCase {

    /**
     * Test the {@link BleService} <code>SERVICE_PRIORITY_COMPARATOR</code>
     */
    @SmallTest
    public void testPriorityComparator() {
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
        Collections.sort(serviceList, MockPeripheral.SERVICE_PRIORITY_COMPARATOR);
        // Checks if the services are ordered from high to low priority.
        assertEquals(testServiceHighPriority, serviceList.get(0));
        assertEquals(testServiceNormalPriority, serviceList.get(1));
        assertEquals(testServiceLowPriority, serviceList.get(2));
    }
}
