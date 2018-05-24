package com.espressif.espblufi.app;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.espressif.espblufi.constants.SettingsConstants;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import libs.espressif.utils.RandomUtil;

public class BlufiApp extends Application {
    private static BlufiApp instance;

    private final HashMap<String, Object> mCache = new HashMap<>();

    private SharedPreferences mSettingsShared;

    public static BlufiApp getInstance() {
        if (instance == null) {
            throw new NullPointerException("App instance hasn't registered");
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;
        mSettingsShared = getSharedPreferences(SettingsConstants.PREF_SETTINGS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        mCache.clear();
    }

    public String putCache(Object value) {
        synchronized (mCache) {
            int keyLength = new Random().nextInt(20) + 20;
            String key = RandomUtil.randomString(keyLength);
            mCache.put(key, value);
            return key;
        }
    }

    public Object takeCache(String key) {
        synchronized (mCache) {
            Object result = mCache.get(key);
            if (result != null) {
                mCache.remove(key);
            }

            return result;
        }
    }

    public boolean settingsPut(String key, Object value) {
        SharedPreferences.Editor editor = mSettingsShared.edit();
        if (value instanceof String) {
            editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
        } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
        } else  if (value instanceof Long) {
            editor.putLong(key, (Long) value);
        } else if (value instanceof Set) {
            Set set = (Set) value;
            if (!set.isEmpty()) {
                if (!(set.iterator().next() instanceof String)) {
                    return false;
                }
            }
            editor.putStringSet(key, (Set<String>)set);
        } else {
            return false;
        }

        editor.apply();
        return true;
    }

    public Object settingsGet(String key, Object defaultValue) {
        if (defaultValue instanceof String) {
            return mSettingsShared.getString(key, (String) defaultValue);
        } else if (defaultValue instanceof Boolean) {
            return mSettingsShared.getBoolean(key, (Boolean) defaultValue);
        } else if (defaultValue instanceof Float) {
            return mSettingsShared.getFloat(key, (Float) defaultValue);
        } else if (defaultValue instanceof Integer) {
            return mSettingsShared.getInt(key, (Integer) defaultValue);
        } else if (defaultValue instanceof Long) {
            return mSettingsShared.getLong(key, (Long) defaultValue);
        } else if (defaultValue instanceof Set) {
            return mSettingsShared.getStringSet(key, (Set<String>) defaultValue);
        } else {
            return null;
        }
    }
}
