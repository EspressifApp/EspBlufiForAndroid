package blufi.espressif;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.List;

import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;

public abstract class BlufiCallback {
    public static final int STATUS_SUCCESS = 0;

    public static final int CODE_INVALID_NOTIFICATION = -1000;
    public static final int CODE_CATCH_EXCEPTION = -1001;
    public static final int CODE_WRITE_DATA_FAILED = -1002;
    public static final int CODE_INVALID_DATA = -1003;

    public static final int CODE_NEG_POST_FAILED = -2000;
    public static final int CODE_NEG_ERR_DEV_KEY = -2001;
    public static final int CODE_NEG_ERR_SECURITY = -2002;
    public static final int CODE_NEG_ERR_SET_SECURITY = -2003;

    public static final int CODE_CONF_INVALID_OPMODE = -3000;
    public static final int CODE_CONF_ERR_SET_OPMODE = -3001;
    public static final int CODE_CONF_ERR_POST_STA = -3002;
    public static final int CODE_CONF_ERR_POST_SOFTAP = -3003;

    public static final int CODE_GATT_WRITE_TIMEOUT = -4000;

    public static final int CODE_WIFI_SCAN_FAIL = 11;

    /**
     * Callback invoked after BluetoothGattCallback receive onServicesDiscovered
     * User can post Blufi packet now.
     *
     * @param client BlufiClient
     * @param gatt BluetoothGatt
     * @param service null if discover Blufi GattService failed
     * @param writeChar null if discover Blufi write GattCharacteristic failed
     * @param notifyChar null if discover Blufi notify GattCharacteristic failed
     */
    public void onGattPrepared(BlufiClient client, BluetoothGatt gatt, BluetoothGattService service,
                               BluetoothGattCharacteristic writeChar, BluetoothGattCharacteristic notifyChar) {
    }

    /**
     * Callback invoked when receive Gatt notification
     *
     * @param client BlufiClient
     * @param pkgType Blufi package type
     * @param subType Blufi subtype
     * @param data Blufi data
     * @return true if the callback consumed the notification, false otherwise.
     */
    public boolean onGattNotification(BlufiClient client, int pkgType, int subType, byte[] data) {
        return false;
    }

    /**
     * Callback invoked when received error code from device
     *
     * @param client BlufiClient
     * @param errCode received
     */
    public void onError(BlufiClient client, int errCode) {
    }

    /**
     * Callback invoked when negotiate security over with device
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means negotiate security success
     */
    public void onNegotiateSecurityResult(BlufiClient client, int status) {
    }

    /**
     * @deprecated use {@link #onPostConfigureParams(BlufiClient, int)}
     */
    @Deprecated
    public void onConfigureResult(BlufiClient client, int status) {
    }

    /**
     * Callback invoked when post config data over
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means post data success
     */
    public void onPostConfigureParams(BlufiClient client, int status) {
        onConfigureResult(client, status);
    }

    /**
     * Callback invoked when received device version
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means response is valid
     * @param response BlufiVersionResponse
     */
    public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
    }

    /**
     * Callback invoked when received device status
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means response is valid
     * @param response BlufiStatusResponse
     */
    public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
    }

    /**
     * Callback invoked when received device scan results
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means response is valid
     * @param results scan result list
     */
    public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
    }

    /**
     * Callback invoked when post custom data over
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means post data success
     * @param data posted
     */
    public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
    }

    /**
     * Callback invoked when received custom data from device
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means receive data success
     * @param data received
     */
    public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
    }
}
