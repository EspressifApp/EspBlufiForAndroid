package com.espressif.espblufi.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.esp.iot.blufi.communiation.BlufiCommunicator;
import com.esp.iot.blufi.communiation.BlufiConfigureParams;
import com.esp.iot.blufi.communiation.response.BlufiSecurityResult;
import com.esp.iot.blufi.communiation.response.BlufiStatusResponse;
import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;
import com.espressif.libs.ble.EspBleHelper;
import com.espressif.libs.log.EspLog;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiConfigureActivity extends BlufiAbsActivity {
    private final Object mConnectLock = new Object();
    private final EspLog EspLog = new EspLog(getClass());

    private TextView mTextView;

    private int mMultithreadCount;
    private BlufiConfigureParams mParam;
    private ConfigureDevice mRootDevice;

    private Adapter mAdapter;
    private List<ConfigureDevice> mAllDevices;
    private Queue<ConfigureDevice> mDeviceQueue;

    private View mProgressView;

    private volatile boolean mDestroy = false;

    private List<Subscription> mSubs;
    private BlockingQueue<Object> mOverQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.blufi_configure_activity);

        mAllDevices = new LinkedList<>();
        String devicesKey = getIntent().getStringExtra(BlufiConstants.KEY_BLE_DEVICES);
        List deviceList = (List) BlufiApp.getInstance().takeCache(devicesKey);
        for (Object obj : deviceList) {
            mAllDevices.add(new ConfigureDevice((BluetoothDevice) obj));
        }
        mRootDevice = mAllDevices.get(0);

        mSubs = new LinkedList<>();

        mParam = (BlufiConfigureParams) getIntent().getSerializableExtra(BlufiConstants.KEY_CONFIGURE_PARAM);
        if (mParam.getMeshID() == null) {
            String[] mac = mRootDevice.device.getAddress().split(":");
            byte[] meshId = new byte[mac.length];
            for (int i = 0; i < mac.length; i++) {
                meshId[i] = (byte) Integer.parseInt(mac[i], 16);
            }
            mParam.setMeshID(meshId);
            getSharedPreferences(BlufiConstants.PREF_MESH_IDS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(mRootDevice.device.getAddress(), mRootDevice.device.getAddress())
                    .apply();
        }
        mMultithreadCount = getIntent().getIntExtra(BlufiConstants.KEY_CONFIGURE_MULTITHREAD, 1);
        mOverQueue = new ArrayBlockingQueue<>(mMultithreadCount);

        mProgressView = findViewById(R.id.progress);
        mTextView = (TextView) findViewById(R.id.text);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(llm);
        mAdapter = new Adapter();
        recyclerView.setAdapter(mAdapter);

        mDeviceQueue = new LinkedBlockingQueue<>(mAllDevices.size());
        mDeviceQueue.addAll(mAllDevices);

        configure();
    }

    private void showProgress(boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateInfo(final CharSequence info) {
        runOnUiThread(() -> mTextView.setText(info));
    }

    private void notifyAdapter(final int index) {
        int sucCount = 0;
        for (ConfigureDevice cd : mAllDevices) {
            if (cd.success) {
                sucCount++;
            }
        }
        updateInfo("Current success " + sucCount);

        runOnUiThread(() -> {
            mAdapter.notifyItemChanged(index);
        });
    }

    private void configure() {
        showProgress(true);
        final int retryTime = 3;
        final long startTime = SystemClock.elapsedRealtime();
        for (int i = 0; i < mMultithreadCount; i++) {
            Subscription subscription = Observable.just(i)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(integer -> {
                        ConfigureDevice cd;
                        while ((cd = mDeviceQueue.poll()) != null) {
                            int devIndex = mAllDevices.indexOf(cd);
                            cd.running = true;
                            notifyAdapter(devIndex);

                            ConfigureResult cr = executeTask(cd);
                            if (cr == null) {
                                return;
                            }

                            if (mDestroy) {
                                return;
                            }

                            cd.success = cr.success;
                            cd.results.add(cr);
                            cd.tryCount++;
                            cd.running = false;

                            if (!cd.success && cd.tryCount < retryTime) {
                                mDeviceQueue.add(cd);
                            } else {
                                cd.over = true;
                            }
                            notifyAdapter(devIndex);
                        }
                    })
                    .subscribe(new Subscriber<Integer>() {
                        @Override
                        public void onCompleted() {
                            mOverQueue.add(new Object());
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            mOverQueue.add(new Object());
                        }

                        @Override
                        public void onNext(Integer integer) {
                        }
                    });
            mSubs.add(subscription);
        }

        // Wait all task over
        mSubs.add(Observable.just(1)
                .subscribeOn(Schedulers.io())
                .doOnNext(integer -> {
                    for (int i = 0; i < mMultithreadCount; i++) {
                        try {
                            mOverQueue.take();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Integer>() {
                    @Override
                    public void onCompleted() {
                        showProgress(false);

                        long cost = SystemClock.elapsedRealtime() - startTime;
                        int sucCount = 0;
                        for (ConfigureDevice cd : mAllDevices) {
                            if (cd.success) {
                                sucCount++;
                            }
                        }
                        updateInfo(
                                String.format(Locale.ENGLISH,
                                        "Cost %d millisenonds, success %d",
                                        cost, sucCount)
                        );
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        showProgress(false);
                    }

                    @Override
                    public void onNext(Integer integer) {
                    }
                }));
    }

    private ConfigureResult executeTask(ConfigureDevice configureDevice) {
        Task task = new Task(configureDevice);
        ConfigureResult result = null;
        try {
            result = task.run();
            return result;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        } finally {
            task.close();
            EspLog.d("configure task close");
            if (result != null) {
                result.msg.append("-Disconnect BLE\n");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDestroy = true;
        mAllDevices.clear();
        for (Subscription s : mSubs) {
            s.unsubscribe();
        }
        mSubs.clear();
    }

    private class Task {
        ConfigureDevice device;
        EspBleHelper mBleHelper;
        BluetoothGattService service;
        BluetoothGattCharacteristic send;
        BluetoothGattCharacteristic recv;
        BlufiCommunicator communicator;

        Task(ConfigureDevice dev) {
            device = dev;
        }

        void close() {
            if (mBleHelper != null) {
                mBleHelper.close();
            }
        }

        ConfigureResult run() throws InterruptedException {
            EspLog.d("BLE task start");
            ConfigureResult result = new ConfigureResult();
            result.msg.append("Start configure.\n");

            mBleHelper = new EspBleHelper(getApplicationContext());
            boolean connect;
            synchronized (mConnectLock) {
                connect = mBleHelper.connectGatt(device.device);
            }
            if (!connect) {
                result.msg.append("-Connect failed.\n");
                result.success = false;
                return result;
            }
            EspLog.d("BLE task connect suc");
            result.msg.append("-Connect success.\n");

            service = mBleHelper.discoverService(BlufiConstants.UUID_WIFI_SERVICE);
            if (service == null) {
                result.msg.append("-Discover gatt service failed.\n");
                result.success = false;
                return result;
            }
            EspLog.d("BLE task service suc");
            result.msg.append("-Discover gattservice success.\n");

            send = service.getCharacteristic(BlufiConstants.UUID_WRITE_CHARACTERISTIC);
            if (send == null) {
                result.msg.append("-Discover write characteristic failed.\n");
                result.success = false;
                return result;
            }
            result.msg.append("-Discover write characteristic success.\n");
            recv = service.getCharacteristic(BlufiConstants.UUID_NOTIFICATION_CHARACTERISTIC);
            if (recv == null) {
                result.msg.append("-Discover notification characteristic failed.\n");
                result.success = false;
                return result;
            }
            result.msg.append("-Discover notification characteristic success.\n");

            SharedPreferences shared = getSharedPreferences(SettingsConstants.PREF_SETTINGS_NAME, MODE_PRIVATE);
            int mtuLen = shared.getInt(SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH, BlufiConstants.DEFAULT_MTU_LENGTH);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                boolean mtu = mBleHelper.requestMtu(mtuLen);
                boolean pri = mBleHelper.requestConnectionPriority(EspBleHelper.CONNECTION_PRIORITY_HIGH);

                result.msg.append("-Request mtu ").append(mtuLen).append(" ").append(mtu).append('\n');
                result.msg.append("-Request connection priority high ").append(pri).append('\n');
            }
            EspLog.d("BLE task mtu suc");

            communicator = new BlufiCommunicator(mBleHelper, send, recv);
            communicator.setPostPackageLengthLimit(mtuLen - BlufiConstants.POST_DATA_LENGTH_LESS);

            BlufiSecurityResult negsec = communicator.negotiateSecurity();
            EspLog.d("BLE task neg suc");
            switch (negsec) {
                case SUCCESS:
                    result.msg.append("-Negotiate security success\n");
                    break;
                case POST_PGK_FAILED:
                    result.msg.append("-Negotiate post p,g,public key failed\n");
                    result.success = false;
                    return result;
                case RECV_PV_FAILED:
                    result.msg.append("-Negotiate receive device public key failed\n");
                    result.success = false;
                    return result;
                case POST_SET_MODE_FAILED:
                    result.msg.append("-Negotiate post set mode failed\n");
                    result.success = false;
                    return result;
                case CHECK_FAILED:
                    result.msg.append("-Negotiate check failed\n");
                    result.success = false;
                    return result;
            }

            mParam.setMeshRoot(mRootDevice == device);
            mParam.setConfigureSequence(mAllDevices.indexOf(device));
            BlufiStatusResponse confResp = communicator.configure(mParam);
            EspLog.d("BLE task config suc");

            switch (confResp.getResultCode()) {
                case BlufiStatusResponse.RESULT_SUCCESS:
                    result.msg.append("-Configure success.\n")
                            .append("\nCurrent status:\n")
                            .append(confResp.generateValidInfo())
                            .append('\n');
                    result.success = true;
                    return result;
                case BlufiStatusResponse.RESULT_TIMEOUT:
                    result.msg.append("-Receive wifi state timeout\n");
                    result.success = false;
                    return result;
                case BlufiStatusResponse.RESULT_PARSE_FAILED:
                    result.msg.append("-Receive wifi sstate parse data error\n");
                    result.success = false;
                    return result;
                case BlufiStatusResponse.RESULT_POST_FAILED:
                    result.msg.append("-Post wifi info failed\n");
                    result.success = false;
                    return result;
            }

            return result;
        }
    }

    private class ConfigureDevice {
        BluetoothDevice device;
        boolean success = false;
        boolean running = false;
        boolean over = false;
        int tryCount = 0;
        LinkedList<ConfigureResult> results = new LinkedList<>();

        ConfigureDevice(BluetoothDevice bd) {
            device = bd;
        }
    }

    private class ConfigureResult {
        boolean success;
        StringBuilder msg = new StringBuilder();
    }

    private class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ConfigureDevice configureDevice;

        TextView text1;
        TextView text2;
        View progress;

        Holder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(R.id.text1);
            text2 = (TextView) itemView.findViewById(R.id.text2);
            progress = itemView.findViewById(R.id.progress);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (configureDevice.results.isEmpty()) {
                return;
            }

            StringBuilder msg = new StringBuilder();
            for (ConfigureResult cr : configureDevice.results) {
                msg.append(cr.msg).append("\n===============\n");
            }
            new AlertDialog.Builder(BlufiConfigureActivity.this)
                    .setMessage(msg)
                    .show()
                    .setCanceledOnTouchOutside(true);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.blufi_configure_item, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            ConfigureDevice cd = mAllDevices.get(position);
            holder.configureDevice = cd;

            holder.text1.setText(cd.device.getName());
            holder.text2.setText("");
            if (cd.running) {
                holder.progress.setVisibility(View.VISIBLE);
                holder.text2.append("Configuring...");
            } else {
                holder.progress.setVisibility(View.INVISIBLE);
                if (cd.success) {
                    holder.text2.append("Complete");
                } else {
                    if (cd.over) {
                        holder.text2.append("Failed, click to show information");
                    } else {
                        holder.text2.append("Waiting...");
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
            return mAllDevices.size();
        }
    }
}
