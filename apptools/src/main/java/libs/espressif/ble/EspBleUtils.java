package libs.espressif.ble;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import libs.espressif.app.SdkUtil;
import libs.espressif.utils.DataUtil;

public class EspBleUtils {
    private static final String UUID_INDICATION_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String BASE_UUID_FORMAT = "0000%s-0000-1000-8000-00805f9b34fb";

    private static final Map<ScanListener, Object> mScanListenerMap = new HashMap<>();

    public static UUID newUUID(String address) {
        String string = String.format(BASE_UUID_FORMAT, address);
        return UUID.fromString(string);
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
            BleScanCallback scanCallback = new BleScanCallback(listener);
            mScanListenerMap.put(listener, scanCallback);
            BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .build();
            scanner.startScan(null, settings, scanCallback);
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static class BleScanCallback extends ScanCallback {
        ScanListener mScanListener;

        BleScanCallback(ScanListener scanListener) {
            mScanListener = scanListener;
        }

        void onScanDevice(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            int rssi = result.getRssi();
            byte[] scanRecord = result.getScanRecord() == null ? null : result.getScanRecord().getBytes();
            if (mScanListenerMap.get(mScanListener) != null) {
                mScanListener.onLeScan(device, rssi, scanRecord);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            onScanDevice(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onScanDevice(result);
            }
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

    public static UUID newUUID(byte[] bytes) {
        String address = String.format("%02x%02x", bytes[0], bytes[1]);
        return newUUID(address);
    }

    public static UUID getIndicationDescriptorUUID() {
        return UUID.fromString(UUID_INDICATION_DESCRIPTOR);
    }

    public static BluetoothGatt connectGatt(BluetoothDevice device, Context context, BluetoothGattCallback callback) {
        if (SdkUtil.isAtLeastM_23()) {
            return device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE);
        } else {
            return device.connectGatt(context, false, callback);
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static String getDeviceName(ScanResult sr) {
        String name = sr.getDevice().getName();;
        if (name == null) {
            ScanRecord record = sr.getScanRecord();
            if (record != null) {
                name = record.getDeviceName();
            }
        }

        return name;
    }

    public static List<BleAdvData> resolveScanRecord(byte[] record) {
        if (record == null) {
            return Collections.emptyList();
        }

        List<BleAdvData> result = new ArrayList<>();

        int offset = 0;

        do {
            int len = record[offset] & 0xff;
            if (len == 0) {
                break;
            }
            if (offset + 1 + len >= record.length) {
                break;
            }

            int type = record[offset + 1] & 0xff;
            byte[] data = DataUtil.subBytes(record, offset + 2, len - 1);

            BleAdvData advData = new BleAdvData();
            advData.setType(type);
            advData.setData(data);
            result.add(advData);

            offset += (len + 1);
        } while (offset < record.length);

        return result;
    }
}
