package com.espressif.espblufi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.afunx.ble.blelitelib.proxy.BleGattClientProxy;
import com.afunx.ble.blelitelib.proxy.BleGattClientProxyImpl;
import com.afunx.ble.blelitelib.scanner.BleScanner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiConnectActivity extends BlufiAbsActivity {
    private static final int TIMEOUT_SCAN = 5;
    private static final int TIMEOUT_CONNECT = 5000;

    private static final int MENU_ID_SETTINGS = 0x10;

    private PermisiionHelper mPermisiionHelper;

    private BleGattClientProxy mProxy;

    private SwipeRefreshLayout mRefreshLayout;

    private BTAdapter mBTAdapter;
    private List<BluetoothDevice> mBTList;

    private List<BluetoothDevice> mTempDevices;

    private Looper mBackgroundLooper;

    private BluetoothAdapter.LeScanCallback mBTCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            Observable.from(mTempDevices).subscribeOn(AndroidSchedulers.from(mBackgroundLooper))
                    .exists(bluetoothDevice -> bluetoothDevice.equals(device))
                    .filter(exist -> {
                        if (!exist) {
                            mTempDevices.add(device);
                        }
                        return !exist;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<Boolean>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {

                        }

                        @Override
                        public void onNext(Boolean aBoolean) {
                            if (!mBTList.contains(device)) {
                                mBTList.add(device);
                                mBTAdapter.notifyItemInserted(mBTList.size() - 1);
                            }
                        }
                    });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_connect_activity);

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresh_layout);
        mRefreshLayout.setColorSchemeResources(R.color.colorAccent);
        mRefreshLayout.setOnRefreshListener(this::scan);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mBTList = new ArrayList<>();
        mBTAdapter = new BTAdapter();
        mRecyclerView.setAdapter(mBTAdapter);

        mProxy = new BleGattClientProxyImpl(this);

        BackgroundThread backgroundThread = new BackgroundThread();
        backgroundThread.start();
        mBackgroundLooper = backgroundThread.getLooper();

        mTempDevices = new ArrayList<>();
        mRefreshLayout.setRefreshing(true);
        scan();

        new Handler().post(() -> {
            mPermisiionHelper = new PermisiionHelper(BlufiConnectActivity.this);
            mPermisiionHelper.requestAuthorities();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BleScanner.stopLeScan(mBTCallback);
        mProxy.close();
        mProxy = null;
        BlufiBridge.release();
        mBackgroundLooper.quit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPermisiionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ID_SETTINGS, 0, R.string.connect_menu_settings);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ID_SETTINGS:
                startActivity(new Intent(this, BlufiSettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Scan bluetooth devices
     */
    private void scan() {
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Toast.makeText(this, R.string.bt_disable_msg, Toast.LENGTH_SHORT).show();
            mRefreshLayout.setRefreshing(false);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check location enable
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean locationGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean locationNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!locationGPS && !locationNetwork) {
                Toast.makeText(this, R.string.location_disable_msg, Toast.LENGTH_SHORT).show();
                mRefreshLayout.setRefreshing(false);
                return;
            }
        }

        BleScanner.startLeScan(mBTCallback);
        Observable.timer(TIMEOUT_SCAN, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Long>() {
                    @Override
                    public void onCompleted() {
                        BleScanner.stopLeScan(mBTCallback);
                        removeGarbageDevices();
                        mRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(Long aLong) {
                    }
                });
    }

    private void removeGarbageDevices() {
        for (int i = mBTList.size() - 1; i >= 0; i--) {
            BluetoothDevice device = mBTList.get(i);
            for (int j = i - 1; j >= 0; j--) {
                BluetoothDevice compareDevice = mBTList.get(j);
                if (device.equals(compareDevice)) {
                    mBTList.remove(i);
                    mBTAdapter.notifyItemRemoved(i);
                    break;
                }
            }
        }

        final List<BluetoothDevice> removeDevices = new LinkedList<>();
        Observable.range(0, mBTList.size())
                .subscribeOn(AndroidSchedulers.from(mBackgroundLooper))
                .map(index -> mBTList.get(index))
                .doOnNext(bluetoothDevice -> {
                    if (!mTempDevices.contains(bluetoothDevice)) {
                        removeDevices.add(bluetoothDevice);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BluetoothDevice>() {
                    @Override
                    public void onCompleted() {
                        for (BluetoothDevice removeDevice : removeDevices) {
                            int position = mBTList.indexOf(removeDevice);
                            mBTList.remove(position);
                            mBTAdapter.notifyItemRemoved(position);
                        }
                        mTempDevices.clear();
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(BluetoothDevice bluetoothDevice) {
                    }
                });
    }

    private void connect(final BluetoothDevice device) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(String.format(Locale.ENGLISH, "Connecting %s", device.getName()));
        dialog.setCancelable(false);
        dialog.show();

        Observable.just(device.getAddress()).subscribeOn(Schedulers.io())
                .map(addr -> {
                    mProxy.close();
                    return mProxy.connect(addr, TIMEOUT_CONNECT);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(connectResult -> {
                    dialog.dismiss();
                    Activity activity = BlufiConnectActivity.this;
                    if (!connectResult) {
                        Toast.makeText(activity, "Connect failed", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(activity, "Connect completed", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(activity, BlufiNegSecActivity.class);
                        BlufiBridge.sBleGattClientProxy = mProxy;
                        activity.startActivity(intent);
                    }
                });
    }

    static class BackgroundThread extends HandlerThread {
        BackgroundThread() {
            super("Bluetooth-Update-BackgroundThread", android.os.Process.THREAD_PRIORITY_BACKGROUND);
        }
    }

    private class BTHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView text1;
        private TextView text2;

        private BluetoothDevice device;

        BTHolder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);

            itemView.setOnClickListener(BTHolder.this);
        }

        @Override
        public void onClick(View v) {
            connect(device);
        }
    }

    private class BTAdapter extends RecyclerView.Adapter<BTHolder> {
        private LayoutInflater mInflater = BlufiConnectActivity.this.getLayoutInflater();

        @Override
        public BTHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.bluetooth_device_item, parent, false);
            return new BTHolder(itemView);
        }

        @Override
        public void onBindViewHolder(BTHolder holder, int position) {
            holder.device = mBTList.get(position);

            String name = holder.device.getName();
            if (TextUtils.isEmpty(name)) {
                name = "Unnamed";
            }
            holder.text1.setText(name);

            holder.text2.setText(holder.device.getAddress());
        }

        @Override
        public int getItemCount() {
            return mBTList.size();
        }
    }
}
