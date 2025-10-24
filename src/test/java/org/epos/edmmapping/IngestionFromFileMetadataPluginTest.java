package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Ontology;
import model.StatusType;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionFromFileMetadataPluginTest extends TestcontainersLifecycle {

    static User user = null;
    static String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
    static String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
    static String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/main/edm-schema-shapes.ttl";


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
    public void testIngestionComplex() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("plugin-test.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED);

        AbstractAPI softwareApplicationAPI = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        AbstractAPI softwareSourceCodeAPI = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());
        AbstractAPI softwareApplicationParameterAPI = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name());

        List<org.epos.eposdatamodel.SoftwareApplication> categoryList = softwareApplicationAPI.retrieveAll();
        List<org.epos.eposdatamodel.SoftwareSourceCode> categorySchemeList = softwareSourceCodeAPI.retrieveAll();
        List<org.epos.eposdatamodel.SoftwareApplicationParameter> softwareApplicationParametersList = softwareApplicationParameterAPI.retrieveAll();

        for(org.epos.eposdatamodel.SoftwareApplication item : categoryList){
            System.out.println(item);
        }

        for(org.epos.eposdatamodel.SoftwareSourceCode item : categorySchemeList){
            System.out.println(item);
        }

        for(org.epos.eposdatamodel.SoftwareApplicationParameter item : softwareApplicationParametersList){
            System.out.println(item);
        }

    }
}
