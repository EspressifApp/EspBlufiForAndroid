package com.espressif.espblufi.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BaseActivity;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;
import com.espressif.espblufi.task.BlufiAppReleaseTask;

import java.util.concurrent.atomic.AtomicReference;

import blufi.espressif.BlufiClient;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import libs.espressif.app.AppUtil;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        getFragmentManager().beginTransaction().replace(R.id.frame, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private static final String KEY_MTU_LENGTH = SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH;
        private static final String KEY_BLE_PREFIX = SettingsConstants.PREF_SETTINGS_KEY_BLE_PREFIX;

        private BlufiApp mApp;

        private EditTextPreference mMtuPref;
        private EditTextPreference mBlePrefixPref;

        private Preference mVersionCheckPref;
        private volatile BlufiAppReleaseTask.ReleaseInfo mAppLatestRelease;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.blufi_settings);

            mApp = BlufiApp.getInstance();

            findPreference(getString(R.string.settings_version_key)).setSummary(getVersionName());
            findPreference(getString(R.string.settings_blufi_version_key)).setSummary(BlufiClient.VERSION);

            mMtuPref = (EditTextPreference) findPreference(getString(R.string.settings_mtu_length_key));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                PreferenceCategory blufiCategory = (PreferenceCategory) findPreference(getString(R.string.settings_category_blufi_key));
                blufiCategory.removePreference(mMtuPref);
            } else {
                mMtuPref.getEditText().setHint(getString(R.string.settings_mtu_length_hint, BlufiConstants.MIN_MTU_LENGTH));
                int mtuLen = (int) mApp.settingsGet(KEY_MTU_LENGTH, BlufiConstants.DEFAULT_MTU_LENGTH);
                mMtuPref.setOnPreferenceChangeListener(this);
                if (mtuLen >= BlufiConstants.MIN_MTU_LENGTH) {
                    mMtuPref.setSummary(String.valueOf(mtuLen));
                }
            }

            mBlePrefixPref = (EditTextPreference) findPreference(getString(R.string.settings_ble_prefix_key));
            mBlePrefixPref.setOnPreferenceChangeListener(this);
            String blePrefix = (String) mApp.settingsGet(KEY_BLE_PREFIX, BlufiConstants.BLUFI_PREFIX);
            mBlePrefixPref.setSummary(blePrefix);

            mVersionCheckPref = findPreference(getString(R.string.settings_upgrade_check_key));
        }

        public String getVersionName() {
            String version;
            try {
                PackageInfo pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                version = getString(R.string.string_unknown);
            }
            return version;
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
            if (preference == mVersionCheckPref) {
                if (mAppLatestRelease == null) {
                    mVersionCheckPref.setEnabled(false);
                    checkAppLatestRelease();
                } else {
                    downloadLatestRelease();
                }
                return true;
            }

            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference == mMtuPref) {
                String lenStr = newValue.toString();
                int mtuLen = BlufiConstants.DEFAULT_MTU_LENGTH;
                if (!TextUtils.isEmpty(lenStr)) {
                    int newLen = Integer.parseInt(lenStr);
                    if (newLen > BlufiConstants.MIN_MTU_LENGTH) {
                        mtuLen = newLen;
                    }
                }
                mMtuPref.setSummary(String.valueOf(mtuLen));
                mApp.settingsPut(KEY_MTU_LENGTH, mtuLen);
                return true;
            } else if (preference == mBlePrefixPref) {
                String prefix = newValue.toString();
                mBlePrefixPref.setSummary(prefix);
                mApp.settingsPut(KEY_BLE_PREFIX, prefix);
                return true;
            }

            return false;
        }

        private void checkAppLatestRelease() {
            Observable.just(new BlufiAppReleaseTask())
                    .subscribeOn(Schedulers.io())
                    .map(task -> new AtomicReference<>(task.requestLatestRelease()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(reference -> {
                        mVersionCheckPref.setEnabled(true);

                        mAppLatestRelease = null;
                        BlufiAppReleaseTask.ReleaseInfo latestRelease = reference.get();
                        if (latestRelease == null) {
                            mVersionCheckPref.setSummary(R.string.settings_upgrade_check_failed);
                            return;
                        } else if (latestRelease.getVersionCode() < 0) {
                            mVersionCheckPref.setSummary(R.string.settings_upgrade_check_not_found);
                            return;
                        }

                        int currentVersion = AppUtil.getVersionCode(getActivity());
                        int latestVersion = latestRelease.getVersionCode();
                        if (latestVersion > currentVersion) {
                            mVersionCheckPref.setSummary(R.string.settings_upgrade_check_disciver_new);
                            mAppLatestRelease = latestRelease;

                            new AlertDialog.Builder(getActivity())
                                    .setTitle(R.string.settings_upgrade_dialog_title)
                                    .setMessage(R.string.settings_upgrade_dialog_message)
                                    .setNegativeButton(android.R.string.cancel, null)
                                    .setPositiveButton(R.string.settings_upgrade_dialog_upgrade,
                                            (dialog, which) -> downloadLatestRelease())
                                    .show();
                        } else {
                            mVersionCheckPref.setSummary(R.string.settings_upgrade_check_current_latest);
                        }
                    })
                    .subscribe();

        }

        private void downloadLatestRelease() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(mAppLatestRelease.getDownloadUrl());
            intent.setData(uri);
            startActivity(intent);
        }
    }  // Fragment end
} // Activity end
