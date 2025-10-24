package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Ontology;
import model.StatusType;
import org.apache.jena.rdf.model.Model;
import org.epos.core.BeansCreation;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.epos.core.SPARQLManager;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionFromFileMetadataTest extends TestcontainersLifecycle {

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

        URL resource = getClass().getClassLoader().getResource("facets-ANTH.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED);

        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        AbstractAPI dataProductApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.Category> categoryList = categoryApi.retrieveAll();
        List<org.epos.eposdatamodel.CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();
        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataProductApi.retrieveAll();

        for(org.epos.eposdatamodel.Category category : categoryList){
            System.out.println(category);
        }

        for(org.epos.eposdatamodel.CategoryScheme categoryScheme : categorySchemeList){
            System.out.println(categoryScheme);
        }

        for(org.epos.eposdatamodel.DataProduct dataProduct : dataProductList){
            System.out.println(dataProduct);
        }


        assertAll(
                () -> assertEquals(10, categoryList.size()),
                () -> assertEquals(1, categorySchemeList.size())
        );
    }
}
