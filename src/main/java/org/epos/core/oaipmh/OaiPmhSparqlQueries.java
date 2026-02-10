package org.epos.core.oaipmh;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SPARQL query templates for OAI-PMH operations.
 * Provides methods to generate SPARQL queries for listing records, 
 * retrieving single records, and listing sets from the triplestore.
 */
public class OaiPmhSparqlQueries {

	// Namespaces used in queries
	private static final String PREFIXES = """
		PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
		PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
		PREFIX dcat: <http://www.w3.org/ns/dcat#>
		PREFIX dct: <http://purl.org/dc/terms/>
		PREFIX schema: <http://schema.org/>
		PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
		PREFIX epos: <https://www.epos-eu.org/epos-dcat-ap#>
		PREFIX hydra: <http://www.w3.org/ns/hydra/core#>
		PREFIX foaf: <http://xmlns.com/foaf/0.1/>
		PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
		""";

	/**
	 * List of RDF types that are exposed as OAI-PMH records.
	 * Only named resources (not blank nodes) of these types are harvestable.
	 */
	public static final List<String> HARVESTABLE_TYPES = Arrays.asList(
		"dcat:Dataset",
		"dcat:Distribution",
		"dcat:Catalog",
		"dcat:CatalogRecord",
		"dcat:DataService",
		"schema:Organization",
		"schema:Person",
		"schema:ContactPoint",
		"schema:PostalAddress",
		"schema:SoftwareApplication",
		"schema:SoftwareSourceCode",
		"epos:WebService",
		"epos:Equipment",
		"epos:Facility",
		"hydra:Operation",
		"hydra:IriTemplate",
		"hydra:IriTemplateMapping",
		"skos:Concept",
		"skos:ConceptScheme",
		"foaf:Project",
		"dct:Location",
		"dct:PeriodOfTime"
	);

	/**
	 * Maps RDF type prefixed names to full URIs.
	 */
	public static String expandTypeUri(String prefixedType) {
		if (prefixedType == null) return null;
		return switch (prefixedType.split(":")[0]) {
			case "dcat" -> "http://www.w3.org/ns/dcat#" + prefixedType.split(":")[1];
			case "schema" -> "http://schema.org/" + prefixedType.split(":")[1];
			case "epos" -> "https://www.epos-eu.org/epos-dcat-ap#" + prefixedType.split(":")[1];
			case "hydra" -> "http://www.w3.org/ns/hydra/core#" + prefixedType.split(":")[1];
			case "skos" -> "http://www.w3.org/2004/02/skos/core#" + prefixedType.split(":")[1];
			case "foaf" -> "http://xmlns.com/foaf/0.1/" + prefixedType.split(":")[1];
			case "dct" -> "http://purl.org/dc/terms/" + prefixedType.split(":")[1];
			default -> prefixedType;
		};
	}

	/**
	 * Maps full URIs to prefixed type names.
	 */
	public static String compactTypeUri(String fullUri) {
		if (fullUri == null) return null;
		if (fullUri.startsWith("http://www.w3.org/ns/dcat#")) {
			return "dcat:" + fullUri.substring("http://www.w3.org/ns/dcat#".length());
		}
		if (fullUri.startsWith("http://schema.org/")) {
			return "schema:" + fullUri.substring("http://schema.org/".length());
		}
		if (fullUri.startsWith("https://www.epos-eu.org/epos-dcat-ap#")) {
			return "epos:" + fullUri.substring("https://www.epos-eu.org/epos-dcat-ap#".length());
		}
		if (fullUri.startsWith("http://www.w3.org/ns/hydra/core#")) {
			return "hydra:" + fullUri.substring("http://www.w3.org/ns/hydra/core#".length());
		}
		if (fullUri.startsWith("http://www.w3.org/2004/02/skos/core#")) {
			return "skos:" + fullUri.substring("http://www.w3.org/2004/02/skos/core#".length());
		}
		if (fullUri.startsWith("http://xmlns.com/foaf/0.1/")) {
			return "foaf:" + fullUri.substring("http://xmlns.com/foaf/0.1/".length());
		}
		if (fullUri.startsWith("http://purl.org/dc/terms/")) {
			return "dct:" + fullUri.substring("http://purl.org/dc/terms/".length());
		}
		return fullUri;
	}

	/**
	 * Generates a SPARQL query to list all harvestable records with optional filtering.
	 *
	 * @param typeFilter    RDF type to filter by (e.g., "dcat:Dataset"), or null for all types
	 * @param categoryUri   Category URI to filter by, or null for all categories
	 * @param fromDate      Lower bound for date filter (ISO 8601), or null
	 * @param untilDate     Upper bound for date filter (ISO 8601), or null
	 * @param offset        Number of records to skip (for pagination)
	 * @param limit         Maximum number of records to return
	 * @return SPARQL SELECT query string
	 */
	public static String listRecords(String typeFilter, String categoryUri, 
			String fromDate, String untilDate, int offset, int limit) {
		
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT DISTINCT ?subject ?type ?modified ?issued ?created WHERE {\n");
		
		// Type constraint
		if (typeFilter != null && !typeFilter.isEmpty()) {
			query.append("  ?subject a ").append(typeFilter).append(" .\n");
			query.append("  BIND(").append(typeFilter).append(" AS ?type)\n");
		} else {
			// Filter to only harvestable types
			String typeUnion = HARVESTABLE_TYPES.stream()
				.map(t -> "{ ?subject a " + t + " . BIND(" + t + " AS ?type) }")
				.collect(Collectors.joining(" UNION "));
			query.append("  ").append(typeUnion).append("\n");
		}
		
		// Exclude blank nodes - only named resources
		query.append("  FILTER(isIRI(?subject))\n");
		
		// Category filter
		if (categoryUri != null && !categoryUri.isEmpty()) {
			query.append("  ?subject dcat:theme <").append(categoryUri).append("> .\n");
		}
		
		// Date properties (optional)
		query.append("  OPTIONAL { ?subject dct:modified ?modified }\n");
		query.append("  OPTIONAL { ?subject dct:issued ?issued }\n");
		query.append("  OPTIONAL { ?subject dct:created ?created }\n");
		query.append("  OPTIONAL { ?subject schema:dateModified ?schemaModified }\n");
		query.append("  OPTIONAL { ?subject schema:datePublished ?schemaPublished }\n");
		
		// Date range filter
		if (fromDate != null || untilDate != null) {
			query.append("  BIND(COALESCE(?modified, ?issued, ?created, ?schemaModified, ?schemaPublished) AS ?effectiveDate)\n");
			if (fromDate != null) {
				query.append("  FILTER(!BOUND(?effectiveDate) || ?effectiveDate >= \"")
					.append(fromDate).append("\"^^xsd:dateTime)\n");
			}
			if (untilDate != null) {
				query.append("  FILTER(!BOUND(?effectiveDate) || ?effectiveDate <= \"")
					.append(untilDate).append("\"^^xsd:dateTime)\n");
			}
		}
		
		query.append("}\n");
		query.append("ORDER BY ?subject\n");
		query.append("OFFSET ").append(offset).append("\n");
		query.append("LIMIT ").append(limit).append("\n");
		
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to count all harvestable records with optional filtering.
	 *
	 * @param typeFilter    RDF type to filter by, or null for all types
	 * @param categoryUri   Category URI to filter by, or null for all categories
	 * @param fromDate      Lower bound for date filter, or null
	 * @param untilDate     Upper bound for date filter, or null
	 * @return SPARQL SELECT query string returning count
	 */
	public static String countRecords(String typeFilter, String categoryUri, 
			String fromDate, String untilDate) {
		
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT (COUNT(DISTINCT ?subject) AS ?count) WHERE {\n");
		
		// Type constraint
		if (typeFilter != null && !typeFilter.isEmpty()) {
			query.append("  ?subject a ").append(typeFilter).append(" .\n");
		} else {
			String typeUnion = HARVESTABLE_TYPES.stream()
				.map(t -> "{ ?subject a " + t + " }")
				.collect(Collectors.joining(" UNION "));
			query.append("  ").append(typeUnion).append("\n");
		}
		
		// Exclude blank nodes
		query.append("  FILTER(isIRI(?subject))\n");
		
		// Category filter
		if (categoryUri != null && !categoryUri.isEmpty()) {
			query.append("  ?subject dcat:theme <").append(categoryUri).append("> .\n");
		}
		
		// Date range filter
		if (fromDate != null || untilDate != null) {
			query.append("  OPTIONAL { ?subject dct:modified ?modified }\n");
			query.append("  OPTIONAL { ?subject dct:issued ?issued }\n");
			query.append("  OPTIONAL { ?subject dct:created ?created }\n");
			query.append("  BIND(COALESCE(?modified, ?issued, ?created) AS ?effectiveDate)\n");
			if (fromDate != null) {
				query.append("  FILTER(!BOUND(?effectiveDate) || ?effectiveDate >= \"")
					.append(fromDate).append("\"^^xsd:dateTime)\n");
			}
			if (untilDate != null) {
				query.append("  FILTER(!BOUND(?effectiveDate) || ?effectiveDate <= \"")
					.append(untilDate).append("\"^^xsd:dateTime)\n");
			}
		}
		
		query.append("}\n");
		
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to retrieve a single record by identifier.
	 *
	 * @param identifier The resource URI
	 * @return SPARQL SELECT query string
	 */
	public static String getRecord(String identifier) {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT ?type ?modified ?issued ?created WHERE {\n");
		query.append("  <").append(identifier).append("> a ?type .\n");
		query.append("  OPTIONAL { <").append(identifier).append("> dct:modified ?modified }\n");
		query.append("  OPTIONAL { <").append(identifier).append("> dct:issued ?issued }\n");
		query.append("  OPTIONAL { <").append(identifier).append("> dct:created ?created }\n");
		query.append("  OPTIONAL { <").append(identifier).append("> schema:dateModified ?schemaModified }\n");
		query.append("  OPTIONAL { <").append(identifier).append("> schema:datePublished ?schemaPublished }\n");
		query.append("}\n");
		query.append("LIMIT 1\n");
		return query.toString();
	}

	/**
	 * Generates a SPARQL CONSTRUCT query to retrieve the full RDF for a record.
	 * Includes the resource itself and any blank nodes connected to it.
	 *
	 * @param identifier The resource URI
	 * @return SPARQL CONSTRUCT query string
	 */
	public static String constructRecord(String identifier) {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("CONSTRUCT {\n");
		query.append("  <").append(identifier).append("> ?p ?o .\n");
		query.append("  ?o ?p2 ?o2 .\n");
		query.append("  ?o2 ?p3 ?o3 .\n");
		query.append("} WHERE {\n");
		query.append("  <").append(identifier).append("> ?p ?o .\n");
		query.append("  OPTIONAL {\n");
		query.append("    ?o ?p2 ?o2 .\n");
		query.append("    FILTER(isBlank(?o))\n");
		query.append("    OPTIONAL {\n");
		query.append("      ?o2 ?p3 ?o3 .\n");
		query.append("      FILTER(isBlank(?o2))\n");
		query.append("    }\n");
		query.append("  }\n");
		query.append("}\n");
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to get the categories (themes) for a record.
	 *
	 * @param identifier The resource URI
	 * @return SPARQL SELECT query string
	 */
	public static String getRecordCategories(String identifier) {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT ?category WHERE {\n");
		query.append("  <").append(identifier).append("> dcat:theme ?category .\n");
		query.append("}\n");
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to list all entity types present in the triplestore.
	 *
	 * @return SPARQL SELECT query string
	 */
	public static String listEntityTypes() {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT DISTINCT ?type (COUNT(?subject) AS ?count) WHERE {\n");
		
		String typeUnion = HARVESTABLE_TYPES.stream()
			.map(t -> "{ ?subject a " + t + " . BIND(" + t + " AS ?type) }")
			.collect(Collectors.joining(" UNION "));
		query.append("  ").append(typeUnion).append("\n");
		query.append("  FILTER(isIRI(?subject))\n");
		query.append("}\n");
		query.append("GROUP BY ?type\n");
		query.append("ORDER BY ?type\n");
		
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to list all categories (skos:Concept and skos:ConceptScheme).
	 *
	 * @return SPARQL SELECT query string
	 */
	public static String listCategories() {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT ?category ?label ?description ?broader ?inScheme WHERE {\n");
		query.append("  { ?category a skos:Concept } UNION { ?category a skos:ConceptScheme }\n");
		query.append("  FILTER(isIRI(?category))\n");
		query.append("  OPTIONAL { ?category skos:prefLabel ?label }\n");
		query.append("  OPTIONAL { ?category rdfs:label ?rdfsLabel }\n");
		query.append("  OPTIONAL { ?category dct:description ?description }\n");
		query.append("  OPTIONAL { ?category skos:broader ?broader }\n");
		query.append("  OPTIONAL { ?category skos:inScheme ?inScheme }\n");
		query.append("}\n");
		query.append("ORDER BY ?category\n");
		
		return query.toString();
	}

	/**
	 * Generates a SPARQL query to get Dublin Core metadata for a record.
	 * Used to build oai_dc format responses.
	 *
	 * @param identifier The resource URI
	 * @return SPARQL SELECT query string
	 */
	public static String getDublinCoreMetadata(String identifier) {
		StringBuilder query = new StringBuilder(PREFIXES);
		query.append("SELECT ?title ?creator ?subject ?description ?publisher ");
		query.append("?date ?type ?identifier ?rights ?format ?language ?relation WHERE {\n");
		query.append("  BIND(<").append(identifier).append("> AS ?resource)\n");
		
		// Title
		query.append("  OPTIONAL { ?resource dct:title ?title }\n");
		query.append("  OPTIONAL { ?resource schema:name ?schemaName }\n");
		query.append("  OPTIONAL { ?resource rdfs:label ?rdfsLabel }\n");
		
		// Creator/Publisher
		query.append("  OPTIONAL { ?resource dct:creator ?creator }\n");
		query.append("  OPTIONAL { ?resource dct:publisher ?publisher }\n");
		query.append("  OPTIONAL { ?resource schema:provider ?provider }\n");
		query.append("  OPTIONAL { ?resource schema:manufacturer ?manufacturer }\n");
		
		// Subject (categories and keywords)
		query.append("  OPTIONAL { ?resource dcat:theme ?themeSubject }\n");
		query.append("  OPTIONAL { ?resource dcat:keyword ?keywordSubject }\n");
		query.append("  OPTIONAL { ?resource schema:keywords ?schemaKeywords }\n");
		
		// Description
		query.append("  OPTIONAL { ?resource dct:description ?description }\n");
		query.append("  OPTIONAL { ?resource schema:description ?schemaDescription }\n");
		
		// Date
		query.append("  OPTIONAL { ?resource dct:issued ?issued }\n");
		query.append("  OPTIONAL { ?resource dct:modified ?modified }\n");
		query.append("  OPTIONAL { ?resource dct:created ?created }\n");
		query.append("  OPTIONAL { ?resource schema:datePublished ?datePublished }\n");
		
		// Type
		query.append("  OPTIONAL { ?resource dct:type ?dctType }\n");
		query.append("  OPTIONAL { ?resource a ?rdfType }\n");
		
		// Identifier
		query.append("  OPTIONAL { ?resource dct:identifier ?dctIdentifier }\n");
		query.append("  OPTIONAL { ?resource schema:identifier ?schemaIdentifier }\n");
		
		// Rights
		query.append("  OPTIONAL { ?resource dct:rights ?rights }\n");
		query.append("  OPTIONAL { ?resource dct:accessRights ?accessRights }\n");
		query.append("  OPTIONAL { ?resource dct:license ?license }\n");
		
		// Format
		query.append("  OPTIONAL { ?resource dct:format ?format }\n");
		
		// Language
		query.append("  OPTIONAL { ?resource dct:language ?language }\n");
		
		// Relation
		query.append("  OPTIONAL { ?resource dct:relation ?relation }\n");
		
		query.append("}\n");
		
		return query.toString();
	}
}
