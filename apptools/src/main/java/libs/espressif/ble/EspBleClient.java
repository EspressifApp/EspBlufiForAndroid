package libs.espressif.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import libs.espressif.app.SdkUtil;
import libs.espressif.log.EspLog;

public class EspBleClient {
    public static final int CONNECTION_PRIORITY_BALANCED;
    public static final int CONNECTION_PRIORITY_HIGH;
    public static final int CONNECTION_PRIORITY_LOW_POWER;

    private static final long TIMEOUT_CONN = 4000L;
    private static final long TIMEOUT_SERVICE = 8000L;
    private static final long TIMEOUT_WRITE = 4000L;

    private static final Map<ScanListener, Object> mScanListenerMap = new HashMap<>();

    static {
        if (SdkUtil.isAtLeastL()) {
            CONNECTION_PRIORITY_BALANCED = BluetoothGatt.CONNECTION_PRIORITY_BALANCED;
            CONNECTION_PRIORITY_HIGH = BluetoothGatt.CONNECTION_PRIORITY_HIGH;
            CONNECTION_PRIORITY_LOW_POWER = BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER;
        } else {
            CONNECTION_PRIORITY_BALANCED = 0;
            CONNECTION_PRIORITY_HIGH = 1;
            CONNECTION_PRIORITY_LOW_POWER = 2;
        }
    }

    private final EspLog log = new EspLog(getClass());

    private final Object mConnectLock = new Object();

    private final List<GattCallback> mUserCallbacks;

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothGatt mGatt;

    private Callback mCallback;

    private int mConnectState;

    public EspBleClient(Context context) {
        mContext = context.getApplicationContext();
        mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mUserCallbacks = new LinkedList<>();

        mConnectState = BluetoothProfile.STATE_DISCONNECTED;
    }

    /**
     * Starts a scan for Bluetooth LE devices.
     *
     * @param listener the callback LE scan results are delivered.
     * @return true, if the scan was started successfully.
     */
    public static boolean startScanBle(@NonNull final ScanListener listener) {
        // This listener scanning has started.
        if (mScanListenerMap.get(listener) != null) {
            return false;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ScanCallback callback = new ScanCallback() {
                @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    BluetoothDevice device = result.getDevice();
                    int rssi = result.getRssi();
                    byte[] scanRecord = result.getScanRecord() == null ? null : result.getScanRecord().getBytes();
                    if (mScanListenerMap.get(listener) != null) {
                        listener.onLeScan(device, rssi, scanRecord);
                    }
                }
            };
            mScanListenerMap.put(listener, callback);
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan(null, settings, callback);
            return true;
        } else {
            BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    if (mScanListenerMap.get(listener) != null) {
                        listener.onLeScan(device, rssi, scanRecord);
                    }
                }
            };
            mScanListenerMap.put(listener, callback);
            //noinspection deprecation
            return adapter.startLeScan(callback);
        }
    }

    /**
     * Stops an ongoing Bluetooth LE device scan.
     *
     * @param listener callback used to identify which scan to stop
     *                 must be the same handle used to start the scan
     */
    public static void stopScanBle(@NonNull ScanListener listener) {
        Object callback = mScanListenerMap.remove(listener);
        if (callback == null) {
            return;
        }

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (callback instanceof ScanCallback) {
                adapter.getBluetoothLeScanner().stopScan((ScanCallback) callback);
            } else {
                //noinspection deprecation
                adapter.stopLeScan((BluetoothAdapter.LeScanCallback) callback);
            }

        } else {
            //noinspection deprecation
            adapter.stopLeScan((BluetoothAdapter.LeScanCallback) callback);
        }
    }

    /**
     * Get Ble connection state
     *
     * @return the state of ble connection
     */
    public int getConnectState() {
        return mConnectState;
    }

    /**
     * Register the Gattt callback.
     *
     * @param callback the callback is used to deliver results to Caller, such as connection status as well
     *                 as any further GATT client operations.
     */
    public void registerGattCallback(GattCallback callback) {
        synchronized (mUserCallbacks) {
            if (!mUserCallbacks.contains(callback)) {
                mUserCallbacks.add(callback);
            }
        }
    }

    /**
     * Ungreister the callback.
     *
     * @param callback the callback has registered.
     */
    public void unregisterGattCallback(GattCallback callback) {
        synchronized (mUserCallbacks) {
            if (mUserCallbacks.contains(callback)) {
                mUserCallbacks.remove(callback);
            }
        }
    }

    /**
     * Connect to GATT Server hosted by this device.
     *
     * @param device the device need connect
     * @return true, if connect successfully.
     */
    public boolean connectGatt(BluetoothDevice device) {
        synchronized (mConnectLock) {
            log.d(String.format("EspBleHelper %s connectGatt", device.getName()));
            if (mGatt != null) {
                throw new IllegalStateException("the gatt has connected a device already");
            }

            final int tryCount = 2;
            boolean result = false;
            for (int i = 0; i < tryCount; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                log.d(String.format(Locale.ENGLISH, "EspBleHelper %s connect %d", device.getName(), i));
                mCallback = new Callback();
                mGatt = connect(device, mCallback);
                if (mGatt == null) {
                    return false;
                }
                result = mCallback.waitConnect(TIMEOUT_CONN);

                if (result) {
                    log.d(String.format("EspBleHelper %s discoverServices", device.getName()));
                    mCallback.clear();
                    if (!isBleOn()) {
                        return false;
                    }
                    mGatt.discoverServices();
                    result = mCallback.waitService(TIMEOUT_SERVICE);
                }

                if (!result) {
                    log.d(String.format("EspBleHelper %s connectGatt close", device.getName()));
                    if (isBleOn()) {
                        disconnect();
                        refreshDeviceCache(mGatt);
                        mGatt.close();
                    }
                    mGatt = null;
                    mCallback.clear();
                    mConnectState = BluetoothProfile.STATE_DISCONNECTED;
                }

                if (result) {
                    break;
                }
            }

            log.d(String.format("EspBleHelper %s connectGatt result %b", device.getName(), result));
            return result;
        }
    }

    private BluetoothGatt connect(BluetoothDevice device, Callback callback) {
        if (!isBleOn()) {
            return null;
        }

        BluetoothGatt gatt;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            gatt = device.connectGatt(mContext, false, callback, BluetoothDevice.TRANSPORT_LE);
        } else {
            gatt = device.connectGatt(mContext, false, callback);
        }

        return gatt;
    }

    private void disconnect() {
        if (mGatt != null && mConnectState != BluetoothProfile.STATE_DISCONNECTED) {
            mGatt.disconnect();
            mCallback.waitConnect(1000);
        }
    }

    /**
     * Close the Ble connection.
     */
    public void close() {
        synchronized (mConnectLock) {
            log.d("EspBleHelper close");
            if (mGatt != null) {
                log.d("EspBleHelper close gatt not null");
                if (isBleOn()) {
                    disconnect();
                    boolean refresh = refreshDeviceCache(mGatt);
                    log.i("close refresh deviced cache " + refresh);
                    mGatt.close();
                    log.d("EspBleHelper close gatt close");
                }
                mConnectState = BluetoothProfile.STATE_DISCONNECTED;

                mUserCallbacks.clear();
                mCallback.clear();
                mCallback = null;
                mGatt.close();

                mGatt = null;
            }
        }
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        boolean result = false;
        if (gatt != null) {
            try {
                Method localMethod = gatt.getClass().getMethod("refresh");
                if (localMethod != null) {
                    log.d("refreshDeviceCache execute ble refresh");
                    result = (Boolean) localMethod.invoke(gatt);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                log.d("refreshDeviceCache remove bond");
                Method m = gatt.getDevice().getClass().getMethod("removeBond");
                m.invoke(gatt.getDevice());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private boolean isBleOn() {
        return mBluetoothManager.getAdapter().getState() == BluetoothAdapter.STATE_ON;
    }

    private boolean enable() {
        return isBleOn()
                && mGatt != null
                && mConnectState == BluetoothGatt.STATE_CONNECTED;
    }

    /**
     * Get the BluetoothGattService if the requested UUID is
     * supported by the remote device.
     *
     * @param uuid UUID of the requested service
     * @return BluetoothGattService if supported, or null if the requested
     * service is not offered by the remote device.
     */
    public BluetoothGattService discoverService(UUID uuid) {
        log.d("EspBleHelper discoverService");
        if (!enable()) {
            return null;
        }

        return mGatt.getService(uuid);
    }

    /**
     * Request an MTU size used for a given connection.
     *
     * @return true, if the new MTU value has been requested successfully
     */
    public boolean requestMtu(int mtu) {
        log.d("EspBleHelper requestMtu");
        if (!enable()) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mGatt.requestMtu(mtu)) {
                return mCallback.waitMtu();
            }
        }

        return false;
    }

    public boolean requestConnectionPriority(int priority) {
        if (!enable()) {
            return false;
        }

        if (SdkUtil.isAtLeastL()) {
            return mGatt.requestConnectionPriority(priority);
        }

        return false;
    }

    /**
     * Writes a given characteristic and its values to the associated remote device.
     *
     * @param characteristic Characteristic to write on the remote device
     * @param data           the write data
     * @return true, if the write operation was initiated successfully
     */
    public boolean write(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (!enable()) {
            return false;
        }

        characteristic.setValue(data);
        mGatt.writeCharacteristic(characteristic);
        return mCallback.waitWrite(TIMEOUT_WRITE);
    }

    public boolean write(BluetoothGattCharacteristic characteristic, byte[] data, boolean resp) {
        if (!enable()) {
            return false;
        }

        int writeType = resp ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
        characteristic.setWriteType(writeType);
        mGatt.writeCharacteristic(characteristic);
        switch (writeType) {
            case BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:
                return true;
            default:
                return mCallback.waitWrite(TIMEOUT_WRITE);
        }
    }

    /**
     * Enable or disable notifications/indications for a given characteristic.
     *
     * @param characteristic The characteristic for which to enable notifications
     * @param enable         Set to true to enable notifications/indications
     * @return true, if the requested notification status was set successfully
     */
    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (!enable()) {
            return false;
        }

        return mGatt.setCharacteristicNotification(characteristic, enable);
    }

    public static class GattCallback {
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        }

        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        }

        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        }

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        }
    }

    private class Callback extends BluetoothGattCallback {
        private LinkedBlockingQueue<Boolean> mConnectQueue;
        private LinkedBlockingQueue<Boolean> mServiceQueue;
        private LinkedBlockingQueue<Boolean> mMtuQueue;
        private LinkedBlockingQueue<Boolean> mWriteQueue;

        Callback() {
            mConnectQueue = new LinkedBlockingQueue<>();
            mServiceQueue = new LinkedBlockingQueue<>();
            mMtuQueue = new LinkedBlockingQueue<>();
            mWriteQueue = new LinkedBlockingQueue<>();
        }

        void notifyDisconnected() {
            log.w("notifyDisconnected");
            mServiceQueue.add(false);
            mMtuQueue.add(false);
            mWriteQueue.add(false);
        }

        void clear() {
            mConnectQueue.clear();
            mServiceQueue.clear();
            mMtuQueue.clear();
            mWriteQueue.clear();
        }

        boolean waitConnect() {
            try {
                return mConnectQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        boolean waitConnect(long timeout) {
            try {
                Boolean result = mConnectQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (result == null) {
                    return false;
                } else {
                    return result;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return false;
        }

        boolean waitService() {
            try {
                return mServiceQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        boolean waitService(long timeout) {
            try {
                Boolean result = mServiceQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (result == null) {
                    return false;
                } else {
                    return result;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        boolean waitMtu() {
            try {
                return mMtuQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        boolean waitWrite() {
            try {
                return mWriteQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        boolean waitWrite(long timeout) {
            try {
                Boolean result = mWriteQueue.poll(timeout, TimeUnit.MILLISECONDS);
                if (result == null) {
                    return false;
                } else {
                    return result;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            log.i(String.format(Locale.ENGLISH, "EspBleHelper %s onConnectionStateChange status=%d, state=%d",
                    gatt.getDevice().getName(), status, newState));
            mConnectState = newState;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        mConnectQueue.add(true);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        boolean refresh = refreshDeviceCache(gatt);
                        gatt.close();
                        log.i("onConnectionStateChange refresh deviced cache " + refresh);
                        mConnectQueue.add(false);
                        notifyDisconnected();
                        break;
                }
            } else {
                refreshDeviceCache(gatt);
                gatt.close();
                mConnectQueue.add(false);
                notifyDisconnected();
            }

            for (GattCallback callback : mUserCallbacks) {
                callback.onConnectionStateChange(gatt, status, newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log.i(String.format(Locale.ENGLISH, "EspBleHelper %s onServicesDiscovered status=%d",
                    gatt.getDevice().getName(), status));
            mServiceQueue.add(status == BluetoothGatt.GATT_SUCCESS);

            for (GattCallback callback : mUserCallbacks) {
                callback.onServicesDiscovered(gatt, status);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            log.i(String.format(Locale.ENGLISH, "EspBleHelper %s onMtuChanged status=%d, mtu=%d",
                    gatt.getDevice().getName(), status, mtu));
            mMtuQueue.add(status == BluetoothGatt.GATT_SUCCESS);

            for (GattCallback callback : mUserCallbacks) {
                callback.onMtuChanged(gatt, mtu, status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic, int status) {
            log.i(String.format(Locale.ENGLISH, "EspBleHelper %s onCharacteristicWrite status=%d",
                    gatt.getDevice().getName(), status));
            mWriteQueue.add(status == BluetoothGatt.GATT_SUCCESS);
            for (GattCallback callback : mUserCallbacks) {
                callback.onCharacteristicWrite(gatt, characteristic, status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            for (GattCallback callback : mUserCallbacks) {
                callback.onCharacteristicChanged(gatt, characteristic);
            }
        }
    }
}
