package com.sensirion.libble.devices;

import android.support.annotation.NonNull;
import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.SmallTest;

import com.sensirion.libble.services.BleService;
import com.sensirion.libble.services.BleServiceSynchronizationPriority;
import com.sensirion.libble.services.MockBleService;

import java.util.Arrays;
import java.util.List;

/**
 * Test the {@link BleService} synchronization methods of {@link Peripheral}.
 */
public class PeripheralServiceSynchronizationTest extends AndroidTestCase {

    private SynchronizationMarkedService mTestServiceLowPriority;
    private SynchronizationMarkedService mTestServiceNormalPriority;
    private SynchronizationMarkedService mTestServiceHighPriority;
    private MockSynchronizationPeripheral mMockPeripheral;

    /**
     * Prepares the mock {@link Peripheral} and {@link BleService}'s before
     * each test execution.
     *
     * {@inheritDoc}
     */
    @Override
    public void setUp() {
        mTestServiceLowPriority =
                new SynchronizationMarkedService(BleServiceSynchronizationPriority.LOW_PRIORITY);
        mTestServiceNormalPriority =
                new SynchronizationMarkedService(BleServiceSynchronizationPriority.NORMAL_PRIORITY);
        mTestServiceHighPriority =
                new SynchronizationMarkedService(BleServiceSynchronizationPriority.HIGH_PRIORITY);
        mMockPeripheral = new MockSynchronizationPeripheral();
    }

    /**
     * Test {@link Peripheral#synchronizeDeviceServices(Iterable)}()}
     */
    @SmallTest
    public void testSynchronizeDeviceServices() {
        setUp();
        final List<BleService> serviceToSynchronize =
                Arrays.asList(
                        (BleService) mTestServiceLowPriority, mTestServiceNormalPriority
                );

        //noinspection StatementWithEmptyBody
        while (!mMockPeripheral.synchronizeDeviceServices(serviceToSynchronize)){
            // Service is not fully synchronized yet, continue with the marking.
        }

        assertTrue(mTestServiceLowPriority.isServiceReady());
        assertTrue(mTestServiceNormalPriority.isServiceReady());
        assertFalse(mTestServiceHighPriority.isServiceReady());
    }

    /**
     * Test {@link Peripheral#synchronizeAllDeviceServices()}
     */
    @SmallTest
    public void testSynchronizeAllDeviceServices() {
        setUp();

        //noinspection StatementWithEmptyBody
        while (!mMockPeripheral.synchronizeAllDeviceServices()){
            // Service is not fully synchronized yet, continue with the marking.
        }
        assertTrue(mTestServiceLowPriority.isServiceReady());
        assertTrue(mTestServiceNormalPriority.isServiceReady());
        assertTrue(mTestServiceHighPriority.isServiceReady());
    }

    /**
     * Mock class that marks the service as marked only if the method
     * {@link BleService#synchronizeService()} has being called previously.
     */
    private class SynchronizationMarkedService extends MockBleService {

        private boolean isMarked = false;

        public SynchronizationMarkedService(@NonNull BleServiceSynchronizationPriority priority) {
            super(priority);
        }

        @Override
        public void synchronizeService() {
            isMarked = true;
        }

        @Override
        public boolean isServiceReady() {
            return isMarked;
        }
    }

    /**
     * Mock class returning the as discovers services each of
     * {@link #mTestServiceLowPriority}, {@link #mTestServiceNormalPriority} and
     * {@link #mTestServiceHighPriority}
     */
    private class MockSynchronizationPeripheral extends MockPeripheral {

        @Override
        @NonNull
        public Iterable<BleService> getDiscoveredServices() {
            return Arrays.asList(
                    (BleService) mTestServiceLowPriority,
                    mTestServiceNormalPriority,
                    mTestServiceHighPriority
            );
        }

        /**
         * Implementation following the same behaviour as the one
         * {@link Peripheral#synchronizeDeviceServices(Iterable<BleService>)} is supposed to have.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean synchronizeDeviceServices(@NonNull final Iterable<BleService> services) {
            for (final BleService service : services) {
                if (!service.isServiceReady()) {
                    service.synchronizeService();
                    return false;
                }
            }
            return true;
        }

        /**
         * Implementation following the same behaviour as the one
         * {@link Peripheral#synchronizeAllDeviceServices()} is supposed to have.
         *
         * {@inheritDoc}
         */
        @Override
        public boolean synchronizeAllDeviceServices() {
           return synchronizeDeviceServices(getDiscoveredServices());
        }
    }
}
