package edu.pitt.isg.deserializers;


import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class CustomDeserializer<T> implements JsonDeserializer<T> {
    protected List<String> getJsonMembers(JsonElement json) {
        try {
            List<String> jsonMembers = null;
            if (json.isJsonObject()) {
                jsonMembers = ((JsonObject) json).entrySet()
                        .stream()
                        .map(i -> i.getKey())
                        .collect(Collectors.toCollection(ArrayList::new));
            } else if (json.isJsonArray()) {
                JsonArray jsonArray = json.getAsJsonArray();
                for (int j = 0; j < jsonArray.size(); j++) {
                    jsonMembers = (jsonArray.get(j).getAsJsonObject()).entrySet()
                            .stream()
                            .map(i -> i.getKey())
                            .collect(Collectors.toCollection(ArrayList::new));
                }

            }
            return jsonMembers;
        } catch (ClassCastException e) {
            System.out.println(e);
        }
        return null;
    }
}
