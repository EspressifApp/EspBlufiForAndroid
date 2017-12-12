package com.espressif.espblufi.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;
import com.espressif.libs.app.PermissionHelper;
import com.espressif.libs.ble.EspBleHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class BlufiListActivity extends BlufiAbsActivity {
    private static final long SCAN_LEAST_TIME = 5000L;
    private static final long SCAN_TIMEOUT = 2500L;

    private static final int REQUEST_PERMISSION = 1;
    private static final int REQUEST_SETTINGS = 0x10;

    private PermissionHelper mPermissionHelper;

    private SwipeRefreshLayout mRefreshLayout;

    private BTAdapter mBTAdapter;
    private List<EspBleDevice> mBleList;

    private View mButtonBar;

    private TextView mCheckCountTV;

    private String mBlufiFilter;
    private HashSet<EspBleDevice> mTempDevices;
    private long mScanPrevTime;
    private long mScanStartTime;

    private volatile boolean mDestroy = false;

    private EspBleHelper.ScanListener mBleListener = new EspBleHelper.ScanListener() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            String deviceName = device.getName();
            if (deviceName != null && deviceName.startsWith(mBlufiFilter)) {
                EspBleDevice newDevice = new EspBleDevice(device);
                newDevice.rssi = rssi;
                if (!mTempDevices.contains(newDevice)) {
                    mTempDevices.add(newDevice);
                    mScanPrevTime = SystemClock.elapsedRealtime();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_connect_activity);

        mCheckCountTV = new TextView(this);
        mCheckCountTV.setTextColor(Color.WHITE);
        mCheckCountTV.setPadding(0, 0, getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setCustomView(mCheckCountTV, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
        }

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(this::scan);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBleList = new LinkedList<>();
        mBTAdapter = new BTAdapter();
        mRecyclerView.setAdapter(mBTAdapter);

        mButtonBar = findViewById(R.id.button_bar);
        mButtonBar.findViewById(R.id.button_bar_batch_configure).setOnClickListener(v -> batchConfigure());
        mButtonBar.findViewById(R.id.button_bar_all).setOnClickListener(v -> selectAll());

        mTempDevices = new HashSet<>();

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

        EspBleHelper.stopScanBle(mBleListener);
        mDestroy = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, 0, 0, R.string.settings_title);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mBTAdapter.mCheckable) {
            clearBleChecked();
            setCheckMode(false);
            return;
        }

        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SETTINGS:
                switch (resultCode) {
                    case RESULT_OK:
                        scan();
                        break;
                }
                break;
        }
    }

    private void selectAll() {
        boolean checkedAll = true;
        for (EspBleDevice d : mBleList) {
            if (!d.checked) {
                checkedAll = false;
                break;
            }
        }

        boolean actionCheck = !checkedAll;
        for (EspBleDevice d : mBleList) {
            d.checked = actionCheck;
        }

        mBTAdapter.notifyDataSetChanged();

        updateSelectDeviceCountInfo();
    }

    private void batchConfigure() {
        ArrayList<BluetoothDevice> bles = new ArrayList<>();
        for (EspBleDevice ble : mBleList) {
            if (ble.checked) {
                bles.add(ble.device);
            }
        }

        if (bles.isEmpty()) {
            Toast.makeText(this, R.string.esp_blufi_list_no_seleted_devices, Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, BlufiSettingsActivity.class);
            String rKey = BlufiApp.getInstance().putCache(bles);
            intent.putExtra(BlufiConstants.KEY_BLE_DEVICES, rKey);
            startActivityForResult(intent, REQUEST_SETTINGS);

            for (BluetoothDevice dev : bles) {
                for (EspBleDevice espDev : mBleList) {
                    if (dev == espDev.device) {
                        mBleList.remove(espDev);
                        break;
                    }
                }
            }
            mBTAdapter.notifyDataSetChanged();
        }
    }

    /**
     * Scan bluetooth devices
     */
    private void scan() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Toast.makeText(this, R.string.esp_blufi_list_bt_disable_msg, Toast.LENGTH_SHORT).show();
            mRefreshLayout.setRefreshing(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check location enable
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean locationGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean locationNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!locationGPS && !locationNetwork) {
                Toast.makeText(this, R.string.esp_blufi_list_location_disable_msg, Toast.LENGTH_SHORT).show();
                mRefreshLayout.setRefreshing(false);
                return;
            }
        }

        mTempDevices.clear();
        mBlufiFilter = (String) BlufiApp.getInstance().settingsGet(SettingsConstants.PREF_SETTINGS_KEY_BLE_PREFIX, BlufiConstants.BLUFI_PREFIX);
        mScanStartTime = mScanPrevTime = SystemClock.elapsedRealtime();
        EspBleHelper.startScanBle(mBleListener);
        Observable.just(new LinkedList<EspBleDevice>())
                .subscribeOn(Schedulers.io())
                .doOnNext(new Action1<LinkedList<EspBleDevice>>() {
                    @Override
                    public void call(LinkedList<EspBleDevice> scanResults) {
                        while (!mDestroy) {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                return;
                            }

                            long currentTime = SystemClock.elapsedRealtime();
                            long cost = currentTime - mScanStartTime;
                            if (cost > SCAN_LEAST_TIME && currentTime - mScanPrevTime > SCAN_TIMEOUT) {
                                // Scan Over
                                EspBleHelper.stopScanBle(mBleListener);
                                for (EspBleDevice newBle : mTempDevices) {
                                    for (EspBleDevice ble : mBleList) {
                                        if (ble.equals(newBle)) {
                                            newBle.checked = ble.checked;
                                            break;
                                        }
                                    }
                                }

                                // Sort by rssi
                                scanResults.addAll(mTempDevices);
                                mTempDevices.clear();
                                Collections.sort(scanResults, new Comparator<EspBleDevice>() {
                                    @Override
                                    public int compare(EspBleDevice o1, EspBleDevice o2) {
                                        if (o1.rssi < o2.rssi) {
                                            return 1;
                                        } else if (o1.rssi == o2.rssi) {
                                            return 0;
                                        } else {
                                            return -1;
                                        }
                                    }
                                });

                                break;
                            } else {
                                // Scan continue
                                if (mBleList.size() != mTempDevices.size()) {
                                    // Refresh scan results every one second
                                    Observable.unsafeCreate(new Observable.OnSubscribe<Object>() {
                                        @Override
                                        public void call(Subscriber<? super Object> subscriber) {
                                            mBleList.clear();
                                            mBleList.addAll(mTempDevices);
                                            mBTAdapter.notifyDataSetChanged();
                                            subscriber.onCompleted();
                                        }
                                    }).subscribeOn(AndroidSchedulers.mainThread())
                                            .subscribe();
                                }
                            }
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<LinkedList<EspBleDevice>>() {
                    @Override
                    public void onCompleted() {
                        System.out.println("Ble scan task over");
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(LinkedList<EspBleDevice> espBleDevices) {
                        if (!mDestroy) {
                            mBleList.clear();
                            mBleList.addAll(espBleDevices);
                            mBTAdapter.notifyDataSetChanged();
                            mRefreshLayout.setRefreshing(false);
                        }
                    }
                });
    }

    private void clearBleChecked() {
        for (EspBleDevice ble : mBleList) {
            ble.checked = false;
        }
    }

    private void setCheckMode(boolean checkMode) {
        mBTAdapter.setCheckable(checkMode);
        mButtonBar.setVisibility(checkMode ? View.VISIBLE : View.GONE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(checkMode);
        }
        updateSelectDeviceCountInfo();
    }

    private void updateSelectDeviceCountInfo() {
        int count = 0;
        for (EspBleDevice d : mBleList) {
            if (d.checked) {
                count++;
            }
        }

        mCheckCountTV.setText(getString(R.string.esp_blufi_list_selected_device_info, count));
    }

    private class EspBleDevice {
        BluetoothDevice device;
        boolean checked = false;
        int rssi = 1;

        EspBleDevice(BluetoothDevice d) {
            device = d;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof EspBleDevice)) {
                return false;
            }

            return device.equals(((EspBleDevice) obj).device);
        }

        @Override
        public int hashCode() {
            return device.hashCode();
        }
    }

    private class BTHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener, View.OnLongClickListener {
        View view;
        TextView text1;
        TextView text2;
        CheckBox checkBox;

        TextView status1;
        ProgressBar progress1;

        EspBleDevice ble;

        BTHolder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            status1 = (TextView) itemView.findViewById(R.id.status1);
            progress1 = (ProgressBar) itemView.findViewById(R.id.progress1);
            checkBox = (CheckBox) itemView.findViewById(R.id.check);
            checkBox.setOnClickListener(this);

            itemView.setOnClickListener(BTHolder.this);
            itemView.setOnLongClickListener(BTHolder.this);
            view = itemView;
        }

        @Override
        public void onClick(View v) {
            if (v == checkBox) {
                ble.checked = checkBox.isChecked();
                updateSelectDeviceCountInfo();
            } else if (v == view) {
                setCheckMode(true);
                ble.checked = !checkBox.isChecked();
                checkBox.setChecked(ble.checked);
                updateSelectDeviceCountInfo();
            }

            updateSelectDeviceCountInfo();
        }

        @Override
        public boolean onLongClick(View v) {
            setCheckMode(true);
            return true;
        }
    }

    private class BTAdapter extends RecyclerView.Adapter<BTHolder> {
        LayoutInflater mInflater = BlufiListActivity.this.getLayoutInflater();

        boolean mCheckable = false;

        @Override
        public BTHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.blufi_device_item, parent, false);
            return new BTHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BTHolder holder, int position) {
            holder.ble = mBleList.get(position);

            String name = holder.ble.device.getName();
            if (TextUtils.isEmpty(name)) {
                name = "Unnamed";
            }
            holder.text1.setText(String.format(Locale.ENGLISH,
                    "%d. %s", position + 1, name));

            holder.text2.setText(String.format(Locale.ENGLISH,
                    "%s  %d", holder.ble.device.getAddress(), holder.ble.rssi));

            holder.checkBox.setVisibility(mCheckable ? View.VISIBLE : View.GONE);
            holder.checkBox.setChecked(holder.ble.checked);

            holder.status1.setBackgroundResource(R.drawable.ic_bluetooth);
        }

        @Override
        public int getItemCount() {
            return mBleList.size();
        }

        void setCheckable(boolean checkable) {
            if (mCheckable != checkable) {
                mCheckable = checkable;
                notifyDataSetChanged();
            }
        }
    }
}
