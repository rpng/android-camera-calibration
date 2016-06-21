package io.rpng.calibration;


import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import io.rpng.calibration.dialogs.ConfirmationDialog;
import io.rpng.calibration.dialogs.ErrorDialog;

public class PermissionManager implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static Activity activity;
    public static int REQUEST_CAMERA_PERMISSION = 1;

    public static void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
            new ConfirmationDialog().show(activity.getFragmentManager(), "dialog");
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length != 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                ErrorDialog.newInstance(activity.getString(R.string.request_permission)).show(activity.getFragmentManager(), "dialog");
            }
        } else {
            // Call super
            onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
