package edu.pitt.isg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.pitt.isg.mdc.v1_0.*;
import edu.pitt.isg.objectserializer.XMLDeserializer;
import edu.pitt.isg.objectserializer.XMLSerializer;
import edu.pitt.isg.objectserializer.exceptions.DeserializationException;
import edu.pitt.isg.objectserializer.exceptions.SerializationException;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by jdl50 on 5/2/17.
 */
public class Converter {

    public List<Software> convertToJava(String str) {
        Map<String, Object> javaMap = new Gson().fromJson(
                str, new TypeToken<HashMap<String, HashMap<String, Object>>>() {}.getType()
        );

        List softwareList = new ArrayList<Software>();
        for ( String key : javaMap.keySet() ) {
            Map<String, Object> innerMapping = ((Map) javaMap.get(key));
            String subtype = (String) innerMapping.get("subtype");
            String representation = new Gson().toJson(javaMap.get(key));

            boolean hasSubtype = true;
            Software sw = new Software();
            if(subtype.equals("Disease transmission models")) {
                sw = new Gson().fromJson(representation, DiseaseTransmissionModel.class);
            } else if(subtype.equals("Disease forecasters")) {
                sw = new Gson().fromJson(representation, DiseaseForecasters.class);
            } else if(subtype.equals("Pathogen evolution models")) {
                sw = new Gson().fromJson(representation, PathogenEvolutionModels.class);
            } else if(subtype.equals("Population dynamics models")) {
                sw = new Gson().fromJson(representation, PopulationDynamicsModel.class);
            } else if(subtype.equals("Synthetic ecosystem constructor")) {
                sw = new Gson().fromJson(representation, SyntheticEcosystemConstructors.class);
            } else if(subtype.equals("Disease transmission tree estimators")) {
                sw = new Gson().fromJson(representation, DiseaseTransmissionTreeEstimators.class);
            } else if(subtype.equals("Phylogenetic tree constructors")) {
                sw = new Gson().fromJson(representation, PhylogeneticTreeConstructors.class);
            } else if(subtype.equals("Data visualizers")) {
                sw = new Gson().fromJson(representation, DataVisualizers.class);
            } else if(subtype.equals("Modeling platforms")) {
                sw = new Gson().fromJson(representation, ModelingPlatforms.class);
            } else if(subtype.equals("Data-format converters")) {
                sw = new Gson().fromJson(representation, DataFormatConverters.class);
            } else {
                hasSubtype = false;
            }

            if(hasSubtype) {
                sw.setTitle(key);
                softwareList.add(sw);
            }
        }

        return softwareList;
    }

    public boolean validate(String xml) throws MalformedURLException {
        URL schemaFile = getClass().getClassLoader().getResource("software.xsd");
// or File schemaFile = new File("/location/to/xsd"); // etc.
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
            return false;
        }
        return true;
    }


    String convertToXml(Software software) throws JsonProcessingException, SerializationException {
        List<Class> classList = new ArrayList<>();
        classList.add(Software.class);
        XMLSerializer xmlSerializer = new XMLSerializer(classList);
        String xml = xmlSerializer.serializeObject(software);
        System.out.println(xml);
        return xml;

    }

    Software convertFromXml(String xml) throws DeserializationException {
        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        Software software = xmlDeserializer.getObjectFromMessage(xml, Software.class);
        return software;
    }

    String xmlToJson(String xml) throws DeserializationException {
        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        Software software = xmlDeserializer.getObjectFromMessage(xml, Software.class);

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String json = gson.toJson(software);
        json.replace("&lt;", "<").replace("&gt;", ">");
        return gson.toJson(software);
    }


}
