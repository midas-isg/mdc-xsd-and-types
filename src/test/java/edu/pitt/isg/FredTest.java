package edu.pitt.isg;

import com.google.gson.Gson;
import edu.pitt.isg.mdc.dats2_2.Dataset;
import edu.pitt.isg.mdc.v1_0.DataService;
import edu.pitt.isg.mdc.v1_0.DataServiceAccessPointType;
import edu.pitt.isg.mdc.v1_0.DataServiceDescription;
import edu.pitt.isg.mdc.v1_0.Software;
import edu.pitt.isg.objectserializer.exceptions.DeserializationException;
import edu.pitt.isg.objectserializer.exceptions.SerializationException;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import javax.persistence.Convert;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jdl50 on 5/2/17.
 */
public class FredTest extends TestCase {
    @Test
    public void testHello() throws IOException, SerializationException, DeserializationException {
        String file = FileUtils.readFileToString(new File("./src/main/resources/software.json"));

        Converter converter = new Converter();

        List<Software> softwareList = converter.convertToJava(file);
        List<String> invalidSoftwareList = new ArrayList<String>();

        for (Software sw : softwareList) {
            String xml = converter.convertToXml(sw);
            //System.out.println(xml);
            boolean isValid = converter.validate(xml);

            if(!isValid) {
                invalidSoftwareList.add(sw.getTitle());
            }
            //String json = converter.xmlToJson(xml);
            //System.out.println(json);
        }

        //System.out.println(invalidSoftwareList);

        /*DataService testDataService = new DataService();
        DataServiceDescription testDataServiceDescription = new DataServiceDescription();

        testDataServiceDescription.setAccessPointDescription("");
        testDataServiceDescription.setAccessPointType(DataServiceAccessPointType.fromValue("SOAP"));
        testDataServiceDescription.setAccessPointUrl("");

        testDataService.getDataServiceDescription().add(testDataServiceDescription);
        System.out.print(converter.xmlToJson(converter.convertToXml(testDataService)));*/
    }

    @Test
    public void testDats() throws IOException, SerializationException, DeserializationException {
        String file = FileUtils.readFileToString(new File("./src/main/resources/test-dats.json"));
        Converter converter = new Converter();

        Dataset dataset = converter.convertToJavaDataset(file);
    }
}
