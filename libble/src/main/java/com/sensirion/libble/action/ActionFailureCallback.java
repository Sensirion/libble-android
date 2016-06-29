package com.sensirion.libble.action;

public interface ActionFailureCallback {
    void onActionFailed(final GattAction action);
}
