package com.sensirion.libble.action;

import android.support.annotation.Nullable;

import com.sensirion.libble.log.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * NOTE:
 * This Class is not thread safe! you have to take care of this outside of this class.
 */
class ActionQueue {
    private final static String TAG = ActionQueue.class.getSimpleName();

    private final int mDefaultIterationsUntilTimeout;
    private final Queue<GattAction> mQueue;

    private ActionFailureCallback mFailureCallback;
    private ActionState mActionState;
    private int mIterationsUntilTimeout;

    public ActionQueue(final ActionFailureCallback callback, final int iterationsUntilTimeout) {
        mQueue = new LinkedList<>();
        mFailureCallback = callback;
        mDefaultIterationsUntilTimeout = iterationsUntilTimeout;

        resetState();
    }

    public void add(final GattAction action) {
        mQueue.add(action);
        Log.d(TAG, "Adding action ot type: %s - current queue size = %d",
                action.toString(), mQueue.size());
    }

    public void confirmAction(final String deviceAddress) {
        if (mActionState == ActionState.PENDING_CONFIRMATION) {
            Log.d(TAG, "Confirming action for Device %s", deviceAddress);
            dismissCurrentAction();
        }
    }

    public void clear() {
        mQueue.clear();
        resetState();
    }

    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    public void processAction() {
        final GattAction currentAction = checkPreconditionsAndGetNextAction();
        if (currentAction == null) return;

        if (currentAction.failsTillDropOut <= 0) {
            onActionFailed(currentAction);
            return;
        }

        switch (mActionState) {
            case IDLE:
                if (execute(currentAction)) {
                    onExecuteSucceeded(currentAction);
                } else {
                    onExecuteFailed(currentAction);
                }
                break;
            case PENDING_CONFIRMATION:
                if (--mIterationsUntilTimeout <= 0) {
                    onExecuteFailed(currentAction);
                }
                break;
        }
    }

    @Nullable
    private GattAction checkPreconditionsAndGetNextAction() {
        if (mQueue.size() == 0) {
            // Action Queue is empty
            resetState();
            return null;
        }

        final GattAction currentAction = mQueue.peek();
        if (currentAction == null || currentAction.mGatt == null) {
            Log.w(TAG, "Can not execute action - Action or Gatt is null.");
            dismissCurrentAction();
            return null;
        }

        return currentAction;
    }

    private boolean execute(final GattAction currentAction) {
        return currentAction.execute();
    }

    private void onExecuteSucceeded(final GattAction action) {
        mActionState = ActionState.PENDING_CONFIRMATION;
        Log.d(TAG, "Executing action %s for device %s - %s",
                action.toString(), action.mGatt.getDevice().getAddress(), " - success");
    }

    private void onExecuteFailed(final GattAction action) {
        resetState();
        action.failsTillDropOut--;
        Log.d(TAG, "Executing action %s for device %s - %s",
                action.toString(), action.mGatt.getDevice().getAddress(), " - failed");
    }

    private void onActionFailed(final GattAction action) {
        Log.d(TAG, "Action timeout reached - drop action %s in action queue", action.toString());
        if (mFailureCallback != null) mFailureCallback.onActionFailed(action);
        dismissCurrentAction();
    }

    private void dismissCurrentAction() {
        mQueue.remove();
        resetState();
    }

    private void resetState() {
        mActionState = ActionState.IDLE;
        mIterationsUntilTimeout = mDefaultIterationsUntilTimeout;
    }
}