package edu.pitt.isg.deserializers;

import com.google.gson.*;
import edu.pitt.isg.mdc.dats2_2.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class IsAboutDeserializer extends CustomDeserializer<IsAbout> {
    Gson gson = new GsonBuilder().serializeNulls().enableComplexMapKeySerialization().create();
    @Override
    public IsAbout deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        List<String> annotationQualifiers = Arrays.asList("value", "valueIRI");
        List<String> biologicalEntityQualifiers = Arrays.asList("name", "description", "identifier", "alternateIdentifiers");

        List<String> jsonMembers = getJsonMembers(json);

        if (!Collections.disjoint(jsonMembers, annotationQualifiers)) {
            return gson.fromJson(json, Annotation.class);
        } else if (!Collections.disjoint(jsonMembers, biologicalEntityQualifiers)) {
            return gson.fromJson(json, BiologicalEntity.class);
        }
        return null;
    }
}
