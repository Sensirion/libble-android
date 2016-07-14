package com.sensirion.libsmartgadget.utils;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public final class BLEUtility {
    private static final String TAG = BLEUtility.class.getSimpleName();

    private BLEUtility() {
    }

    /**
     * Checks if BLE connections are supported and if Bluetooth is enabled on the device.
     *
     * @return <code>true</code> if it's enabled - <code>false</code> otherwise.
     */
    public static boolean isBLEEnabled(@NonNull final Context context) {
        // Use this check to determine whether BLE is supported on the device.
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.e(TAG, "Bluetooth LE is not supported on this device");
            return false;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth is not supported on this device");
            return false;
        }

        return bluetoothAdapter.isEnabled();
    }

    /**
     * Runtime request for ACCESS_FINE_LOCATION. This is required on Android 6.0 and higher in order
     * to perform BLE scans.
     *
     * @param requestingActivity The activity requesting the permission.
     * @param requestCode        The request code used to deliver the user feedback to the calling
     *                           activity.
     */
    public static void requestScanningPermission(@NonNull final Activity requestingActivity,
                                                 final int requestCode) {
        final String permission = Manifest.permission.ACCESS_FINE_LOCATION;
        if (ContextCompat.checkSelfPermission(requestingActivity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(requestingActivity, permission)) {
                ActivityCompat.requestPermissions(requestingActivity, new String[]{permission}, requestCode);
            }
        }
    }

    /**
     * Request the user to enable bluetooth in case it's disabled.
     *
     * @param context {@link android.content.Context} of the requesting activity.
     */
    public static void requestEnableBluetooth(@NonNull final Context context) {
        if (isBLEEnabled(context)) {
            return;
        } else {
            final Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBluetoothIntent);
        }
    }
}
