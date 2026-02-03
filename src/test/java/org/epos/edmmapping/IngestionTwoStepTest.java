package org.epos.edmmapping;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Group;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionTwoStepTest extends TestcontainersLifecycle {

    // UID univoci per il test
    private static final String DATA_PRODUCT_UID = "https://example.org/dataproduct/two-step-test-" + UUID.randomUUID();
    private static final String DISTRIBUTION_UID = "https://example.org/distribution/two-step-test-" + UUID.randomUUID();
    private static final String PUBLISHER_UID = "PIC:999472675"; // Un publisher esistente o stub

    static String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
    static String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
    static String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/refs/heads/main/edm-schema-shapes.ttl";


    @BeforeEach
    public void setup() throws IOException {
        cleanup(); // Pulizia preventiva

        OntologiesManager.createOntology("EPOS-DCAT-AP-V1", "BASE", metadataOntologyDCATAPIV1);
        OntologiesManager.createOntology("EPOS-DCAT-AP-V3", "BASE", metadataOntologyDCATAPIV3);
        OntologiesManager.createOntology("EDM-TO-DCAT-AP", "MAPPING", metadataMappingEPOSDataModel);

    }

    @AfterEach
    public void tearDown() {
        cleanup(); // Pulizia post-test
    }

    private void cleanup() {
        try {
            AbstractAPI dpApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
            AbstractAPI distApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

            DataProduct dp = (DataProduct) dpApi.retrieveByUID(DATA_PRODUCT_UID);
            if (dp != null) dpApi.delete(dp.getInstanceId());

            Distribution dist = (Distribution) distApi.retrieveByUID(DISTRIBUTION_UID);
            if (dist != null) distApi.delete(dist.getInstanceId());
        } catch (Exception e) {
            System.out.println("Cleanup warning (safe to ignore): " + e.getMessage());
        }
    }

    @Test
    public void testDeferredResolution() throws Exception {
        // =================================================================================
        // STEP 1: Inseriamo il DataProduct.
        // La Distribution referenziata (<DISTRIBUTION_UID>) NON è definita in questo file.
        // =================================================================================
        String ttlFile1 =
                "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n" +
                        "@prefix dct: <http://purl.org/dc/terms/> .\n" +
                        "@prefix schema: <http://schema.org/> .\n" +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n\n" +
                        "<" + DATA_PRODUCT_UID + "> a dcat:Dataset ;\n" +
                        "    dct:title \"Test DataProduct Two Step\" ;\n" +
                        "    dct:description \"Product inserted BEFORE its distribution\" ;\n" +
                        "    dct:identifier \"DP-TEST-001\" ;\n" +
                        "    dcat:distribution <" + DISTRIBUTION_UID + "> ;\n" +  // <--- Riferimento a entità mancante
                        "    dct:publisher <" + PUBLISHER_UID + "> ;\n" +
                        ".\n" +
                        "<" + PUBLISHER_UID + "> a schema:Organization;\n" +
                        "    schema:identifier [ a schema:PropertyValue; schema:propertyID \"pic\"; schema:value \"999472675\"; ];\n" +
                        "    schema:legalName \"Test Org\";\n" +
                        ".";

        System.out.println("--- STEP 1: Ingestione DataProduct (Distribution mancante) ---");
        runIngestion(createTempTtlFile("step1.ttl", ttlFile1));

        // VERIFICA STEP 1
        AbstractAPI dpApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        DataProduct dpStep1 = (DataProduct) dpApi.retrieveByUID(DATA_PRODUCT_UID);

        assertNotNull(dpStep1, "Il DataProduct dovrebbe essere stato creato");

        // Verifica cruciale: La distribuzione NON deve essere presente nella lista (perché non esiste ancora),
        // ma nel database è stata creata una relazione PENDING.
        boolean distributionPresent = dpStep1.getDistribution() != null && !dpStep1.getDistribution().isEmpty();
        assertFalse(distributionPresent, "La distribuzione NON dovrebbe essere visibile ora (dovrebbe essere PENDING)");

        System.out.println("Verifica Step 1 OK: DataProduct creato, Distribuzione ancora assente.");

        // =================================================================================
        // STEP 2: Inseriamo la Distribution.
        // Questa è l'entità che il DataProduct stava aspettando.
        // =================================================================================
        String ttlFile2 =
                "@prefix dcat: <http://www.w3.org/ns/dcat#> .\n" +
                        "@prefix dct: <http://purl.org/dc/terms/> .\n" +
                        "@prefix schema: <http://schema.org/> .\n\n" +
                        "<" + DISTRIBUTION_UID + "> a dcat:Distribution ;\n" +
                        "    dct:identifier \"DIST-TEST-001\" ;\n" +
                        "    dct:title \"The missing distribution\" ;\n" +
                        "    dct:format \"application/json\" ;\n" +
                        "    dcat:accessURL <http://example.org/access> ;\n" +
                        ".";

        System.out.println("--- STEP 2: Ingestione Distribution (Risoluzione Pending) ---");
        runIngestion(createTempTtlFile("step2.ttl", ttlFile2));

        // VERIFICA STEP 2
        // Ricarichiamo il DataProduct dal DB
        DataProduct dpStep2 = (DataProduct) dpApi.retrieveByUID(DATA_PRODUCT_UID);

        assertNotNull(dpStep2);

        // Verifica cruciale: Ora la distribuzione DEVE essere presente e collegata
        assertNotNull(dpStep2.getDistribution(), "La lista distribuzioni non deve essere nulla");
        assertEquals(1, dpStep2.getDistribution().size(), "Ora dovrebbe esserci 1 distribuzione collegata");

        String linkedDistUid = dpStep2.getDistribution().get(0).getUid();
        assertEquals(DISTRIBUTION_UID, linkedDistUid, "L'UID della distribuzione collegata deve corrispondere");

        System.out.println("Verifica Step 2 OK: Relazione risolta con successo!");
    }

    private void runIngestion(File ttlFile) {
        try {
            Group selectedGroup = null;
            for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
                if(group.getName().equals("ALL")){
                    selectedGroup = group;
                    break;
                }
            }
            MetadataPopulator.startMetadataPopulation(
                    ttlFile.toURI().toString(),
                    "EDM-TO-DCAT-AP",
                    selectedGroup,
                    StatusType.PUBLISHED,
                    "ingestor"
            );
        } catch (Exception e) {
            throw new RuntimeException("Errore durante l'ingestione: " + e.getMessage(), e);
        }
    }

    private File createTempTtlFile(String name, String content) throws IOException {
        File file = File.createTempFile(name, ".ttl");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}