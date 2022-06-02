package com.espressif.espblufi.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BaseActivity;
import com.espressif.espblufi.app.BlufiLog;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.databinding.ConfigureOptionActivityBinding;
import com.espressif.espblufi.databinding.ConfigureOptionSelectFormBinding;
import com.espressif.espblufi.databinding.ConfigureOptionSoftapFormBinding;
import com.espressif.espblufi.databinding.ConfigureOptionStationFormBinding;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blufi.espressif.params.BlufiConfigureParams;
import blufi.espressif.params.BlufiParameter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConfigureOptionsActivity extends BaseActivity implements AdapterView.OnItemSelectedListener {
    private static final int OP_MODE_POS_STA = 0;
    private static final int OP_MODE_POS_SOFTAP = 1;
    private static final int OP_MODE_POS_STASOFTAP = 2;

    private static final int[] OP_MODE_VALUES = {
            BlufiParameter.OP_MODE_STA,
            BlufiParameter.OP_MODE_SOFTAP,
            BlufiParameter.OP_MODE_STASOFTAP
    };
    private static final int[] SOFTAP_SECURITY_VALUES = {
            BlufiParameter.SOFTAP_SECURITY_OPEN,
//            BlufiParameter.SOFTAP_SECURITY_WEP,
            BlufiParameter.SOFTAP_SECURITY_WPA,
            BlufiParameter.SOFTAP_SECURITY_WPA2,
            BlufiParameter.SOFTAP_SECURITY_WPA_WPA2
    };

    private static final String PREF_AP = "blufi_conf_aps";

    private BlufiLog mLog = new BlufiLog(getClass());

    private WifiManager mWifiManager;
    private List<ScanResult> mWifiList;
    private boolean mScanning = false;

    private HashMap<String, String> mApMap;
    private List<String> mAutoCompleteSSIDs;
    private ArrayAdapter<String> mAutoCompleteSSIDAdapter;

    private SharedPreferences mApPref;

    private ConfigureOptionActivityBinding mBinding;
    private ConfigureOptionSelectFormBinding mSelectForm;
    private ConfigureOptionSoftapFormBinding mSoftAPForm;
    private ConfigureOptionStationFormBinding mStationForm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ConfigureOptionActivityBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        setSupportActionBar(mBinding.toolbar);
        setHomeAsUpEnable(true);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        mApPref = getSharedPreferences(PREF_AP, MODE_PRIVATE);

        mSelectForm = mBinding.optionContent.selectForm;
        mSoftAPForm = mBinding.optionContent.softapForm;
        mStationForm = mBinding.optionContent.stationForm;

        mSelectForm.deviceModeSp.setOnItemSelectedListener(this);
        mSelectForm.deviceModeSp.setSelection(0);

        mSoftAPForm.softapSecuritySp.setOnItemSelectedListener(this);

        mApMap = new HashMap<>();
        mAutoCompleteSSIDs = new LinkedList<>();
        loadAPs();
        mWifiList = new ArrayList<>();
        mStationForm.stationWifiSsidForm.setEndIconOnClickListener(v -> scanWifi());
        mAutoCompleteSSIDAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, mAutoCompleteSSIDs);
        mStationForm.stationSsid.setAdapter(mAutoCompleteSSIDAdapter);
        mStationForm.stationSsid.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String pwd = mApMap.get(s.toString());
                mStationForm.stationWifiPassword.setText(pwd);
                mStationForm.stationSsid.setTag(null);
            }
        });
        mStationForm.stationSsid.setText(getConnectionSSID());
        WifiInfo info = mWifiManager.getConnectionInfo();
        if (info != null) {
            byte[] ssidBytes = getSSIDRawData(info);
            mStationForm.stationSsid.setTag(ssidBytes);
        }

        findViewById(R.id.confirm).setOnClickListener(v -> configure());

        Observable.just(this)
                .subscribeOn(Schedulers.io())
                .doOnNext(ConfigureOptionsActivity::updateWifi)
                .subscribe();
    }

    private boolean is5GHz(int freq) {
        return freq > 4900 && freq < 5900;
    }

    private byte[] getSSIDRawData(WifiInfo info) {
        try {
            Method method = info.getClass().getMethod("getWifiSsid");
            method.setAccessible(true);
            Object wifiSsid = method.invoke(info);
            if (wifiSsid == null) {
                return null;
            }
            method = wifiSsid.getClass().getMethod("getOctets");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getSSIDRawData(ScanResult scanResult) {
        try {
            Field field = scanResult.getClass().getField("wifiSsid");
            field.setAccessible(true);
            Object wifiSsid = field.get(scanResult);
            if (wifiSsid == null) {
                return null;
            }
            Method method = wifiSsid.getClass().getMethod("getOctets");
            method.setAccessible(true);
            return (byte[]) method.invoke(wifiSsid);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }

        return null;
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
        Map<String, ?> aps = mApPref.getAll();
        for (Map.Entry<String, ?> entry : aps.entrySet()) {
            mApMap.put(entry.getKey(), entry.getValue().toString());
            mAutoCompleteSSIDs.add(entry.getKey());
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mSelectForm.deviceModeSp) {
            switch (position) {
                case OP_MODE_POS_STA:
                    mSoftAPForm.softapOptions.setVisibility(View.GONE);
                    mStationForm.stationOptions.setVisibility(View.VISIBLE);
                    break;
                case OP_MODE_POS_SOFTAP:
                    mSoftAPForm.softapOptions.setVisibility(View.VISIBLE);
                    mStationForm.stationOptions.setVisibility(View.GONE);
                    break;
                case OP_MODE_POS_STASOFTAP:
                    mSoftAPForm.softapOptions.setVisibility(View.VISIBLE);
                    mStationForm.stationOptions.setVisibility(View.VISIBLE);
                    break;
            }
        } else if (parent == mSoftAPForm.softapSecuritySp) {
            if (position == 0) {
                // OPEN
                mSoftAPForm.softapPasswordForm.setVisibility(View.GONE);
            } else {
                mSoftAPForm.softapPasswordForm.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void scanWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.configure_wifi_disable_msg, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mScanning) {
            return;
        }

        mScanning = true;

        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.configure_station_wifi_scanning));
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
                .doOnComplete(() -> {
                    dialog.dismiss();
                    showWifiListDialog();
                    mScanning = false;
                })
                .subscribe();
    }

    private void updateWifi() {
        final List<ScanResult> scans = new LinkedList<>();
        Observable.fromIterable(mWifiManager.getScanResults())
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
                .doOnComplete(() -> {
                    mWifiList.clear();
                    mWifiList.addAll(scans);

                    mAutoCompleteSSIDs.clear();
                    Set<String> apDBSet = mApMap.keySet();
                    mAutoCompleteSSIDs.addAll(apDBSet);
                    Observable.fromIterable(mWifiList)
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
            Toast.makeText(this, R.string.configure_station_wifi_scanning_nothing, Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedItem = -1;
        String inputSsid = mStationForm.stationSsid.getText().toString();
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
                    mStationForm.stationSsid.setText(wifiSSIDs[which]);
                    ScanResult scanResult = mWifiList.get(which);
                    byte[] ssidBytes = getSSIDRawData(scanResult);
                    mStationForm.stationSsid.setTag(ssidBytes);
                    dialog.dismiss();
                })
                .show();
    }

    private BlufiConfigureParams checkInfo() {
        BlufiConfigureParams params = new BlufiConfigureParams();
        int deviceMode = OP_MODE_VALUES[mSelectForm.deviceModeSp.getSelectedItemPosition()];
        params.setOpMode(deviceMode);
        switch (deviceMode) {
            case BlufiParameter.OP_MODE_NULL:
                return params;
            case BlufiParameter.OP_MODE_STA:
                if (checkSta(params)) {
                    return params;
                } else {
                    return null;
                }
            case BlufiParameter.OP_MODE_SOFTAP:
                if (checkSoftAP(params)) {
                    return params;
                } else {
                    return null;
                }
            case BlufiParameter.OP_MODE_STASOFTAP:
                if (checkSoftAP(params) && checkSta(params)) {
                    return params;
                } else {
                    return null;
                }
        }

        return null;
    }

    private boolean checkSta(BlufiConfigureParams params) {
        String ssid = mStationForm.stationSsid.getText().toString();
        if (TextUtils.isEmpty(ssid)) {
            mStationForm.stationSsid.setError(getString(R.string.configure_station_ssid_error));
            return false;
        }
        byte[] ssidBytes = (byte[]) mStationForm.stationSsid.getTag();
        params.setStaSSIDBytes(ssidBytes != null ? ssidBytes : ssid.getBytes());
        String password = mStationForm.stationWifiPassword.getText().toString();
        params.setStaPassword(password);

        int freq = -1;
        if (ssid.equals(getConnectionSSID())) {
            freq = getConnectionFrequncy();
        }
        if (freq == -1) {
            for (ScanResult sr : mWifiList) {
                if (ssid.equals(sr.SSID)) {
                    freq = sr.frequency;
                    break;
                }
            }
        }
        if (is5GHz(freq)) {
            mStationForm.stationSsid.setError(getString(R.string.configure_station_wifi_5g_error));
            new AlertDialog.Builder(this)
                    .setMessage(R.string.configure_station_wifi_5g_dialog_message)
                    .setPositiveButton(R.string.configure_station_wifi_5g_dialog_continue, (dialog, which) -> {
                        finishWithParams(params);
                    })
                    .setNegativeButton(R.string.configure_station_wifi_5g_dialog_cancel, null)
                    .show();
            return false;
        }

        return true;
    }

    public boolean checkSoftAP(BlufiConfigureParams params) {
        String ssid = mSoftAPForm.softapSsid.getText().toString();
        params.setSoftAPSSID(ssid);
        String password = mSoftAPForm.softapPassword.getText().toString();
        params.setSoftAPPAssword(password);
        int channel = mSoftAPForm.softapChannel.getSelectedItemPosition();
        params.setSoftAPChannel(channel);
        int maxConnection = mSoftAPForm.softapMaxConnection.getSelectedItemPosition();
        params.setSoftAPMaxConnection(maxConnection);

        int security = SOFTAP_SECURITY_VALUES[mSoftAPForm.softapSecuritySp.getSelectedItemPosition()];
        params.setSoftAPSecurity(security);
        switch (security) {
            case BlufiParameter.SOFTAP_SECURITY_OPEN:
                return true;
            case BlufiParameter.SOFTAP_SECURITY_WEP:
            case BlufiParameter.SOFTAP_SECURITY_WPA:
            case BlufiParameter.SOFTAP_SECURITY_WPA2:
            case BlufiParameter.SOFTAP_SECURITY_WPA_WPA2:
                if (TextUtils.isEmpty(password) || password.length() < 8) {
                    mSoftAPForm.softapPassword.setError(getString(R.string.configure_softap_password_error));
                    return false;
                }

                return true;
        }

        return false;
    }

    private void configure() {
        mSoftAPForm.softapSsid.setError(null);
        mSoftAPForm.softapPassword.setError(null);
        mStationForm.stationSsid.setError(null);

        final BlufiConfigureParams params = checkInfo();
        if (params == null) {
            mLog.w("Generate configure params null");
            return;
        }

        finishWithParams(params);
    }

    private void saveAP(BlufiConfigureParams params) {
        switch (params.getOpMode()) {
            case BlufiParameter.OP_MODE_STA:
            case BlufiParameter.OP_MODE_STASOFTAP:
                String ssid = new String(params.getStaSSIDBytes());
                String pwd = params.getStaPassword();
                mApPref.edit().putString(ssid, pwd).apply();
                break;
            default:
                break;
        }
    }

    private void finishWithParams(BlufiConfigureParams params) {
        Intent intent = new Intent();
        intent.putExtra(BlufiConstants.KEY_CONFIGURE_PARAM, params);

        saveAP(params);

        setResult(RESULT_OK, intent);
        finish();
    }
}
