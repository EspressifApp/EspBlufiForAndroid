package com.espressif.espblufi.ui;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BaseActivity;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.app.BlufiLog;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import blufi.espressif.BlufiCallback;
import blufi.espressif.BlufiClient;
import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.response.BlufiScanResult;
import blufi.espressif.response.BlufiStatusResponse;
import blufi.espressif.response.BlufiVersionResponse;

public class BlufiActivity extends BaseActivity {
    private static final int REQUEST_CONFIGURE = 0x20;

    private final BlufiLog mLog = new BlufiLog(getClass());

    private BluetoothDevice mDevice;
    private BlufiClient mBlufiClient;
    private volatile boolean mConnected;

    private RecyclerView mMsgRecyclerView;
    private List<Message> mMsgList;
    private MsgAdapter mMsgAdapter;

    private Button mBlufiConnectBtn;
    private Button mBlufiDisconnectBtn;
    private Button mBlufiSecurityBtn;
    private Button mBlufiVersionBtn;
    private Button mBlufiConfigureBtn;
    private Button mBlufiDeviceStatusBtn;
    private Button mBlufiDeviceScanBtn;
    private Button mBlufiCustomBtn;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        mDevice = getIntent().getParcelableExtra(BlufiConstants.KEY_BLE_DEVICE);
        assert mDevice != null;
        String deviceName = mDevice.getName() == null ? getString(R.string.string_unknown) : mDevice.getName();
        setTitle(deviceName);

        mMsgRecyclerView = findViewById(R.id.recycler_view);
        mMsgList = new LinkedList<>();
        mMsgAdapter = new MsgAdapter();
        mMsgRecyclerView.setAdapter(mMsgAdapter);

        BlufiButtonListener clickListener = new BlufiButtonListener();

        mBlufiConnectBtn = findViewById(R.id.blufi_connect);
        mBlufiConnectBtn.setOnClickListener(clickListener);
        mBlufiConnectBtn.setOnLongClickListener(clickListener);

        mBlufiDisconnectBtn = findViewById(R.id.blufi_disconnect);
        mBlufiDisconnectBtn.setOnClickListener(clickListener);
        mBlufiDisconnectBtn.setOnLongClickListener(clickListener);
        mBlufiDisconnectBtn.setEnabled(false);

        mBlufiSecurityBtn = findViewById(R.id.blufi_security);
        mBlufiSecurityBtn.setOnClickListener(clickListener);
        mBlufiSecurityBtn.setOnLongClickListener(clickListener);
        mBlufiSecurityBtn.setEnabled(false);

        mBlufiVersionBtn = findViewById(R.id.blufi_version);
        mBlufiVersionBtn.setOnClickListener(clickListener);
        mBlufiVersionBtn.setOnLongClickListener(clickListener);
        mBlufiVersionBtn.setEnabled(false);

        mBlufiConfigureBtn = findViewById(R.id.blufi_configure);
        mBlufiConfigureBtn.setOnClickListener(clickListener);
        mBlufiConfigureBtn.setOnLongClickListener(clickListener);
        mBlufiConfigureBtn.setEnabled(false);

        mBlufiDeviceScanBtn = findViewById(R.id.blufi_device_scan);
        mBlufiDeviceScanBtn.setOnClickListener(clickListener);
        mBlufiDeviceScanBtn.setOnLongClickListener(clickListener);
        mBlufiDeviceScanBtn.setEnabled(false);

        mBlufiDeviceStatusBtn = findViewById(R.id.blufi_device_status);
        mBlufiDeviceStatusBtn.setOnClickListener(clickListener);
        mBlufiDeviceStatusBtn.setOnLongClickListener(clickListener);
        mBlufiDeviceStatusBtn.setEnabled(false);

        mBlufiCustomBtn = findViewById(R.id.blufi_custom);
        mBlufiCustomBtn.setOnClickListener(clickListener);
        mBlufiCustomBtn.setOnLongClickListener(clickListener);
        mBlufiCustomBtn.setEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBlufiClient != null) {
            mBlufiClient.close();
            mBlufiClient = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIGURE) {
            if (!mConnected) {
                return;
            }
            if (resultCode == RESULT_OK) {
                BlufiConfigureParams params =
                        (BlufiConfigureParams) data.getSerializableExtra(BlufiConstants.KEY_CONFIGURE_PARAM);
                configure(params);
            }

            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateMessage(String message, boolean isNotificaiton) {
        runOnUiThread(() -> {
            Message msg = new Message();
            msg.text = message;
            msg.isNotification = isNotificaiton;
            mMsgList.add(msg);
            mMsgAdapter.notifyItemInserted(mMsgList.size() - 1);
            mMsgRecyclerView.scrollToPosition(mMsgList.size() - 1);
        });
    }

    /**
     * Try to connect device
     */
    private void connect() {
        mBlufiConnectBtn.setEnabled(false);

        if (mBlufiClient != null) {
            mBlufiClient.close();
            mBlufiClient = null;
        }

        mBlufiClient = new BlufiClient(getApplicationContext(), mDevice);
        mBlufiClient.setGattCallback(new GattCallback());
        mBlufiClient.setBlufiCallback(new BlufiCallbackMain());
        mBlufiClient.connect();
    }

    /**
     * Request device disconnect the connection.
     */
    private void disconnectGatt() {
        mBlufiDisconnectBtn.setEnabled(false);

        if (mBlufiClient != null) {
            mBlufiClient.requestCloseConnection();
        }
    }

    /**
     * If negotiate security success, the continue communication data will be encrypted.
     */
    private void negotiateSecurity() {
        mBlufiSecurityBtn.setEnabled(false);

        mBlufiClient.negotiateSecurity();
    }

    /**
     * Go to configure options
     */
    private void configureOptions() {
        Intent intent = new Intent(BlufiActivity.this, ConfigureOptionsActivity.class);
        startActivityForResult(intent, REQUEST_CONFIGURE);
    }

    /**
     * Request to configure station or softap
     *
     * @param params configure params
     */
    private void configure(BlufiConfigureParams params) {
        mBlufiConfigureBtn.setEnabled(false);

        mBlufiClient.configure(params);
    }

    /**
     * Request to get device current status
     */
    private void requestDeviceStatus() {
        mBlufiDeviceStatusBtn.setEnabled(false);

        mBlufiClient.requestDeviceStatus();
    }

    /**
     * Request to get device blufi version
     */
    private void requestDeviceVersion() {
        mBlufiVersionBtn.setEnabled(false);

        mBlufiClient.requestDeviceVersion();
    }

    /**
     * Request to get AP list that the device scanned
     */
    private void requestDeviceWifiScan() {
        mBlufiDeviceScanBtn.setEnabled(false);

        mBlufiClient.requestDeviceWifiScan();
    }

    /**
     * Try to post custom data
     */
    private void postCustomData() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.blufi_custom_dialog_title)
                .setView(R.layout.blufi_custom_data_dialog)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    EditText editText = ((AlertDialog) dialog).findViewById(R.id.edit_text);
                    assert editText != null;
                    String dataStr = editText.getText().toString();
                    if (!TextUtils.isEmpty(dataStr)) {
                        mBlufiClient.postCustomData(dataStr.getBytes());
                    }
                })
                .show();
    }

    private void onGattConnected() {
        mConnected = true;
        runOnUiThread(() -> {
            mBlufiConnectBtn.setEnabled(false);

            mBlufiDisconnectBtn.setEnabled(true);
        });
    }

    private void onGattServiceCharacteristicDiscovered() {
        runOnUiThread(() -> {
            mBlufiSecurityBtn.setEnabled(true);
            mBlufiVersionBtn.setEnabled(true);
            mBlufiConfigureBtn.setEnabled(true);
            mBlufiDeviceStatusBtn.setEnabled(true);
            mBlufiDeviceScanBtn.setEnabled(true);
            mBlufiCustomBtn.setEnabled(true);
        });
    }

    private void onGattDisconnected() {
        mConnected = false;
        runOnUiThread(() -> {
            mBlufiConnectBtn.setEnabled(true);

            mBlufiDisconnectBtn.setEnabled(false);
            mBlufiSecurityBtn.setEnabled(false);
            mBlufiVersionBtn.setEnabled(false);
            mBlufiConfigureBtn.setEnabled(false);
            mBlufiDeviceStatusBtn.setEnabled(false);
            mBlufiDeviceScanBtn.setEnabled(false);
            mBlufiCustomBtn.setEnabled(false);
        });
    }

    /**
     * mBlufiClient call onCharacteristicWrite and onCharacteristicChanged is required
     */
    private class GattCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String devAddr = gatt.getDevice().getAddress();
            mLog.d(String.format(Locale.ENGLISH, "onConnectionStateChange addr=%s, status=%d, newState=%d",
                    devAddr, status, newState));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        onGattConnected();
                        updateMessage(String.format("Connected %s", devAddr), false);
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        gatt.close();
                        onGattDisconnected();
                        updateMessage(String.format("Disconnected %s", devAddr), false);
                        break;
                }
            } else {
                gatt.close();
                onGattDisconnected();
                updateMessage(String.format(Locale.ENGLISH, "Disconnect %s, status=%d", devAddr, status),
                        false);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            mLog.d(String.format(Locale.ENGLISH, "onMtuChanged status=%d, mtu=%d", status, mtu));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                updateMessage(String.format(Locale.ENGLISH, "Set mtu complete, mtu=%d ", mtu), false);

                if (mtu > 0 && mBlufiClient != null) {
                    int blufiPkgLenLimit = mtu - 5;
                    mLog.d("BluFiClient setPostPackageLengthLimit " + blufiPkgLenLimit);
                    mBlufiClient.setPostPackageLengthLimit(blufiPkgLenLimit);
                }
            } else {
                updateMessage(String.format(Locale.ENGLISH, "Set mtu failed, mtu=%d, status=%d", mtu, status), false);
            }

            onGattServiceCharacteristicDiscovered();
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mLog.d(String.format(Locale.ENGLISH, "onServicesDiscovered status=%d", status));
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
                updateMessage(String.format(Locale.ENGLISH, "Discover services error status %d", status), false);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
                updateMessage(String.format(Locale.ENGLISH, "WriteChar error status %d", status), false);
            }
        }
    }

    private class BlufiCallbackMain extends BlufiCallback {
        @Override
        public void onGattPrepared(BlufiClient client, BluetoothGatt gatt, BluetoothGattService service,
                                   BluetoothGattCharacteristic writeChar, BluetoothGattCharacteristic notifyChar) {
            if (service == null) {
                mLog.w("Discover service failed");
                gatt.disconnect();
                updateMessage("Discover service failed", false);
                return;
            }
            if (writeChar == null) {
                mLog.w("Get write characteristic failed");
                gatt.disconnect();
                updateMessage("Get write characteristic failed", false);
                return;
            }
            if (notifyChar == null) {
                mLog.w("Get notification characteristic failed");
                gatt.disconnect();
                updateMessage("Get notification characteristic failed", false);
                return;
            }

            updateMessage("Discover service and characteristics success", false);

            int mtu = (int) BlufiApp.getInstance().settingsGet(
                    SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH, BlufiConstants.DEFAULT_MTU_LENGTH);
            boolean requestMtu = gatt.requestMtu(mtu);
            if (!requestMtu) {
                mLog.w("Request mtu failed");
                updateMessage(String.format(Locale.ENGLISH, "Request mtu %d failed", mtu), false);
                onGattServiceCharacteristicDiscovered();
            }
        }

        @Override
        public void onNegotiateSecurityResult(BlufiClient client, int status) {
            switch (status) {
                case STATUS_SUCCESS:
                    updateMessage("Negotiate security complete", false);
                    break;
                default:
                    updateMessage("Negotiate security failed， code=" + status, false);
                    break;
            }

            mBlufiSecurityBtn.setEnabled(mConnected);
        }

        @Override
        public void onConfigureResult(BlufiClient client, int status) {
            switch (status) {
                case STATUS_SUCCESS:
                    updateMessage("Post configure params complete", false);
                    break;
                default:
                    updateMessage("Post configure params failed, code=" + status, false);
                    break;
            }

            mBlufiConfigureBtn.setEnabled(mConnected);
        }

        @Override
        public void onDeviceStatusResponse(BlufiClient client, int status, BlufiStatusResponse response) {
            switch (status) {
                case STATUS_SUCCESS:
                    updateMessage(String.format("Receive device status response:\n%s", response.generateValidInfo()),
                            true);
                    break;
                default:
                    updateMessage("Device status response error, code=" + status, false);
                    break;
            }

            mBlufiDeviceStatusBtn.setEnabled(mConnected);
        }

        @Override
        public void onDeviceScanResult(BlufiClient client, int status, List<BlufiScanResult> results) {
            switch (status) {
                case STATUS_SUCCESS:
                    StringBuilder msg = new StringBuilder();
                    msg.append("Receive device scan result:\n");
                    for (BlufiScanResult scanResult : results) {
                        msg.append(scanResult.toString()).append("\n");
                    }
                    updateMessage(msg.toString(), true);
                    break;
                default:
                    updateMessage("Device scan result error, code=" + status, false);
                    break;
            }

            mBlufiDeviceScanBtn.setEnabled(mConnected);
        }

        @Override
        public void onDeviceVersionResponse(BlufiClient client, int status, BlufiVersionResponse response) {
            switch (status) {
                case STATUS_SUCCESS:
                    updateMessage(String.format("Receive device version: %s", response.getVersionString()),
                            true);
                    break;
                default:
                    updateMessage("Device version error, code=" + status, false);
                    break;
            }

            mBlufiVersionBtn.setEnabled(mConnected);
        }

        @Override
        public void onPostCustomDataResult(BlufiClient client, int status, byte[] data) {
            String dataStr = new String(data);
            String format = "Post data %s %s";
            switch (status) {
                case STATUS_SUCCESS:
                    updateMessage(String.format(format, dataStr, "complete"), false);
                    break;
                default:
                    updateMessage(String.format(format, dataStr, "failed"), false);
                    break;
            }
        }

        @Override
        public void onReceiveCustomData(BlufiClient client, int status, byte[] data) {
            switch (status) {
                case STATUS_SUCCESS:
                    String customStr = new String(data);
                    updateMessage(String.format("Receive custom data:\n%s", customStr), true);
                    break;
                default:
                    updateMessage("Receive custom data error, code=" + status, false);
                    break;
            }
        }

        @Override
        public void onError(BlufiClient client, int errCode) {
            updateMessage(String.format(Locale.ENGLISH, "Receive error code %d", errCode), false);
        }
    }

    private class BlufiButtonListener implements View.OnClickListener, View.OnLongClickListener {
        private Toast mToast;

        @Override
        public void onClick(View v) {
            if (v == mBlufiConnectBtn) {
                connect();
            } else if (v == mBlufiDisconnectBtn) {
                disconnectGatt();
            } else if (v == mBlufiSecurityBtn) {
                negotiateSecurity();
            } else if (v == mBlufiConfigureBtn) {
                configureOptions();
            } else if (v == mBlufiDeviceScanBtn) {
                requestDeviceWifiScan();
            } else if (v == mBlufiVersionBtn) {
                requestDeviceVersion();
            } else if (v == mBlufiDeviceStatusBtn) {
                requestDeviceStatus();
            } else if (v == mBlufiCustomBtn) {
                postCustomData();
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mToast != null) {
                mToast.cancel();
            }

            int msgRes = 0;
            if (v == mBlufiConnectBtn) {
                msgRes = R.string.blufi_function_connect_msg;
            } else if (v == mBlufiDisconnectBtn) {
                msgRes = R.string.blufi_function_disconnect_msg;
            } else if (v == mBlufiSecurityBtn) {
                msgRes = R.string.blufi_function_security_msg;
            } else if (v == mBlufiConfigureBtn) {
                msgRes = R.string.blufi_function_configure_msg;
            } else if (v == mBlufiDeviceScanBtn) {
                msgRes = R.string.blufi_function_device_scan_msg;
            } else if (v == mBlufiVersionBtn) {
                msgRes = R.string.blufi_function_version_msg;
            } else if (v == mBlufiDeviceStatusBtn) {
                msgRes = R.string.blufi_function_device_status_msg;
            } else if (v == mBlufiCustomBtn) {
                msgRes = R.string.blufi_function_custom_msg;
            }

            mToast = Toast.makeText(BlufiActivity.this, msgRes, Toast.LENGTH_SHORT);
            mToast.show();

            return true;
        }
    }

    private class MsgHolder extends RecyclerView.ViewHolder {
        TextView text1;

        MsgHolder(View itemView) {
            super(itemView);

            text1 = itemView.findViewById(android.R.id.text1);
        }
    }

    private class MsgAdapter extends RecyclerView.Adapter<MsgHolder> {

        @NonNull
        @Override
        public MsgHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.blufi_message_item, parent, false);
            return new MsgHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MsgHolder holder, int position) {
            Message msg = mMsgList.get(position);
            holder.text1.setText(msg.text);
            holder.text1.setTextColor(msg.isNotification ? Color.RED : Color.BLACK);
        }

        @Override
        public int getItemCount() {
            return mMsgList.size();
        }
    }

    private class Message {
        String text;
        boolean isNotification;
    }
}
