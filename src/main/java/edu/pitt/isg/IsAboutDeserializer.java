package edu.pitt.isg;

import com.google.gson.*;
import edu.pitt.isg.mdc.dats2_2.Annotation;
import edu.pitt.isg.mdc.dats2_2.BiologicalEntity;
import edu.pitt.isg.mdc.dats2_2.IsAbout;

import java.lang.reflect.Type;

public class IsAboutDeserializer implements JsonDeserializer<IsAbout> {
    Gson gson = new GsonBuilder().serializeNulls().enableComplexMapKeySerialization().create();
    @Override
    public IsAbout deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {



            if (json.toString().contains("\"name\":")) {
                return gson.fromJson(json, BiologicalEntity.class);
            } else if (json.toString().contains("\"value\":")) {
                return gson.fromJson(json, Annotation.class);
            }


        return null;
    }
}
