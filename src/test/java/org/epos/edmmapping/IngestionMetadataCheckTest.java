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
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionMetadataCheckTest extends TestcontainersLifecycle {

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

    /*@Test
    @Order(2)
    public void testInsertMetadataInformation() throws IOException {

        String metadataURL = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/examples/EPOS-DCAT-AP_metadata_template.ttl";

        MetadataPopulator.startMetadataPopulation(metadataURL, "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED);

        AbstractAPI dataproductAPI = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataproductAPI.retrieveAll();

        AbstractAPI distributionAPI = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        List<org.epos.eposdatamodel.Distribution> distributionList = distributionAPI.retrieveAll();

        AbstractAPI webserviceAPI = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        List<org.epos.eposdatamodel.WebService> webServiceList = webserviceAPI.retrieveAll();

    }*/

    @Test
    @Order(2)
    public void testRetrievePropertiesFromDataProduct() throws IOException {

        String metadataURL = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/examples/EPOS-DCAT-AP_metadata_template.ttl";

        MetadataPopulator.startMetadataPopulation(metadataURL, "EDM-TO-DCAT-AP", null, StatusType.PUBLISHED);

        AbstractAPI dataproductAPI = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        List<org.epos.eposdatamodel.DataProduct> dataProductList = dataproductAPI.retrieveAll();

        assertEquals(1, dataProductList.size());

        DataProduct dataProduct = dataProductList.get(0);

        System.out.println(dataProduct);

        for(LinkedEntity le : dataProduct.getIdentifier()){
            System.out.println(AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name()).retrieve(le.getInstanceId()));
        }

        /** GENERAL ASSETS **/
        assertAll(
                () -> assertNotNull(dataProduct.getIdentifier()),
                () -> assertEquals(1, dataProduct.getIdentifier().size()),
                () -> assertEquals(List.of("Primary Seismic Waveform Data"), dataProduct.getTitle()),
                () -> assertEquals(List.of("Primary Seismic Waveform Data description"), dataProduct.getDescription()),
                () -> assertEquals("2016-01-01T00:00:00Z", dataProduct.getIssued().format(DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss'Z']"))),
                () -> assertEquals("2016-01-01T00:00:00Z", dataProduct.getModified().format(DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss'Z']"))),
                () -> assertEquals("2016-01-01T00:00:00Z", dataProduct.getCreated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss'Z']"))),
                () -> assertEquals("1.0.0", dataProduct.getVersionInfo()),
                () -> assertEquals("http://purl.org/dc/dcmitype/Collection", dataProduct.getType()),
                () -> assertEquals("http://purl.org/cld/freq/continuous", dataProduct.getAccrualPeriodicity()),
                () -> assertEquals("epos:SeismicWaveform", dataProduct.getCategory().get(0).getUid()),
                () -> assertEquals("http://orcid.org/0000-0002-6250-0000/contactPoint", dataProduct.getContactPoint().get(0).getUid()),
                () -> assertEquals("PIC:007012076", dataProduct.getPublisher().get(0).getUid())//,
                //() -> assertEquals("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/Distribution/001", dataProduct.getDistribution().get(0).getUid())
        );

        /** IDENTIFIER ASSETS **/
        AbstractAPI identifierAPI = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());
        List<org.epos.eposdatamodel.Identifier> identifierList = identifierAPI.retrieveAll();

        Identifier identifier = null;
        for(Identifier identifier1 : identifierList){
            if(identifier1.getUid().equals(dataProduct.getIdentifier().get(0).getUid())){
                identifier = identifier1;
            }
        }

        Identifier finalIdentifier = identifier;
        assertAll(
                () -> assertNotNull(finalIdentifier),
                () -> assertEquals("DDSS-ID", finalIdentifier.getType()),
                () -> assertEquals("WP08-DDSS-001", finalIdentifier.getIdentifier())
        );

        /** SPATIAL ASSETS **/

        AbstractAPI spatialAPI = AbstractAPI.retrieveAPI(EntityNames.LOCATION.name());
        List<org.epos.eposdatamodel.Location> locationList = spatialAPI.retrieveAll();

        Location location = null;
        for(Location location1 : locationList){
            if(location1.getUid().equals(dataProduct.getSpatialExtent().get(0).getUid())){
                location = location1;
            }
        }

        Location finalLocation = location;
        assertAll(
                () -> assertNotNull(finalLocation),
                () -> assertEquals("POLYGON((180.0 -90.0 , -180.0 -90.0, -180.0 90.0 , 180.0 90.0,180.0 -90.0))", finalLocation.getLocation())
        );

        /** TEMPORAL ASSETS **/

        AbstractAPI temporalAPI = AbstractAPI.retrieveAPI(EntityNames.PERIODOFTIME.name());
        List<org.epos.eposdatamodel.PeriodOfTime> periodOfTimeList = temporalAPI.retrieveAll();

        PeriodOfTime periodOfTime = null;
        for(PeriodOfTime periodOfTime1 : periodOfTimeList){
            if(periodOfTime1.getUid().equals(dataProduct.getTemporalExtent().get(0).getUid())){
                periodOfTime = periodOfTime1;
            }
        }

        PeriodOfTime finalPeriodOfTime = periodOfTime;
        assertAll(
                () -> assertNotNull(finalPeriodOfTime),
                //() -> assertEquals("1988-01-01T00:00:00Z", finalPeriodOfTime.getStartDate()),
                () -> assertEquals(null, finalPeriodOfTime.getEndDate())
        );

    }

}
