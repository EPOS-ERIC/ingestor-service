package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Ontology;
import model.StatusType;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class IngestionComplexFullFileMetadatoneUpdateTest extends TestcontainersLifecycle {

    static User user = null;
    static String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
    static String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
    static String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/refs/heads/main/edm-schema-shapes.ttl";


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

        URL resource = getClass().getClassLoader().getResource("metadatone.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        Group selectedGroup = null;

        for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
            if(group.getName().equals("ALL")){
                selectedGroup = group;
            }
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", selectedGroup, StatusType.PUBLISHED, "ingestor");

        AbstractAPI operationsAPI = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        List<org.epos.eposdatamodel.Operation> operationList = operationsAPI.retrieveAll();

        for (org.epos.eposdatamodel.Operation operation : operationList) {

            System.out.println(operation);
        }

        AbstractAPI payloads = AbstractAPI.retrieveAPI(EntityNames.PAYLOAD.name());
        List<org.epos.eposdatamodel.Payload> payloadsList = payloads.retrieveAll();

        for (org.epos.eposdatamodel.Payload payload : payloadsList) {

            System.out.println(payload);
        }

        AbstractAPI outputapi = AbstractAPI.retrieveAPI(EntityNames.OUTPUTMAPPING.name());
        List<org.epos.eposdatamodel.OutputMapping> outputMappings = outputapi.retrieveAll();

        for (org.epos.eposdatamodel.OutputMapping mapping : outputMappings) {

            System.out.println(mapping);
        }

        AbstractAPI webservices = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        List<org.epos.eposdatamodel.WebService> webServiceList = webservices.retrieveAll();

        for (org.epos.eposdatamodel.WebService webService : webServiceList) {

            System.out.println(webService);
        }

        AbstractAPI dataproducts = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        List<org.epos.eposdatamodel.DataProduct> dataProductList1 = dataproducts.retrieveAll();

        for (org.epos.eposdatamodel.DataProduct dataProduct : dataProductList1) {

            System.out.println(dataProduct);
        }

        AbstractAPI distributions = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        List<org.epos.eposdatamodel.Distribution> distributionList = distributions.retrieveAll();

        for (org.epos.eposdatamodel.Distribution distribution : distributionList) {

            System.out.println(distribution);
        }

        AbstractAPI facilities = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name());
        List<org.epos.eposdatamodel.Facility> facilities1 = facilities.retrieveAll();

        for (org.epos.eposdatamodel.Facility facility : facilities1) {

            System.out.println(facility);
        }

        AbstractAPI categories = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        List<org.epos.eposdatamodel.Category> categoriesList = categories.retrieveAll();

        for (org.epos.eposdatamodel.Category category : categoriesList) {

            System.out.println(category);
        }

        AbstractAPI softwareapplications = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        List<org.epos.eposdatamodel.SoftwareApplication> softwareapplicationList = softwareapplications.retrieveAll();

        for (org.epos.eposdatamodel.SoftwareApplication soft : softwareapplicationList) {

            System.out.println(soft);
        }

        AbstractAPI softwaresourcecodes = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());
        List<org.epos.eposdatamodel.SoftwareSourceCode> softwaresourcecodeList = softwaresourcecodes.retrieveAll();

        for (org.epos.eposdatamodel.SoftwareSourceCode soft : softwaresourcecodeList) {

            System.out.println(soft);
        }

        AbstractAPI org = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
        List<org.epos.eposdatamodel.Organization> orgList = org.retrieveAll();

        for (org.epos.eposdatamodel.Organization or : orgList) {

            System.out.println(or);
        }

        AbstractAPI att = AbstractAPI.retrieveAPI(EntityNames.ATTRIBUTION.name());
        List<org.epos.eposdatamodel.Attribution> attList = att.retrieveAll();

        for (org.epos.eposdatamodel.Attribution at : attList) {

            System.out.println(at);
        }

    }

    @Test
    @Order(3)
    public void testIngestionComplexUpdate() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("metadatone2.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        Group selectedGroup = null;

        for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
            if(group.getName().equals("ALL")){
                selectedGroup = group;
            }
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", selectedGroup, StatusType.DRAFT, "user");

        AbstractAPI operationsAPI = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        List<org.epos.eposdatamodel.Operation> operationList = operationsAPI.retrieveAll();

        for (org.epos.eposdatamodel.Operation operation : operationList) {

            System.out.println(operation);
        }

        AbstractAPI payloads = AbstractAPI.retrieveAPI(EntityNames.PAYLOAD.name());
        List<org.epos.eposdatamodel.Payload> payloadsList = payloads.retrieveAll();

        for (org.epos.eposdatamodel.Payload payload : payloadsList) {

            System.out.println(payload);
        }

        AbstractAPI outputapi = AbstractAPI.retrieveAPI(EntityNames.OUTPUTMAPPING.name());
        List<org.epos.eposdatamodel.OutputMapping> outputMappings = outputapi.retrieveAll();

        for (org.epos.eposdatamodel.OutputMapping mapping : outputMappings) {

            System.out.println(mapping);
        }

        AbstractAPI webservices = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        List<org.epos.eposdatamodel.WebService> webServiceList = webservices.retrieveAll();

        for (org.epos.eposdatamodel.WebService webService : webServiceList) {

            System.out.println(webService);
        }

        AbstractAPI dataproducts = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        List<org.epos.eposdatamodel.DataProduct> dataProductList1 = dataproducts.retrieveAll();

        for (org.epos.eposdatamodel.DataProduct dataProduct : dataProductList1) {

            System.out.println(dataProduct);
        }

        AbstractAPI distributions = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        List<org.epos.eposdatamodel.Distribution> distributionList = distributions.retrieveAll();

        for (org.epos.eposdatamodel.Distribution distribution : distributionList) {

            System.out.println(distribution);
        }

        AbstractAPI facilities = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name());
        List<org.epos.eposdatamodel.Facility> facilities1 = facilities.retrieveAll();

        for (org.epos.eposdatamodel.Facility facility : facilities1) {

            System.out.println(facility);
        }

        AbstractAPI categories = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        List<org.epos.eposdatamodel.Category> categoriesList = categories.retrieveAll();

        for (org.epos.eposdatamodel.Category category : categoriesList) {

            System.out.println(category);
        }

        AbstractAPI softwareapplications = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        List<org.epos.eposdatamodel.SoftwareApplication> softwareapplicationList = softwareapplications.retrieveAll();

        for (org.epos.eposdatamodel.SoftwareApplication soft : softwareapplicationList) {

            System.out.println(soft);
        }

        AbstractAPI softwaresourcecodes = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());
        List<org.epos.eposdatamodel.SoftwareSourceCode> softwaresourcecodeList = softwaresourcecodes.retrieveAll();

        for (org.epos.eposdatamodel.SoftwareSourceCode soft : softwaresourcecodeList) {

            System.out.println(soft);
        }

        AbstractAPI org = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
        List<org.epos.eposdatamodel.Organization> orgList = org.retrieveAll();

        for (org.epos.eposdatamodel.Organization or : orgList) {

            System.out.println(or);
        }

        AbstractAPI att = AbstractAPI.retrieveAPI(EntityNames.ATTRIBUTION.name());
        List<org.epos.eposdatamodel.Attribution> attList = att.retrieveAll();

        for (org.epos.eposdatamodel.Attribution at : attList) {

            System.out.println(at);
        }

    }

}
