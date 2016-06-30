package com.sensirion.libble.action;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

public class ActionSchedulerTest {
    private static final String TEST_ADDRESS = "TEST_ADDRESS";
    private Runnable mPostDelayedRunnable;

    // We need to pass some mocks here due to internal calls to the BluetoothGatt in the GattActions.
    BluetoothGatt getMockGatt() {
        final BluetoothGatt mockGatt = PowerMockito.mock(BluetoothGatt.class);
        final BluetoothDevice mockDevice = PowerMockito.mock(BluetoothDevice.class);
        Mockito.when(mockGatt.getDevice()).thenReturn(mockDevice);
        Mockito.when(mockDevice.getAddress()).thenReturn(TEST_ADDRESS);
        return mockGatt;
    }

    Handler getHandlerMock() {
        final Handler mockHandler = PowerMockito.mock(Handler.class);
        Mockito.when(mockHandler.post((Runnable) Mockito.anyObject())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                runnable.run();
                return true;
            }
        });
        Mockito.when(mockHandler.postDelayed((Runnable) Mockito.anyObject(), Mockito.anyLong())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Runnable runnable = (Runnable) invocation.getArguments()[0];
                mPostDelayedRunnable = runnable;
                return true;
            }
        });
        return mockHandler;
    }

    @Test
    public void scheduleExecutesTheAction() throws Exception {
        final TestAction successTestAction = new TestAction(getMockGatt(), TEST_ADDRESS) {
            @Override
            boolean execute() {
                executeCount++;
                return true;
            }
        };

        final ActionScheduler actionScheduler = new ActionScheduler(null, getHandlerMock());
        actionScheduler.schedule(successTestAction);
        Assert.assertFalse(actionScheduler.isEmpty());
        Assert.assertEquals(1, successTestAction.executeCount);

        Assert.assertNotNull(mPostDelayedRunnable);
        mPostDelayedRunnable.run();
        Assert.assertFalse(actionScheduler.isEmpty());

        actionScheduler.confirm(TEST_ADDRESS);
        mPostDelayedRunnable.run();
        Assert.assertTrue(actionScheduler.isEmpty());
    }

}
