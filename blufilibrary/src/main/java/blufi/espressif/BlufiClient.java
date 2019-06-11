package blufi.espressif;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattCallback;

import java.util.List;

import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;

public class BlufiClient {
    public static final String VERSION = "2.2.1";

    private BlufiClientImpl mImpl;

    public BlufiClient() {
        mImpl = new BlufiClientImpl(this);
    }

    /**
     * The client to data communication with device which run BluFi.
     * When communicate complete, the client should call {@link #close()} to release the resources.
     */
    public BlufiClient(BluetoothGatt gatt, BluetoothGattCharacteristic writeCharact,
                       BluetoothGattCharacteristic notiCharact, BlufiCallback callback) {
        mImpl = new BlufiClientImpl(this, gatt, writeCharact, notiCharact, callback);
    }

    /**
     * Set BluetoothGatt.
     *
     * @param gatt Device BLE gatt
     */
    public void setBluetoothGatt(BluetoothGatt gatt) {
        mImpl.setBluetoothGatt(gatt);
    }

    /**
     * @return BluetoothGatt
     */
    public BluetoothGatt getBluetoothGatt() {
        return mImpl.getBluetoothGatt();
    }

    /**
     * Set write BluetoothGattCharacteristic
     *
     * @param characteristic App send data to Device
     */
    public void setWriteGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        mImpl.setWriteGattCharacteristic(characteristic);
    }

    /**
     * Set notification BluetoothGattCharacteristic
     *
     * @param characteristic Device send data to App
     */
    public void setNotificationGattCharacteristic(BluetoothGattCharacteristic characteristic) {
        mImpl.setNotificationGattCharacteristic(characteristic);
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
     * Close the client
     */
    public void close() {
        mImpl.close();
    }

    /**
     * Call this function in
     * {@link BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic)}
     *
     * @param gatt BluetoothGatt
     * @param characteristic BluetoothGattCharacteristic
     */
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        mImpl.onCharacteristicChanged(gatt, characteristic);
    }

    /**
     * Call this function in
     * {@link BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)}
     *
     * @param gatt BluetoothGatt
     * @param characteristic BluetoothGattCharacteristic
     * @param status gatt status
     */
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        mImpl.onCharacteristicWrite(gatt, characteristic, status);
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
