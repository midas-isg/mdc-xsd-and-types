package edu.pitt.isg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import edu.pitt.isg.deserializers.IsAboutDeserializer;
import edu.pitt.isg.deserializers.PersonComprisedEntityDeserializer;
import edu.pitt.isg.mdc.dats2_2.Dataset;
import edu.pitt.isg.mdc.dats2_2.IsAbout;
import edu.pitt.isg.mdc.dats2_2.PersonComprisedEntity;
import edu.pitt.isg.mdc.v1_0.*;
import edu.pitt.isg.objectserializer.XMLDeserializer;
import edu.pitt.isg.objectserializer.XMLSerializer;
import edu.pitt.isg.objectserializer.exceptions.DeserializationException;
import edu.pitt.isg.objectserializer.exceptions.SerializationException;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.openarchives.oai._2.OAIPMHtype;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXB;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.stream.IntStream;


/**
 * Created by jdl50 on 5/2/17.
 */
public class Converter {
    private static final String MDC_PACKAGE = "edu.pitt.isg.mdc.v1_0.";
    private static final String DATS_PACKAGE = "edu.pitt.isg.mdc.dats2_2.";
    private static final String[] DATS_CLASSES = {
            "Dataset",
            "DatasetWithOrganization",
            "DataStandard"
    };

    private static final HashMap<String, String> classTypeToJsonType;

    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RESET = "\u001B[0m";

    static {
        classTypeToJsonType = new HashMap<String, String>();
        classTypeToJsonType.put("PhylogeneticTreeConstructors", "Phylogenetic tree constructors");
        classTypeToJsonType.put("SyntheticEcosystemConstructors", "Synthetic ecosystem constructors");
        classTypeToJsonType.put("DiseaseTransmissionModel", "Disease transmission models");
        classTypeToJsonType.put("PathogenEvolutionModels", "Pathogen evolution models");
        classTypeToJsonType.put("DataFormatConverters", "Data-format converters");
        classTypeToJsonType.put("DataService", "Data services");
        classTypeToJsonType.put("DataVisualizers", "Data visualizers");
        classTypeToJsonType.put("ModelingPlatforms", "Modeling platforms");
        classTypeToJsonType.put("DiseaseTransmissionTreeEstimators", "Disease transmission tree estimators");
        classTypeToJsonType.put("DiseaseForecasters", "Disease forecasters");
        classTypeToJsonType.put("PopulationDynamicsModel", "Population dynamics models");
    }

    public List<Software> convertToJava(String str) {
        List<HashMap<String, Object>> softwareListFromJson = new Gson().fromJson(
                str, new TypeToken<List<HashMap<String, Object>>>() {
                }.getType()
        );

        List softwareList = new ArrayList<Software>();
        for (HashMap<String, Object> map : softwareListFromJson) {
            GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
            Gson gson = gsonBuilder.create();

            String subtype = (String) map.get("subtype");
            String representation = new Gson().toJson(map);

            boolean hasSubtype = true;
            Software sw = new Software();
            if (subtype.equals("Disease transmission models")) {
                sw = gson.fromJson(representation, DiseaseTransmissionModel.class);
            } else if (subtype.equals("Disease forecasters")) {
                sw = gson.fromJson(representation, DiseaseForecasters.class);
            } else if (subtype.equals("Pathogen evolution models")) {
                sw = gson.fromJson(representation, PathogenEvolutionModels.class);
            } else if (subtype.equals("Population dynamics models")) {
                sw = gson.fromJson(representation, PopulationDynamicsModel.class);
            } else if (subtype.equals("Synthetic ecosystem constructors")) {
                sw = gson.fromJson(representation, SyntheticEcosystemConstructors.class);
            } else if (subtype.equals("Disease transmission tree estimators")) {
                sw = gson.fromJson(representation, DiseaseTransmissionTreeEstimators.class);
            } else if (subtype.equals("Phylogenetic tree constructors")) {
                sw = gson.fromJson(representation, PhylogeneticTreeConstructors.class);
            } else if (subtype.equals("Data visualizers")) {
                sw = gson.fromJson(representation, DataVisualizers.class);
            } else if (subtype.equals("Modeling platforms")) {
                sw = gson.fromJson(representation, ModelingPlatforms.class);
            } else if (subtype.equals("Data-format converters")) {
                sw = gson.fromJson(representation, DataFormatConverters.class);
            } else if (subtype.equals("Data services")) {
                sw = gson.fromJson(representation, DataService.class);
            } else {
                hasSubtype = false;
            }

            if (hasSubtype) {
                softwareList.add(sw);
            }
        }

        return softwareList;
    }

    public Object convertFromJsonToClass(String json, Class clazz) {
        return new Gson().fromJson(json, clazz);
    }

    public Dataset convertToJavaDataset(String str) {
        Dataset dataset = new Gson().fromJson(str, Dataset.class);
        return dataset;
    }

    public boolean validate(String xml) throws MalformedURLException {
        URL schemaFile = getClass().getClassLoader().getResource("software.xsd");
        Source xmlFile = new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        SchemaFactory schemaFactory = SchemaFactory
                .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(schemaFile);
            Validator validator = schema.newValidator();

            validator.validate(xmlFile);
            //System.out.println(xmlFile.getSystemId() + " is valid");
        } catch (Exception e) {
            System.out.println(xmlFile.getSystemId() + " is NOT valid reason:" + e);
            System.out.println("\nXML is:\n" + xml);

            return false;
        }
        return true;
    }



    public JsonObject toJsonObject(Class clazz, Object object) {
        return toJsonObject(clazz, object, false);
    }

    public JsonObject toJsonObject(Class clazz, Object object, boolean stripHtml) {
        JsonObject jsonObject = null;
        String xmlString = null;
        try {
            xmlString = convertToXml(clazz, object);
            if (stripHtml)
                xmlString = xmlString.replaceAll("(?s)&lt;.*?&gt;", "");
        } catch (Exception e) {
            System.out.println("Error: " + e);
        }

        if (stripHtml)
            xmlString = xmlString.substring(0, xmlString.lastIndexOf('>') + 1);

        String jsonString = null;
        try {
            jsonString = xmlToJson(xmlString, !stripHtml);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        if (jsonString.length() > 0) {
            JsonParser parser = new JsonParser();
            jsonObject = parser.parse(jsonString).getAsJsonObject();
        }
        return jsonObject;
    }

    public String convertToXml(Class clazz, Object object) throws SerializationException {
        List<Class> classList = new ArrayList<>();
        classList.add(object.getClass());
        classList.add(clazz);
        XMLSerializer xmlSerializer = new XMLSerializer(classList);
        String xml = xmlSerializer.serializeObject(object);
        return xml;
    }

    enum CharacterIndexBehavior {
        GET_CHARACTER_INDEX_FROM_INDEX_COUNTING_WHITESPACE,
        SEEK_TO_CHARACTER_INDEX

    }

    private static int getOrFindCharactersGivenIndex(String string, int index, CharacterIndexBehavior characterIndexBehavior) {
        IntStream stream = string.chars();
        PrimitiveIterator.OfInt it = stream.iterator();
        int nonWhitespaceCharacterCount = 0;
        int characterCount = 0;
        while (it.hasNext()) {
            Integer cur = it.next();
            if (!Character.isWhitespace(cur)) nonWhitespaceCharacterCount++;
            characterCount++;

            if (characterIndexBehavior.equals(CharacterIndexBehavior.GET_CHARACTER_INDEX_FROM_INDEX_COUNTING_WHITESPACE) && characterCount == index)
                return nonWhitespaceCharacterCount;
            else if (characterIndexBehavior.equals(CharacterIndexBehavior.SEEK_TO_CHARACTER_INDEX) && nonWhitespaceCharacterCount == index)
                return characterCount;
        }
        return -1;

    }


    public static void printHelpfulJsonError(String json, JsonSyntaxException syntaxException, MalformedJsonException malformedException) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(new JsonParser().parse(json));

        Exception e = syntaxException != null ? syntaxException : malformedException;
        String errStr = e.getMessage();
        Integer begin = errStr.indexOf("1 column ") + 9;
        Integer end = errStr.indexOf(" path $");
        if(begin < 0 || end < 0){
            System.out.println("Error parsing json error messages:" + errStr);
        } else {
            Integer errorPos = Integer.valueOf(errStr.substring(begin, end));

            int characterIndexOfError = getOrFindCharactersGivenIndex(json, errorPos, CharacterIndexBehavior.GET_CHARACTER_INDEX_FROM_INDEX_COUNTING_WHITESPACE);
            int indexOfErrorInPretty = getOrFindCharactersGivenIndex(prettyJson, characterIndexOfError, CharacterIndexBehavior.SEEK_TO_CHARACTER_INDEX);

            System.out.println(prettyJson.substring(1, indexOfErrorInPretty) + ANSI_CYAN + "(!!!-- " + e.getMessage() + "--!!!)\n" + prettyJson.substring(indexOfErrorInPretty, prettyJson.length()) + ANSI_RESET);
        }
    }

    public Object fromJson(String json, Class clazz) {
        try {
            Gson gson = new GsonBuilder().serializeNulls().enableComplexMapKeySerialization().registerTypeHierarchyAdapter(PersonComprisedEntity.class, new PersonComprisedEntityDeserializer()).registerTypeHierarchyAdapter(IsAbout.class, new IsAboutDeserializer()).create();
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            printHelpfulJsonError(json, e, null);
            throw e;
        }
    }

    public String convertToXml(OAIPMHtype oaipmHtype) throws JsonProcessingException, SerializationException {
        List<Class> classList = new ArrayList<>();
        try {
            classList.add(OAIPMHtype.class);
        }
        catch (Exception e){
            classList.add(org.openarchives.oai._2_0.oai_dc.OaiDcType.class);
        }
        classList.add(org.openarchives.oai._2_0.oai_dc.ObjectFactory.class);
        classList.add(org.purl.dc.elements._1.ObjectFactory.class);
        XMLSerializer xmlSerializer = new XMLSerializer(classList);
        String xml = xmlSerializer.serializeObject(oaipmHtype);
        //System.out.println(xml);
        return xml;

    }

    public Software convertFromXml(String xml) throws DeserializationException {
        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        Software software = xmlDeserializer.getObjectFromMessage(xml, Software.class);
        return software;
    }

    public String xmlToJson(String xml, boolean keepHtml) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String className = doc.getDocumentElement().getTagName();

        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        //Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        Gson gson = new GsonBuilder().create();
        String packageNamespace = MDC_PACKAGE;
        String json = "";
        boolean packageFound = false;

        for (int i = 0; i < DATS_CLASSES.length; i++) {
            if (className.equals(DATS_CLASSES[i])) {
                packageNamespace = DATS_PACKAGE;
                packageFound = true;
                break;
            }
        }

        /*String output;

        Dataset dataset = new Dataset();
        Creator creator = new Creator();
        Person person = new Person();
        person.setFirstName("Adam");
        creator.setPerson(person);
        dataset.getCreators().add(creator);
        JAXB.marshal(dataset, System.out);*/

        Object item = JAXB.unmarshal(new StringReader(xml), Class.forName(packageNamespace + className));

        //Object item = xmlDeserializer.getObjectFromMessage(xml, Class.forName(packageNamespace + className));
        JsonObject jsonObject = gson.toJsonTree((Class.forName(packageNamespace + className)).cast(item)).getAsJsonObject();

        if (!packageFound) {
            jsonObject.addProperty("subtype", classTypeToJsonType.get(className));
        }

        jsonObject.addProperty("class", packageNamespace + className);
        json = jsonObject.toString();

        if (keepHtml)
            return json;
        else {

            org.jsoup.nodes.Document jsonDocument =
                    Jsoup.parse(json, "", Parser.xmlParser());
            String parsedJson = jsonDocument.text();

            //TODO: Properly decode URL formats
            //json.replace("&lt;", "<").replace("&gt;", ">");

            return parsedJson;
        }
    }

}
