package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Ontology;
import model.StatusType;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionDeferredResolutionTest extends TestcontainersLifecycle {

    static String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
    static String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
    static String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/refs/heads/main/edm-schema-shapes.ttl";

    @BeforeEach
    public void setup() {
        // Pulizia del DB prima di ogni test per garantire risultati consistenti
        cleanupDatabase();
    }

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

        List<Group> selectedGroup = new ArrayList<>();

        for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
            if(group.getName().equals("ALL")){
                selectedGroup.add(group);
            }
        }

        // Avvio Ingestione
        MetadataPopulator.startMetadataPopulation(resource.toURI().toString(), "EDM-TO-DCAT-AP", selectedGroup, StatusType.PUBLISHED, "ingestor");

        // --- VERIFICHE ---

        // 1. Verifica Categorie e Schemi
        // Il file tsutest.ttl contiene 14 skos:Concept e 8 skos:ConceptScheme.
        // Grazie a enableStore=true in DataProductAPI, anche se i DataProduct venissero processati prima,
        // verrebbero creati gli stub, poi aggiornati dalle definizioni.

        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        AbstractAPI categorySchemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        List<Category> categoryList = categoryApi.retrieveAll();
        List<CategoryScheme> categorySchemeList = categorySchemeApi.retrieveAll();

        System.out.println("Categories found: " + categoryList.size());
        System.out.println("Schemes found: " + categorySchemeList.size());

        assertAll("Verifica Conteggi Categorie",
                () -> assertEquals(14, categoryList.size(), "Il numero di Categorie (Concept) non corrisponde"),
                () -> assertEquals(8, categorySchemeList.size(), "Il numero di Schemi (ConceptScheme) non corrisponde")
        );

        // 2. Verifica Collegamento DataProduct -> Categoria
        // Cerchiamo un DataProduct specifico e verifichiamo che la categoria sia collegata
        AbstractAPI dataproductsApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        DataProduct targetDp = (DataProduct) dataproductsApi.retrieveByUID("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events");

        assertNotNull(targetDp, "Il DataProduct 'AHEAD/events' dovrebbe esistere");
        assertFalse(targetDp.getCategory().isEmpty(), "Il DataProduct dovrebbe avere delle categorie collegate");

        // Verifichiamo che una delle categorie sia quella attesa (category:earthquakeparameters)
        boolean categoryFound = targetDp.getCategory().stream()
                .anyMatch(cat -> "category:earthquakeparameters".equals(cat.getUid()));

        assertTrue(categoryFound, "Il DataProduct non è collegato alla categoria 'category:earthquakeparameters'");

        System.out.println("Test IngestionComplex completato con successo.");
    }

    /**
     * Metodo di utilità per pulire il database dalle entità create.
     * Ordine di cancellazione importante per rispettare i vincoli di Foreign Key.
     */
    private void cleanupDatabase() {
        try {
            System.out.println("Cleaning up database...");

            // 1. Cancella entità che dipendono da Categorie o Organizzazioni
            deleteEntities(EntityNames.DATAPRODUCT.name());
            deleteEntities(EntityNames.WEBSERVICE.name());
            deleteEntities(EntityNames.SOFTWAREAPPLICATION.name());
            deleteEntities(EntityNames.FACILITY.name());
            deleteEntities(EntityNames.EQUIPMENT.name());

            // 2. Cancella entità "Core" referenziate
            deleteEntities(EntityNames.CATEGORY.name());
            deleteEntities(EntityNames.CATEGORYSCHEME.name());
            deleteEntities(EntityNames.ORGANIZATION.name());
            deleteEntities(EntityNames.PERSON.name());
            deleteEntities(EntityNames.CONTACTPOINT.name());

            System.out.println("Database cleaned.");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private void deleteEntities(String apiName) {
        try {
            AbstractAPI api = AbstractAPI.retrieveAPI(apiName);
            List<Object> list = api.retrieveAll();
            for (Object obj : list) {
                // Reflection per ottenere l'instanceId da qualsiasi entità EPOS
                String instanceId = (String) obj.getClass().getMethod("getInstanceId").invoke(obj);
                api.delete(instanceId);
            }
        } catch (Exception e) {
            // Ignora errori se la tabella è già vuota o l'API non risponde
        }
    }
}