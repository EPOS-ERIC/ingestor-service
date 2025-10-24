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

public class IngestionComplexRegistryFileMetadataTest extends TestcontainersLifecycle {

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

        URL resource = getClass().getClassLoader().getResource("registry.ttl");
        if (resource == null) {
            throw new IllegalArgumentException("file not found!");
        }

        Group selectedGroup = null;

        for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
            if(group.getName().equals("ALL")){
                selectedGroup = group;
            }
        }

        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", selectedGroup, StatusType.PUBLISHED);

        // Assert Categories and schemes
        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        List<org.epos.eposdatamodel.Category> categoryList = categoryApi.retrieveAll();
        List<org.epos.eposdatamodel.CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();


        assertAll(
                () -> assertEquals(4, categoryList.size()),
                () -> assertEquals(3, categorySchemeList.size())
        );

        AbstractAPI facilities = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name());
        List<org.epos.eposdatamodel.Facility> facilityList = facilities.retrieveAll();

        for (org.epos.eposdatamodel.Facility facility : facilityList) {

            System.out.println(facility);
        }

        AbstractAPI equipments = AbstractAPI.retrieveAPI(EntityNames.EQUIPMENT.name());
        List<org.epos.eposdatamodel.Equipment> equipmentList = equipments.retrieveAll();

        for (org.epos.eposdatamodel.Equipment equipment : equipmentList) {

            System.out.println(equipment);
        }
    }

}
