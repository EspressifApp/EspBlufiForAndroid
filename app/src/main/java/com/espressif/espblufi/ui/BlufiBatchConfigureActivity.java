package com.espressif.espblufi.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.espressif.blufi.ble.proxy.BleGattClientProxy;
import com.espressif.blufi.ble.proxy.BleGattClientProxyImpl;
import com.espressif.blufi.communiation.BlufiCommunicator;
import com.espressif.blufi.communiation.BlufiConfigureParams;
import com.espressif.blufi.communiation.response.BlufiStatusResponse;
import com.espressif.blufi.communiation.response.BlufiVersionResponse;
import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiBatchConfigureActivity extends BlufiAbsActivity {
    private List<BluetoothDevice> mDevices;

    private View mProgressView;
    private List<String> mMsgList;
    private MsgAdapter mMsgAdapter;

    private BlufiConfigureParams mParam;
    private BluetoothDevice mRootDevice;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.blufi_batch_configure_activity);

        String devicesKey = getIntent().getStringExtra(BlufiConstants.KEY_BLE_DEVICES);
        List deviceList = (List) BlufiApp.getInstance().takeCache(devicesKey);
        mDevices = new ArrayList<>();
        for (Object obj : deviceList) {
            mDevices.add((BluetoothDevice) obj);
        }
        mRootDevice = mDevices.get(0);

        mParam = (BlufiConfigureParams) getIntent().getSerializableExtra(BlufiConstants.KEY_CONFIGURE_PARAM);
        if (mParam.getMeshID() == null) {
            String[] mac = mRootDevice.getAddress().split(":");
            byte[] meshId = new byte[mac.length];
            for (int i = 0; i < mac.length; i++) {
                meshId[i] = (byte) Integer.parseInt(mac[i], 16);
            }
            mParam.setMeshID(meshId);
            getSharedPreferences(BlufiConstants.PREF_MESH_IDS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(mRootDevice.getAddress(), mRootDevice.getAddress())
                    .apply();
        }

        mProgressView = findViewById(R.id.progress);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(llm);
        mMsgList = new ArrayList<>();
        mMsgAdapter = new MsgAdapter();
        mRecyclerView.setAdapter(mMsgAdapter);

        configure();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void showProgress(boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void appendMsg(String msg) {
        runOnUiThread(() -> {
            mMsgList.add(msg);
            mMsgAdapter.notifyItemInserted(mMsgList.size() - 1);
        });
    }

    private void configure() {
        final long startTime = SystemClock.elapsedRealtime();
        showProgress(true);
        final long timeout = 5000L;
        Observable.from(mDevices)
                .subscribeOn(Schedulers.io())
                .map(device -> {
                    String result = null;
                    for (int i = 0; i < 3; i++) {
                        BleGattClientProxy proxy = new BleGattClientProxyImpl(getApplicationContext());
                        ConfigureResult cr = configure(proxy, device, timeout);
                        proxy.close();

                        if (cr == null) {
                            continue;
                        }
                        result = cr.msg;
                        if (cr.success || !cr.retry) {
                            break;
                        } else {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    return result;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                        showProgress(false);
                        appendMsg(String.format(Locale.ENGLISH,
                                "Configure cost %d milliseconds",
                                SystemClock.elapsedRealtime() - startTime));
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(String s) {
                        appendMsg(s);
                    }
                });
    }

    private ConfigureResult configure(BleGattClientProxy proxy, BluetoothDevice device, long timeout) {
        ConfigureResult result = new ConfigureResult();
        boolean connect = proxy.connect(device, timeout);
        if (!connect) {
            result.msg = String.format("%s connect failed", device.getName());
            result.success = false;
            return result;
        }

        BluetoothGattService service = proxy.discoverService(BlufiConstants.UUID_WIFI_SERVICE, timeout);
        if (service == null) {
            result.msg = String.format("%s discover gatt service failed", device.getName());
            result.success = false;
            return result;
        }

        BluetoothGattCharacteristic writeBGC = proxy.discoverCharacteristic(service, BlufiConstants.UUID_SEND_CHARACTERISTIC);
        if (writeBGC == null) {
            result.msg = String.format("%s discover write characteristic failed", device.getName());
            result.success = false;
            return result;
        }

        BluetoothGattCharacteristic notifyBGC = proxy.discoverCharacteristic(service, BlufiConstants.UUID_READ_CHARACTERISTIC);
        if (notifyBGC == null) {
            result.msg = String.format("%s discover notification characteristic failed", device.getName());
            result.success = false;
            return result;
        }

        BlufiCommunicator communicator = new BlufiCommunicator(proxy, writeBGC, notifyBGC);
        SharedPreferences shared = getSharedPreferences(BlufiConstants.PREF_SETTINGS_NAME, MODE_PRIVATE);
        int mtuLen = shared.getInt(BlufiConstants.PREF_SETTINGS_KEY_MTU_LENGTH, BlufiConstants.MIN_MTU_LENGTH);
        communicator.requestMtu(mtuLen);
        if (mtuLen >= BlufiConstants.MIN_MTU_LENGTH) {
            communicator.setPostPackageLengthLimit(mtuLen - BlufiConstants.POST_DATA_LENGTH_LESS);
        }
        BlufiVersionResponse verResp = communicator.getVersion();
        switch (verResp.getResultCode()) {
            case BlufiVersionResponse.RESULT_VALID:
                break;
            case BlufiVersionResponse.RESULT_APP_VERSION_INVALID:
                result.msg = String.format("%s, App version is too low", device.getName());
                result.success = false;
                result.retry = false;
                return result;
            case BlufiVersionResponse.RESULT_DEVICE_VERSION_INVALID:
                result.msg = String.format("%s, Device version is too low", device.getName());
                result.success = false;
                result.retry = false;
                return result;
            case BlufiVersionResponse.RESULT_GET_VERSION_FAILED:
                result.msg = String.format("Get %s version info failed", device.getName());
                result.success = false;
                return result;
        }

        boolean negsec = communicator.negotiateSecurity();
        if (!negsec) {
            result.msg = String.format("%s negotiate security failed", device.getName());
            result.success = false;
            return result;
        }

        communicator.setRequireAck(true);
        mParam.setMeshRoot(mRootDevice == device);
        mParam.setConfigureSequence(mDevices.indexOf(device));
        BlufiStatusResponse confResp = communicator.configure(mParam, false);
        switch (confResp.getResultCode()) {
            case BlufiStatusResponse.RESULT_SUCCESS:
                result.msg = String.format("Configure %s completed", device.getName());
                result.success = true;
                return result;
            case BlufiStatusResponse.RESULT_TIMEOUT:
                result.msg = String.format("Receive %s wifi state timeout", device.getName());
                result.success = false;
                return result;
            case BlufiStatusResponse.RESULT_PARSE_FAILED:
                result.msg = String.format("Receive %s wifi sstate parse data error", device.getName());
                result.success = false;
                return result;
            case BlufiStatusResponse.RESULT_POST_FAILED:
                result.msg = String.format("Post %s wifi info failed", device.getName());
                result.success = false;
                return result;
        }

        return null;
    }

    private class ConfigureResult {
        boolean success;
        String msg;
        boolean retry = true;
    }

    private class MsgHolder extends RecyclerView.ViewHolder {
        TextView text1;

        MsgHolder(View itemView) {
            super(itemView);

            text1 = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder> {

        @Override
        public MsgHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MsgHolder(view);
        }

        @Override
        public void onBindViewHolder(MsgHolder holder, int position) {
            String msg = mMsgList.get(position);
            holder.text1.setText(msg);
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }
    }
}
