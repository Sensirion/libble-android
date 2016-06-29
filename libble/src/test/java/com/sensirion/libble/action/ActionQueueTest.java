package com.sensirion.libble.action;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

/*
Note, that for these tests to run you need to set the DBG flat in com.sensirion.libble.log.Log to
false. Otherwise we'd need to mock it.
 */
public class ActionQueueTest {
    private static final String TEST_ADDRESS = "TEST_ADDRESS";

    // We need to pass some mocks here due to internal calls to the BluetoothGatt in the GattActions.
    BluetoothGatt getMockGatt() {
        final BluetoothGatt mockGatt = PowerMockito.mock(BluetoothGatt.class);
        final BluetoothDevice mockDevice = PowerMockito.mock(BluetoothDevice.class);
        Mockito.when(mockGatt.getDevice()).thenReturn(mockDevice);
        Mockito.when(mockDevice.getAddress()).thenReturn(TEST_ADDRESS);
        return mockGatt;
    }

    @Test
    public void testDefaultWorkflow() throws Exception {
        final TestAction successTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return true;
            }
        };

        final ActionQueue testedQueue = new ActionQueue(null, 1);

        testedQueue.add(successTestAction);
        Assert.assertFalse(testedQueue.isEmpty());

        testedQueue.processAction();
        Assert.assertEquals(1, successTestAction.executeCount);
        Assert.assertFalse(testedQueue.isEmpty());

        testedQueue.confirmAction(TEST_ADDRESS);
        Assert.assertTrue(testedQueue.isEmpty());
    }

    @Test
    public void testConfirmingIdleActionHasNoEffect() throws Exception {
        final TestAction successTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return true;
            }
        };

        final ActionQueue testedQueue = new ActionQueue(null, 1);

        testedQueue.add(successTestAction);
        testedQueue.confirmAction(TEST_ADDRESS);
        Assert.assertEquals(0, successTestAction.executeCount);
        Assert.assertFalse(testedQueue.isEmpty());
    }

    @Test
    public void testFailsTillDropOut() throws Exception {
        final int failsTillDropOut = 2;
        final TestAction failureTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return false;
            }
        };
        failureTestAction.failsTillDropOut = failsTillDropOut;

        final ActionQueue testedQueue = new ActionQueue(null, 1);

        testedQueue.add(failureTestAction);

        for (int i = 0; i < failsTillDropOut; i++) {
            testedQueue.processAction();
            Assert.assertEquals((i + 1), failureTestAction.executeCount);
            Assert.assertFalse(testedQueue.isEmpty());
        }

        testedQueue.processAction();
        Assert.assertEquals(2, failureTestAction.executeCount);
        Assert.assertTrue(testedQueue.isEmpty());
    }

    @Test
    public void testTimeoutAndNrOfExecutionsInFailScenario() throws Exception {
        final int timeoutIterationCount = 10;
        final int failsTillDropOut = 10;
        final TestAction successTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return true;
            }
        };
        successTestAction.failsTillDropOut = failsTillDropOut;

        final ActionQueue testedQueue = new ActionQueue(null, timeoutIterationCount);
        testedQueue.add(successTestAction);

        for (int j = 0; j < failsTillDropOut; j++) {
            for (int i = 0; i < (timeoutIterationCount + 1); i++) {
                testedQueue.processAction();
                Assert.assertEquals((j + 1), successTestAction.executeCount);
                Assert.assertFalse(testedQueue.isEmpty());
            }
        }

        testedQueue.processAction();
        Assert.assertEquals(failsTillDropOut, successTestAction.executeCount);
        Assert.assertTrue(testedQueue.isEmpty());
    }

    class FailureCallContainer {
        public boolean failureCalled = false;
    }

    @Test
    public void testFailureCallback() throws Exception {
        final int timeoutIterationCount = 1;
        final int failsTillDropOut = 1;
        final TestAction failureTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return true;
            }
        };
        failureTestAction.failsTillDropOut = failsTillDropOut;
        final FailureCallContainer failureCalledContainer = new FailureCallContainer();
        final ActionQueue testedQueue = new ActionQueue(new ActionFailureCallback() {
            @Override
            public void onActionFailed(GattAction action) {
                failureCalledContainer.failureCalled = true;
            }
        }, timeoutIterationCount);
        testedQueue.add(failureTestAction);

        for (int j = 0; j < failsTillDropOut; j++) {
            for (int i = 0; i < (timeoutIterationCount + 1); i++) {
                testedQueue.processAction();
            }
        }
        Assert.assertFalse(testedQueue.isEmpty());

        // The failing call
        testedQueue.processAction();
        Assert.assertTrue(testedQueue.isEmpty());
        Assert.assertTrue(failureCalledContainer.failureCalled);
    }
}