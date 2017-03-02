package com.espressif.espblufi;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermisiionHelper {
    private static final int REQUEST_CODE = 1;

    private Activity mActivity;

    public PermisiionHelper(Activity activity) {
        mActivity = activity;
    }

    private String[] getManifextPermissions() {
        // all permissions in AndroidManifext.xml
        // for android don't let me get them dynamically, it is ugly to code like this
        return new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };
    }

    public void requestAuthorities() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // permissions0 means permissions require requesting
            final List<String> permissions0 = new ArrayList<>();

            for (String permission : getManifextPermissions()) {
                if (ContextCompat.checkSelfPermission(mActivity, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissions0.add(permission);
                }
            }

            if (!permissions0.isEmpty()) {
                String[] permissions1 = new String[permissions0.size()];
                for (int i = 0; i < permissions0.size(); i++) {
                    permissions1[i] = permissions0.get(i);
                }
                // request permission one by one
                ActivityCompat.requestPermissions(mActivity,
                        permissions1,
                        REQUEST_CODE
                );
            } else {
                permissionsPermited();
            }
        } else {
            permissionsPermited();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            boolean isForbidden = false;
            for (int grantResult : grantResults) {
                if (grantResult != 0) {
                    isForbidden = true;
                    break;
                }
            }
            if (isForbidden) {
                permissionsDenied();
            } else {
                permissionsPermited();
            }
        }
    }

    private void permissionsPermited() {
    }

    private void permissionsDenied() {
    }
}
