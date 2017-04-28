package com.espressif.espblufi.app;

import android.app.Application;

import java.util.HashMap;

public class BlufiApp extends Application {
    private static BlufiApp instance;

    private final HashMap<String, Object> mCache = new HashMap<>();

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
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        mCache.clear();
    }

    public void putCache(String key, Object value) {
        synchronized (mCache) {
            mCache.put(key, value);
        }
    }

    public Object takeCache(String key) {
        synchronized (mCache) {
            Object obj = mCache.get(key);
            if (obj != null) {
                mCache.remove(key);
            }

            return obj;
        }
    }
}
