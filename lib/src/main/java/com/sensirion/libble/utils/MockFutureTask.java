package com.sensirion.libble.utils;

import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * This Future Task returns immediately the value inserted by the user.
 * This class is called when we already now the {@link FutureTask} result,
 * avoiding to code 5-6 lines of boilerplate code.
 */
public class MockFutureTask<Value> extends FutureTask<Value> {

    /**
     * This constructor creates automatically a callable which only function is return the
     * value inserted by the user.
     *
     * @see FutureTask#FutureTask(Callable)
     */
    public MockFutureTask(@NonNull final Value value) {
        super(new Callable<Value>() {
            @Override
            public Value call() throws Exception {
                return value;
            }
        });
    }
}