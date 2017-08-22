package com.espressif.espblufi.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.esp.iot.blufi.communiation.BlufiProtocol;
import com.espressif.espblufi.R;
import com.espressif.espblufi.constants.BlufiConstants;
import com.espressif.espblufi.constants.SettingsConstants;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getFragmentManager().beginTransaction().replace(R.id.frame, new BlufiSettingsFragment()).commit();
    }

    public static class BlufiSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        private EditTextPreference mMtuPref;

        private SharedPreferences mShared;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.blufi_settings);

            mShared = getActivity().getSharedPreferences(SettingsConstants.PREF_SETTINGS_NAME, Context.MODE_PRIVATE);

            findPreference(getString(R.string.settings_version_key)).setSummary(getVersionName());

            String supportProtocolVersion = String.format(Locale.ENGLISH,
                    "%d.%d",
                    BlufiProtocol.SUPPORT_PROTOCOL_VERSION[0], BlufiProtocol.SUPPORT_PROTOCOL_VERSION[1]);
            findPreference(getString(R.string.settings_support_protocol_key)).setSummary(supportProtocolVersion);

            mMtuPref = (EditTextPreference) findPreference(getString(R.string.settings_mtu_length_key));
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                PreferenceCategory blufiCategory = (PreferenceCategory) findPreference(getString(R.string.settings_category_blufi_key));
                blufiCategory.removePreference(mMtuPref);
                getPreferenceScreen().removePreference(blufiCategory);
            } else {
                mMtuPref.getEditText().setHint(getString(R.string.settings_mtu_length_hint, BlufiConstants.MIN_MTU_LENGTH));
                int mtuLen = mShared.getInt(SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH, BlufiConstants.DEFAULT_MTU_LENGTH);
                mMtuPref.setOnPreferenceChangeListener(this);
                if (mtuLen >= BlufiConstants.MIN_MTU_LENGTH) {
                    mMtuPref.setSummary(String.valueOf(mtuLen));
                }
            }
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
                mShared.edit().putInt(SettingsConstants.PREF_SETTINGS_KEY_MTU_LENGTH, mtuLen).apply();
            }
            return false;
        }

        // Fragment end
    }
}
