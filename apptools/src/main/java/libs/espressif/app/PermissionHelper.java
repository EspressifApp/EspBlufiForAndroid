package libs.espressif.app;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private final int mRequestCode;

    private Activity mActivity;

    private OnPermissionsListener mListener;

    public PermissionHelper(@NonNull Activity activity, int requestCode) {
        mActivity = activity;
        mRequestCode = requestCode;
    }

    public int getRequestCode() {
        return mRequestCode;
    }

    /**
     * Call this function in AppCompatActivity#onRequestPermissionsResult.
     *
     * @param requestCode  the request code
     * @param permissions  the request permissions
     * @param grantResults the grant results
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mRequestCode) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                boolean granted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                if (mListener != null) {
                    mListener.onPermissonsChange(permission, granted);
                }
            }
        }
    }

    /**
     * Requests permissions to be granted to this application.
     *
     * @param permissions the requested permissions.
     */
    public void requestAuthorities(@NonNull String[] permissions) {
        if (SdkUtil.isAtLeastM_23()) {
            final List<String> requirePermissionList = new ArrayList<>();

            for (String permission : permissions) {
                if (!isPermissionGranted(permission)) {
                    requirePermissionList.add(permission);
                } else {
                    if (mListener != null) {
                        mListener.onPermissonsChange(permission, true);
                    }
                }
            }

            if (!requirePermissionList.isEmpty()) {
                String[] requirePermissionArray = new String[requirePermissionList.size()];
                for (int i = 0; i < requirePermissionList.size(); i++) {
                    requirePermissionArray[i] = requirePermissionList.get(i);
                }
                // request permission one by one
                ActivityCompat.requestPermissions(mActivity, requirePermissionArray, mRequestCode);
            }
        } else {
            if (mListener != null) {
                for (String permission : permissions) {
                    mListener.onPermissonsChange(permission, true);
                }
            }
        }
    }

    /**
     * Determine whether user have been granted a particular permission.
     *
     * @param permission the name of the permission being checked.
     * @return granted or not.
     */
    public boolean isPermissionGranted(String permission) {
        return ContextCompat.checkSelfPermission(mActivity, permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Register a callback to be invoked when a permission changed.
     * It will take effect after call #onRequestPermissionsResult in AppCompatActivity.
     *
     * @param listener the callback that will run
     */
    public void setOnPermissionsListener(OnPermissionsListener listener) {
        mListener = listener;
    }

    /**
     * Interface definition for a callback to be invoked when a permission changed.
     */
    public interface OnPermissionsListener {
        /**
         * Call when the permission has changed.
         *
         * @param permission the changed permisson.
         * @param granted or not.
         */
        void onPermissonsChange(String permission, boolean granted);
    }
}
