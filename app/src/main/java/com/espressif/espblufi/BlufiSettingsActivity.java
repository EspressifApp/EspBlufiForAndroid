package com.espressif.espblufi;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.espressif.espblufi.communication.BlufiProtocol;

import java.util.Locale;

public class BlufiSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.blufi_settings_activity);

        getFragmentManager().beginTransaction().replace(R.id.frame, new BlufiSettingsFragment()).commit();
    }

    public static class BlufiSettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.blufi_settings);

            findPreference(getString(R.string.settings_version_key)).setSummary(getVersionName());

            String supportProtocolVersion = String.format(Locale.ENGLISH,
                    "%d.%d",
                    BlufiProtocol.SUPPORT_PROTOCOL_VERSION[0], BlufiProtocol.SUPPORT_PROTOCOL_VERSION[1]);
            findPreference(getString(R.string.settings_support_protocol_key)).setSummary(supportProtocolVersion);
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
    }
}
