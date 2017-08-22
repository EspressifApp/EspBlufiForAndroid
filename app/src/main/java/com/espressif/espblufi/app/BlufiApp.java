package com.espressif.espblufi.app;

import android.app.Application;

import com.espressif.libs.utils.RandomUtil;

import java.util.HashMap;
import java.util.Random;

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
}
