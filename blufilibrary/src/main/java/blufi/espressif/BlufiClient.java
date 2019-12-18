package blufi.espressif;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;

import java.util.List;

import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;

public class BlufiClient {
    public static final String VERSION = BuildConfig.VERSION_NAME;

    private BlufiClientImpl mImpl;

    public BlufiClient(Context context, BluetoothDevice device) {
        mImpl = new BlufiClientImpl(this, context, device);
    }

    /**
     * Set BluetoothGattCallback
     *
     * @param callback the BluetoothGattCallback
     */
    public void setGattCallback(BluetoothGattCallback callback) {
        mImpl.setGattCallback(callback);
    }

    /**
     * Set the callback
     *
     * @param callback the BlufiCallback
     */
    public void setBlufiCallback(BlufiCallback callback) {
        mImpl.setBlufiCallback(callback);
    }

    /**
     * Establish a BLE connection with BluetoothDevice
     */
    public void connect() {
        mImpl.connect();
    }

    /**
     * Close the client
     */
    public void close() {
        mImpl.close();
    }

    /**
     * Set the maximum length of each packet of data, the excess part will be subcontracted.
     *
     * @param lengthLimit the maximum length
     */
    public void setPostPackageLengthLimit(int lengthLimit) {
        mImpl.setPostPackageLengthLimit(lengthLimit);
    }

    /**
     * Request to get device version. The result will notified in
     * {@link BlufiCallback#onDeviceVersionResponse(BlufiClient, int, BlufiVersionResponse)}
     */
    public void requestDeviceVersion() {
        mImpl.requestDeviceVersion();
    }

    /**
     * Request to get device current status. The result will be notified in
     * {@link BlufiCallback#onDeviceStatusResponse(BlufiClient, int, BlufiStatusResponse)}
     */
    public void requestDeviceStatus() {
        mImpl.requestDeviceStatus();
    }

    /**
     * Negotiate security with device. The result will be notified in
     * {@link BlufiCallback#onNegotiateSecurityResult(BlufiClient, int)}
     */
    public void negotiateSecurity() {
        mImpl.negotiateSecurity();
    }

    /**
     * Configure the device to a station or soft AP. The posted result will be notified in
     * {@link BlufiCallback#onConfigureResult(BlufiClient, int)}
     *
     * @param params the config parameter
     */
    public void configure(final BlufiConfigureParams params) {
        mImpl.configure(params);
    }

    /**
     * Request to get wifi list that the device scanned. The wifi list will be notified in
     * {@link BlufiCallback#onDeviceScanResult(BlufiClient, int, List)}
     */
    public void requestDeviceWifiScan() {
        mImpl.requestDeviceWifiScan();
    }

    /**
     * Request to post custom data to device. The posted result will be notified in
     * {@link BlufiCallback#onPostCustomDataResult(BlufiClient, int, byte[])}
     *
     * @param data the custom data
     */
    public void postCustomData(byte[] data) {
        mImpl.postCustomData(data);
    }

    /**
     * Request device to disconnect the ble connection
     */
    public void requestCloseConnection() {
        mImpl.requestCloseConnection();
    }
}
