package com.example.libsmartgadgetdemo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.sensirion.libsmartgadget.Gadget;
import com.sensirion.libsmartgadget.GadgetDownloadService;
import com.sensirion.libsmartgadget.GadgetListener;
import com.sensirion.libsmartgadget.GadgetService;
import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.smartgadget.BatteryService;

import java.util.List;
import java.util.Locale;

public class GadgetInfoActivity extends AppCompatActivity implements GadgetListener {

    private static final String TAG = GadgetInfoActivity.class.getSimpleName();
    private static final int UNKNOWN_BATTERY_LEVEL = 0;
    private static final int UNKNOWN_LOGGING_INTERVAL = -1;
    private static final int DOWNLOAD_COMPLETED_RESET_DELAY_MS = 2000;

    private Gadget mGadget;
    private Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gadget_info);

        String address = getIntent().getStringExtra("EXTRA_GADGET_ADDRESS");
        mGadget = MainActivity.mConnectedAdapter.getFromAddress(address);
        mGadget.addListener(this);

        initUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        findViewById(R.id.progress).removeCallbacks(mRunnable);
    }

    @Override
    public void onGadgetConnected(@NonNull Gadget gadget) {
        // Ignore
    }

    @Override
    public void onGadgetDisconnected(@NonNull Gadget gadget) {
        if (gadget.equals(mGadget)) {
            Toast.makeText(this, "Disconnected from gadget " + gadget.getAddress(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service, @NonNull GadgetValue[] values) {
        if (gadget.equals(mGadget)) {
            if (service instanceof BatteryService) {
                int level = (int) service.getLastValues()[0].getValue();
                updateBatteryLevel(level);
            }
        }
    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress) {
        ((ProgressBar) findViewById(R.id.progress_bar)).setProgress(progress);
        ((TextView) findViewById(R.id.progress)).setText(String.format(Locale.ENGLISH, "%d%%", progress));
    }

    @Override
    public void onSetGadgetLoggingEnabledFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        initUI();
        Toast.makeText(this, "Setting the logging state failed. Please try again", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSetLoggerIntervalFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        initUI();
        Toast.makeText(this, "Setting the interval failed. Please try again", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSetLoggerIntervalSuccess(@NonNull Gadget gadget) {
        initIntervalButton();
    }

    @Override
    public void onDownloadFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        ((TextView) findViewById(R.id.progress)).setTextColor(getResources().getColor(R.color.orange));
        resetAfterDownload("Failed... Retry");
    }

    @Override
    public void onDownloadCompleted(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        resetAfterDownload("Download completed");
    }

    @Override
    public void onDownloadNoData(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {
        ((TextView) findViewById(R.id.progress)).setTextColor(getResources().getColor(R.color.orange));
        resetAfterDownload("No data available");
    }

    /*
     * UI functions
     */

    private void initUI() {
        ((TextView) findViewById(R.id.gadget_address)).setText(mGadget.getAddress());
        ((TextView) findViewById(R.id.gadget_name)).setText(mGadget.getName());
        updateBatteryLevel(getBatteryLevel());
        initToggle();
        initIntervalButton();
        initDownloadButton();
    }

    private void updateBatteryLevel(int batteryLevel) {
        ((ProgressBar) findViewById(R.id.battery_bar)).setProgress(batteryLevel);
        ((TextView) findViewById(R.id.battery_level)).setText(String.format(Locale.ENGLISH, "%d%%", batteryLevel));
    }

    private void initToggle() {
        if (isLoggingStateEditable()) {
            Switch toggle = (Switch) findViewById(R.id.toggle);
            toggle.setOnClickListener(null);
            toggle.setEnabled(isDownloadingEnabled() && !isDownloading());
            toggle.setChecked(isLoggingStateEnabled());

            toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setLoggingStateEnabled(isChecked);
                    findViewById(R.id.logging_interval).setEnabled(!isChecked);
                }
            });
        } else {
            findViewById(R.id.logging_layout).setVisibility(View.GONE);
        }
    }

    private void initIntervalButton() {
        Button intervalButton = (Button) findViewById(R.id.logging_interval);

        final int loggerIntervalMs = getLoggerInterval();
        if (loggerIntervalMs == UNKNOWN_LOGGING_INTERVAL) {
            return;
        }
        final int intervalSeconds = loggerIntervalMs / 1000;
        intervalButton.setText(new TimeFormatter(intervalSeconds).getShortTime());
        intervalButton.setEnabled(!(isLoggingStateEditable() && isLoggingStateEnabled()) && !isDownloading());
        intervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(@NonNull final View v) {
                showIntervalSelector();
            }
        });
    }

    private void initDownloadButton() {
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (isDownloading()) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }

        final TextView btnText = (TextView) findViewById(R.id.progress);
        btnText.setEnabled(isDownloadingEnabled() && !isDownloading());
        btnText.setText("Download");
        btnText.setTextColor(getColorStateList(R.color.button_states));
        btnText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.toggle).setEnabled(false);
                findViewById(R.id.logging_interval).setEnabled(false);
                btnText.setEnabled(false);
                downloadData();
            }
        });
    }

    public void onDisconnectClick(View view) {
        mGadget.disconnect();
    }

    /*
     * Gadget management functions
     */

    private int getBatteryLevel() {
        final GadgetService batteryService = getServiceOfType(mGadget, BatteryService.class);
        if (batteryService == null) {
            return UNKNOWN_BATTERY_LEVEL;
        }
        final GadgetValue[] lastValues = batteryService.getLastValues();
        return (lastValues.length > 0) ? lastValues[0].getValue().intValue() : UNKNOWN_BATTERY_LEVEL;
    }

    private GadgetService getServiceOfType(@NonNull final Gadget gadget,
                                           @NonNull final Class<? extends GadgetService> gadgetServiceClass) {
        final List<GadgetService> services = gadget.getServicesOfType(gadgetServiceClass);
        if (services.size() == 0) {
            return null;
        }

        if (services.size() > 1) {
            Log.w(TAG, String.format("Multiple services of type %s available - Application can only handle one", gadgetServiceClass));
        }

        return services.get(0);
    }

    private void setLoggerInterval(final int valueInMilliseconds) {
        final GadgetDownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }
        downloadService.setLoggerInterval(valueInMilliseconds);
        Button intervalButton = (Button) findViewById(R.id.logging_interval);
        intervalButton.setEnabled(false);
        intervalButton.setText("Updating...");
    }

    private int getLoggerInterval() {
        final GadgetDownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return UNKNOWN_LOGGING_INTERVAL;
        }
        return downloadService.getLoggerInterval();
    }

    private boolean isLoggingStateEnabled() {
        final GadgetDownloadService downloadService = getDownloadService();
        return downloadService != null && downloadService.isGadgetLoggingEnabled();
    }

    private boolean isLoggingStateEditable() {
        final GadgetDownloadService downloadService = getDownloadService();
        return downloadService != null && downloadService.isGadgetLoggingStateEditable();
    }

    private void setLoggingStateEnabled(boolean enabled) {
        final GadgetDownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }
        downloadService.setGadgetLoggingEnabled(enabled);
    }

    private boolean isDownloadingEnabled() {
        return getDownloadService() != null;
    }

    private boolean isDownloading() {
        final GadgetDownloadService downloadService = getDownloadService();
        return downloadService != null && downloadService.isDownloading();
    }

    private void downloadData() {
        final GadgetDownloadService downloadService = getDownloadService();
        if (downloadService == null) {
            return;
        }
        ((TextView) findViewById(R.id.progress)).setText("Starting download...");
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);
        downloadService.download();
    }

    private GadgetDownloadService getDownloadService() {
        return (GadgetDownloadService) getServiceOfType(mGadget, GadgetDownloadService.class);
    }

    /*
     * Helpers
     */

    private void showIntervalSelector() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false)
                .setTitle("Choose what to show")
                .setItems(R.array.array_interval_choices, new DialogInterface.OnClickListener() {
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        int seconds = 0;
                        switch(which) {
                            case 0: seconds = 1; break;
                            case 1: seconds = 10; break;
                            case 2: seconds = 60; break;
                            case 3: seconds = 300; break;
                            case 4: seconds = 600; break;
                            case 5: seconds = 3600; break;
                            case 6: seconds = 10800; break;
                            default:
                                throw new IllegalStateException("Invalid logger interval selected");
                        }
                        setLoggerInterval(seconds * 1000);
                    }
                });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void resetAfterDownload(String string) {
        ((ProgressBar) findViewById(R.id.progress_bar)).setProgress(0);
        final TextView btnText = ((TextView) findViewById(R.id.progress));
        btnText.setText(string);

        View intervalButton = findViewById(R.id.logging_interval);
        if (isLoggingStateEditable()) {
            Switch toggle = (Switch) findViewById(R.id.toggle);
            toggle.setEnabled(true);
            intervalButton.setEnabled(!toggle.isChecked());
        } else {
            intervalButton.setEnabled(true);
        }

        mRunnable = new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.progress_bar).setVisibility(View.GONE);
                btnText.setText("Download");
                btnText.setEnabled(true);
                btnText.setTextColor(getColorStateList(R.color.button_states));
            }
        };
        btnText.postDelayed(mRunnable, DOWNLOAD_COMPLETED_RESET_DELAY_MS);
    }
}
