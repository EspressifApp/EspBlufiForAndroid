package com.espressif.espblufi.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import libs.espressif.app.PermissionHelper;
import libs.espressif.ble.EspBleUtils;
import libs.espressif.ble.ScanListener;
import libs.espressif.log.EspLog;

public class MainActivity extends AppCompatActivity {
    private static final long TIMEOUT_SCAN = 4000L;

    private static final int REQUEST_PERMISSION = 0x01;
    private static final int REQUEST_BLUFI = 0x10;

    private static final int MENU_SETTINGS = 0x01;

    private final EspLog mLog = new EspLog(getClass());

    private PermissionHelper mPermissionHelper;

    private SwipeRefreshLayout mRefreshLayout;

    private RecyclerView mRecyclerView;
    private List<BluetoothDevice> mBleList;
    private BleAdapter mBleAdapter;

    private Map<BluetoothDevice, Integer> mDeviceRssiMap;
    private ScanCallback mScanCallback;
    private String mBlufiFilter;
    private volatile long mScanStartTime;

    private ExecutorService mThreadPool;
    private Future mUpdateFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mThreadPool = Executors.newSingleThreadExecutor();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        fab.setVisibility(View.GONE);

        mRefreshLayout = findViewById(R.id.refresh_layout);
        mRefreshLayout.setOnRefreshListener(this::scan);

        mRecyclerView = findViewById(R.id.recycler_view);
        mBleList = new LinkedList<>();
        mBleAdapter = new BleAdapter();
        mRecyclerView.setAdapter(mBleAdapter);

        mDeviceRssiMap = new HashMap<>();
        mScanCallback = new ScanCallback();

        mPermissionHelper = new PermissionHelper(this, REQUEST_PERMISSION);
        mPermissionHelper.setOnPermissionsListener((permission, permited) -> {
            if (permited && permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                mRefreshLayout.setRefreshing(true);
                scan();
            }
        });
        mPermissionHelper.requestAuthorities(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION});
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
        mThreadPool.shutdownNow();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_BLUFI:
                mRefreshLayout.setRefreshing(true);
                scan();
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_SETTINGS, 0, R.string.main_menu_settings);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scan() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Toast.makeText(this, R.string.main_bt_disable_msg, Toast.LENGTH_SHORT).show();
            mRefreshLayout.setRefreshing(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check location enable
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (locationManager != null) {
                boolean locationGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                boolean locationNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
                if (!locationGPS && !locationNetwork) {
                    Toast.makeText(this, R.string.main_location_disable_msg, Toast.LENGTH_SHORT).show();
                    mRefreshLayout.setRefreshing(false);
                    return;
                }
            }

        }

        mDeviceRssiMap.clear();
        mBleList.clear();
        mBleAdapter.notifyDataSetChanged();
        mBlufiFilter = (String) BlufiApp.getInstance().settingsGet(SettingsConstants.PREF_SETTINGS_KEY_BLE_PREFIX,
                BlufiConstants.BLUFI_PREFIX);
        mScanStartTime = SystemClock.elapsedRealtime();

        mLog.d("Start scan ble");
        EspBleUtils.startScanBle(mScanCallback);
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

            EspBleUtils.stopScanBle(mScanCallback);
            onIntervalScanUpdate(true);
            mLog.d("Scan ble thread is interrupted");
        });
    }

    private void stopScan() {
        EspBleUtils.stopScanBle(mScanCallback);
        if (mUpdateFuture != null) {
            mUpdateFuture.cancel(true);
        }
        mLog.d("Stop scan ble");
    }

    private void onIntervalScanUpdate(boolean over) {
        List<BluetoothDevice> devices = new LinkedList<>(mDeviceRssiMap.keySet());
        Collections.sort(devices, (dev1, dev2) -> {
            Integer rssi1 = mDeviceRssiMap.get(dev1);
            Integer rssi2 = mDeviceRssiMap.get(dev2);
            return rssi2.compareTo(rssi1);
        });
        runOnUiThread(() -> {
            mBleList.clear();
            mBleList.addAll(devices);
            mBleAdapter.notifyDataSetChanged();

            if (over) {
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void gotoDevice(BluetoothDevice device) {
        Intent intent = new Intent(MainActivity.this, BlufiActivity.class);
        intent.putExtra(BlufiConstants.KEY_BLE_DEVICE, device);
        startActivityForResult(intent, REQUEST_BLUFI);

        mDeviceRssiMap.clear();
        mBleList.clear();
        mBleAdapter.notifyDataSetChanged();
    }

    private class BleHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        BluetoothDevice device;
        TextView text1;
        TextView text2;

        BleHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            stopScan();
            gotoDevice(device);
        }
    }

    private class ScanCallback implements ScanListener {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String name = device.getName();
            if (!TextUtils.isEmpty(mBlufiFilter)) {
                if (name == null || !name.startsWith(mBlufiFilter)) {
                    return;
                }
            }

            mDeviceRssiMap.put(device, rssi);
        }
    }

    private class BleAdapter extends RecyclerView.Adapter<BleHolder> {

        @NonNull
        @Override
        public BleHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new BleHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BleHolder holder, int position) {
            BluetoothDevice device = mBleList.get(position);
            holder.device = device;

            String name =  device.getName() == null ? "Unknow" : device.getName();
            holder.text1.setText(name);
            String info = String.format(Locale.ENGLISH, "%s %d", device.getAddress(), mDeviceRssiMap.get(device));
            holder.text2.setText(info);
        }

        @Override
        public int getItemCount() {
            return mBleList.size();
        }
    }
}
