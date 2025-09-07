package net.slidermc.sliderproxy.utils;

import com.google.gson.Gson;

public class GsonUtils {
    private static final Gson GSON = new Gson();

    public static Gson getGson() {
        synchronized (GsonUtils.class) {
            return GSON;
        }
    }
}
