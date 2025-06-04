package com.espressif.espblufi.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.app.BlufiLog;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;
import com.espressif.espblufi.databinding.MainActivityBinding;
import com.espressif.espblufi.databinding.MainBleItemBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SuppressLint("MissingPermission")
public class MainActivity extends AppCompatActivity {
    private static final long TIMEOUT_SCAN = 4000L;

    private static final int REQUEST_PERMISSION = 0x01;
    private static final int REQUEST_BLUFI = 0x10;

    private static final int MENU_SETTINGS = 0x01;

    private final BlufiLog mLog = new BlufiLog(getClass());

    private MainActivityBinding mBinding;

    private List<ScanResult> mBleList;
    private BleAdapter mBleAdapter;

    private Map<String, ScanResult> mDeviceMap;
    private ScanCallback mScanCallback;
    private String mBlufiFilter;
    private volatile long mScanStartTime;

    private ExecutorService mThreadPool;
    private Future<Boolean> mUpdateFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);

        mThreadPool = Executors.newSingleThreadExecutor();


        mBinding.refreshLayout.setColorSchemeResources(R.color.colorAccent);
        mBinding.refreshLayout.setOnRefreshListener(this::scan);

        mBleList = new LinkedList<>();
        mBleAdapter = new BleAdapter();
        mBinding.recyclerView.setAdapter(mBleAdapter);

        mDeviceMap = new HashMap<>();
        mScanCallback = new ScanCallback();

        List<String> permissionList = new ArrayList<>();
        permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            permissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        ActivityCompat.requestPermissions(
                this,
                permissionList.toArray(new String[0]),
                REQUEST_PERMISSION
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
        mThreadPool.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int size = permissions.length;
        for (int i = 0; i < size; ++i) {
            String permission = permissions[i];
            int grant = grantResults[i];

            if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grant == PackageManager.PERMISSION_GRANTED) {
                    mBinding.refreshLayout.setRefreshing(true);
                    scan();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BLUFI) {
            mBinding.refreshLayout.setRefreshing(true);
            scan();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.main_menu_settings);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_SETTINGS) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scan() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (!adapter.isEnabled() || scanner == null) {
            Toast.makeText(this, R.string.main_bt_disable_msg, Toast.LENGTH_SHORT).show();
            mBinding.refreshLayout.setRefreshing(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check location enable
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean locationEnable = locationManager != null && LocationManagerCompat.isLocationEnabled(locationManager);
            if (!locationEnable) {
                Toast.makeText(this, R.string.main_location_disable_msg, Toast.LENGTH_SHORT).show();
                mBinding.refreshLayout.setRefreshing(false);
                return;
            }
        }

        mDeviceMap.clear();
        mBleList.clear();
        mBleAdapter.notifyDataSetChanged();
        mBlufiFilter = (String) BlufiApp.getInstance().settingsGet(SettingsConstants.PREF_SETTINGS_KEY_BLE_PREFIX,
                BlufiConstants.BLUFI_PREFIX);
        mScanStartTime = SystemClock.elapsedRealtime();

        mLog.d("Start scan ble");
        ScanSettings.Builder scanSettingsBuilder = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            scanSettingsBuilder.setLegacy(false);
        }
        scanner.startScan(null, scanSettingsBuilder.build(), mScanCallback);
        mUpdateFuture = mThreadPool.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }

                long scanCost = SystemClock.elapsedRealtime() - mScanStartTime;
                if (scanCost > TIMEOUT_SCAN) {
                    break;
                }

                onIntervalScanUpdate(false);
            }

            BluetoothLeScanner inScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
            if (inScanner != null) {
                inScanner.stopScan(mScanCallback);
            }
            onIntervalScanUpdate(true);
            mLog.d("Scan ble thread is interrupted");
            return true;
        });
    }

    private void stopScan() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.stopScan(mScanCallback);
        }
        if (mUpdateFuture != null) {
            mUpdateFuture.cancel(true);
        }
        mLog.d("Stop scan ble");
    }

    private void onIntervalScanUpdate(boolean over) {
        List<ScanResult> devices = new ArrayList<>(mDeviceMap.values());
        Collections.sort(devices, (dev1, dev2) -> {
            Integer rssi1 = dev1.getRssi();
            Integer rssi2 = dev2.getRssi();
            return rssi2.compareTo(rssi1);
        });
        runOnUiThread(() -> {
            mBleList.clear();
            mBleList.addAll(devices);
            mBleAdapter.notifyDataSetChanged();

            if (over) {
                mBinding.refreshLayout.setRefreshing(false);
            }
        });
    }

    private void gotoDevice(BluetoothDevice device) {
        Intent intent = new Intent(MainActivity.this, BlufiActivity.class);
        intent.putExtra(BlufiConstants.KEY_BLE_DEVICE, device);
        startActivityForResult(intent, REQUEST_BLUFI);

        mDeviceMap.clear();
        mBleList.clear();
        mBleAdapter.notifyDataSetChanged();
    }

    private class BleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ScanResult scanResult;
        MainBleItemBinding binding;

        BleHolder(MainBleItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
            binding.itemContent.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            stopScan();
            gotoDevice(scanResult.getDevice());
        }
    }

    private class ScanCallback extends android.bluetooth.le.ScanCallback {

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                onLeScan(result);
            }
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            onLeScan(result);
        }

        private void onLeScan(ScanResult scanResult) {
            String name = scanResult.getDevice().getName();
            if (!TextUtils.isEmpty(mBlufiFilter)) {
                if (name == null || !name.startsWith(mBlufiFilter)) {
                    return;
                }
            }

            mDeviceMap.put(scanResult.getDevice().getAddress(), scanResult);
        }
    }

    private class BleAdapter extends RecyclerView.Adapter<BleHolder> {

        @NonNull
        @Override
        public BleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            MainBleItemBinding binding = MainBleItemBinding.inflate(getLayoutInflater(), parent, false);
            return new BleHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull BleHolder holder, int position) {
            ScanResult scanResult = mBleList.get(position);
            holder.scanResult = scanResult;

            BluetoothDevice device = scanResult.getDevice();
            String name = device.getName() == null ? getString(R.string.string_unknown) : device.getName();
            holder.binding.text1.setText(name);

            SpannableStringBuilder info = new SpannableStringBuilder();
            info.append("Mac:").append(device.getAddress())
                    .append(" RSSI:").append(String.valueOf(scanResult.getRssi()));
            info.setSpan(new ForegroundColorSpan(0xFF9E9E9E), 0, 21, Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            info.setSpan(new ForegroundColorSpan(0xFF8D6E63), 21, info.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            holder.binding.text2.setText(info);
        }

        @Override
        public int getItemCount() {
            return mBleList.size();
        }
    }
}
