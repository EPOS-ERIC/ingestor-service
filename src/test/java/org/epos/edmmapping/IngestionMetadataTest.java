package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Category;
import model.CategoryScheme;
import model.Ontology;
import model.StatusType;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.epos.core.*;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionMetadataTest extends TestcontainersLifecycle {

    static User user = null;
    static String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
    static String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
    static String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/main/edm-schema-shapes.ttl";

    static Model model;

    static Model modelmapping;

    static Map<String, Map<String, String>> classesMap;
    static List<EPOSDataModelEntity> classes;

    @Test
    @Order(1)
    public void testCreateOntologies() throws IOException {

        OntologiesManager.createOntology("EPOS-DCAT-AP-V1", "BASE", metadataOntologyDCATAPIV1);
        OntologiesManager.createOntology("EPOS-DCAT-AP-V3", "BASE", metadataOntologyDCATAPIV3);
        OntologiesManager.createOntology("EDM-TO-DCAT-AP", "MAPPING", metadataMappingEPOSDataModel);

        EposDataModelDAO eposDataModelDAO = EposDataModelDAO.getInstance();
        List<Ontology> ontologiesList = eposDataModelDAO.getAllFromDB(Ontology.class);


        assertNotNull(ontologiesList);
        assertEquals(3, ontologiesList.size());
    }

    @Test
    @Order(2)
    public void testRetrieveMainEntities() throws IOException {

        String metadataURL = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/examples/EPOS-DCAT-AP_metadata_template.ttl";

        String mapping = "EDM-TO-DCAT-AP";

        modelmapping = MetadataPopulator.retrieveModelMapping(mapping);
        model = MetadataPopulator.retrieveMetadataModelFromTTL(metadataURL);

        classesMap = SPARQLManager.retrieveMainEntities(model);

        assertNotNull(classesMap);
        assertEquals(32, classesMap.keySet().size());
    }

    @Test
    @Order(3)
    public void testRetrieveClassesFromEntities() throws IOException {

        String returnValue = SPARQLManager.retrieveEDMMappedClass("http://www.w3.org/ns/dcat#Dataset", modelmapping);

        classes = new ArrayList<>();
        BeansCreation beansCreation = new BeansCreation();

        for(String uid : classesMap.keySet()){
            String className = SPARQLManager.retrieveEDMMappedClass(classesMap.get(uid).get("class").toString(), modelmapping);
            classes.add(beansCreation.getEPOSDataModelClass(className,uid, null));
        }

        System.out.println(classes);

        assertEquals("DataProduct", returnValue);
        assertEquals(32, classes.size());
    }




    @Test
    @Order(4)
    public void testRetrievePropertiesFromClasses() throws IOException {

        String metadataURL = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/examples/EPOS-DCAT-AP_metadata_template.ttl";

        Map<String, LinkedEntity> returnMap = MetadataPopulator.startMetadataPopulation(metadataURL, "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED);

        System.out.println(returnMap);

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        List<org.epos.eposdatamodel.WebService> webServiceList = api.retrieveAll();

        AbstractAPI api2 = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());

        List<org.epos.eposdatamodel.Operation> operationList = api2.retrieveAll();

        AbstractAPI api3 = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());

        List<org.epos.eposdatamodel.Mapping> mappingList = api3.retrieveAll();

        AbstractAPI api4 = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        List<org.epos.eposdatamodel.Distribution> distributionList = api4.retrieveAll();


        for(org.epos.eposdatamodel.Mapping mapping : mappingList){
            System.out.println(mapping);
        }

        for(org.epos.eposdatamodel.Distribution distribution : distributionList){
            System.out.println(distribution);
        }

        for(org.epos.eposdatamodel.Operation operation : operationList){
            System.out.println(operation);
        }

        assertAll(
                () -> assertEquals(1, webServiceList.size()),
                () -> assertEquals(1, operationList.size()),
                () -> assertEquals(2, mappingList.size())
        );

    }

}
