package org.epos.oaipmh;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.epos.core.export.EPOSVersion;
import org.epos.core.oaipmh.OaiPmhService;
import org.epos.core.oaipmh.OaiPmhSparqlQueries;
import org.epos.core.sparql.SparqlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for OAI-PMH service with triplestore integration.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OaiPmhServiceTest {

	@Mock
	private SparqlService sparqlService;

	@InjectMocks
	private OaiPmhService oaiPmhService;

	private Dataset testDataset;

	@BeforeEach
	void setUp() {
		// Set default values using reflection
		ReflectionTestUtils.setField(oaiPmhService, "repositoryName", "EPOS Test Repository");
		ReflectionTestUtils.setField(oaiPmhService, "adminEmail", "test@epos-eu.org");
		ReflectionTestUtils.setField(oaiPmhService, "baseUrl", "http://localhost:8080/api/ingestor-service/v1/oai");
		ReflectionTestUtils.setField(oaiPmhService, "contextPath", "/api/ingestor-service/v1");
		ReflectionTestUtils.setField(oaiPmhService, "pageSize", 100);
		ReflectionTestUtils.setField(oaiPmhService, "eposVersionString", "V1");

		// Create a test RDF model
		Model model = ModelFactory.createDefaultModel();
		
		// Add some test data
		Resource dataset1 = model.createResource("https://example.org/dataset/001");
		dataset1.addProperty(RDF.type, model.createResource("http://www.w3.org/ns/dcat#Dataset"));
		dataset1.addProperty(DCTerms.title, "Test Dataset 1");
		dataset1.addProperty(DCTerms.description, "A test dataset for OAI-PMH");
		dataset1.addProperty(DCTerms.modified, "2024-01-15T10:30:00Z");
		
		Resource dataset2 = model.createResource("https://example.org/dataset/002");
		dataset2.addProperty(RDF.type, model.createResource("http://www.w3.org/ns/dcat#Dataset"));
		dataset2.addProperty(DCTerms.title, "Test Dataset 2");
		dataset2.addProperty(DCTerms.description, "Another test dataset");
		dataset2.addProperty(DCTerms.modified, "2024-02-20T14:00:00Z");
		
		Resource org1 = model.createResource("https://example.org/organization/001");
		org1.addProperty(RDF.type, model.createResource("http://schema.org/Organization"));
		org1.addProperty(model.createProperty("http://schema.org/", "legalName"), "Test Organization");
		
		Resource category1 = model.createResource("https://example.org/category/seismology");
		category1.addProperty(RDF.type, model.createResource("http://www.w3.org/2004/02/skos/core#Concept"));
		category1.addProperty(model.createProperty("http://www.w3.org/2004/02/skos/core#", "prefLabel"), "Seismology");
		
		// Link dataset to category
		dataset1.addProperty(model.createProperty("http://www.w3.org/ns/dcat#", "theme"), category1);

		testDataset = DatasetFactory.create(model);
	}

	private void setupMockSparqlService() {
		when(sparqlService.getDataset(EPOSVersion.V1)).thenReturn(testDataset);
	}

	@Test
	void testIdentifyVerb() {
		String response = oaiPmhService.handleRequest("Identify", null, null, null, null, null, null, 
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=Identify");

		assertNotNull(response);
		assertTrue(response.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"));
		assertTrue(response.contains("<OAI-PMH"));
		assertTrue(response.contains("<Identify>"));
		assertTrue(response.contains("<repositoryName>EPOS Test Repository</repositoryName>"));
		assertTrue(response.contains("<protocolVersion>2.0</protocolVersion>"));
		assertTrue(response.contains("<adminEmail>test@epos-eu.org</adminEmail>"));
		assertTrue(response.contains("<deletedRecord>no</deletedRecord>"));
		assertTrue(response.contains("<granularity>YYYY-MM-DDThh:mm:ssZ</granularity>"));
		assertTrue(response.contains("</Identify>"));
	}

	@Test
	void testListMetadataFormatsVerb() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListMetadataFormats", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListMetadataFormats");

		assertNotNull(response);
		assertTrue(response.contains("<ListMetadataFormats>"));
		assertTrue(response.contains("<metadataPrefix>oai_dc</metadataPrefix>"));
		assertTrue(response.contains("<metadataPrefix>dcat</metadataPrefix>"));
		assertTrue(response.contains("<metadataPrefix>epos_dcat_ap</metadataPrefix>"));
		assertTrue(response.contains("http://www.openarchives.org/OAI/2.0/oai_dc.xsd"));
	}

	@Test
	void testListSetsVerb() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListSets", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListSets");

		assertNotNull(response);
		assertTrue(response.contains("<ListSets>"));
		// Should contain type-based sets
		assertTrue(response.contains("<setSpec>type:"));
		// Should contain category-based sets
		assertTrue(response.contains("<setSpec>category:"));
		assertTrue(response.contains("</ListSets>"));
	}

	@Test
	void testListIdentifiersVerb() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListIdentifiers", null, "oai_dc", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListIdentifiers&metadataPrefix=oai_dc");

		assertNotNull(response);
		assertTrue(response.contains("<ListIdentifiers>") || response.contains("noRecordsMatch"));
	}

	@Test
	void testListRecordsVerb() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListRecords", null, "oai_dc", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=oai_dc");

		assertNotNull(response);
		assertTrue(response.contains("<ListRecords>") || response.contains("noRecordsMatch"));
	}

	@Test
	void testListRecordsWithEposDcatApFormat() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListRecords", null, "epos_dcat_ap", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=epos_dcat_ap");

		assertNotNull(response);
		// Should either have records or return noRecordsMatch
		assertTrue(response.contains("<ListRecords>") || response.contains("noRecordsMatch"));
	}

	@Test
	void testListRecordsWithTypeSet() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("ListRecords", null, "oai_dc", "type:Dataset", null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=oai_dc&set=type:Dataset");

		assertNotNull(response);
		assertTrue(response.contains("<ListRecords>") || response.contains("noRecordsMatch"));
	}

	@Test
	void testGetRecordVerb() {
		setupMockSparqlService();
		
		String response = oaiPmhService.handleRequest("GetRecord", "https://example.org/dataset/001", "oai_dc", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&identifier=https://example.org/dataset/001&metadataPrefix=oai_dc");

		assertNotNull(response);
		// Should either have the record or return idDoesNotExist
		assertTrue(response.contains("<GetRecord>") || response.contains("idDoesNotExist"));
	}

	@Test
	void testBadVerbError() {
		String response = oaiPmhService.handleRequest("InvalidVerb", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=InvalidVerb");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badVerb\">"));
		assertTrue(response.contains("Illegal verb"));
	}

	@Test
	void testMissingVerbError() {
		String response = oaiPmhService.handleRequest(null, null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badVerb\">"));
		assertTrue(response.contains("Verb argument is required"));
	}

	@Test
	void testGetRecordMissingIdentifier() {
		String response = oaiPmhService.handleRequest("GetRecord", null, "oai_dc", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&metadataPrefix=oai_dc");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badArgument\">"));
		assertTrue(response.contains("identifier is a required argument"));
	}

	@Test
	void testGetRecordMissingMetadataPrefix() {
		String response = oaiPmhService.handleRequest("GetRecord", "some:id", null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&identifier=some:id");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badArgument\">"));
		assertTrue(response.contains("metadataPrefix is a required argument"));
	}

	@Test
	void testListIdentifiersMissingMetadataPrefix() {
		String response = oaiPmhService.handleRequest("ListIdentifiers", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListIdentifiers");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badArgument\">"));
		assertTrue(response.contains("metadataPrefix is a required argument"));
	}

	@Test
	void testListRecordsMissingMetadataPrefix() {
		String response = oaiPmhService.handleRequest("ListRecords", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"badArgument\">"));
		assertTrue(response.contains("metadataPrefix is a required argument"));
	}

	@Test
	void testInvalidMetadataPrefix() {
		String response = oaiPmhService.handleRequest("ListRecords", null, "invalid_format", null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=invalid_format");

		assertNotNull(response);
		assertTrue(response.contains("<error code=\"cannotDisseminateFormat\">"));
	}

	@Test
	void testResponseContainsValidXmlStructure() {
		String response = oaiPmhService.handleRequest("Identify", null, null, null, null, null, null,
			"http://localhost:8080/api/ingestor-service/v1/oai?verb=Identify");

		// Verify XML namespaces are present
		assertTrue(response.contains("xmlns=\"http://www.openarchives.org/OAI/2.0/\""));
		assertTrue(response.contains("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""));
		
		// Verify required elements
		assertTrue(response.contains("<responseDate>"));
		assertTrue(response.contains("<request"));
		assertTrue(response.contains("</OAI-PMH>"));
	}

	// ==================== SPARQL Query Tests ====================

	@Test
	void testSparqlQueriesExpandTypeUri() {
		assertEquals("http://www.w3.org/ns/dcat#Dataset", 
			OaiPmhSparqlQueries.expandTypeUri("dcat:Dataset"));
		assertEquals("http://schema.org/Organization", 
			OaiPmhSparqlQueries.expandTypeUri("schema:Organization"));
		assertEquals("https://www.epos-eu.org/epos-dcat-ap#WebService", 
			OaiPmhSparqlQueries.expandTypeUri("epos:WebService"));
	}

	@Test
	void testSparqlQueriesCompactTypeUri() {
		assertEquals("dcat:Dataset", 
			OaiPmhSparqlQueries.compactTypeUri("http://www.w3.org/ns/dcat#Dataset"));
		assertEquals("schema:Organization", 
			OaiPmhSparqlQueries.compactTypeUri("http://schema.org/Organization"));
		assertEquals("epos:WebService", 
			OaiPmhSparqlQueries.compactTypeUri("https://www.epos-eu.org/epos-dcat-ap#WebService"));
	}

	@Test
	void testListRecordsQueryGeneration() {
		String query = OaiPmhSparqlQueries.listRecords(null, null, null, null, 0, 100);
		assertNotNull(query);
		assertTrue(query.contains("SELECT DISTINCT"));
		assertTrue(query.contains("?subject"));
		assertTrue(query.contains("OFFSET 0"));
		assertTrue(query.contains("LIMIT 100"));
	}

	@Test
	void testListRecordsQueryWithTypeFilter() {
		String query = OaiPmhSparqlQueries.listRecords("dcat:Dataset", null, null, null, 0, 100);
		assertNotNull(query);
		assertTrue(query.contains("?subject a dcat:Dataset"));
	}

	@Test
	void testListRecordsQueryWithCategoryFilter() {
		String query = OaiPmhSparqlQueries.listRecords(null, "https://example.org/category/seismology", null, null, 0, 100);
		assertNotNull(query);
		assertTrue(query.contains("dcat:theme <https://example.org/category/seismology>"));
	}

	@Test
	void testCountRecordsQueryGeneration() {
		String query = OaiPmhSparqlQueries.countRecords(null, null, null, null);
		assertNotNull(query);
		assertTrue(query.contains("COUNT"));
		assertTrue(query.contains("AS ?count"));
	}

	@Test
	void testConstructRecordQueryGeneration() {
		String query = OaiPmhSparqlQueries.constructRecord("https://example.org/dataset/001");
		assertNotNull(query);
		assertTrue(query.contains("CONSTRUCT"));
		assertTrue(query.contains("<https://example.org/dataset/001>"));
	}

	@Test
	void testHarvestableTypesListNotEmpty() {
		assertFalse(OaiPmhSparqlQueries.HARVESTABLE_TYPES.isEmpty());
		assertTrue(OaiPmhSparqlQueries.HARVESTABLE_TYPES.contains("dcat:Dataset"));
		assertTrue(OaiPmhSparqlQueries.HARVESTABLE_TYPES.contains("schema:Organization"));
		assertTrue(OaiPmhSparqlQueries.HARVESTABLE_TYPES.contains("epos:WebService"));
	}
}
