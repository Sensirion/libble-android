package com.sensirion.libble.action;

import android.os.Handler;
import android.support.annotation.NonNull;

import com.sensirion.libble.log.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * NOTE:
 * There should be one ActionQueue per {@code BluetoothGatt}, hence only Actions with the same Gatt
 * instance belong into the same queue. It's considered a programming error if not applied and might
 * lead to undetermined behavior. It might even be enforced in future releases.
 */
public class ActionScheduler {
    private final static String TAG = ActionScheduler.class.getSimpleName();
    private final static long ACTION_LOOP_INTERVAL_MS = 50;
    private final static int ITERATIONS_UNTIL_ACTION_TIMEOUT = 100;

    private final ActionFailureCallback mActionFailureCallback;
    private final Map<String, ActionQueue> mActions;
    private final Handler mActionHandler;
    private final Runnable mActionLoopRunnable;

    public ActionScheduler(final ActionFailureCallback callback, @NonNull final Handler handler) {
        mActionFailureCallback = callback;
        mActions = new HashMap<>();
        mActionHandler = handler;
        mActionLoopRunnable = new ActionIterator();
    }

    private synchronized void touch() {
        mActionHandler.removeCallbacks(mActionLoopRunnable);
        mActionHandler.post(mActionLoopRunnable);
    }

    private synchronized void pause() {
        mActionHandler.removeCallbacks(mActionLoopRunnable);
        Log.d(TAG, "Pausing Action Scheduler");
    }

    public void schedule(@NonNull final GattAction action) {
        synchronized (mActions) {
            ActionQueue queue = mActions.get(action.getDeviceAddress());
            if (queue == null) {
                queue = new ActionQueue(mActionFailureCallback, ITERATIONS_UNTIL_ACTION_TIMEOUT);
                mActions.put(action.getDeviceAddress(), queue);
            }
            queue.add(action);
        }
        touch();
    }

    public void confirm(@NonNull final String deviceAddress) {
        synchronized (mActions) {
            ActionQueue queue = mActions.get(deviceAddress);
            if (queue == null) {
                return;
            }
            queue.confirmAction(deviceAddress);
        }
    }

    public boolean isEmpty() {
        synchronized (mActions) {
            return mActions.isEmpty();
        }
    }

    public void clear(@NonNull final String deviceAddress) {
        synchronized (mActions) {
            mActions.remove(deviceAddress);
        }
    }

    public void clearAll() {
        synchronized (mActions) {
            for (ActionQueue queue : mActions.values()) {
                queue.clear();
            }
            mActions.clear();
        }
    }

    class ActionIterator implements Runnable {

        @Override
        public void run() {
            synchronized (mActions) {
                for (final String key : new HashSet<>(mActions.keySet())) {
                    final ActionQueue queue = mActions.get(key);
                    queue.processAction();

                    if (queue.isEmpty()) {
                        mActions.remove(key);
                    }
                }

                if (mActions.isEmpty()) {
                    pause();
                    return;
                }

                mActionHandler.postDelayed(mActionLoopRunnable, ACTION_LOOP_INTERVAL_MS);
            }
        }
    }
}
