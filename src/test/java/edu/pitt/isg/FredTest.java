package edu.pitt.isg;

import edu.pitt.isg.mdc.v1_0.Software;
import edu.pitt.isg.objectserializer.exceptions.DeserializationException;
import edu.pitt.isg.objectserializer.exceptions.SerializationException;
import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by jdl50 on 5/2/17.
 */
public class FredTest extends TestCase {
    @Test
    public void testHello() throws IOException, SerializationException, DeserializationException {
        String file = FileUtils.readFileToString(new File("./src/main/resources/fred.json"));
        Converter converter = new Converter();
        Object software = converter.convertToJava(file);
        Map<String, Software> myMap = (Map<String, Software>) software;
        assertEquals(myMap.get("aaa").getProduct(), "FRED (3 versions)");
        String xml = converter.convertToXml(myMap.get("aaa"));
        software = converter.convertFromXml(xml);
        assertEquals(myMap.get("aaa").getProduct(), "FRED (3 versions)");
        //assertTrue(converter.validate(xml));


    }
}
