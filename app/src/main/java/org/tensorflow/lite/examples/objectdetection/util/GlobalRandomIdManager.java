package org.tensorflow.lite.examples.objectdetection.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class GlobalRandomIdManager {

    private static final String PREF_NAME = "GlobalRandomIdPref";
    private static final String KEY_GLOBAL_ID = "global_random_id";
    private static String globalId =null;
    private SharedPreferences sharedPreferences;

    public GlobalRandomIdManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static String getGlobalRandomId() {

        if (globalId == null) {
            // 如果没有，生成一个新的 UUID 并保存
            globalId = UUID.randomUUID().toString();
        }

        return globalId;
    }
}