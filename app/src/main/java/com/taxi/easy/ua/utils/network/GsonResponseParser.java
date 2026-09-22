package com.taxi.easy.ua.utils.network;

import com.google.gson.Gson;

/**
 * Retrofit+R8 may give {@link com.google.gson.internal.LinkedTreeMap} instead of the DTO
 * when generic signatures are stripped. Re-parse via Gson so callers do not ClassCast.
 */
public final class GsonResponseParser {

    private GsonResponseParser() {
    }

    public static <T> T as(Object body, Class<T> type) {
        if (body == null || type == null) {
            return null;
        }
        if (type.isInstance(body)) {
            return type.cast(body);
        }
        try {
            Gson gson = ApiGsonHelper.create();
            return gson.fromJson(gson.toJson(body), type);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
