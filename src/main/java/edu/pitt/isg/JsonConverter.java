package edu.pitt.isg;

import com.google.gson.Gson;

/**
 * Created by jdl50 on 5/23/17.
 */
public class JsonConverter<T> {
    public T convert(String json, T t) {
        Gson gson = new Gson();
        return (T) gson.fromJson(json, t.getClass());
    }
}
