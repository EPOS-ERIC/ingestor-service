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

public class IngestionComplexFromFileMetadataTest extends TestcontainersLifecycle {

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
    public void testFacetsIngestionComplex() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("facets-test.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED, "ingestor");

        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        AbstractAPI dataProductApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.Category> categoryList = categoryApi.retrieveAll();
        List<org.epos.eposdatamodel.CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();
        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataProductApi.retrieveAll();

        for(org.epos.eposdatamodel.Category category : categoryList){
            System.out.println(category.getInstanceId()+" "+category.getMetaId()+" "+category.getUid()+" "+category.getStatus());
        }

        for(org.epos.eposdatamodel.CategoryScheme categoryScheme : categorySchemeList){
            System.out.println(categoryScheme.getInstanceId()+" "+categoryScheme.getMetaId()+" "+categoryScheme.getUid()+" "+categoryScheme.getStatus());
        }

        for(org.epos.eposdatamodel.DataProduct dataProduct : dataProductList){
            System.out.println(dataProduct);
        }


        assertAll(
                () -> assertEquals(2, categoryList.size()),
                () -> assertEquals(1, categorySchemeList.size())
        );
    }

    @Test
    @Order(3)
    public void testOrganizationsIngestionComplex() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("data-providers.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED, "ingestor");

        AbstractAPI organizationAPI = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        List<org.epos.eposdatamodel.Organization> organizationList = organizationAPI.retrieveAll();

        for(org.epos.eposdatamodel.Organization organization : organizationList){
            System.out.println(organization);
        }

        assertAll(
                () -> assertEquals(2, organizationList.size())
        );
    }

    @Test
    @Order(4)
    public void testDatasetsIngestionComplex() throws IOException, URISyntaxException {

        URL resource = getClass().getClassLoader().getResource("test-example.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED, "ingestor");

        AbstractAPI dataproductAPI = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataproductAPI.retrieveAll();

        for(org.epos.eposdatamodel.DataProduct dataProduct : dataProductList){
            System.out.println(dataProduct.getInstanceId()+" "+dataProduct.getMetaId()+" "+dataProduct.getUid()+" "+dataProduct.getStatus());
        }
        for(org.epos.eposdatamodel.DataProduct dataProduct : dataProductList){
            System.out.println(dataProduct);
        }

        assertAll(
                () -> assertEquals(1, dataProductList.size())
        );
    }

    @Test
    @Order(5)
    public void testNotEmptyCategories() throws IOException, URISyntaxException {

        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        AbstractAPI dataProductApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.Category> categoryList = categoryApi.retrieveAll();
        List<org.epos.eposdatamodel.CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();
        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataProductApi.retrieveAll();

        for(org.epos.eposdatamodel.Category category : categoryList){
            System.out.println(category.getInstanceId()+" "+category.getMetaId()+" "+category.getUid()+" "+category.getStatus());
        }

        for(org.epos.eposdatamodel.CategoryScheme categoryScheme : categorySchemeList){
            System.out.println(categoryScheme.getInstanceId()+" "+categoryScheme.getMetaId()+" "+categoryScheme.getUid()+" "+categoryScheme.getStatus());
        }

        for(org.epos.eposdatamodel.DataProduct dataProduct : dataProductList){
            System.out.println(dataProduct);
        }


        assertAll(
                () -> assertEquals(2, categoryList.size()),
                () -> assertEquals(1, categorySchemeList.size())
        );
    }

    @Test
    @Order(6)
    public void testNotEmptyOrganizations() throws IOException, URISyntaxException {

        AbstractAPI organizationAPI = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        List<org.epos.eposdatamodel.Organization> organizationList = organizationAPI.retrieveAll();

        for(org.epos.eposdatamodel.Organization organization : organizationList){
            System.out.println(organization);
        }

        assertAll(
                () -> assertEquals(2, organizationList.size())
        );
    }
}
