package edu.pitt.isg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.Gson;
import edu.pitt.isg.mdc.v1_0.Software;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by jdl50 on 5/2/17.
 */
public class Converter {

    public Software convertToJava(String str) {
        Software targetObject = new Gson().fromJson(str, Software.class);
        return targetObject;



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
            System.out.println(xmlFile.getSystemId() + " is valid");
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


}
