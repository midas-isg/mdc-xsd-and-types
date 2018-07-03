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
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;


public class CkanToDatsConverter {
    private static HashMap<String, List<String>> snomedMap;
    private static HashMap<String, String> ncbiMap;
    private static String APIKEY = "f060b29e-fc9a-4dcd-b3be-9741466dbc4a";
    private static Set<String> failures;
    private static Set<String> blacklist;

    public class ConverterResult {
        public List<String> getDiseaseLookupLogMessages() {
            return diseaseLookupLogMessages;
        }

        public void setDiseaseLookupLogMessages(List<String> diseaseLookupLogMessages) {
            this.diseaseLookupLogMessages = diseaseLookupLogMessages;
        }

        public Object getDataset() {
            return dataset;
        }

        public void setDataset(Object dataset) {
            this.dataset = dataset;
        }

        private List<String> diseaseLookupLogMessages;
        private Object dataset;
    }

    public static void main(String[] args) {
        CkanToDatsConverter converter = new CkanToDatsConverter();
        converter.manuallyAddToDiseaseCache("Spotted Fever Rickettsiosis", "186772009");

        CkanClient ckanClient = new CkanClient("http://catalog.data.gov/");
        CkanQuery query = CkanQuery.filter().byTagNames("nndss");
//        CkanQuery query = CkanQuery.filter().byTagNames("vaccination");

//        List<CkanDataset> filteredDatasets = ckanClient.searchDatasets(query, 100, 0).getResults();
//        List<DatasetWithOrganization> convertedDatasets = new ArrayList<>();
//        for (CkanDataset dataset : filteredDatasets) {
//            ConverterResult convertedDataset = new CkanToDatsConverter().convertCkanToDats(dataset, ckanClient.getCatalogUrl());
//            if (convertedDataset != null) {
//                convertedDatasets.add((DatasetWithOrganization) convertedDataset.getDataset());
//            }
//        }
//
        ConverterResult result = new CkanToDatsConverter().convertCkanToDats( ckanClient.getDataset("70dca0aa-1598-4925-af8c-5464fdb212b1"), ckanClient.getCatalogUrl());

        System.out.println("Done");
    }

    public ConverterResult convertCkanToDats(CkanDataset ckanDataset, String catalogUrl) {
        ConverterResult result = new ConverterResult();
        DatasetWithOrganization dataset = new DatasetWithOrganization();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        //Set Distributions
        List<CkanResource> resources = ckanDataset.getResources();
        for (CkanResource resource : resources) {
            //If the resources contained in the ckan dataset are not csv, rdf, json, or xml then it is not the type we want in the MDC
            if (resource.getFormat().equals("CSV") || resource.getFormat().equals("RDF") || resource.getFormat().equals("JSON") || resource.getFormat().equals("XML")) {
                Distribution distribution = new Distribution();

                distribution.getFormats().add(resource.getFormat());
                Identifier distributionIdentifier = new Identifier();
                distributionIdentifier.setIdentifier(resource.getId());
                distributionIdentifier.setIdentifierSource(catalogUrl);
                distribution.setIdentifier(distributionIdentifier);

                Access access = new Access();
                access.setAccessURL(resource.getUrl());
                access.setLandingPage(ckanDataset.getExtrasAsHashMap().get("landingPage"));
                distribution.setAccess(access);

                Date date = new Date();
                Annotation distributionType = new Annotation();
                java.util.Date datetime = new java.util.Date(resource.getCreated().getTime());
                date.setDate(sdf.format(datetime));
                distributionType.setValue("creation");
                distributionType.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001882");
                date.setType(distributionType);
                distribution.getDates().add(date);

                dataset.getDistributions().add(distribution);
            }
        }

        //if no distributions are added then the ckan dataset is not the format we want to convert, return null
        if (dataset.getDistributions().isEmpty()) {
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

        study.setStartDate(modifiedDate);
        dataset.setProducedBy(study);

        //Set extraProperties
        CategoryValuePair createdMetaData = new CategoryValuePair();
        createdMetaData.setCategory("CKAN metadata_created");
        Annotation createdMetaDataAnnotation = new Annotation();
        createdMetaDataAnnotation.setValue(sdf.format(ckanDataset.getMetadataCreated()));
        createdMetaDataAnnotation.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001882");
        createdMetaData.getValues().add(createdMetaDataAnnotation);
        dataset.getExtraProperties().add(createdMetaData);

        if (ckanDataset.getMetadataModified() != null) {
            CategoryValuePair modifiedMetaData = new CategoryValuePair();
            modifiedMetaData.setCategory("CKAN metadata_modified");
            Annotation modifiedMetaDataAnnotation = new Annotation();
            modifiedMetaDataAnnotation.setValue(sdf.format(ckanDataset.getMetadataModified()));
            modifiedMetaDataAnnotation.setValueIRI("http://purl.obolibrary.org/obo/GENEPIO_0001874");
            modifiedMetaData.getValues().add(modifiedMetaDataAnnotation);
            dataset.getExtraProperties().add(modifiedMetaData);
        }

        CategoryValuePair revisionMetaData = new CategoryValuePair();
        revisionMetaData.setCategory("CKAN revision_id");
        Annotation revisionAnnotation = new Annotation();
        revisionAnnotation.setValue(ckanDataset.getRevisionId());
        revisionMetaData.getValues().add(revisionAnnotation);
        dataset.getExtraProperties().add(revisionMetaData);


        //Set isAbout
        List<CkanTag> tags = ckanDataset.getTags();
        result.diseaseLookupLogMessages = new ArrayList<>();

        for (CkanTag tag : tags) {
            String diseaseInfo[] = lookupIsAboutInformation(tag.getDisplayName(), result.diseaseLookupLogMessages);
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

        result.setDataset(dataset);
        return result;
    }

    public String[] lookupIsAboutInformation(String diseaseName, List<String> diseaseLookupLogMessages) {
        //Disease name, snomed or ncbi code, identifier source
        String diseaseInfo[] = new String[3];
        //Normalize name
        diseaseName = diseaseName.replace("-", " ");
        diseaseName = WordUtils.capitalize(diseaseName);
        if (diseaseName.equals("Hansen039s Disease")) {
            diseaseName = "Hansen Disease";
        }

        if (snomedMap == null) {
            snomedMap = new HashMap<>();
            failures = new HashSet<>();
            populateBlacklist();
            populateNcbiMap();
            try {
                InputStream file = CkanToDatsConverter.class.getClassLoader().getResourceAsStream("diseases.csv");
                Scanner diseaseScan = new Scanner(file, "UTF-8");
                while (diseaseScan.hasNextLine()) {
                    String diseaseInFile = diseaseScan.nextLine();
                    String[] diseaseArray = diseaseInFile.split(",");
                    if (diseaseArray.length > 1) {
                        List<String> list = new ArrayList<>();
                        list.add(diseaseArray[1]);
                        list.add(diseaseArray[2]);
                        snomedMap.put(diseaseArray[0].trim(), list);
                    }
                }
                diseaseScan.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //If it is a disease
        if (blacklist.contains(diseaseName)) {
            //skip
            diseaseInfo = null;
        }
        else if (snomedMap.containsKey(diseaseName)) {
            diseaseInfo[0] = diseaseName;
            List<String> diseaseInfoList = snomedMap.get(diseaseName);
            diseaseInfo[1] = diseaseInfoList.get(0);
            diseaseLookupLogMessages.add(diseaseInfoList.get(1));
            diseaseInfo[2] = "https://biosharing.org/bsg-s000098";
        } else if (ncbiMap.containsKey(diseaseName)) {
            diseaseInfo[0] = diseaseName;
            diseaseInfo[1] = ncbiMap.get(diseaseName);
            diseaseInfo[2] = "https://biosharing.org/bsg-s000154";
        } else {
            try {
                String snomed = lookupSNOMED(diseaseName, diseaseLookupLogMessages);
                if (snomed != "") {
                    List<String> diseaseInfoList = new ArrayList<>();
                    diseaseInfoList.add(snomed);
                    diseaseInfoList.add(diseaseLookupLogMessages.get(diseaseLookupLogMessages.size()-1));
                    snomedMap.put(diseaseName, diseaseInfoList);
                    diseaseInfo[0] = diseaseName;
                    diseaseInfo[1] = snomed;
                    diseaseInfo[2] = "https://biosharing.org/bsg-s000098";
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

    public String lookupSNOMED(String diseaseName, List<String> diseaseLookupLogMessages) throws MalformedURLException, IOException {
        if (failures.contains(diseaseName)) {
            return "";
        }
        URL url = new URL("http://data.bioontology.org/search?q=" + URLEncoder.encode(diseaseName, "UTF-8") + "&apikey=" + APIKEY);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        if (connection.getResponseCode() != 200) {
            diseaseLookupLogMessages.add("Failed : HTTP error code : " + connection.getResponseCode() + ".  Searching with: " + diseaseName);
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
                StringBuilder builder = new StringBuilder();
                if (memberObject.get("synonym") != null) {
                    for (JsonElement synonym : memberObject.get("synonym").getAsJsonArray()) {
                        if (builder.length() > 0) {
                            builder.append("; ");
                        }
                        builder.append(synonym.getAsString());
                    }
                }
                snomed = id.substring(id.lastIndexOf('/') + 1);
                System.out.println("Searching for: " + diseaseName + ". Found: " + memberObject.get("prefLabel").getAsString() + ". Synonyms are: " + builder.toString());
                diseaseLookupLogMessages.add("Searching for: " + diseaseName + ". Found: " + memberObject.get("prefLabel").getAsString() + ". Synonyms are: " + builder.toString());

                break;
            }
        }
        if (snomed.equals("")) {
            System.out.println("Failed to find SNOMED code for " + diseaseName);
            diseaseLookupLogMessages.add("Failed to find SNOMED code for " + diseaseName);
            failures.add(diseaseName);
        }
        return snomed;
    }

    private void writeDiseaseFile() {
        //This writes to the diseases.csv file in target. Copy to diseases.csv in resources to update cache
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<String>> kvp : snomedMap.entrySet()) {
            builder.append(kvp.getKey());
            builder.append(",");
            builder.append(kvp.getValue().get(0));
            builder.append(",");
            builder.append(kvp.getValue().get(1));
            builder.append("\r\n");
        }

        String content = builder.toString().trim();
        try {
            PrintWriter writer = new PrintWriter(new File(CkanToDatsConverter.class.getClassLoader().getResource("diseases.csv").getPath()));
            writer.println(content);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void manuallyAddToDiseaseCache(String diseaseName, String snomedCode){
        try {
            URL url = new URL("http://data.bioontology.org/search?q=" + URLEncoder.encode(snomedCode, "UTF-8") + "&apikey=" + APIKEY);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() != 200) {
                System.out.println("Failed : HTTP error code : " + connection.getResponseCode() + ".  Searching with: " + diseaseName);
                return;
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
            JsonObject memberObject = collection.get(0).getAsJsonObject();
            StringBuilder builder = new StringBuilder();
            if (memberObject.get("synonym") != null) {
                for (JsonElement synonym : memberObject.get("synonym").getAsJsonArray()) {
                    if (builder.length() > 0) {
                        builder.append("; ");
                    }
                    builder.append(synonym.getAsString());
                }
            }

            BufferedWriter bw  = new BufferedWriter(new FileWriter(CkanToDatsConverter.class.getClassLoader().getResource("diseases.csv").getPath(), true));
            bw.write(diseaseName + "," + snomedCode + ",Searching for: " + diseaseName + ". Found: " + memberObject.get("prefLabel").getAsString() + ". Synonyms are: " + builder.toString());
            bw.newLine();
            bw.flush();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void populateNcbiMap() {
        ncbiMap = new HashMap<>();
        ncbiMap.put("Human", "9606");
    }

    private void populateBlacklist() {
        blacklist = new HashSet<>();
        blacklist.add("Invasive Disease All Ages");
        blacklist.add("Paralytic");
        blacklist.add("Week");
        blacklist.add("Undetermined");
        blacklist.add("Serogroup B");
        blacklist.add("Serotype B");
        blacklist.add("Congenital");
        blacklist.add("Nonparalytic");
        blacklist.add("Perinatal Virus Infection");
        blacklist.add("Surveillance");
        blacklist.add("Congenital Syndrome");
        blacklist.add("Unknown Serotype");
        blacklist.add("By Type C");
        blacklist.add("Perinatal Infection");
        blacklist.add("Invasive Disease Age Lt5 Yrs");
        blacklist.add("Foodborne");
        blacklist.add("Other Than Toxigenic Vibrio Cholerae O1 Or O139");
        blacklist.add("Age Lt5");
        blacklist.add("Primary And Secondary");
        blacklist.add("By Type A Amp B");
        blacklist.add("Neuroinvasive And Nonneuroinvasive");
        blacklist.add("Reported Diseases");
        blacklist.add("All Serogroups");
        blacklist.add("Non Hantavirus Pulmonary Syndrome");
        blacklist.add("Unknown Serogroups");
        blacklist.add("Non Congenital");
        blacklist.add("All Serotypes");
        blacklist.add("All Ages");
        blacklist.add("Nontypeable");
        blacklist.add("Other Serogroups");
        blacklist.add("Non Hps");
        blacklist.add("Total");
        blacklist.add("Influenza Associated Pediatric Mortality");
        blacklist.add("Meningococcal Disease Neisseria Meningitidis");
        blacklist.add("Non B Serotype");
        blacklist.add("Environmental Health");
        blacklist.add("Kindergarten");
        blacklist.add("Location");
        blacklist.add("Child Health");
        blacklist.add("San Francisco");
        blacklist.add("Lifelong Learning");
        blacklist.add("Statistic");
        blacklist.add("Youth");
        blacklist.add("Zip");
        blacklist.add("Zip Code");
        blacklist.add("Air Quality");
        blacklist.add("Air Quality Index");
        blacklist.add("Air Quality System");
        blacklist.add("Caa 109 Clean Air Act Section 109");
        blacklist.add("Daily 24 Hour Average Concentration");
        blacklist.add("Daily Maximum 8 Hour Average Concentration");
        blacklist.add("Hourly Observations");
        blacklist.add("National Ambient Air Quality Standards");
        blacklist.add("National Environmental Health Tracking Network");
        blacklist.add("O3");
        blacklist.add("Ozone Residual");
        blacklist.add("Particle Pollution");
        blacklist.add("Particulate Matter");
        blacklist.add("Particulate Matter Pm2 5");
        blacklist.add("Particulate Matter Lt 2 5 Um");
        blacklist.add("Pm Fine 0 2 5 Um Stp");
        blacklist.add("Pm2 5");
        blacklist.add("Pm2 5 Local Conditions");
        blacklist.add("Site Monitoring Data");
        blacklist.add("Tracking");
        blacklist.add("Tracking Network");
        blacklist.add("Tracking Program");
        blacklist.add("Code");
    }
}
