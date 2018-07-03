package edu.pitt.isg;

import com.google.gson.*;
import edu.pitt.isg.mdc.dats2_2.Organization;
import edu.pitt.isg.mdc.dats2_2.Person;
import edu.pitt.isg.mdc.dats2_2.PersonComprisedEntity;

import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class PersonComprisedEntityDeserializer implements JsonDeserializer<PersonComprisedEntity> {
    Gson gson = new GsonBuilder().serializeNulls().enableComplexMapKeySerialization().create();


    @Override
    public PersonComprisedEntity deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

        List<String> personQualifiers = Arrays.asList("fullName", "firstName", "lastName", "affiliations", "email");
        List<String> organizationQualifiers = Arrays.asList("name", "abbreviation", "location");
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

            if (!Collections.disjoint(jsonMembers, organizationQualifiers)) {
                return gson.fromJson(json, Organization.class);
            } else if (!Collections.disjoint(jsonMembers, personQualifiers)) {
                return gson.fromJson(json, Person.class);
            }
        } catch (ClassCastException e) {
            System.out.println(e);
        }

        return null;
    }
}
