package edu.pitt.isg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.pitt.isg.mdc.dats2_2.*;
import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.model.CkanDataset;
import eu.trentorise.opendata.jackan.model.CkanResource;
import eu.trentorise.opendata.jackan.model.CkanTag;
import org.apache.commons.lang.WordUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


public class CkanToDatsConverter {
    private static HashMap<String, String> diseaseMap;
    private static String APIKEY = "f060b29e-fc9a-4dcd-b3be-9741466dbc4a";

    public static void main(String[] args) {

        CkanClient ckanClient = new CkanClient("http://catalog.data.gov/");
        CkanDataset dataset = ckanClient.getDataset("02b5e413-d746-43ee-bd52-eac4e33ecb41");
        System.out.println(dataset.getNotes());
        convertCkanToDats(dataset);
    }

    public static Dataset convertCkanToDats(CkanDataset ckanDataset) {
        Dataset dataset = new Dataset();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        dataset.setTitle(ckanDataset.getTitle());
        dataset.setDescription(ckanDataset.getNotes());

        //Set Creator
        Person person = new Person();
        person.setEmail(ckanDataset.getMaintainerEmail());
        person.setFirstName(ckanDataset.getMaintainer());
        dataset.getCreators().add(person);

        //Set Identifier
        Identifier identifier = new Identifier();
        identifier.setIdentifierSource("https://data.cdc.gov");
        identifier.setIdentifier(ckanDataset.getExtrasAsHashMap().get("identifier"));
        dataset.setIdentifier(identifier);

        //Set Produced By
        Study study = new Study();
        study.setName(ckanDataset.getExtrasAsHashMap().get("publisher"));
        java.util.Date metaDataDate = new java.util.Date(ckanDataset.getMetadataCreated().getTime());
        Date startDate = new Date();
        Date endDate = new Date();
        Annotation type = new Annotation();
        type.setValue("created");
        type.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001882");
        startDate.setDate(sdf.format(metaDataDate));
        startDate.setType(type);

        if (ckanDataset.getExtrasAsHashMap().containsKey("modified") && ckanDataset.getExtrasAsHashMap().get("modified") != null) {
            endDate.setDate(ckanDataset.getExtrasAsHashMap().get("modified"));
            Annotation endDateType = new Annotation();
            endDateType.setValue("modified");
            endDateType.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001874");
            endDate.setType(endDateType);
        } else {
            endDate.setDate(sdf.format(metaDataDate));
            endDate.setType(type);
        }
        study.setStartDate(startDate);
        study.setEndDate(endDate);

        dataset.setProducedBy(study);

        //Set Distributions
        List<CkanResource> resources = ckanDataset.getResources();
        for (CkanResource resource : resources) {
            Distribution distribution = new Distribution();

            Identifier distributionIdentifier = new Identifier();
            distributionIdentifier.setIdentifier(resource.getId());
            distributionIdentifier.setIdentifierSource("https://catalog.data.gov");
            distribution.setIdentifier(distributionIdentifier);

            Access access = new Access();
            access.setAccessURL(resource.getUrl());
            access.setLandingPage(ckanDataset.getExtrasAsHashMap().get("landingPage"));
            distribution.setAccess(access);

            Date date = new Date();
            Annotation distributionType = new Annotation();
            java.util.Date datetime = new java.util.Date(resource.getCreated().getTime());
            date.setDate(sdf.format(datetime));
            distributionType.setValue("created");
            distributionType.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001882");
            date.setType(distributionType);
            distribution.getDates().add(date);

            dataset.getDistributions().add(distribution);
        }

        //Set isAbout
        List<CkanTag> tags = ckanDataset.getTags();
        for (CkanTag tag : tags) {
            String diseaseInfo[] = lookupDiseaseInformation(tag.getDisplayName());
            if (diseaseInfo != null) {

                BiologicalEntity entity = new BiologicalEntity();

                entity.setName(diseaseInfo[0]);
                Identifier tagIdentifier = new Identifier();
                tagIdentifier.setIdentifier(diseaseInfo[1]);
                tagIdentifier.setIdentifierSource("https://biosharing.org/bsg-s000098");
                entity.setIdentifier(tagIdentifier);

                dataset.getIsAbout().add(entity);
            }
        }

        writeDiseaseFile();

        return dataset;
    }

    public static String[] lookupDiseaseInformation(String diseaseName) {
        String diseaseInfo[] = new String[2];
        //Normalize name
        diseaseName = diseaseName.replace("-", " ");
        diseaseName = WordUtils.capitalize(diseaseName);

        if (diseaseMap == null) {
            diseaseMap = new HashMap<>();
            try {
                InputStream file = CkanToDatsConverter.class.getClassLoader().getResourceAsStream("diseases.txt");
                Scanner diseaseScan = new Scanner(file);
                while (diseaseScan.hasNextLine()) {
                    String diseaseInFile = diseaseScan.nextLine();
                    String[] diseaseArray = diseaseInFile.split(",");
                    if (diseaseArray.length > 1) {
                        diseaseMap.put(diseaseArray[0], diseaseArray[1]);
                    }
                }
                diseaseScan.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //If it is a disease
        if (diseaseMap.containsKey(diseaseName)) {
            diseaseInfo[0] = diseaseName;
            diseaseInfo[1] = diseaseMap.get(diseaseName);
        } else {
            try {
                String snomed = lookupSNOMED(diseaseName);
                if (snomed != "") {
                    diseaseMap.put(diseaseName, snomed);
                    diseaseInfo[0] = diseaseName;
                    diseaseInfo[1] = snomed;
                } else {
                    diseaseInfo = null;
                }
            } catch (IOException e) {
                diseaseInfo = null;
                e.printStackTrace();
            }
        }

        return diseaseInfo;
    }

    public static String lookupSNOMED(String diseaseName) throws MalformedURLException, IOException {
        URL url = new URL("http://data.bioontology.org/search?q=" + URLEncoder.encode(diseaseName, "UTF-8") + "&apikey=" + APIKEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() != 200) {
            System.out.println("Failed : HTTP error code : " + connection.getResponseCode() + ".  Searching with: "  + diseaseName);
//            throw new RuntimeException("Failed : HTTP error code : "
//                    + connection.getResponseCode());
            return "";
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(
                (connection.getInputStream())));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = br.readLine()) != null) {
            response.append(inputLine);
        }

        connection.disconnect();

        JsonObject obj = new JsonParser().parse(response.toString()).getAsJsonObject();

        //iterate over "collection" until we find @id with SNOMEDCT in it
        JsonArray collection = obj.get("collection").getAsJsonArray();
        String snomed = "";
        for (JsonElement member : collection) {
            JsonObject memberObject = member.getAsJsonObject();
            String id = memberObject.get("@id").getAsString();
            if (id.contains("SNOMEDCT")) {
                snomed = id.substring(id.lastIndexOf('/') + 1);
                break;
            }
        }
        if(snomed.equals("")) {
            System.out.println("Failed to find SNOMED code for " + diseaseName);
        }
        return snomed;
    }

    private static void writeDiseaseFile() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> kvp : diseaseMap.entrySet()) {
            builder.append(kvp.getKey());
            builder.append(",");
            builder.append(kvp.getValue());
            builder.append("\r\n");
        }

        String content = builder.toString().trim();
        try {
            PrintWriter writer = new PrintWriter(new File(CkanToDatsConverter.class.getClassLoader().getResource("diseases.txt").getPath()));
            writer.println(content);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
