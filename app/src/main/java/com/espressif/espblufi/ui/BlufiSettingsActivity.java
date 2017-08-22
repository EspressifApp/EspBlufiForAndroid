package com.espressif.espblufi.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.esp.iot.blufi.communiation.BlufiConfigureParams;
import com.esp.iot.blufi.communiation.IBlufiCommunicator;
import com.espressif.espblufi.R;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.libs.log.EspLog;
import com.espressif.libs.net.NetUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiSettingsActivity extends BlufiAbsActivity implements AdapterView.OnItemSelectedListener {
    private static final int OP_MODE_POS_STA = 0;
    private static final int OP_MODE_POS_SOFTAP = 1;
    private static final int OP_MODE_POS_STASOFTAP = 2;

    private static final int[] OP_MODE_VALUES = {
            IBlufiCommunicator.OP_MODE_STA,
            IBlufiCommunicator.OP_MODE_SOFTAP,
            IBlufiCommunicator.OP_MODE_STASOFTAP
    };
    private static final int[] SOFTAP_SECURITY_VALUES = {
            IBlufiCommunicator.SOFTAP_SECURITY_OPEN,
            IBlufiCommunicator.SOFTAP_SECURITY_WEP,
            IBlufiCommunicator.SOFTAP_SECURITY_WPA,
            IBlufiCommunicator.SOFTAP_SECURITY_WPA2,
            IBlufiCommunicator.SOFTAP_SECURITY_WPA_WPA2
    };

    private Spinner mDeviceModeSp;
    private Spinner mMultithreadSp;

    private View mSoftAPForm;
    private Spinner mSoftapSecuritSP;
    private View mSoftAPPasswordForm;
    private EditText mSoftAPSsidET;
    private EditText mSoftAPPAsswordET;
    private Spinner mSoftAPChannelSp;
    private Spinner mSoftAPMaxConnectionSp;

    private WifiManager mWifiManager;
    private List<ScanResult> mWifiList;
    private boolean mScanning = false;
    private View mStationForm;
    private AutoCompleteTextView mStationSsidET;
    private EditText mStationPasswordET;
    private Spinner mStationMeshIDSp;

    private String mBatchKey;

    private HashMap<String, String> mApMap;
    private List<String> mAutoCompleteSSIDs;
    private ArrayAdapter<String> mAutoCompleteSSIDAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_settings_activity);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        mBatchKey = getIntent().getStringExtra(BlufiConstants.KEY_BLE_DEVICES);

        mDeviceModeSp = (Spinner) findViewById(R.id.device_mode_sp);
        mDeviceModeSp.setOnItemSelectedListener(this);
        mDeviceModeSp.setSelection(0);

        mMultithreadSp = (Spinner) findViewById(R.id.multithread_sp);
        mMultithreadSp.setSelection(0);

        mSoftAPForm = findViewById(R.id.softap_security_form);
        mSoftapSecuritSP = (Spinner) findViewById(R.id.softap_security_sp);
        mSoftapSecuritSP.setOnItemSelectedListener(this);
        mSoftAPPasswordForm = findViewById(R.id.softap_password_form);
        mSoftAPSsidET = (EditText) findViewById(R.id.softap_ssid);
        mSoftAPPAsswordET = (EditText) findViewById(R.id.softap_password);
        mSoftAPChannelSp = (Spinner) findViewById(R.id.softap_channel);
        mSoftAPMaxConnectionSp = (Spinner) findViewById(R.id.softap_max_connection);

        mApMap = new HashMap<>();
        mAutoCompleteSSIDs = new LinkedList<>();
        loadAPs();
        mWifiList = new ArrayList<>();
        mStationForm = findViewById(R.id.station_wifi_form);
        mStationSsidET = (AutoCompleteTextView) findViewById(R.id.station_ssid);
        mStationSsidET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String ssid = s.toString();
                String password = mApMap.get(ssid);
                mStationPasswordET.setText(password);
            }
        });
        mStationPasswordET = (EditText) findViewById(R.id.station_wifi_password);
        findViewById(R.id.station_wifi_scan).setOnClickListener(v -> scanWifi());
        mStationSsidET.setText(getConnectionSSID());
        mAutoCompleteSSIDAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mAutoCompleteSSIDs);
        mStationSsidET.setAdapter(mAutoCompleteSSIDAdapter);

        SharedPreferences shared = getSharedPreferences(BlufiConstants.PREF_MESH_IDS_NAME, MODE_PRIVATE);
        Set<String> bssidSet = shared.getAll().keySet();
        if (bssidSet.isEmpty()) {
            findViewById(R.id.station_mesh_id_form).setVisibility(View.GONE);
        }
        mStationMeshIDSp = (Spinner) findViewById(R.id.station_mesh_id);
        List<String> meshIds = new ArrayList<>();
        meshIds.add("");
        meshIds.addAll(bssidSet);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, meshIds);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStationMeshIDSp.setAdapter(adapter);

        findViewById(R.id.confirm).setOnClickListener(v -> configure());

        Observable.just(this)
                .subscribeOn(Schedulers.io())
                .doOnNext(BlufiSettingsActivity::updateWifi)
                .subscribe();
    }

    private String getConnectionSSID() {
        if (!mWifiManager.isWifiEnabled()) {
            return null;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return null;
        }

        String ssid = wifiInfo.getSSID();
        if (ssid.startsWith("\"") && ssid.endsWith("\"") && ssid.length() >= 2) {
            ssid = ssid.substring(1, ssid.length() - 1);
        }
        return ssid;
    }

    private int getConnectionFrequncy() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return -1;
        }

        if (!mWifiManager.isWifiEnabled()) {
            return -1;
        }

        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        if (wifiInfo == null) {
            return -1;
        }

        return wifiInfo.getFrequency();
    }

    private void loadAPs() {
//        List<IApDB> aps = EspDBManager.getInstance().ap().loadAps();
//        for (IApDB ap : aps) {
//            mApMap.put(ap.getSsid(), ap.getPassword());
//            mAutoCompleteSSIDs.add(ap.getSsid());
//        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDeviceModeSp) {
            switch (position) {
                case OP_MODE_POS_STA:
                    mSoftAPForm.setVisibility(View.GONE);
                    mStationForm.setVisibility(View.VISIBLE);
                    break;
                case OP_MODE_POS_SOFTAP:
                    mSoftAPForm.setVisibility(View.VISIBLE);
                    mStationForm.setVisibility(View.GONE);
                    break;
                case OP_MODE_POS_STASOFTAP:
                    mSoftAPForm.setVisibility(View.VISIBLE);
                    mStationForm.setVisibility(View.VISIBLE);
                    break;
            }
        } else if (parent == mSoftapSecuritSP) {
            switch (position) {
                case 0: // OPEN
                    mSoftAPPasswordForm.setVisibility(View.GONE);
                    break;
                default:
                    mSoftAPPasswordForm.setVisibility(View.VISIBLE);
                    break;
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void scanWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.esp_blufi_configure_wifi_disable_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mScanning) {
            return;
        }

        mScanning = true;

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.esp_blufi_configure_station_wifi_scanning));
        dialog.show();

        Observable.just(mWifiManager)
                .subscribeOn(Schedulers.io())
                .doOnNext(wm -> {
                    wm.startScan();
                    try {
                        Thread.sleep(1500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    updateWifi();
                })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                    dialog.dismiss();
                    showWifiListDialog();
                    mScanning = false;
                })
                .subscribe();
    }

    private void updateWifi() {
        final List<ScanResult> scans = new LinkedList<>();
        Observable.from(mWifiManager.getScanResults())
                .filter(scanResult -> {
                    if (TextUtils.isEmpty(scanResult.SSID)) {
                        return false;
                    }

                    boolean contain = false;
                    for (ScanResult srScaned : scans) {
                        if (srScaned.SSID.equals(scanResult.SSID)) {
                            contain = true;
                            break;
                        }
                    }
                    return !contain;
                })
                .doOnNext(scans::add)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnCompleted(() -> {
                    mWifiList.clear();
                    mWifiList.addAll(scans);

                    mAutoCompleteSSIDs.clear();
                    Set<String> apDBSet = mApMap.keySet();
                    mAutoCompleteSSIDs.addAll(apDBSet);
                    Observable.from(mWifiList)
                            .filter(scanResult -> !apDBSet.contains(scanResult.SSID))
                            .doOnNext(scanResult -> mAutoCompleteSSIDs.add(scanResult.SSID))
                            .subscribe();
                    mAutoCompleteSSIDAdapter.notifyDataSetChanged();
                })
                .subscribe();
    }

    private void showWifiListDialog() {
        int count = mWifiList.size();
        if (count == 0) {
            Toast.makeText(this, R.string.esp_blufi_configure_station_wifi_scanning_nothing, Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedItem = -1;
        String inputSsid = mStationSsidET.getText().toString();
        final String[] wifiSSIDs = new String[count];
        for (int i = 0; i < count; i++) {
            ScanResult sr = mWifiList.get(i);
            wifiSSIDs[i] = sr.SSID;
            if (inputSsid.equals(sr.SSID)) {
                checkedItem = i;
            }
        }
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(wifiSSIDs, checkedItem, (dialog, which) -> {
                    mStationSsidET.setText(wifiSSIDs[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private BlufiConfigureParams checkInfo() {
        BlufiConfigureParams params = new BlufiConfigureParams();
        int deviceMode = OP_MODE_VALUES[mDeviceModeSp.getSelectedItemPosition()];
        params.setOpMode(deviceMode);
        switch (deviceMode) {
            case IBlufiCommunicator.OP_MODE_NULL:
                return params;
            case IBlufiCommunicator.OP_MODE_STA:
                if (checkSta(params)) {
                    return params;
                } else {
                    return null;
                }
            case IBlufiCommunicator.OP_MODE_SOFTAP:
                if (checkSoftAP(params)) {
                    return params;
                } else {
                    return null;
                }
            case IBlufiCommunicator.OP_MODE_STASOFTAP:
                if (checkSta(params) && checkSoftAP(params)) {
                    return params;
                } else {
                    return null;
                }
        }

        return null;
    }

    private boolean checkSta(BlufiConfigureParams params) {
        String ssid = mStationSsidET.getText().toString();
        if (TextUtils.isEmpty(ssid)) {
            mStationSsidET.setError(getString(R.string.esp_blufi_configure_station_ssid_error));
            return false;
        }
        params.setStaSSID(ssid);
        String password = mStationPasswordET.getText().toString();
        params.setStaPassword(password);

        int freq = -1;
        if (ssid.equals(getConnectionSSID())) {
            freq = getConnectionFrequncy();
            if (freq != -1) {
                params.setWifiChannel(NetUtil.getWifiChannel(freq));
            }
        }
        if (freq == -1) {
            for (ScanResult sr : mWifiList) {
                if (ssid.equals(sr.SSID)) {
                    freq = sr.frequency;
                    params.setWifiChannel(NetUtil.getWifiChannel(freq));
                    break;
                }
            }
        }

        String bssid = mStationMeshIDSp.getSelectedItem().toString();
        if (!TextUtils.isEmpty(bssid)) {
            String[] mac = bssid.split(":");
            byte[] meshId = new byte[mac.length];
            for (int i = 0; i < mac.length; i++) {
                meshId[i] = (byte) Integer.parseInt(mac[i], 16);
            }
            params.setMeshID(meshId);
        }

        // Save in db
//        EspDBManager.getInstance().ap().insertOrReplace(ssid, password);

        return true;
    }

    public boolean checkSoftAP(BlufiConfigureParams params) {
        String ssid = mSoftAPSsidET.getText().toString();
        params.setSoftAPSSID(ssid);
        String password = mSoftAPPAsswordET.getText().toString();
        params.setSoftAPPAssword(password);
        int channel = mSoftAPChannelSp.getSelectedItemPosition();
        params.setSoftAPChannel(channel);
        int maxConnection = mSoftAPMaxConnectionSp.getSelectedItemPosition();
        params.setSoftAPMaxConnection(maxConnection);

        int security = SOFTAP_SECURITY_VALUES[mSoftapSecuritSP.getSelectedItemPosition()];
        params.setSoftAPSecurity(security);
        switch (security) {
            case IBlufiCommunicator.SOFTAP_SECURITY_OPEN:
                return true;
            case IBlufiCommunicator.SOFTAP_SECURITY_WEP:
            case IBlufiCommunicator.SOFTAP_SECURITY_WPA:
            case IBlufiCommunicator.SOFTAP_SECURITY_WPA2:
            case IBlufiCommunicator.SOFTAP_SECURITY_WPA_WPA2:
                if (TextUtils.isEmpty(password) || password.length() < 8) {
                    mSoftAPPAsswordET.setError(getString(R.string.esp_blufi_configure_softap_password_error));
                    return false;
                }

                return true;
        }

        return false;
    }

    private void configure() {
        mSoftAPSsidET.setError(null);
        mSoftAPPAsswordET.setError(null);
        mStationSsidET.setError(null);

        final BlufiConfigureParams params = checkInfo();
        if (params == null) {
            return;
        }

        int multithread = 1;
        try {
            multithread = Integer.parseInt(mMultithreadSp.getSelectedItem().toString());
        } catch (Exception e) {
            EspLog.e("BlufiSettingsActivity configure exception: " + e.getMessage());
        }

        Intent intent = new Intent(this, BlufiConfigureActivity.class);
        intent.putExtra(BlufiConstants.KEY_BLE_DEVICES, mBatchKey);
        intent.putExtra(BlufiConstants.KEY_CONFIGURE_PARAM, params);
        intent.putExtra(BlufiConstants.KEY_CONFIGURE_MULTITHREAD, multithread);

        startActivity(intent);
        setResult(RESULT_OK);
        finish();
    }
}
