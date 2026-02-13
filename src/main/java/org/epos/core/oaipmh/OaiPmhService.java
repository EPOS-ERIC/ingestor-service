package org.epos.core.oaipmh;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.epos.core.export.EPOSVersion;
import org.epos.core.sparql.SparqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service implementing OAI-PMH 2.0 protocol operations using the in-memory triplestore.
 * 
 * This implementation queries the SPARQL service to retrieve all entity types
 * and supports multiple metadata formats including full EPOS-DCAT-AP RDF.
 */
@Service
public class OaiPmhService {

	private static final Logger LOGGER = LoggerFactory.getLogger(OaiPmhService.class);

	// OAI-PMH Namespaces
	private static final String OAI_PMH_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
	private static final String OAI_DC_NAMESPACE = "http://www.openarchives.org/OAI/2.0/oai_dc/";
	private static final String DC_NAMESPACE = "http://purl.org/dc/elements/1.1/";
	private static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	// RDF Namespaces for epos_dcat_ap format
	private static final String EPOS_DCAT_AP_NAMESPACE = "https://www.epos-eu.org/epos-dcat-ap#";
	private static final String DCAT_NAMESPACE = "http://www.w3.org/ns/dcat#";
	private static final String DCT_NAMESPACE = "http://purl.org/dc/terms/";
	private static final String SCHEMA_NAMESPACE = "http://schema.org/";

	private static final DateTimeFormatter OAI_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

	@Autowired
	private SparqlService sparqlService;

	@Value("${server.servlet.contextPath:/api/ingestor-service/v1}")
	private String contextPath;

	@Value("${oaipmh.repository.name:EPOS Metadata Repository}")
	private String repositoryName;

	@Value("${oaipmh.admin.email:info@epos-eu.org}")
	private String adminEmail;

	@Value("${oaipmh.base.url:}")
	private String baseUrl;

	@Value("${oaipmh.page.size:100}")
	private int pageSize;

	@Value("${oaipmh.epos.version:V1}")
	private String eposVersionString;

	/**
	 * Handles all OAI-PMH requests and routes to appropriate verb handler.
	 */
	public String handleRequest(String verb, String identifier, String metadataPrefix,
			String set, String from, String until, String resumptionToken, String requestUrl) {
		
		LOGGER.info("[OAI-PMH] Request received - verb: {}, identifier: {}, metadataPrefix: {}, set: {}, from: {}, until: {}", 
			verb, identifier, metadataPrefix, set, from, until);

		try {
			if (verb == null || verb.isEmpty()) {
				return buildErrorResponse("badVerb", "Verb argument is required", requestUrl);
			}

			return switch (verb) {
				case "Identify" -> handleIdentify(requestUrl);
				case "ListMetadataFormats" -> handleListMetadataFormats(identifier, requestUrl);
				case "ListSets" -> handleListSets(resumptionToken, requestUrl);
				case "ListIdentifiers" -> handleListIdentifiers(metadataPrefix, set, from, until, resumptionToken, requestUrl);
				case "ListRecords" -> handleListRecords(metadataPrefix, set, from, until, resumptionToken, requestUrl);
				case "GetRecord" -> handleGetRecord(identifier, metadataPrefix, requestUrl);
				default -> buildErrorResponse("badVerb", "Illegal verb: " + verb, requestUrl);
			};
		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error processing request: {}", e.getMessage(), e);
			return buildErrorResponse("badArgument", "Internal error: " + e.getMessage(), requestUrl);
		}
	}

	// ==================== Verb Handlers ====================

	/**
	 * Identify verb - returns repository information.
	 */
	private String handleIdentify(String requestUrl) {
		StringBuilder xml = new StringBuilder();
		xml.append(getXmlHeader(requestUrl, "Identify"));
		xml.append("  <Identify>\n");
		xml.append("    <repositoryName>").append(escapeXml(repositoryName)).append("</repositoryName>\n");
		xml.append("    <baseURL>").append(escapeXml(getBaseUrl(requestUrl))).append("</baseURL>\n");
		xml.append("    <protocolVersion>2.0</protocolVersion>\n");
		xml.append("    <adminEmail>").append(escapeXml(adminEmail)).append("</adminEmail>\n");
		xml.append("    <earliestDatestamp>").append(getEarliestDatestamp()).append("</earliestDatestamp>\n");
		xml.append("    <deletedRecord>no</deletedRecord>\n");
		xml.append("    <granularity>YYYY-MM-DDThh:mm:ssZ</granularity>\n");
		xml.append("    <description>\n");
		xml.append("      <oai-identifier xmlns=\"http://www.openarchives.org/OAI/2.0/oai-identifier\"\n");
		xml.append("                      xmlns:xsi=\"").append(XSI_NAMESPACE).append("\"\n");
		xml.append("                      xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai-identifier\n");
		xml.append("                      http://www.openarchives.org/OAI/2.0/oai-identifier.xsd\">\n");
		xml.append("        <scheme>uri</scheme>\n");
		xml.append("        <repositoryIdentifier>epos-eu.org</repositoryIdentifier>\n");
		xml.append("        <delimiter>/</delimiter>\n");
		xml.append("        <sampleIdentifier>https://www.epos-eu.org/epos-dcat-ap/Dataset/001</sampleIdentifier>\n");
		xml.append("      </oai-identifier>\n");
		xml.append("    </description>\n");
		xml.append("  </Identify>\n");
		xml.append(getXmlFooter());
		return xml.toString();
	}

	/**
	 * ListMetadataFormats verb - returns supported metadata formats.
	 */
	private String handleListMetadataFormats(String identifier, String requestUrl) {
		// If identifier is provided, verify the record exists
		if (identifier != null && !identifier.isEmpty()) {
			OaiPmhRecord record = getRecordByIdentifier(identifier);
			if (record == null) {
				return buildErrorResponse("idDoesNotExist", 
					"No matching identifier: " + identifier, requestUrl);
			}
		}

		StringBuilder xml = new StringBuilder();
		xml.append(getXmlHeader(requestUrl, "ListMetadataFormats"));
		xml.append("  <ListMetadataFormats>\n");
		
		// Dublin Core format (required by OAI-PMH)
		xml.append("    <metadataFormat>\n");
		xml.append("      <metadataPrefix>oai_dc</metadataPrefix>\n");
		xml.append("      <schema>http://www.openarchives.org/OAI/2.0/oai_dc.xsd</schema>\n");
		xml.append("      <metadataNamespace>").append(OAI_DC_NAMESPACE).append("</metadataNamespace>\n");
		xml.append("    </metadataFormat>\n");
		
		// DCAT format
		xml.append("    <metadataFormat>\n");
		xml.append("      <metadataPrefix>dcat</metadataPrefix>\n");
		xml.append("      <schema>http://www.w3.org/ns/dcat#</schema>\n");
		xml.append("      <metadataNamespace>").append(DCAT_NAMESPACE).append("</metadataNamespace>\n");
		xml.append("    </metadataFormat>\n");
		
		// EPOS-DCAT-AP format (full RDF)
		xml.append("    <metadataFormat>\n");
		xml.append("      <metadataPrefix>epos_dcat_ap</metadataPrefix>\n");
		xml.append("      <schema>https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl</schema>\n");
		xml.append("      <metadataNamespace>").append(EPOS_DCAT_AP_NAMESPACE).append("</metadataNamespace>\n");
		xml.append("    </metadataFormat>\n");
		
		xml.append("  </ListMetadataFormats>\n");
		xml.append(getXmlFooter());
		return xml.toString();
	}

	/**
	 * ListSets verb - returns hierarchical sets (entity types and categories).
	 */
	private String handleListSets(String resumptionToken, String requestUrl) {
		try {
			StringBuilder xml = new StringBuilder();
			xml.append(getXmlHeader(requestUrl, "ListSets"));
			xml.append("  <ListSets>\n");

			// Add entity type sets
			Map<String, Integer> entityTypes = getEntityTypeCounts();
			for (Map.Entry<String, Integer> entry : entityTypes.entrySet()) {
				String typeUri = entry.getKey();
				String typeName = OaiPmhSparqlQueries.compactTypeUri(typeUri);
				String localName = getLocalName(typeUri);
				
				xml.append("    <set>\n");
				xml.append("      <setSpec>type:").append(escapeXml(localName)).append("</setSpec>\n");
				xml.append("      <setName>").append(escapeXml(typeName)).append(" (").append(entry.getValue()).append(" records)</setName>\n");
				xml.append("      <setDescription>\n");
				xml.append("        <oai_dc:dc xmlns:oai_dc=\"").append(OAI_DC_NAMESPACE).append("\" ");
				xml.append("xmlns:dc=\"").append(DC_NAMESPACE).append("\">\n");
				xml.append("          <dc:description>All resources of type ").append(escapeXml(typeName)).append("</dc:description>\n");
				xml.append("        </oai_dc:dc>\n");
				xml.append("      </setDescription>\n");
				xml.append("    </set>\n");
			}

			// Add category sets
			List<CategoryInfo> categories = getCategories();
			for (CategoryInfo category : categories) {
				xml.append("    <set>\n");
				xml.append("      <setSpec>category:").append(escapeXml(encodeSetSpec(category.uri))).append("</setSpec>\n");
				String name = category.label != null ? category.label : category.uri;
				xml.append("      <setName>").append(escapeXml(name)).append("</setName>\n");
				if (category.description != null) {
					xml.append("      <setDescription>\n");
					xml.append("        <oai_dc:dc xmlns:oai_dc=\"").append(OAI_DC_NAMESPACE).append("\" ");
					xml.append("xmlns:dc=\"").append(DC_NAMESPACE).append("\">\n");
					xml.append("          <dc:description>").append(escapeXml(category.description)).append("</dc:description>\n");
					xml.append("        </oai_dc:dc>\n");
					xml.append("      </setDescription>\n");
				}
				xml.append("    </set>\n");
			}

			xml.append("  </ListSets>\n");
			xml.append(getXmlFooter());
			return xml.toString();
		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error in ListSets: {}", e.getMessage(), e);
			return buildErrorResponse("badArgument", "Error retrieving sets: " + e.getMessage(), requestUrl);
		}
	}

	/**
	 * ListIdentifiers verb - returns record headers only.
	 */
	private String handleListIdentifiers(String metadataPrefix, String set, String from, 
			String until, String resumptionToken, String requestUrl) {
		
		// Validate metadataPrefix
		if (metadataPrefix == null || metadataPrefix.isEmpty()) {
			if (resumptionToken == null || resumptionToken.isEmpty()) {
				return buildErrorResponse("badArgument", 
					"metadataPrefix is a required argument", requestUrl);
			}
		} else if (!isValidMetadataPrefix(metadataPrefix)) {
			return buildErrorResponse("cannotDisseminateFormat", 
				"Metadata format '" + metadataPrefix + "' is not supported", requestUrl);
		}

		try {
			// Parse resumption token if provided
			int offset = 0;
			if (resumptionToken != null && !resumptionToken.isEmpty()) {
				ResumptionTokenData tokenData = parseResumptionToken(resumptionToken);
				if (tokenData == null) {
					return buildErrorResponse("badResumptionToken", 
						"Invalid resumption token", requestUrl);
				}
				offset = tokenData.offset;
				if (metadataPrefix == null) metadataPrefix = tokenData.metadataPrefix;
				if (set == null) set = tokenData.set;
				if (from == null) from = tokenData.from;
				if (until == null) until = tokenData.until;
			}

			// Parse set specification
			String typeFilter = null;
			String categoryUri = null;
			if (set != null && !set.isEmpty()) {
				if (set.startsWith("type:")) {
					typeFilter = resolveTypeFromSetSpec(set.substring(5));
				} else if (set.startsWith("category:")) {
					categoryUri = decodeSetSpec(set.substring(9));
				}
			}

			// Get total count
			int totalCount = countRecords(typeFilter, categoryUri, from, until);
			
			if (totalCount == 0) {
				return buildErrorResponse("noRecordsMatch", 
					"No records match the request criteria", requestUrl);
			}

			// Retrieve records
			List<OaiPmhRecord> records = listRecords(typeFilter, categoryUri, from, until, offset, pageSize);

			StringBuilder xml = new StringBuilder();
			xml.append(getXmlHeader(requestUrl, "ListIdentifiers"));
			xml.append("  <ListIdentifiers>\n");

			for (OaiPmhRecord record : records) {
				xml.append(buildHeader(record));
			}

			// Add resumption token if there are more records
			if (offset + records.size() < totalCount) {
				String newToken = buildResumptionToken(offset + pageSize, 
					metadataPrefix, set, from, until);
				xml.append("    <resumptionToken completeListSize=\"").append(totalCount)
					.append("\" cursor=\"").append(offset).append("\">")
					.append(escapeXml(newToken)).append("</resumptionToken>\n");
			} else if (resumptionToken != null) {
				// Empty resumption token to indicate end of list
				xml.append("    <resumptionToken completeListSize=\"").append(totalCount)
					.append("\" cursor=\"").append(offset).append("\"/>\n");
			}

			xml.append("  </ListIdentifiers>\n");
			xml.append(getXmlFooter());
			return xml.toString();
		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error in ListIdentifiers: {}", e.getMessage(), e);
			return buildErrorResponse("badArgument", "Error retrieving identifiers: " + e.getMessage(), requestUrl);
		}
	}

	/**
	 * ListRecords verb - returns full records with metadata.
	 */
	private String handleListRecords(String metadataPrefix, String set, String from, 
			String until, String resumptionToken, String requestUrl) {
		
		// Validate metadataPrefix
		if (metadataPrefix == null || metadataPrefix.isEmpty()) {
			if (resumptionToken == null || resumptionToken.isEmpty()) {
				return buildErrorResponse("badArgument", 
					"metadataPrefix is a required argument", requestUrl);
			}
		} else if (!isValidMetadataPrefix(metadataPrefix)) {
			return buildErrorResponse("cannotDisseminateFormat", 
				"Metadata format '" + metadataPrefix + "' is not supported", requestUrl);
		}

		try {
			// Parse resumption token if provided
			int offset = 0;
			if (resumptionToken != null && !resumptionToken.isEmpty()) {
				ResumptionTokenData tokenData = parseResumptionToken(resumptionToken);
				if (tokenData == null) {
					return buildErrorResponse("badResumptionToken", 
						"Invalid resumption token", requestUrl);
				}
				offset = tokenData.offset;
				if (metadataPrefix == null) metadataPrefix = tokenData.metadataPrefix;
				if (set == null) set = tokenData.set;
				if (from == null) from = tokenData.from;
				if (until == null) until = tokenData.until;
			}

			// Parse set specification
			String typeFilter = null;
			String categoryUri = null;
			if (set != null && !set.isEmpty()) {
				if (set.startsWith("type:")) {
					typeFilter = resolveTypeFromSetSpec(set.substring(5));
				} else if (set.startsWith("category:")) {
					categoryUri = decodeSetSpec(set.substring(9));
				}
			}

			// Get total count
			int totalCount = countRecords(typeFilter, categoryUri, from, until);
			
			if (totalCount == 0) {
				return buildErrorResponse("noRecordsMatch", 
					"No records match the request criteria", requestUrl);
			}

			// Retrieve records with full metadata
			List<OaiPmhRecord> records = listRecordsWithMetadata(typeFilter, categoryUri, from, until, offset, pageSize);

			StringBuilder xml = new StringBuilder();
			xml.append(getXmlHeader(requestUrl, "ListRecords"));
			xml.append("  <ListRecords>\n");

			for (OaiPmhRecord record : records) {
				xml.append(buildRecord(record, metadataPrefix));
			}

			// Add resumption token if there are more records
			if (offset + records.size() < totalCount) {
				String newToken = buildResumptionToken(offset + pageSize, 
					metadataPrefix, set, from, until);
				xml.append("    <resumptionToken completeListSize=\"").append(totalCount)
					.append("\" cursor=\"").append(offset).append("\">")
					.append(escapeXml(newToken)).append("</resumptionToken>\n");
			} else if (resumptionToken != null) {
				xml.append("    <resumptionToken completeListSize=\"").append(totalCount)
					.append("\" cursor=\"").append(offset).append("\"/>\n");
			}

			xml.append("  </ListRecords>\n");
			xml.append(getXmlFooter());
			return xml.toString();
		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error in ListRecords: {}", e.getMessage(), e);
			return buildErrorResponse("badArgument", "Error retrieving records: " + e.getMessage(), requestUrl);
		}
	}

	/**
	 * GetRecord verb - returns a single record.
	 */
	private String handleGetRecord(String identifier, String metadataPrefix, String requestUrl) {
		// Validate arguments
		if (identifier == null || identifier.isEmpty()) {
			return buildErrorResponse("badArgument", 
				"identifier is a required argument", requestUrl);
		}
		if (metadataPrefix == null || metadataPrefix.isEmpty()) {
			return buildErrorResponse("badArgument", 
				"metadataPrefix is a required argument", requestUrl);
		}
		if (!isValidMetadataPrefix(metadataPrefix)) {
			return buildErrorResponse("cannotDisseminateFormat", 
				"Metadata format '" + metadataPrefix + "' is not supported", requestUrl);
		}

		try {
			OaiPmhRecord record = getRecordByIdentifierWithMetadata(identifier);
			
			if (record == null) {
				return buildErrorResponse("idDoesNotExist", 
					"No matching identifier: " + identifier, requestUrl);
			}

			StringBuilder xml = new StringBuilder();
			xml.append(getXmlHeader(requestUrl, "GetRecord"));
			xml.append("  <GetRecord>\n");
			xml.append(buildRecord(record, metadataPrefix));
			xml.append("  </GetRecord>\n");
			xml.append(getXmlFooter());
			return xml.toString();
		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error in GetRecord: {}", e.getMessage(), e);
			return buildErrorResponse("badArgument", "Error retrieving record: " + e.getMessage(), requestUrl);
		}
	}

	// ==================== SPARQL Query Methods ====================

	/**
	 * Get the dataset from the SPARQL service based on configured version.
	 */
	private Dataset getDataset() {
		EPOSVersion version = EPOSVersion.V1;
		try {
			version = EPOSVersion.valueOf(eposVersionString);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("[OAI-PMH] Unsupported configured EPOS version '{}', falling back to V1", eposVersionString);
		}
		if (version != EPOSVersion.V1) {
			LOGGER.warn("[OAI-PMH] Configured EPOS version '{}' is not supported by SPARQL endpoint, falling back to V1", version);
			version = EPOSVersion.V1;
		}
		return sparqlService.getDataset(version);
	}

	/**
	 * List records from the triplestore (headers only, no full metadata).
	 */
	private List<OaiPmhRecord> listRecords(String typeFilter, String categoryUri, 
			String from, String until, int offset, int limit) {
		
		List<OaiPmhRecord> records = new ArrayList<>();
		Dataset dataset = getDataset();
		
		String queryString = OaiPmhSparqlQueries.listRecords(typeFilter, categoryUri, from, until, offset, limit);
		LOGGER.debug("[OAI-PMH] Executing query: {}", queryString);
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			
			while (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Resource subject = sol.getResource("subject");
				Resource type = sol.getResource("type");
				
				String datestamp = extractDatestamp(sol);
				List<String> setSpecs = buildSetSpecs(subject.getURI(), type.getURI());
				
				OaiPmhRecord record = OaiPmhRecord.builder()
					.identifier(subject.getURI())
					.datestamp(datestamp)
					.rdfType(type.getURI())
					.setSpecs(setSpecs)
					.build();
				
				records.add(record);
			}
		}
		
		LOGGER.debug("[OAI-PMH] Found {} records", records.size());
		return records;
	}

	/**
	 * List records with full RDF metadata.
	 */
	private List<OaiPmhRecord> listRecordsWithMetadata(String typeFilter, String categoryUri,
			String from, String until, int offset, int limit) {
		
		List<OaiPmhRecord> basicRecords = listRecords(typeFilter, categoryUri, from, until, offset, limit);
		List<OaiPmhRecord> recordsWithMetadata = new ArrayList<>();
		
		for (OaiPmhRecord basic : basicRecords) {
			Model metadata = constructRecordMetadata(basic.getIdentifier());
			OaiPmhRecord full = OaiPmhRecord.builder()
				.identifier(basic.getIdentifier())
				.datestamp(basic.getDatestamp())
				.rdfType(basic.getRdfType())
				.setSpecs(basic.getSetSpecs())
				.metadata(metadata)
				.build();
			recordsWithMetadata.add(full);
		}
		
		return recordsWithMetadata;
	}

	/**
	 * Count records matching the filter criteria.
	 */
	private int countRecords(String typeFilter, String categoryUri, String from, String until) {
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.countRecords(typeFilter, categoryUri, from, until);
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			if (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Literal countLiteral = sol.getLiteral("count");
				return countLiteral.getInt();
			}
		}
		return 0;
	}

	/**
	 * Get a single record by identifier (headers only).
	 */
	private OaiPmhRecord getRecordByIdentifier(String identifier) {
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.getRecord(identifier);
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			if (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Resource type = sol.getResource("type");
				String datestamp = extractDatestamp(sol);
				List<String> setSpecs = buildSetSpecs(identifier, type != null ? type.getURI() : null);
				
				return OaiPmhRecord.builder()
					.identifier(identifier)
					.datestamp(datestamp)
					.rdfType(type != null ? type.getURI() : null)
					.setSpecs(setSpecs)
					.build();
			}
		}
		return null;
	}

	/**
	 * Get a single record with full RDF metadata.
	 */
	private OaiPmhRecord getRecordByIdentifierWithMetadata(String identifier) {
		OaiPmhRecord basic = getRecordByIdentifier(identifier);
		if (basic == null) {
			return null;
		}
		
		Model metadata = constructRecordMetadata(identifier);
		return OaiPmhRecord.builder()
			.identifier(basic.getIdentifier())
			.datestamp(basic.getDatestamp())
			.rdfType(basic.getRdfType())
			.setSpecs(basic.getSetSpecs())
			.metadata(metadata)
			.build();
	}

	/**
	 * Construct the full RDF metadata for a record.
	 */
	private Model constructRecordMetadata(String identifier) {
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.constructRecord(identifier);
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			return qexec.execConstruct();
		}
	}

	/**
	 * Get entity type counts for ListSets.
	 */
	private Map<String, Integer> getEntityTypeCounts() {
		Map<String, Integer> counts = new HashMap<>();
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.listEntityTypes();
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Resource type = sol.getResource("type");
				Literal countLiteral = sol.getLiteral("count");
				if (type != null && countLiteral != null) {
					counts.put(type.getURI(), countLiteral.getInt());
				}
			}
		}
		return counts;
	}

	/**
	 * Get categories for ListSets.
	 */
	private List<CategoryInfo> getCategories() {
		List<CategoryInfo> categories = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.listCategories();
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Resource category = sol.getResource("category");
				if (category != null && !seen.contains(category.getURI())) {
					seen.add(category.getURI());
					CategoryInfo info = new CategoryInfo();
					info.uri = category.getURI();
					
					RDFNode labelNode = sol.get("label");
					if (labelNode != null && labelNode.isLiteral()) {
						info.label = labelNode.asLiteral().getString();
					}
					
					RDFNode descNode = sol.get("description");
					if (descNode != null && descNode.isLiteral()) {
						info.description = descNode.asLiteral().getString();
					}
					
					categories.add(info);
				}
			}
		}
		return categories;
	}

	/**
	 * Get categories for a specific record.
	 */
	private List<String> getRecordCategories(String identifier) {
		List<String> categories = new ArrayList<>();
		Dataset dataset = getDataset();
		String queryString = OaiPmhSparqlQueries.getRecordCategories(identifier);
		
		Query query = QueryFactory.create(queryString);
		try (QueryExecution qexec = QueryExecutionFactory.create(query, dataset)) {
			ResultSet results = qexec.execSelect();
			while (results.hasNext()) {
				QuerySolution sol = results.nextSolution();
				Resource category = sol.getResource("category");
				if (category != null) {
					categories.add(category.getURI());
				}
			}
		}
		return categories;
	}

	// ==================== XML Building Methods ====================

	private String getXmlHeader(String requestUrl, String verb) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<OAI-PMH xmlns=\"").append(OAI_PMH_NAMESPACE).append("\"\n");
		xml.append("         xmlns:xsi=\"").append(XSI_NAMESPACE).append("\"\n");
		xml.append("         xsi:schemaLocation=\"").append(OAI_PMH_NAMESPACE).append(" ");
		xml.append("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n");
		xml.append("  <responseDate>").append(getCurrentTimestamp()).append("</responseDate>\n");
		xml.append("  <request verb=\"").append(verb).append("\">")
			.append(escapeXml(getBaseUrl(requestUrl))).append("</request>\n");
		return xml.toString();
	}

	private String getXmlFooter() {
		return "</OAI-PMH>\n";
	}

	private String buildErrorResponse(String errorCode, String message, String requestUrl) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<OAI-PMH xmlns=\"").append(OAI_PMH_NAMESPACE).append("\"\n");
		xml.append("         xmlns:xsi=\"").append(XSI_NAMESPACE).append("\"\n");
		xml.append("         xsi:schemaLocation=\"").append(OAI_PMH_NAMESPACE).append(" ");
		xml.append("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd\">\n");
		xml.append("  <responseDate>").append(getCurrentTimestamp()).append("</responseDate>\n");
		xml.append("  <request>").append(escapeXml(getBaseUrl(requestUrl))).append("</request>\n");
		xml.append("  <error code=\"").append(errorCode).append("\">")
			.append(escapeXml(message)).append("</error>\n");
		xml.append("</OAI-PMH>\n");
		return xml.toString();
	}

	private String buildHeader(OaiPmhRecord record) {
		StringBuilder xml = new StringBuilder();
		xml.append("    <header>\n");
		xml.append("      <identifier>").append(escapeXml(record.getIdentifier())).append("</identifier>\n");
		xml.append("      <datestamp>").append(record.getDatestamp()).append("</datestamp>\n");
		
		for (String setSpec : record.getSetSpecs()) {
			xml.append("      <setSpec>").append(escapeXml(setSpec)).append("</setSpec>\n");
		}
		
		xml.append("    </header>\n");
		return xml.toString();
	}

	private String buildRecord(OaiPmhRecord record, String metadataPrefix) {
		StringBuilder xml = new StringBuilder();
		xml.append("    <record>\n");
		xml.append(buildHeader(record));
		xml.append("      <metadata>\n");
		
		switch (metadataPrefix) {
			case "oai_dc" -> xml.append(buildDublinCoreMetadata(record));
			case "dcat" -> xml.append(buildDcatMetadata(record));
			case "epos_dcat_ap" -> xml.append(buildEposDcatApMetadata(record));
			default -> xml.append(buildDublinCoreMetadata(record));
		}
		
		xml.append("      </metadata>\n");
		xml.append("    </record>\n");
		return xml.toString();
	}

	private String buildDublinCoreMetadata(OaiPmhRecord record) {
		StringBuilder xml = new StringBuilder();
		xml.append("        <oai_dc:dc xmlns:oai_dc=\"").append(OAI_DC_NAMESPACE).append("\"\n");
		xml.append("                   xmlns:dc=\"").append(DC_NAMESPACE).append("\"\n");
		xml.append("                   xmlns:xsi=\"").append(XSI_NAMESPACE).append("\"\n");
		xml.append("                   xsi:schemaLocation=\"").append(OAI_DC_NAMESPACE);
		xml.append(" http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n");

		Model metadata = record.getMetadata();
		if (metadata != null) {
			Resource subject = metadata.getResource(record.getIdentifier());
			
			// dc:title
			extractAndAddDcElement(xml, metadata, subject, "dc:title", 
				DCT_NAMESPACE + "title", SCHEMA_NAMESPACE + "name", "http://www.w3.org/2000/01/rdf-schema#label");
			
			// dc:creator
			extractAndAddDcElement(xml, metadata, subject, "dc:creator",
				DCT_NAMESPACE + "creator", SCHEMA_NAMESPACE + "provider", SCHEMA_NAMESPACE + "manufacturer");
			
			// dc:subject
			extractAndAddDcElement(xml, metadata, subject, "dc:subject",
				DCAT_NAMESPACE + "theme", DCAT_NAMESPACE + "keyword", SCHEMA_NAMESPACE + "keywords");
			
			// dc:description
			extractAndAddDcElement(xml, metadata, subject, "dc:description",
				DCT_NAMESPACE + "description", SCHEMA_NAMESPACE + "description");
			
			// dc:publisher
			extractAndAddDcElement(xml, metadata, subject, "dc:publisher",
				DCT_NAMESPACE + "publisher");
			
			// dc:date
			extractAndAddDcElement(xml, metadata, subject, "dc:date",
				DCT_NAMESPACE + "issued", DCT_NAMESPACE + "modified", DCT_NAMESPACE + "created",
				SCHEMA_NAMESPACE + "datePublished", SCHEMA_NAMESPACE + "dateModified");
			
			// dc:type
			String typeValue = record.getTypeLocalName();
			xml.append("          <dc:type>").append(escapeXml(typeValue)).append("</dc:type>\n");
			
			// dc:identifier
			xml.append("          <dc:identifier>").append(escapeXml(record.getIdentifier())).append("</dc:identifier>\n");
			extractAndAddDcElement(xml, metadata, subject, "dc:identifier",
				DCT_NAMESPACE + "identifier", SCHEMA_NAMESPACE + "identifier");
			
			// dc:rights
			extractAndAddDcElement(xml, metadata, subject, "dc:rights",
				DCT_NAMESPACE + "rights", DCT_NAMESPACE + "accessRights", DCT_NAMESPACE + "license");
			
			// dc:format
			extractAndAddDcElement(xml, metadata, subject, "dc:format",
				DCT_NAMESPACE + "format");
			
			// dc:language
			extractAndAddDcElement(xml, metadata, subject, "dc:language",
				DCT_NAMESPACE + "language");
		} else {
			// Fallback if no metadata model available
			xml.append("          <dc:identifier>").append(escapeXml(record.getIdentifier())).append("</dc:identifier>\n");
			xml.append("          <dc:type>").append(escapeXml(record.getTypeLocalName())).append("</dc:type>\n");
		}

		xml.append("        </oai_dc:dc>\n");
		return xml.toString();
	}

	private void extractAndAddDcElement(StringBuilder xml, Model model, Resource subject, 
			String dcElement, String... propertyUris) {
		Set<String> values = new HashSet<>();
		for (String propUri : propertyUris) {
			model.listStatements(subject, model.getProperty(propUri), (RDFNode) null)
				.forEachRemaining(stmt -> {
					RDFNode obj = stmt.getObject();
					String value = obj.isLiteral() ? obj.asLiteral().getString() 
						: (obj.isResource() ? obj.asResource().getURI() : obj.toString());
					if (value != null && !value.isEmpty()) {
						values.add(value);
					}
				});
		}
		for (String value : values) {
			xml.append("          <").append(dcElement).append(">")
				.append(escapeXml(value)).append("</").append(dcElement).append(">\n");
		}
	}

	private String buildDcatMetadata(OaiPmhRecord record) {
		StringBuilder xml = new StringBuilder();
		String typeLocalName = record.getTypeLocalName();
		
		xml.append("        <dcat:").append(typeLocalName).append(" xmlns:dcat=\"").append(DCAT_NAMESPACE).append("\"\n");
		xml.append("                      xmlns:dct=\"").append(DCT_NAMESPACE).append("\"\n");
		xml.append("                      xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
		xml.append("                      xmlns:xsi=\"").append(XSI_NAMESPACE).append("\"\n");
		xml.append("                      rdf:about=\"").append(escapeXml(record.getIdentifier())).append("\">\n");

		Model metadata = record.getMetadata();
		if (metadata != null) {
			Resource subject = metadata.getResource(record.getIdentifier());
			
			// Add key DCAT properties
			addDcatProperty(xml, metadata, subject, "dct:identifier", DCT_NAMESPACE + "identifier");
			addDcatProperty(xml, metadata, subject, "dct:title", DCT_NAMESPACE + "title");
			addDcatProperty(xml, metadata, subject, "dct:description", DCT_NAMESPACE + "description");
			addDcatProperty(xml, metadata, subject, "dcat:keyword", DCAT_NAMESPACE + "keyword");
			addDcatProperty(xml, metadata, subject, "dct:issued", DCT_NAMESPACE + "issued");
			addDcatProperty(xml, metadata, subject, "dct:modified", DCT_NAMESPACE + "modified");
			
			// Add references
			addDcatReference(xml, metadata, subject, "dcat:theme", DCAT_NAMESPACE + "theme");
			addDcatReference(xml, metadata, subject, "dct:publisher", DCT_NAMESPACE + "publisher");
			addDcatReference(xml, metadata, subject, "dcat:distribution", DCAT_NAMESPACE + "distribution");
			addDcatReference(xml, metadata, subject, "dcat:contactPoint", DCAT_NAMESPACE + "contactPoint");
		}

		xml.append("        </dcat:").append(typeLocalName).append(">\n");
		return xml.toString();
	}

	private void addDcatProperty(StringBuilder xml, Model model, Resource subject, 
			String element, String propertyUri) {
		model.listStatements(subject, model.getProperty(propertyUri), (RDFNode) null)
			.forEachRemaining(stmt -> {
				RDFNode obj = stmt.getObject();
				if (obj.isLiteral()) {
					xml.append("          <").append(element).append(">")
						.append(escapeXml(obj.asLiteral().getString()))
						.append("</").append(element).append(">\n");
				}
			});
	}

	private void addDcatReference(StringBuilder xml, Model model, Resource subject,
			String element, String propertyUri) {
		model.listStatements(subject, model.getProperty(propertyUri), (RDFNode) null)
			.forEachRemaining(stmt -> {
				RDFNode obj = stmt.getObject();
				if (obj.isResource() && obj.asResource().isURIResource()) {
					xml.append("          <").append(element).append(" rdf:resource=\"")
						.append(escapeXml(obj.asResource().getURI())).append("\"/>\n");
				}
			});
	}

	private String buildEposDcatApMetadata(OaiPmhRecord record) {
		StringBuilder xml = new StringBuilder();
		
		Model metadata = record.getMetadata();
		if (metadata != null && metadata.size() > 0) {
			// Serialize the RDF model as RDF/XML
			xml.append("        <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
			xml.append("                 xmlns:dcat=\"").append(DCAT_NAMESPACE).append("\"\n");
			xml.append("                 xmlns:dct=\"").append(DCT_NAMESPACE).append("\"\n");
			xml.append("                 xmlns:epos=\"").append(EPOS_DCAT_AP_NAMESPACE).append("\"\n");
			xml.append("                 xmlns:schema=\"").append(SCHEMA_NAMESPACE).append("\"\n");
			xml.append("                 xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\"\n");
			xml.append("                 xmlns:hydra=\"http://www.w3.org/ns/hydra/core#\"\n");
			xml.append("                 xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n");
			xml.append("                 xmlns:locn=\"http://www.w3.org/ns/locn#\"\n");
			xml.append("                 xmlns:gsp=\"http://www.opengis.net/ont/geosparql#\"\n");
			xml.append("                 xmlns:adms=\"http://www.w3.org/ns/adms#\"\n");
			xml.append("                 xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n");
			
			// Serialize the model content
			StringWriter writer = new StringWriter();
			RDFDataMgr.write(writer, metadata, Lang.RDFXML);
			String rdfXml = writer.toString();
			
			// Extract just the content between <rdf:RDF> tags
			int startIdx = rdfXml.indexOf("<rdf:RDF");
			int endIdx = rdfXml.lastIndexOf("</rdf:RDF>");
			if (startIdx >= 0 && endIdx > startIdx) {
				// Find the end of the opening tag
				int contentStart = rdfXml.indexOf(">", startIdx) + 1;
				String content = rdfXml.substring(contentStart, endIdx).trim();
				// Indent the content
				for (String line : content.split("\n")) {
					if (!line.trim().isEmpty()) {
						xml.append("          ").append(line).append("\n");
					}
				}
			}
			
			xml.append("        </rdf:RDF>\n");
		} else {
			// Fallback to minimal RDF
			xml.append("        <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n");
			xml.append("          <rdf:Description rdf:about=\"").append(escapeXml(record.getIdentifier())).append("\">\n");
			xml.append("            <rdf:type rdf:resource=\"").append(escapeXml(record.getRdfType())).append("\"/>\n");
			xml.append("          </rdf:Description>\n");
			xml.append("        </rdf:RDF>\n");
		}
		
		return xml.toString();
	}

	// ==================== Helper Methods ====================

	private String extractDatestamp(QuerySolution sol) {
		// Try different date properties in priority order
		String[] dateVars = {"modified", "issued", "created", "schemaModified", "schemaPublished"};
		for (String var : dateVars) {
			RDFNode node = sol.get(var);
			if (node != null && node.isLiteral()) {
				String dateStr = node.asLiteral().getString();
				return formatDateForOai(dateStr);
			}
		}
		// Fallback to current timestamp
		return getCurrentTimestamp();
	}

	private String formatDateForOai(String dateStr) {
		if (dateStr == null) return getCurrentTimestamp();
		
		// Try to parse and reformat
		try {
			if (dateStr.contains("T")) {
				// Already has time component
				LocalDateTime dt = LocalDateTime.parse(dateStr.substring(0, Math.min(19, dateStr.length())));
				return dt.format(OAI_DATE_FORMAT);
			} else {
				// Date only - add time
				return dateStr + "T00:00:00Z";
			}
		} catch (DateTimeParseException e) {
			return getCurrentTimestamp();
		}
	}

	private List<String> buildSetSpecs(String identifier, String typeUri) {
		List<String> setSpecs = new ArrayList<>();
		
		// Add type-based set
		if (typeUri != null) {
			String localName = getLocalName(typeUri);
			setSpecs.add("type:" + localName);
		}
		
		// Add category-based sets
		List<String> categories = getRecordCategories(identifier);
		for (String categoryUri : categories) {
			setSpecs.add("category:" + encodeSetSpec(categoryUri));
		}
		
		return setSpecs;
	}

	private String resolveTypeFromSetSpec(String typeLocalName) {
		// Map local names to full prefixed types
		for (String prefixedType : OaiPmhSparqlQueries.HARVESTABLE_TYPES) {
			String fullUri = OaiPmhSparqlQueries.expandTypeUri(prefixedType);
			if (getLocalName(fullUri).equals(typeLocalName)) {
				return prefixedType;
			}
		}
		return null;
	}

	private String getLocalName(String uri) {
		if (uri == null) return "";
		int hashIndex = uri.lastIndexOf('#');
		int slashIndex = uri.lastIndexOf('/');
		int index = Math.max(hashIndex, slashIndex);
		return index >= 0 ? uri.substring(index + 1) : uri;
	}

	private boolean isValidMetadataPrefix(String metadataPrefix) {
		return "oai_dc".equals(metadataPrefix) 
			|| "dcat".equals(metadataPrefix) 
			|| "epos_dcat_ap".equals(metadataPrefix);
	}

	private String getCurrentTimestamp() {
		return LocalDateTime.now(ZoneOffset.UTC).format(OAI_DATE_FORMAT);
	}

	private String getEarliestDatestamp() {
		return "2020-01-01T00:00:00Z";
	}

	private String getBaseUrl(String requestUrl) {
		if (baseUrl != null && !baseUrl.isEmpty()) {
			return baseUrl;
		}
		int queryIndex = requestUrl.indexOf('?');
		return queryIndex > 0 ? requestUrl.substring(0, queryIndex) : requestUrl;
	}

	private String encodeSetSpec(String uid) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(uid.getBytes());
	}

	private String decodeSetSpec(String setSpec) {
		try {
			return new String(Base64.getUrlDecoder().decode(setSpec));
		} catch (Exception e) {
			return setSpec;
		}
	}

	private String escapeXml(String text) {
		if (text == null) return "";
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&apos;");
	}

	// ==================== Resumption Token Handling ====================

	private String buildResumptionToken(int offset, String metadataPrefix, String set, 
			String from, String until) {
		StringBuilder token = new StringBuilder();
		token.append(offset).append("|");
		token.append(metadataPrefix != null ? metadataPrefix : "").append("|");
		token.append(set != null ? set : "").append("|");
		token.append(from != null ? from : "").append("|");
		token.append(until != null ? until : "");
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token.toString().getBytes());
	}

	private ResumptionTokenData parseResumptionToken(String token) {
		try {
			String decoded = new String(Base64.getUrlDecoder().decode(token));
			String[] parts = decoded.split("\\|", -1);
			if (parts.length < 5) {
				return null;
			}
			ResumptionTokenData data = new ResumptionTokenData();
			data.offset = Integer.parseInt(parts[0]);
			data.metadataPrefix = parts[1].isEmpty() ? null : parts[1];
			data.set = parts[2].isEmpty() ? null : parts[2];
			data.from = parts[3].isEmpty() ? null : parts[3];
			data.until = parts[4].isEmpty() ? null : parts[4];
			return data;
		} catch (Exception e) {
			LOGGER.warn("[OAI-PMH] Failed to parse resumption token: {}", e.getMessage());
			return null;
		}
	}

	private static class ResumptionTokenData {
		int offset;
		String metadataPrefix;
		String set;
		String from;
		String until;
	}

	private static class CategoryInfo {
		String uri;
		String label;
		String description;
	}
}
