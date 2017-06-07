package com.example.libsmartgadgetdemo;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.sensirion.libble.BleService;
import com.sensirion.libsmartgadget.Gadget;
import com.sensirion.libsmartgadget.GadgetDownloadService;
import com.sensirion.libsmartgadget.GadgetListener;
import com.sensirion.libsmartgadget.GadgetManager;
import com.sensirion.libsmartgadget.GadgetManagerCallback;
import com.sensirion.libsmartgadget.GadgetService;
import com.sensirion.libsmartgadget.GadgetValue;
import com.sensirion.libsmartgadget.smartgadget.GadgetManagerFactory;
import com.sensirion.libsmartgadget.smartgadget.SHT3xHumidityService;
import com.sensirion.libsmartgadget.smartgadget.SHT3xTemperatureService;
import com.sensirion.libsmartgadget.smartgadget.SHTC1TemperatureAndHumidityService;
import com.sensirion.libsmartgadget.smartgadget.SensorTagTemperatureAndHumidityService;
import com.sensirion.libsmartgadget.utils.BLEUtility;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements GadgetManagerCallback, GadgetListener {

    private static final int SCAN_DURATION_MS = 60000;
    private static final int BUTTON_RESET_DELAY_MS = 2000;
    private static final int LOCATION_PERMISSION = 1234;
    private static final String TAG = MainActivity.class.getSimpleName();

    private String[] nameFilter = new String[]{"SHTC1 smart gadget",
            "SHTC1 smart gadget\u0002", "Smart Humigadget", "SensorTag"};
    private String[] uuidFilter = new String[]{SHT3xTemperatureService.SERVICE_UUID,
            SHT3xHumidityService.SERVICE_UUID,
            SHTC1TemperatureAndHumidityService.SERVICE_UUID,
            SensorTagTemperatureAndHumidityService.SERVICE_UUID};

    private Button mScanButton;
    private GadgetManager mGadgetManager;

    private ListView mDiscoveredList;
    static GadgetAdapter mDiscoveredAdapter;

    private ListView mConnectedList;
    static GadgetAdapter mConnectedAdapter;

    private Runnable mRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScanButton = (Button) findViewById(R.id.btn_scan);
        mGadgetManager = GadgetManagerFactory.create(this);
        mGadgetManager.initialize(this);

        mDiscoveredList = (ListView) findViewById(R.id.device_list);
        mDiscoveredAdapter = new GadgetAdapter(this, false);
        mDiscoveredList.setAdapter(mDiscoveredAdapter);

        mConnectedList = (ListView) findViewById(R.id.connected_list);
        mConnectedAdapter = new GadgetAdapter(this, true);
        mConnectedList.setAdapter(mConnectedAdapter);

        if (!BLEUtility.isBLEEnabled(this)) {
            mScanButton.setEnabled(false);
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGadgetManager.isReady() && mScanButton.isEnabled()) {
            onScanButtonClick(null);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mScanButton.removeCallbacks(mRunnable);
    }

    /*
     * Button Click Methods
     */

    public void onScanButtonClick(View view) {
        if (!BLEUtility.hasScanningPermission(this)) {
            BLEUtility.requestScanningPermission(this, LOCATION_PERMISSION);
            return;
        }

        mDiscoveredAdapter.clear();
        mScanButton.setEnabled(false);
        mScanButton.setText(R.string.scan_btn_scanning);
        if(!mGadgetManager.startGadgetDiscovery(SCAN_DURATION_MS, nameFilter, uuidFilter)) {
            Log.e(TAG, "Could not start discovery");
        }
    }

    public void onClearButtonClick(View view) {
        mDiscoveredAdapter.clear();
        mConnectedAdapter.disconectAllGadgets();
        mConnectedAdapter.clear();
        rescaleListViews();
    }

    public void onConnectButtonClick(View view) {
        if (view instanceof Button) {
            Button btn = (Button) view;
            btn.setEnabled(false);
            btn.setText("Connecting...");

            Gadget gadget = (Gadget) btn.getTag();
            gadget.addListener(this);
            gadget.connect();
        }
    }

    public void onDisconnectButtonClick(View view) {
        if (view instanceof Button) {
            Button btn = (Button) view;
            btn.setEnabled(false);
            btn.setText("Connecting...");

            Gadget gadget = (Gadget) btn.getTag();
            gadget.disconnect();
        }
    }

    public void onInfoButtonClick(View view) {
        if (view instanceof Button) {
            String address = ((Gadget) view.getTag()).getAddress();

            Intent intent = new Intent(this, GadgetInfoActivity.class);
            intent.putExtra("EXTRA_GADGET_ADDRESS", address);
            startActivity(intent);
        }
    }

    /*
     * Gadget Manager Callbacks
     */

    public void onGadgetManagerInitialized() {
        mScanButton.setText(R.string.scan_btn_start);
        mScanButton.setEnabled(true);
        onScanButtonClick(null);
    }

    public void onGadgetManagerInitializationFailed() {
        mScanButton.setEnabled(false);
        mScanButton.setText(R.string.scan_btn_init_failed);
    }

    public void onGadgetDiscovered(Gadget gadget, int rssi) {
        mDiscoveredAdapter.add(gadget, rssi);
        rescaleListView(mDiscoveredList);
    }

    public void onGadgetDiscoveryFailed() {
        mScanButton.setEnabled(false);
        mScanButton.setText(R.string.scan_btn_failed);
    }

    public void onGadgetDiscoveryFinished() {
        mScanButton.setText(R.string.scan_btn_finished);

        mRunnable = new Runnable() {
            @Override
            public void run() {
                mScanButton.setText(R.string.scan_btn_start);
                mScanButton.setEnabled(true);
            }
        };
        mScanButton.postDelayed(mRunnable, BUTTON_RESET_DELAY_MS);
    }

    /*
     * Helper functions
     */

    private void rescaleListView(ListView listView) {
        /*
         * Code found here: http://www.java2s.com/Code/Android/UI/setListViewHeightBasedOnChildren.htm
         */
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(0, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void rescaleListViews() {
        rescaleListView(mDiscoveredList);
        rescaleListView(mConnectedList);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case LOCATION_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, start scanning
                    onScanButtonClick(null);
                }
                // Otherwise: Permission denied, do nothing
                break;
        }
    }

    /*
     * Gadget listener callbacks
     */

    @Override
    public void onGadgetConnected(@NonNull Gadget gadget) {
        mDiscoveredAdapter.remove(gadget);
        mConnectedAdapter.add(gadget, 0);
        rescaleListViews();
    }

    @Override
    public void onGadgetDisconnected(@NonNull Gadget gadget) {
        mConnectedAdapter.remove(gadget);
        rescaleListView(mConnectedList);
    }

    @Override
    public void onGadgetValuesReceived(@NonNull Gadget gadget, @NonNull GadgetService service, @NonNull GadgetValue[] values) {

    }

    @Override
    public void onGadgetDownloadDataReceived(@NonNull Gadget gadget, @NonNull GadgetDownloadService service, @NonNull GadgetValue[] values, int progress) {

    }

    @Override
    public void onSetGadgetLoggingEnabledFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {

    }

    @Override
    public void onSetLoggerIntervalFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {

    }

    @Override
    public void onSetLoggerIntervalSuccess(@NonNull Gadget gadget) {

    }

    @Override
    public void onDownloadFailed(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {

    }

    @Override
    public void onDownloadCompleted(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {

    }

    @Override
    public void onDownloadNoData(@NonNull Gadget gadget, @NonNull GadgetDownloadService service) {

    }
}
