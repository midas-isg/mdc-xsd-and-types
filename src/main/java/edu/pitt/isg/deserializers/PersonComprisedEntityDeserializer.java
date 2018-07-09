package edu.pitt.isg.deserializers;

import com.google.gson.*;
import edu.pitt.isg.mdc.dats2_2.Organization;
import edu.pitt.isg.mdc.dats2_2.Person;
import edu.pitt.isg.mdc.dats2_2.PersonComprisedEntity;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PersonComprisedEntityDeserializer extends CustomDeserializer<PersonComprisedEntity> {
    Gson gson = new GsonBuilder().serializeNulls().enableComplexMapKeySerialization().create();

    @Override
    public PersonComprisedEntity deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        List<String> personQualifiers = Arrays.asList("fullName", "firstName", "lastName", "affiliations", "email");
        List<String> organizationQualifiers = Arrays.asList("name", "abbreviation", "location");

        List<String> jsonMembers = getJsonMembers(json);
        if (!Collections.disjoint(jsonMembers, organizationQualifiers)) {
            return gson.fromJson(json, Organization.class);
        } else if (!Collections.disjoint(jsonMembers, personQualifiers)) {
            return gson.fromJson(json, Person.class);
        }
        return null;
    }
}
