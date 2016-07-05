package com.sensirion.libsmartgadget;

import android.os.Handler;

import java.util.Date;
import java.util.Random;

public class SampleGadgetService implements GadgetNotificationService, GadgetDownloadService {
    private final GadgetListener mGadgetListener;
    private int mDownloadProgress = 0;
    private boolean mDownloading = false;
    private boolean mSubscribed = false;
    private GadgetValue[] mCurrentValue;

    public SampleGadgetService(final GadgetListener listener) {
        mGadgetListener = listener;
        mCurrentValue = new GadgetValue[0];
    }

    @Override
    public GadgetValue[] getLastValues() {
        return mCurrentValue;
    }

    @Override
    public boolean download() {
        if (isDownloading()) {
            return false;
        }

        mDownloading = true;
        mSimulator.downloadLog();
        return true;
    }

    @Override
    public boolean isDownloading() {
        return mDownloading;
    }

    @Override
    public int getDownloadProgress() {
        return mDownloadProgress;
    }

    @Override
    public boolean subscribe() {
        if (isSubscribed()) {
            return false;
        }

        mSubscribed = true;
        mSimulator.subscribe();
        return true;
    }

    @Override
    public boolean unsubscribe() {
        mSimulator.unsubscribe();
        mSubscribed = false;
        return true;
    }

    @Override
    public boolean isSubscribed() {
        return mSubscribed;
    }

    public void terminate() {
        unsubscribe();
        mSimulator.terminate();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////// SIMULATOR //////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /* Behavior Simulator */
    private GadgetSimulator mSimulator = new GadgetSimulator();

    private class GadgetSimulator {
        private final static long DOWNLOAD_PROGRESS_UPDATE_DURATION_MS = 200;
        private final static long DOWNLOAD_TOTAL_DURATION_MS = 20000;
        private final static long SUBSCRIPTION_INTERVAL_MS = 1000;
        private long mSimulatorDownloadProgress = 0;
        private final Handler mEventHandler = new Handler();
        private final Runnable mSubscription = new Runnable() {
            @Override
            public void run() {
                SampleGadgetService.this.mCurrentValue = new GadgetValue[1];
                SampleGadgetService.this.mCurrentValue[0] = new GadgetValue() {
                    @Override
                    public Date getTimestamp() {
                        return new Date();
                    }

                    @Override
                    public Number getValue() {
                        return (Number) (new Random().nextDouble() * 100d);
                    }

                    @Override
                    public String getUnit() {
                        return "%";
                    }
                };
                SampleGadgetService.this.mGadgetListener.onGadgetValuesReceived(null,
                        SampleGadgetService.this, SampleGadgetService.this.getLastValues());

                if (SampleGadgetService.this.isSubscribed()) {
                    mEventHandler.postDelayed(this, SUBSCRIPTION_INTERVAL_MS);
                }
            }
        };
        private final Runnable mDownload = new Runnable() {
            @Override
            public void run() {
                mSimulatorDownloadProgress += DOWNLOAD_PROGRESS_UPDATE_DURATION_MS;
                SampleGadgetService.this.mDownloadProgress = (int) (((float) mSimulatorDownloadProgress / DOWNLOAD_TOTAL_DURATION_MS) * 100.0f);
                mGadgetListener.onGadgetDownloadDataReceived(null, SampleGadgetService.this, new GadgetValue[]{new GadgetValue() {
                    @Override
                    public Date getTimestamp() {
                        return new Date();
                    }

                    @Override
                    public Number getValue() {
                        return (Number) (new Random().nextDouble() * 100d);
                    }

                    @Override
                    public String getUnit() {
                        return "%";
                    }
                }}, SampleGadgetService.this.mDownloadProgress);

                if (mSimulatorDownloadProgress >= DOWNLOAD_TOTAL_DURATION_MS) {
                    SampleGadgetService.this.mDownloading = false;
                } else {
                    mEventHandler.postDelayed(this, DOWNLOAD_PROGRESS_UPDATE_DURATION_MS);
                }
            }
        };

        void downloadLog() {
            mSimulatorDownloadProgress = 0;
            mEventHandler.post(mDownload);
        }

        void subscribe() {
            mEventHandler.post(mSubscription);
        }

        void unsubscribe() {
            mEventHandler.removeCallbacks(mSubscription);
        }

        public void terminate() {
            mEventHandler.removeCallbacks(mSubscription);
            mEventHandler.removeCallbacks(mDownload);
        }
    }
}
