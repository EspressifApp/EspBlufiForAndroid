package blufi.espressif;

import java.util.List;

import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;

public abstract class BlufiCallback {
    public static final int STATUS_SUCCESS = 0;

    /**
     * Callback invoked when receive Gatt notification
     *
     * @param client BlufiClient
     * @param pkgType Blufi package type
     * @param subType Blufi subtype
     * @param data notified
     * @return true if the callback consumed the notification, false otherwise.
     */
    public boolean onGattNotification(BlufiClient client, int pkgType, int subType, byte[] data) {
        return false;
    }

    /**
     * Callback invoked when the client close the Gatt
     *
     * @param client BlufiClient
     */
    public void onGattClose(BlufiClient client) {
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
     * Callback invoked when post config data over
     *
     * @param client BlufiClient
     * @param status {@link #STATUS_SUCCESS} means post data success
     */
    public void onConfigureResult(BlufiClient client, int status) {
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
     * Callback invoked when received device scan result
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
