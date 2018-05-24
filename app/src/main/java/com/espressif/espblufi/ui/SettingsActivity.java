package com.espressif.espblufi.ui;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import com.espressif.espblufi.R;
import com.espressif.espblufi.app.BaseActivity;
import com.espressif.espblufi.app.BlufiApp;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;

import java.util.Locale;

import blufi.espressif.BlufiUtils;

public class SettingsActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setHomeAsUpEnable(true);

        getFragmentManager().beginTransaction().replace(R.id.frame, new BlufiSettingsFragment()).commit();
    }

    public static class BlufiSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private static final String KEY_MTU_LENGTH = SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH;
        private static final String KEY_BLE_PREFIX = SettingsConstants.PREF_SETTINGS_KEY_BLE_PREFIX;

        private BlufiApp mApp;

        private EditTextPreference mMtuPref;
        private EditTextPreference mBlePrefixPref;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.blufi_settings);

            mApp = BlufiApp.getInstance();

            findPreference(getString(R.string.settings_version_key)).setSummary(getVersionName());
            findPreference(getString(R.string.settings_blufi_version_key)).setSummary(BlufiUtils.VERSION);
            String supportProtocolVersion = String.format(Locale.ENGLISH, "%d.%d",
                    BlufiUtils.SUPPORT_BLUFI_VERSION[0], BlufiUtils.SUPPORT_BLUFI_VERSION[1]);
            findPreference(getString(R.string.settings_support_protocol_key)).setSummary(supportProtocolVersion);


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
        }

        public String getVersionName() {
            String version;
            try {
                PackageInfo pi = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                version = pi.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                version = "Unknow";
            }
            return version;
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
    }  // Fragment end
} // Activity end
