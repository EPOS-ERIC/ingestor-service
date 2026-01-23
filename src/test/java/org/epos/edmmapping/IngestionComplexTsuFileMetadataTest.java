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

import static org.junit.jupiter.api.Assertions.*;

public class IngestionComplexTsuFileMetadataTest extends TestcontainersLifecycle {

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

        URL resource = getClass().getClassLoader().getResource("tsutest.ttl");
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

        // Assert Categories and schemes
        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        List<org.epos.eposdatamodel.Category> categoryList = categoryApi.retrieveAll();
        List<org.epos.eposdatamodel.CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();

        System.out.println("CATEGORY LIST: "+categoryList);

        assertAll(
                () -> assertEquals(1, categoryList.size()),
                () -> assertEquals(0, categorySchemeList.size())
        );

        // Assert Datasets

        AbstractAPI datasets = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        List<org.epos.eposdatamodel.DataProduct> dataProductList = datasets.retrieveAll();

        for (org.epos.eposdatamodel.DataProduct dataProduct : dataProductList) {

            System.out.println(dataProduct);

            if(dataProduct.getTitle().get(0).equals("https://ls3gp.icm.csic.es/?page_id=553")){
                assertAll(
                        () -> assertEquals(1, dataProduct.getTitle().size()),
                        () -> assertEquals(1, dataProduct.getDescription().size()),
                        () -> assertEquals(1, dataProduct.getCategory().size())
                );
            }
        }

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
    }

}
