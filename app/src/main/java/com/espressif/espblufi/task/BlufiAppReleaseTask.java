package com.espressif.espblufi.task;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Released APKs' name format: EspBluFi-{version name}-{version code}.apk
 */
public class BlufiAppReleaseTask {
    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/EspressifApp/EspBlufiForAndroid/releases/latest";

    private static final String APK_SUFFIX = ".apk";

    private static final String KEY_ASSETS = "assets";
    private static final String KEY_ASSET_NAME = "name";
    private static final String KEY_SIZE = "size";
    private static final String KEY_DOWNLOAD_URL = "browser_download_url";
    private static final String KEY_BODY = "body";

    public ReleaseInfo requestLatestRelease() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(LATEST_RELEASE_URL);
            connection = (HttpURLConnection) url.openConnection();
            int code = connection.getResponseCode();
            String message = connection.getResponseMessage();
            System.out.println("Code = " + code);
            if (code != HttpURLConnection.HTTP_OK) {
                ReleaseInfo info = new ReleaseInfo();
                info.versionCode = -1;
                info.versionName = message;
                return info;
            }

            InputStream is = connection.getInputStream();
            ByteArrayOutputStream contentArray = new ByteArrayOutputStream();
            for (int read = is.read(); read != -1; read = is.read()) {
                contentArray.write(read);
            }

            JSONObject releaseJSON = new JSONObject(new String(contentArray.toByteArray()));
            return parseRelease(releaseJSON);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return null;
    }

    private ReleaseInfo parseRelease(JSONObject releaseJSON) {
        try {
            String notes = releaseJSON.getString(KEY_BODY);
            JSONArray assetArray = releaseJSON.getJSONArray(KEY_ASSETS);
            for (int i = 0; i < assetArray.length(); i++) {
                JSONObject assetJSON = assetArray.getJSONObject(i);
                String assetName = assetJSON.getString(KEY_ASSET_NAME);
                if (assetName.endsWith(APK_SUFFIX)) {
                    String apkName = assetName.substring(0, assetName.length() - APK_SUFFIX.length());
                    String[] apkNameSplits = apkName.split("-");
                    String versionName = apkNameSplits[1];
                    long versionCode = Long.parseLong(apkNameSplits[2]);

                    long apkSize = assetJSON.getLong(KEY_SIZE);
                    String downloadUrl = assetJSON.getString(KEY_DOWNLOAD_URL);

                    ReleaseInfo result = new ReleaseInfo();
                    result.versionCode = versionCode;
                    result.versionName = versionName;
                    result.apkSize = apkSize;
                    result.downloadUrl = downloadUrl;
                    result.notes = notes;
                    return result;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static class ReleaseInfo {
        String versionName;
        long versionCode;
        String downloadUrl;
        long apkSize;
        String notes;

        public long getVersionCode() {
            return versionCode;
        }

        public String getVersionName() {
            return versionName;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }

        public long getApkSize() {
            return apkSize;
        }

        public String getNotes() {
            return notes;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(Locale.ENGLISH,
                    "VersionName=%s, VersionCode=%d, DownloadUrl=%s, APKSize=%d, notes=%s",
                    versionName, versionCode, downloadUrl, apkSize, notes);
        }
    }
}
