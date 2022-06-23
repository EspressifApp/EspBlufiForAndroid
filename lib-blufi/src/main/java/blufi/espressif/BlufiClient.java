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

    private final BlufiClientImpl mImpl;

    public BlufiClient(Context context, BluetoothDevice device) {
        mImpl = new BlufiClientImpl(this, context, device);
    }

    /**
     * Enable or disable print debug log in BlufiClient
     *
     * @param enable true will print debug log and false will not
     */
    public void printDebugLog(boolean enable) {
        mImpl.printDebugLog(enable);
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
     * Set the maximum length of each Blufi packet, the excess part will be subcontracted.
     *
     * @param lengthLimit the maximum length
     */
    public void setPostPackageLengthLimit(int lengthLimit) {
        mImpl.setPostPackageLengthLimit(lengthLimit);
    }

    /**
     * Set gatt write timeout.
     * If timeout, {@link BlufiCallback#onError(BlufiClient, int)} will be invoked,
     * the errCode is {@link BlufiCallback#CODE_GATT_WRITE_TIMEOUT}
     *
     * @param timeout in milliseconds
     */
    public void setGattWriteTimeout(long timeout) {
        mImpl.setGattWriteTimeout(timeout);
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
     * Negotiate security with device. The result will be notified in
     * {@link BlufiCallback#onNegotiateSecurityResult(BlufiClient, int)}
     */
    public void negotiateSecurity() {
        mImpl.negotiateSecurity();
    }

    /**
     * Request device to disconnect the BLE connection
     */
    public void requestCloseConnection() {
        mImpl.requestCloseConnection();
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
     * Request to get wifi list that the device scanned. The wifi list will be notified in
     * {@link BlufiCallback#onDeviceScanResult(BlufiClient, int, List)}
     */
    public void requestDeviceWifiScan() {
        mImpl.requestDeviceWifiScan();
    }

    /**
     * Configure the device to a station or soft AP. The posted result will be notified in
     * {@link BlufiCallback#onPostConfigureParams(BlufiClient, int)}
     *
     * @param params the config parameter
     */
    public void configure(final BlufiConfigureParams params) {
        mImpl.configure(params);
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
}
