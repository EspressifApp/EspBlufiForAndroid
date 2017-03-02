package com.espressif.espblufi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.espressif.espblufi.communication.BlufiCommunicator;
import com.espressif.espblufi.communication.BlufiConfigureParams;
import com.espressif.espblufi.communication.IBlufiCommunicator;
import com.espressif.espblufi.communication.response.BlufiStatusResponse;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class BlufiConfigureActivity extends BlufiAbsActivity implements AdapterView.OnItemSelectedListener {
    private static final int[] OP_MODE_VALUES = {
            IBlufiCommunicator.OP_MODE_NULL,
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

    private ProgressBar mProgressBar;
    private ScrollView mScrollForm;

    private Spinner mDeviceModeSp;

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
    private EditText mStationSsidET;
    private EditText mStationPasswordET;

    private BlufiCommunicator mCommunicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_configure_activity);

        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mCommunicator = BlufiBridge.sCommunicator;

        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        mScrollForm = (ScrollView) findViewById(R.id.scroll_form);

        mDeviceModeSp = (Spinner) findViewById(R.id.device_mode_sp);
        mDeviceModeSp.setOnItemSelectedListener(this);

        mSoftAPForm = findViewById(R.id.softap_security_form);
        mSoftapSecuritSP = (Spinner) findViewById(R.id.softap_security_sp);
        mSoftapSecuritSP.setOnItemSelectedListener(this);
        mSoftAPPasswordForm = findViewById(R.id.softap_password_form);
        mSoftAPSsidET = (EditText) findViewById(R.id.softap_ssid);
        mSoftAPPAsswordET = (EditText) findViewById(R.id.softap_password);
        mSoftAPChannelSp = (Spinner) findViewById(R.id.softap_channel);
        mSoftAPMaxConnectionSp = (Spinner) findViewById(R.id.softap_max_connection);

        mWifiList = new ArrayList<>();
        mStationForm = findViewById(R.id.station_wifi_form);
        mStationSsidET = (EditText) findViewById(R.id.station_ssid);
        mStationSsidET.setText(getConnectionSSID());
        mStationPasswordET = (EditText) findViewById(R.id.station_wifi_password);
        findViewById(R.id.station_wifi_scan).setOnClickListener(v -> scanWifi());

        findViewById(R.id.confirm).setOnClickListener(v -> configure());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mCommunicator = null;
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

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent == mDeviceModeSp) {
            switch (position) {
                case 0: // NULL
                    mSoftAPForm.setVisibility(View.GONE);
                    mStationForm.setVisibility(View.GONE);
                    break;
                case 1: // Sta
                    mSoftAPForm.setVisibility(View.GONE);
                    mStationForm.setVisibility(View.VISIBLE);
                    break;
                case 2: // SoftAP
                    mSoftAPForm.setVisibility(View.VISIBLE);
                    mStationForm.setVisibility(View.GONE);
                    break;
                case 3: // Sta and SoftAP
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

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mScrollForm.setVisibility(show ? View.GONE : View.VISIBLE);
        mScrollForm.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mScrollForm.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressBar.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void scanWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            Toast.makeText(this, R.string.wifi_disable_msg, Toast.LENGTH_SHORT).show();
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

        final List<ScanResult> scans = new ArrayList<>();
        Observable.just(mWifiManager)
                .subscribeOn(Schedulers.io())
                .flatMap(wifiManager -> {
                    wifiManager.startScan();
                    try {
                        Thread.sleep(1500L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Observable.from(wifiManager.getScanResults());
                })
                .filter(scanResult -> !TextUtils.isEmpty(scanResult.SSID))
                .doOnNext(scans::add)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<ScanResult>() {
                    @Override
                    public void onCompleted() {
                        mWifiList.clear();
                        mWifiList.addAll(scans);
                        dialog.dismiss();
                        showWifiListDialog();
                        mScanning = false;
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(ScanResult scanResult) {
                    }
                });
    }

    private void showWifiListDialog() {
        int count = mWifiList.size();
        if (count == 0) {
            Toast.makeText(this, R.string.configure_station_wifi_scanning_nothing, Toast.LENGTH_SHORT).show();
            return;
        }

        int checkedItem = -1;
        String inputSsid = mStationSsidET.getText().toString();
        String[] wifiSSIDs = new String[count];
        for (int i = 0; i < count; i++) {
            ScanResult sr = mWifiList.get(i);
            wifiSSIDs[i] = sr.SSID;
            if (inputSsid.equals(sr.SSID)) {
                checkedItem = i;
            }
        }
        new AlertDialog.Builder(this)
                .setSingleChoiceItems(wifiSSIDs, checkedItem, ((dialog, which) -> {
                    mStationSsidET.setText(wifiSSIDs[which]);
                    dialog.dismiss();
                }))
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
            mStationSsidET.setError(getString(R.string.configure_station_ssid_error));
            return false;
        }
        params.setStaSSID(ssid);
        String password = mStationPasswordET.getText().toString();
        params.setStaPassword(password);

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
                    mSoftAPPAsswordET.setError(getString(R.string.configure_softap_password_error));
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

        showProgress(true);

        final Intent data = new Intent();
        Observable.just(mCommunicator)
                .subscribeOn(Schedulers.io())
                .map(blufiComm -> blufiComm.configure(params))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<BlufiStatusResponse>() {
                    @Override
                    public void onCompleted() {
                        setResult(RESULT_OK, data);
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        setResult(RESULT_EXCEPTION);
                        finish();
                    }

                    @Override
                    public void onNext(BlufiStatusResponse response) {
                        String dataExtra = "";
                        switch (response.getResultCode()) {
                            case BlufiStatusResponse.RESULT_TIMEOUT:
                                dataExtra = "receive wifi state timeout";
                                break;
                            case BlufiStatusResponse.RESULT_PARSE_FAILED:
                                dataExtra = "receive wifi state parse data error";
                                break;
                            case BlufiStatusResponse.RESULT_SUCCESS:
                                dataExtra = response.generateValidInfo();
                                break;
                        }
                        data.putExtra(BlufiBridge.KEY_CONFIGURE_DATA, dataExtra);
                    }
                });
    }
}
