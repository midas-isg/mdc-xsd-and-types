package edu.pitt.isg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.pitt.isg.mdc.dats2_2.Dataset;
import edu.pitt.isg.mdc.v1_0.*;
import edu.pitt.isg.objectserializer.XMLDeserializer;
import edu.pitt.isg.objectserializer.XMLSerializer;
import edu.pitt.isg.objectserializer.exceptions.DeserializationException;
import edu.pitt.isg.objectserializer.exceptions.SerializationException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;


/**
 * Created by jdl50 on 5/2/17.
 */
public class Converter {
    private static final String MDC_PACKAGE = "edu.pitt.isg.mdc.v1_0.";
    private static final String DATS_PACKAGE = "edu.pitt.isg.mdc.dats2_2.";
    private static final String [] DATS_CLASSES = {
        "Dataset",
        "DataStandard"
    };

    public List<Software> convertToJava(String str) {
        List<HashMap<String,Object>> softwareListFromJson = new Gson().fromJson(
                str, new TypeToken<List<HashMap<String,Object>>>() {}.getType()
        );

        List softwareList = new ArrayList<Software>();
        for ( HashMap<String,Object> map : softwareListFromJson ) {
            GsonBuilder gsonBuilder = new GsonBuilder().serializeNulls();
            Gson gson = gsonBuilder.create();

            String subtype = (String) map.get("subtype");
            String representation = new Gson().toJson(map);

            boolean hasSubtype = true;
            Software sw = new Software();
            if(subtype.equals("Disease transmission models")) {
                sw = gson.fromJson(representation, DiseaseTransmissionModel.class);
            } else if(subtype.equals("Disease forecasters")) {
                sw = gson.fromJson(representation, DiseaseForecasters.class);
            } else if(subtype.equals("Pathogen evolution models")) {
                sw = gson.fromJson(representation, PathogenEvolutionModels.class);
            } else if(subtype.equals("Population dynamics models")) {
                sw = gson.fromJson(representation, PopulationDynamicsModel.class);
            } else if(subtype.equals("Synthetic ecosystem constructors")) {
                sw = gson.fromJson(representation, SyntheticEcosystemConstructors.class);
            } else if(subtype.equals("Disease transmission tree estimators")) {
                sw = gson.fromJson(representation, DiseaseTransmissionTreeEstimators.class);
            } else if(subtype.equals("Phylogenetic tree constructors")) {
                sw = gson.fromJson(representation, PhylogeneticTreeConstructors.class);
            } else if(subtype.equals("Data visualizers")) {
                sw = gson.fromJson(representation, DataVisualizers.class);
            } else if(subtype.equals("Modeling platforms")) {
                sw = gson.fromJson(representation, ModelingPlatforms.class);
            } else if(subtype.equals("Data-format converters")) {
                sw = gson.fromJson(representation, DataFormatConverters.class);
            } else if(subtype.equals("Data services")) {
                sw = gson.fromJson(representation, DataService.class);
            }else {
                hasSubtype = false;
            }

            if(hasSubtype) {
                softwareList.add(sw);
            }
        }

        return softwareList;
    }

    public Dataset convertToJavaDataset(String str) {
        Dataset dataset = new Gson().fromJson(str, Dataset.class);
        return dataset;
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
            System.out.println("\nXML is:\n"+xml);

            return false;
        }
        return true;
    }


    public String convertToXml(Software software) throws JsonProcessingException, SerializationException {
        List<Class> classList = new ArrayList<>();
        classList.add(Software.class);
        XMLSerializer xmlSerializer = new XMLSerializer(classList);
        String xml = xmlSerializer.serializeObject(software);
        //System.out.println(xml);
        return xml;

    }

    public Software convertFromXml(String xml) throws DeserializationException {
        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        Software software = xmlDeserializer.getObjectFromMessage(xml, Software.class);
        return software;
    }

    public String xmlToJson(String xml) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        String className = doc.getDocumentElement().getTagName();

        XMLDeserializer xmlDeserializer = new XMLDeserializer();
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String packageNamespace = MDC_PACKAGE;
        String json = "";
        boolean packageFound = false;

        for(int i = 0; i < DATS_CLASSES.length; i++) {
            if(className.equals(DATS_CLASSES[i])) {
                packageNamespace = DATS_PACKAGE;
                packageFound = true;
                break;
            }
        }

        Object item = xmlDeserializer.getObjectFromMessage(xml, Class.forName(packageNamespace + className));
        json = gson.toJson((Class.forName(packageNamespace + className)).cast(item));
        //TODO: Properly decode URL formats
        //json.replace("&lt;", "<").replace("&gt;", ">");

        return json;
    }


}
