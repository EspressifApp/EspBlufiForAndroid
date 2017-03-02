package com.espressif.espblufi;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afunx.ble.blelitelib.proxy.BleGattClientProxy;
import com.espressif.espblufi.communication.BlufiCommunicator;
import com.espressif.espblufi.communication.response.BlufiStatusResponse;
import com.espressif.espblufi.communication.response.BlufiVersionResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiNegSecActivity extends BlufiAbsActivity {
    private static final UUID UUID_WIFI_SERVICE = UUID.fromString("0000ffff-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_SEND_CHARACTERISTIC = UUID.fromString("0000ff01-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_READ_CHARACTERISTIC = UUID.fromString("0000ff02-0000-1000-8000-00805f9b34fb");

    private static final int REQUEST_CONFIGURE = 0x10;
    private static final int REQUEST_DEAUTHENTICATE = 0x11;

    private BleGattClientProxy mProxy;
    private BlufiCommunicator mCommunicator;

    private View mProgressView;
    private ViewGroup mFuncForm;

    private List<String> mInfoList;
    private InfoAdapter mInfoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_neg_sec_activity);

        mProxy = BlufiBridge.sBleGattClientProxy;

        mProgressView = findViewById(R.id.progress);
        mFuncForm = (ViewGroup) findViewById(R.id.neg_sec_func_form);

        findViewById(R.id.neg_sec_configure).setOnClickListener(v -> {
            BlufiBridge.sCommunicator = mCommunicator;
            startActivityForResult(new Intent(this, BlufiConfigureActivity.class), REQUEST_CONFIGURE);
        });
        findViewById(R.id.neg_sec_deauthenticate).setOnClickListener(v -> {
            BlufiBridge.sCommunicator = mCommunicator;
            startActivityForResult(new Intent(this, BlufiDeauthenticateActivity.class), REQUEST_DEAUTHENTICATE);
        });

        mInfoList = new ArrayList<>();
        RecyclerView infoRV = (RecyclerView) findViewById(R.id.neg_sec_info_rv);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        infoRV.setLayoutManager(llm);
        mInfoAdapter = new InfoAdapter();
        infoRV.setAdapter(mInfoAdapter);

        initCommunicator();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mProxy.close();
        mProxy = null;
        mCommunicator = null;
        BlufiBridge.release();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIGURE) {
            if (resultCode == RESULT_OK) {
                String wifiState = data.getStringExtra(BlufiBridge.KEY_CONFIGURE_DATA);
                notifyAppendInfo(wifiState);
            } else if (resultCode == RESULT_EXCEPTION) {
                notifyAppendInfo("Configure catch Exception");
            }
            return;
        } else if (requestCode == REQUEST_DEAUTHENTICATE) {
            if (resultCode == RESULT_OK) {
                StringBuilder sb = new StringBuilder();
                String[] bssidArray = data.getStringArrayExtra(BlufiBridge.KEY_DEAUTHENTICATE_DATA);
                sb.append("Deauthenticate BSSID:\n");
                for (String bssid : bssidArray) {
                    sb.append(bssid).append('\n');
                }
                notifyAppendInfo(sb.toString());
            } else if (resultCode == RESULT_EXCEPTION) {
                notifyAppendInfo("Deauthenticate catch Exception");
            }
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void showProgress(boolean show) {
        if (show) {
            mProgressView.setVisibility(View.VISIBLE);
            mFuncForm.setVisibility(View.GONE);
        } else {
            mProgressView.setVisibility(View.GONE);
            mFuncForm.setVisibility(View.VISIBLE);
        }
    }

    private void notifyAppendInfo(String info) {
        mInfoList.add(info);
        mInfoAdapter.notifyItemInserted(mInfoList.size() - 1);
    }

    private void setFuncFormEnable(boolean enable) {
        int count = mFuncForm.getChildCount();
        for (int i = 0; i < count; i++) {
            mFuncForm.getChildAt(i).setEnabled(enable);
        }
    }

    private void initCommunicator() {
        showProgress(true);
        final long timeout = 5000L;
        Observable.defer(() -> {
            BluetoothGattService service = mProxy.discoverService(UUID_WIFI_SERVICE, timeout);
            System.out.println(service == null);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mProxy.requestMtu(64, 268);
            }
            return Observable.just(service);
        }).subscribeOn(Schedulers.io())
                .filter(bluetoothGattService -> bluetoothGattService != null)
                .map(bluetoothGattService -> {
                    BluetoothGattCharacteristic[] result = new BluetoothGattCharacteristic[2];
                    result[0] = mProxy.discoverCharacteristic(bluetoothGattService, UUID_SEND_CHARACTERISTIC);
                    result[1] = mProxy.discoverCharacteristic(bluetoothGattService, UUID_READ_CHARACTERISTIC);
                    return result;
                })
                .filter(characteristics -> characteristics.length == 2
                        && characteristics[0] != null
                        && characteristics[1] != null)
                .map(characteristics -> {
                    BluetoothGattCharacteristic sendChara = characteristics[0];
                    BluetoothGattCharacteristic readChara = characteristics[1];
                    return new BlufiCommunicator(mProxy, sendChara, readChara);
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BlufiCommunicator>() {
                    @Override
                    public void onCompleted() {
                        if (mCommunicator != null) {
                            getVersion();
                        } else {
                            showProgress(false);
                            setFuncFormEnable(false);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        finish();
                    }

                    @Override
                    public void onNext(BlufiCommunicator blufiCommunicator) {
                        mCommunicator = blufiCommunicator;
                    }
                });
    }

    private void getVersion() {
        Observable.just(mCommunicator)
                .subscribeOn(Schedulers.io())
                .map(BlufiCommunicator::getVersion)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BlufiVersionResponse>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        finish();
                    }

                    @Override
                    public void onNext(BlufiVersionResponse response) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Check Protocol Version\n");
                        switch (response.getResultCode()) {
                            case BlufiVersionResponse.RESULT_VALID:
                                sb.append("Support device protocol: ")
                                        .append(response.getVersionString());
                                break;
                            case BlufiVersionResponse.RESULT_APP_VERSION_INVALID:
                                sb.append("App version is too low");
                                break;
                            case BlufiVersionResponse.RESULT_DEVICE_VERSION_INVALID:
                                sb.append("Device version is too low");
                                break;
                            case BlufiVersionResponse.RESULT_GET_VERSION_FAILED:
                                sb.append("Get version info failed");
                                break;
                        }
                        notifyAppendInfo(sb.toString());

                        if (response.getResultCode() >= 0) {
                            negSec();
                        } else {
                            showProgress(false);
                            setFuncFormEnable(false);
                        }
                    }
                });
    }

    private void negSec() {
        Observable.just(mCommunicator)
                .subscribeOn(Schedulers.io())
                .map(BlufiCommunicator::negotiateSecurity)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {
                        getStatus();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        finish();
                    }

                    @Override
                    public void onNext(Boolean negSecResult) {
                        notifyAppendInfo("Negotiate security " + (negSecResult ? "complete" : "failed"));

                        setFuncFormEnable(negSecResult);
                    }
                });
    }

    private void getStatus() {
        Observable.just(mCommunicator)
                .subscribeOn(Schedulers.io())
                .map(BlufiCommunicator::getStatus)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BlufiStatusResponse>() {
                    @Override
                    public void onCompleted() {
                        showProgress(false);
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(BlufiStatusResponse response) {
                        String info = "";
                        switch (response.getResultCode()) {
                            case BlufiStatusResponse.RESULT_TIMEOUT:
                                info = "Receive wifi state timeout";
                                break;
                            case BlufiStatusResponse.RESULT_PARSE_FAILED:
                                info = "Receive wifi state parse data error";
                                break;
                            case BlufiStatusResponse.RESULT_SUCCESS:
                                info = response.generateValidInfo();
                                break;
                        }
                        notifyAppendInfo(info);
                    }
                });
    }

    private class InfoHolder extends RecyclerView.ViewHolder {
        TextView text;

        InfoHolder(View itemView) {
            super(itemView);

            text = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }

    public class InfoAdapter extends RecyclerView.Adapter<InfoHolder> {
        LayoutInflater mInflater = getLayoutInflater();

        @Override
        public InfoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            return new InfoHolder(view);
        }

        @Override
        public void onBindViewHolder(InfoHolder holder, int position) {
            String info = mInfoList.get(position);

            holder.text.setText(info);
        }

        @Override
        public int getItemCount() {
            return mInfoList.size();
        }
    }
}
