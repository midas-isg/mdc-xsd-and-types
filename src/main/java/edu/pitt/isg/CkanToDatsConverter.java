package edu.pitt.isg;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.pitt.isg.mdc.dats2_2.*;
import edu.pitt.isg.mdc.dats2_2.Date;
import eu.trentorise.opendata.jackan.CkanClient;
import eu.trentorise.opendata.jackan.CkanQuery;
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
import java.util.*;


public class CkanToDatsConverter {
    private static HashMap<String, String> snomedMap;
    private static HashMap<String, String> ncbiMap;
    private static String APIKEY = "f060b29e-fc9a-4dcd-b3be-9741466dbc4a";
    private static Set<String> failures;

    public static void main(String[] args) {
        CkanClient ckanClient = new CkanClient("http://catalog.data.gov/");
        CkanQuery query = CkanQuery.filter().byTagNames("nndss");
//        CkanQuery query = CkanQuery.filter().byTagNames("vaccination");

        List<CkanDataset> filteredDatasets = ckanClient.searchDatasets(query, 100, 0).getResults();
        List<DatasetWithOrganization> convertedDatasets = new ArrayList<>();
        for (CkanDataset dataset : filteredDatasets) {
            DatasetWithOrganization convertedDataset = convertCkanToDats(dataset);
            if(convertedDataset != null) {
                convertedDatasets.add(convertedDataset);
            }

            System.out.println(dataset.getId());
        }
//        CkanDataset dataset = ckanClient.getDataset("02b5e413-d746-43ee-bd52-eac4e33ecb41");
//        System.out.println(dataset.getNotes());
//        convertCkanToDats(dataset);
        System.out.println("Done");
    }

    public static DatasetWithOrganization convertCkanToDats(CkanDataset ckanDataset) {
        DatasetWithOrganization dataset = new DatasetWithOrganization();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        //Set Distributions
        List<CkanResource> resources = ckanDataset.getResources();
        for (CkanResource resource : resources) {
            //If the resources contained in the ckan dataset are not csv, rdf, json, or xml then it is not the type we want in the MDC
            if(resource.getFormat().equals("CSV") || resource.getFormat().equals("RDF") || resource.getFormat().equals("JSON") || resource.getFormat().equals("XML")) {
                Distribution distribution = new Distribution();

                distribution.getFormats().add(resource.getFormat());
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
        }

        //if no distributions are added then the ckan dataset is not the format we want to convert, return null
        if(dataset.getDistributions().isEmpty()) {
            return null;
        }


        dataset.setTitle(ckanDataset.getTitle());
        dataset.setDescription(ckanDataset.getNotes());

        //Set Creator
        Organization organization = new Organization();
        organization.setName(ckanDataset.getExtrasAsHashMap().get("publisher"));
        dataset.getCreators().add(organization);

        //Set Identifier
        Identifier identifier = new Identifier();
        identifier.setIdentifierSource("https://data.cdc.gov");
        identifier.setIdentifier(ckanDataset.getExtrasAsHashMap().get("identifier"));
        dataset.setIdentifier(identifier);

        //Set Produced By
        Study study = new Study();
        study.setName(ckanDataset.getExtrasAsHashMap().get("publisher"));
        Date modifiedDate = new Date();
        Annotation type = new Annotation();
        type.setValue("modified");
        type.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001874");
        modifiedDate.setDate(ckanDataset.getExtrasAsHashMap().get("modified"));
        modifiedDate.setType(type);

//        if (ckanDataset.getExtrasAsHashMap().containsKey("modified") && ckanDataset.getExtrasAsHashMap().get("modified") != null) {
//            endDate.setDate(ckanDataset.getExtrasAsHashMap().get("modified"));
//            Annotation endDateType = new Annotation();
//            endDateType.setValue("modified");
//            endDateType.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001874");
//            endDate.setType(endDateType);
//        } else {
//            endDate.setDate(sdf.format(metaDataDate));
//            endDate.setType(type);
//        }
        study.setStartDate(modifiedDate);
//        study.setEndDate(endDate);

        dataset.setProducedBy(study);

        //Set isAbout
        List<CkanTag> tags = ckanDataset.getTags();
        for (CkanTag tag : tags) {
            String diseaseInfo[] = lookupIsAboutInformation(tag.getDisplayName());
            if (diseaseInfo != null) {

                BiologicalEntity entity = new BiologicalEntity();

                entity.setName(diseaseInfo[0]);
                Identifier tagIdentifier = new Identifier();
                tagIdentifier.setIdentifier(diseaseInfo[1]);
                tagIdentifier.setIdentifierSource(diseaseInfo[2]);
                entity.setIdentifier(tagIdentifier);
                dataset.getIsAbout().add(entity);
            }
        }

        writeDiseaseFile();

        return dataset;
    }

    public static String[] lookupIsAboutInformation(String diseaseName) {
        //Disease name, snomed or ncbi code, identifier source
        String diseaseInfo[] = new String[3];
        //Normalize name
        diseaseName = diseaseName.replace("-", " ");
        diseaseName = WordUtils.capitalize(diseaseName);
        if(diseaseName.equals("Hansen039s Disease")) {
            diseaseName = "Hansen Disease";
        }

        if (snomedMap == null) {
            snomedMap = new HashMap<>();
            failures = new HashSet<>();
            populateNcbiMap();
            try {
                InputStream file = CkanToDatsConverter.class.getClassLoader().getResourceAsStream("diseases.txt");
                Scanner diseaseScan = new Scanner(file);
                while (diseaseScan.hasNextLine()) {
                    String diseaseInFile = diseaseScan.nextLine();
                    String[] diseaseArray = diseaseInFile.split(",");
                    if (diseaseArray.length > 1) {
                        snomedMap.put(diseaseArray[0], diseaseArray[1]);
                    }
                }
                diseaseScan.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //If it is a disease
        if (snomedMap.containsKey(diseaseName)) {
            diseaseInfo[0] = diseaseName;
            diseaseInfo[1] = snomedMap.get(diseaseName);
            diseaseInfo[2] = "https://biosharing.org/bsg-s000098";
        } else if (ncbiMap.containsKey(diseaseName)) {
            diseaseInfo[0] = diseaseName;
            diseaseInfo[1] = ncbiMap.get(diseaseName);
            diseaseInfo[2] = "https://biosharing.org/bsg-s000154";
        } else {
            try {
                String snomed = lookupSNOMED(diseaseName);
                if (snomed != "") {
                    snomedMap.put(diseaseName, snomed);
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
        if (failures.contains(diseaseName)) {
            return "";
        }
        URL url = new URL("http://data.bioontology.org/search?q=" + URLEncoder.encode(diseaseName, "UTF-8") + "&apikey=" + APIKEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() != 200) {
            System.out.println("Failed : HTTP error code : " + connection.getResponseCode() + ".  Searching with: " + diseaseName);
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
        if (snomed.equals("")) {
            System.out.println("Failed to find SNOMED code for " + diseaseName);
            failures.add(diseaseName);
        }
        return snomed;
    }

    private static void writeDiseaseFile() {
        //This writes to the diseases.txt file in target. Copy to diseases.txt in resources to update cache
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> kvp : snomedMap.entrySet()) {
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

    private static void populateNcbiMap() {
        ncbiMap = new HashMap<>();
        ncbiMap.put("Human", "9606");
    }
}
