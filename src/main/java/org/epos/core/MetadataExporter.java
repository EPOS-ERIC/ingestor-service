package org.epos.core;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.IriTemplate;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;

public class MetadataExporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExporter.class);
	private static final int MAX_DEPTH = 20;
	private static final int MAX_ENTITIES = 1000;
	private static Model shaclModel = null;

	public static String exportToRDF(
			EntityNames entityType,
			String mappingName,
			String format,
			List<String> ids,
			Boolean includeLinked) {

		if (mappingName == null || mappingName.trim().isEmpty()) {
			throw new IllegalArgumentException("Mapping name cannot be null or empty");
		}

		if (format == null || format.trim().isEmpty()) {
			format = "turtle";
		}

		if (!format.matches("(?i)(turtle|json-ld)")) {
			throw new IllegalArgumentException("Format must be one of: turtle, json-ld");
		}

		if (ids != null && !ids.isEmpty() && entityType == null) {
			throw new IllegalArgumentException("Entity type must be specified when providing specific IDs");
		}

		try {
			LOGGER.info("Starting export for entity type '{}' using mapping '{}' in format '{}'",
					entityType != null ? entityType : "all types", mappingName, format);

			Model mappingModel = MetadataPopulator.retrieveModelMapping(mappingName);
			LOGGER.debug("Retrieved original mapping with {} triples", mappingModel.size());

			if (shaclModel == null) {
				shaclModel = RDFDataMgr.loadModel(
						"https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/refs/heads/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl");
				LOGGER.debug("Loaded SHACL model with {} triples", shaclModel.size());
			}

			List<EPOSDataModelEntity> entities;
			if (entityType != null) {
				entities = retrieveEntities(entityType, ids);
				LOGGER.debug("Retrieved {} entities of type '{}' from database", entities.size(), entityType);
			} else {
				entities = retrieveAllEntities(ids);
				LOGGER.debug("Retrieved {} entities from all types from database", entities.size());
			}

			if (entities.isEmpty()) {
				LOGGER.info("No entities found for type: {}", entityType != null ? entityType : "all types");
				return "";
			}

			// Only collect linked entities when exporting a specific type
			// When exporting all entities (entityType=null), all entities are already
			// retrieved
			if (includeLinked && entityType != null) {
				entities = collectAllLinkedEntities(entities);
				LOGGER.debug("After collecting linked entities: {} total entities", entities.size());
			}

			Model rdfModel = ModelFactory.createDefaultModel();

			rdfModel.setNsPrefix("adms", "http://www.w3.org/ns/adms#");
			rdfModel.setNsPrefix("dash", "http://datashapes.org/dash#");
			rdfModel.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
			rdfModel.setNsPrefix("dcat", "http://www.w3.org/ns/dcat#");
			rdfModel.setNsPrefix("dct", "http://purl.org/dc/terms/");
			rdfModel.setNsPrefix("epos", "https://www.epos-eu.org/epos-dcat-ap#");
			rdfModel.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
			rdfModel.setNsPrefix("cnt", "http://www.w3.org/2011/content#");
			rdfModel.setNsPrefix("oa", "http://www.w3.org/ns/oa#");
			rdfModel.setNsPrefix("org", "http://www.w3.org/ns/org#");
			rdfModel.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
			rdfModel.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			rdfModel.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			rdfModel.setNsPrefix("schema", "http://schema.org/");
			rdfModel.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
			rdfModel.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
			rdfModel.setNsPrefix("spdx", "http://spdx.org/rdf/terms#");
			rdfModel.setNsPrefix("vcard", "http://www.w3.org/2006/vcard/ns#");
			rdfModel.setNsPrefix("hydra", "http://www.w3.org/ns/hydra/core#");
			rdfModel.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
			rdfModel.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
			rdfModel.setNsPrefix("http", "http://www.w3.org/2006/http#");
			rdfModel.setNsPrefix("locn", "http://www.w3.org/ns/locn#");
			rdfModel.setNsPrefix("gsp", "http://www.opengis.net/ont/geosparql#");
			rdfModel.setNsPrefix("dqv", "http://www.w3.org/ns/dqv#");

			// Create map for lookups
			Map<String, EPOSDataModelEntity> entityMap = entities.stream()
					.collect(Collectors.toMap(EPOSDataModelEntity::getUid, e -> e, (e1, e2) -> e1));

			// We only iterate over the requested roots, but we use the full set for
			// resolution
			List<EPOSDataModelEntity> rootEntities;
			if (entityType != null) {
				rootEntities = retrieveEntities(entityType, ids);
			} else {
				rootEntities = retrieveAllEntities(ids);
			}

			// Filter out blank nodes from roots to avoid standalone export
			rootEntities = rootEntities.stream()
					.filter(e -> !e.getUid().startsWith("_:"))
					.collect(Collectors.toList());

			// Shared set of processed entities to avoid duplication across the entire
			// export
			Set<String> processedEntities = new HashSet<>();

			int processedCount = 0;
			for (EPOSDataModelEntity entity : rootEntities) {
				LOGGER.debug("Converting entity {} of {}: {}", ++processedCount, rootEntities.size(), entity.getUid());
				Resource subject = rdfModel.createResource(entity.getUid());
				convertEntityToRDF(entity, subject, rdfModel, mappingModel, shaclModel, processedEntities, null,
						entityMap);
			}
			LOGGER.debug("Converted {} entities to RDF triples", entities.size());
			LOGGER.debug("RDF model now has {} statements", rdfModel.size());

			StringWriter writer = new StringWriter();
			Lang lang = getLangForFormat(format);
			RDFDataMgr.write(writer, rdfModel, lang);
			String content = writer.toString();
			LOGGER.info("Serialized RDF model to {} characters of {} content", content.length(), format);

			return cleanupPrefixes(content);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error during export: {}", e.getLocalizedMessage());
			e.printStackTrace();
			throw new RuntimeException("Export failed", e);
		}
	}

	private static RDFDatatype findDatatypeConstraint(String classUri, String propertyUri, Model shaclModel) {
		if (classUri == null || propertyUri == null || shaclModel == null) {
			return null;
		}
		String queryString = "PREFIX sh: <http://www.w3.org/ns/shacl#> " +
				"SELECT ?datatype WHERE { " +
				"  ?shape sh:targetClass <" + classUri + "> ; " +
				"         sh:property [ sh:path <" + propertyUri + "> ; " +
				"                       sh:datatype ?datatype ] . " +
				"}";
		try {
			var query = QueryFactory.create(queryString);
			var qexec = QueryExecutionFactory.create(query, shaclModel);
			var results = qexec.execSelect();
			if (results.hasNext()) {
				var soln = results.next();
				String datatypeUri = soln.get("datatype").toString();
				return mapUriToRDFDatatype(datatypeUri);
			}
		} catch (Exception e) {
			LOGGER.debug("Error querying SHACL for {}.{}: {}", classUri, propertyUri, e.getLocalizedMessage());
		}
		return null;
	}

	private static List<String> findClassConstraints(String classUri, String propertyUri, Model shaclModel) {
		if (classUri == null || propertyUri == null || shaclModel == null) {
			return null;
		}
		List<String> classes = new ArrayList<>();
		String queryString = "PREFIX sh: <http://www.w3.org/ns/shacl#> " +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
				"SELECT ?class WHERE { " +
				"  ?shape sh:targetClass <" + classUri + "> ; " +
				"         sh:property [ sh:path <" + propertyUri + "> ; " +
				"                       sh:or ?or ] . " +
				"  ?or (rdf:rest*/rdf:first) [ sh:class ?class ] . " +
				"}";
		try {
			var query = QueryFactory.create(queryString);
			var qexec = QueryExecutionFactory.create(query, shaclModel);
			var results = qexec.execSelect();
			while (results.hasNext()) {
				var soln = results.next();
				String classUriResult = soln.get("class").toString();
				classes.add(classUriResult);
			}
		} catch (Exception e) {
			LOGGER.debug("Error querying SHACL for class constraints on {}.{}: {}", classUri, propertyUri,
					e.getLocalizedMessage());
		}
		return classes.isEmpty() ? null : classes;
	}

	private static String selectBestClass(List<String> allowedClasses, EPOSDataModelEntity entity) {
		String propertyValueUri = "http://schema.org/PropertyValue";
		String identifierUri = "http://www.w3.org/ns/adms#Identifier";

		// Check if entity has propertyID-like data
		try {
			Method getTypeMethod = entity.getClass().getMethod("getType");
			Object typeValue = getTypeMethod.invoke(entity);
			if (typeValue instanceof String && !isUriLike((String) typeValue)
					&& allowedClasses.contains(propertyValueUri)) {
				return propertyValueUri;
			}
		} catch (Exception e) {
			// Ignore, not propertyID
		}

		// Prefer Identifier if allowed
		if (allowedClasses.contains(identifierUri)) {
			return identifierUri;
		}

		// Else, return the first allowed
		return allowedClasses.get(0);
	}

	private static RDFDatatype mapUriToRDFDatatype(String uri) {
		if (uri.startsWith("http://www.w3.org/2001/XMLSchema#")) {
			switch (uri) {
				case "http://www.w3.org/2001/XMLSchema#string":
					return XSDDatatype.XSDstring;
				case "http://www.w3.org/2001/XMLSchema#anyURI":
					return XSDDatatype.XSDanyURI;
				case "http://www.w3.org/2001/XMLSchema#integer":
					return XSDDatatype.XSDinteger;
				case "http://www.w3.org/2001/XMLSchema#boolean":
					return XSDDatatype.XSDboolean;
				case "http://www.w3.org/2001/XMLSchema#date":
					return XSDDatatype.XSDdate;
				case "http://www.w3.org/2001/XMLSchema#dateTime":
					return XSDDatatype.XSDdateTime;
				case "http://www.w3.org/2001/XMLSchema#decimal":
					return XSDDatatype.XSDdecimal;
				default:
					LOGGER.debug("Unknown XSD datatype URI: {}", uri);
					return null;
			}
		} else {
			return new BaseDatatype(uri);
		}
	}

	private static String getSubjectClassUri(Resource subject) {
		var typeStmt = subject
				.getProperty(ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
		if (typeStmt != null && typeStmt.getObject().isURIResource()) {
			return typeStmt.getObject().asResource().getURI();
		}
		return null;
	}

	private static boolean isUriLike(String value) {
		return value != null && (value.startsWith("http://") || value.startsWith("https://"));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<EPOSDataModelEntity> retrieveEntities(EntityNames entityType, List<String> ids) {
		try {
			LOGGER.debug("Retrieving API for entity type '{}'", entityType.name());
			AbstractAPI api = AbstractAPI.retrieveAPI(entityType.name());

			List<EPOSDataModelEntity> entities;
			if (ids != null && !ids.isEmpty()) {
				LOGGER.debug("Retrieving {} specific entities by IDs: {}", ids.size(), ids);
				entities = ids.stream()
						.map(id -> {
							LOGGER.debug("Retrieving entity with ID: {}", id);
							return (EPOSDataModelEntity) api.retrieveByUID(id);
						})
						.filter(Objects::nonNull)
						.collect(Collectors.toList());
				LOGGER.debug("Retrieved {} entities out of {} requested IDs", entities.size(), ids.size());
			} else {
				LOGGER.debug("Retrieving all entities of type '{}'", entityType);
				entities = (List<EPOSDataModelEntity>) api.retrieveAll();
				LOGGER.debug("Retrieved {} entities from retrieveAll()", entities.size());
			}

			return entities;
		} catch (Exception e) {
			LOGGER.error("Error retrieving entities of type {}: {}", entityType, e.getLocalizedMessage());
			throw new RuntimeException("Failed to retrieve entities", e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<EPOSDataModelEntity> retrieveAllEntities(List<String> ids) {
		try {
			List<EPOSDataModelEntity> allEntities = new ArrayList<>();

			for (EntityNames entityType : EntityNames.values()) {
				try {
					LOGGER.debug("Retrieving entities for type: {}", entityType);
					AbstractAPI api = AbstractAPI.retrieveAPI(entityType.name());

					List<EPOSDataModelEntity> entities;
					if (ids != null && !ids.isEmpty()) {
						LOGGER.debug("Retrieving {} specific entities by IDs for type {}", ids.size(), entityType);
						entities = ids.stream()
								.map(id -> {
									LOGGER.debug("Retrieving entity with ID: {} for type {}", id, entityType);
									return (EPOSDataModelEntity) api.retrieveByUID(id);
								})
								.filter(Objects::nonNull)
								.collect(Collectors.toList());
					} else {
						LOGGER.debug("Retrieving all entities for type {}", entityType);
						entities = (List<EPOSDataModelEntity>) api.retrieveAll();
					}

					LOGGER.debug("Retrieved {} entities for type {}", entities.size(), entityType);
					allEntities.addAll(entities);
				} catch (Exception e) {
					LOGGER.warn("Error retrieving entities for type {}: {}", entityType, e.getLocalizedMessage());
					e.printStackTrace();
					// Continue with other types even if one fails
				}
			}

			LOGGER.debug("Total entities retrieved from all types: {}", allEntities.size());
			return allEntities;
		} catch (Exception e) {
			LOGGER.error("Error retrieving entities from all types: {}", e.getLocalizedMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to retrieve entities from all types", e);
		}
	}

	private static void convertEntityToRDF(
			EPOSDataModelEntity entity,
			Resource subject,
			Model rdfModel,
			Model mappingModel,
			Model shaclModel,
			Set<String> processedEntities,
			String forcedClassUri,
			Map<String, EPOSDataModelEntity> entityMap) {
		if (processedEntities == null) {
			processedEntities = new HashSet<>();
		}

		if (processedEntities.contains(entity.getUid())) {
			LOGGER.debug("Skipping already processed entity: {}", entity.getUid());
			return; // Already processed, avoid infinite recursion
		}

		processedEntities.add(entity.getUid());
		LOGGER.debug("Starting conversion of entity: {} (class: {})", entity.getUid(),
				entity.getClass().getSimpleName());

		try {
			String edmClassName = entity.getClass().getSimpleName();
			String dcatClassUri = forcedClassUri != null ? forcedClassUri
					: findDCATClassUri(edmClassName, mappingModel);

			if (dcatClassUri == null) {
				LOGGER.warn("No DCAT mapping found for EDM class: {}", edmClassName);
				return;
			}

			LOGGER.debug("Found DCAT class mapping: {} -> {}", edmClassName, dcatClassUri);
			LOGGER.debug("Found DCAT class mapping: {} -> {}", edmClassName, dcatClassUri);
			// Resource subject is passed in
			Resource typeResource = rdfModel.createResource(dcatClassUri);
			rdfModel.add(
					subject,
					rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					typeResource);

			// Add dct:identifier for Dataset and Distribution using the UID
			if ("http://www.w3.org/ns/dcat#Dataset".equals(dcatClassUri) ||
					"http://www.w3.org/ns/dcat#Distribution".equals(dcatClassUri)) {
				Property identifierProp = rdfModel.createProperty("http://purl.org/dc/terms/identifier");
				rdfModel.add(subject, identifierProp,
						rdfModel.createTypedLiteral(entity.getUid(), XSDDatatype.XSDanyURI));
				LOGGER.debug("Added dct:identifier for {} using UID: {}", dcatClassUri, entity.getUid());
			}

			// Special handling for Operation to create proper IriTemplate structure
			if (entity instanceof Operation) {
				convertOperationToRDF((Operation) entity, subject, rdfModel, mappingModel, shaclModel,
						processedEntities,
						entityMap);
			}

			// Get all getter methods and convert properties
			Method[] methods = entity.getClass().getMethods();
			int processedProperties = 0;
			int mappedProperties = 0;

			for (Method method : methods) {
				if (method.getName().startsWith("get") && method.getParameterCount() == 0 &&
						!method.getName().equals("getClass") && !method.getName().equals("getUid")) {

					// Skip template and mapping properties for Operation as they are handled
					// specially
					if (entity instanceof Operation
							&& (method.getName().equals("getTemplate") || method.getName().equals("getMapping")
									|| method.getName().equals("getIriTemplateObject")
									|| method.getName().equals("getIriTemplate"))) {
						continue;
					}

					try {
						Object value = method.invoke(entity);
						if (value != null) {
							processedProperties++;
							String propertyName = method.getName().substring(3); // Remove "get"
							propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);

							String dcatPropertyUri = resolveDCATProperty(edmClassName, propertyName, dcatClassUri,
									mappingModel);
							if (dcatPropertyUri != null) {
								mappedProperties++;
								LOGGER.debug("Mapping property {}.{} -> {}", edmClassName, propertyName,
										dcatPropertyUri);
								addPropertyToRDF(
										subject,
										dcatPropertyUri,
										value,
										rdfModel,
										mappingModel,
										shaclModel,
										processedEntities,
										entityMap);
							} else {
								LOGGER.debug("No mapping found for property {}.{}", edmClassName, propertyName);
							}
						}
					} catch (Exception e) {
						LOGGER.warn("Error processing property {}: {}", method.getName(), e.getLocalizedMessage());
						e.printStackTrace();
					}
				}
			}

			LOGGER.debug("Processed {} properties for entity {}, {} were mapped to DCAT", processedProperties,
					entity.getUid(), mappedProperties);

		} catch (Exception e) {
			LOGGER.error("Error converting entity {} to RDF: {}", entity.getUid(), e.getLocalizedMessage());
			e.printStackTrace();
		}
	}

	private static void convertOperationToRDF(
			Operation op,
			Resource subject,
			Model rdfModel,
			Model mappingModel,
			Model shaclModel,
			Set<String> processedEntities,
			Map<String, EPOSDataModelEntity> entityMap) {
		IriTemplate iriTemplate = op.getIriTemplateObject();
		if (iriTemplate != null) {
			Resource templateNode = rdfModel.createResource();
			rdfModel.add(templateNode,
					rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					rdfModel.createResource("http://www.w3.org/ns/hydra/core#IriTemplate"));

			String templateStr = iriTemplate.getTemplate();
			if (templateStr != null) {
				rdfModel.add(templateNode, rdfModel.createProperty("http://www.w3.org/ns/hydra/core#template"),
						rdfModel.createTypedLiteral(templateStr, XSDDatatype.XSDstring));
			}

			List<LinkedEntity> mappingEntities = iriTemplate.getMappings();
			if (mappingEntities != null) {
				for (LinkedEntity le : mappingEntities) {
					addPropertyToRDF(templateNode, "http://www.w3.org/ns/hydra/core#mapping", le, rdfModel,
							mappingModel, shaclModel, processedEntities, entityMap);
				}
			}

			rdfModel.add(subject, rdfModel.createProperty("http://www.w3.org/ns/hydra/core#property"), templateNode);
		}
	}

	private static String findDCATClassUri(String edmClassName, Model mappingModel) {
		LOGGER.debug("Searching for DCAT class mapping for EDM class: {}", edmClassName);

		String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"PREFIX edm: <http://www.epos-eu.org/epos-data-model#>" +
				"SELECT ?dcatClass WHERE { " +
				"  edm:" + edmClassName + " owl:equivalentClass ?dcatClass . " +
				"}";

		try {
			var query = QueryFactory.create(queryString);
			var qexec = QueryExecutionFactory.create(query, mappingModel);
			var results = qexec.execSelect();

			if (results.hasNext()) {
				var soln = results.next();
				String dcatClassUri = soln.get("dcatClass").toString();
				LOGGER.debug("Found DCAT class mapping: {} -> {}", edmClassName, dcatClassUri);
				return dcatClassUri;
			} else {
				LOGGER.debug("No DCAT class mapping found for EDM class: {}", edmClassName);
			}
		} catch (Exception e) {
			LOGGER.warn("Error finding DCAT class for {}: {}", edmClassName, e.getLocalizedMessage());
			e.printStackTrace();
		}

		return null;
	}

	private static String findDCATPropertyUri(String edmClassName, String propertyName, Model mappingModel) {
		LOGGER.debug("Searching for DCAT property mapping for {}.{}", edmClassName, propertyName);

		String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"PREFIX edm: <http://www.epos-eu.org/epos-data-model#> " +
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
				"SELECT ?dcatProperty WHERE { " +
				"  edm:" + propertyName + " rdfs:domain edm:" + edmClassName + " . " +
				"  edm:" + propertyName + " owl:equivalentProperty ?dcatProperty . " +
				"}";

		LOGGER.debug("SPARQL Query: {}", queryString);

		try {
			var query = QueryFactory.create(queryString);
			var qexec = QueryExecutionFactory.create(query, mappingModel);
			var results = qexec.execSelect();

			List<String> foundMappings = new ArrayList<>();
			while (results.hasNext()) {
				var soln = results.next();
				String dcatPropertyUri = soln.get("dcatProperty").toString();
				foundMappings.add(dcatPropertyUri);
			}

			LOGGER.debug("Found {} mappings for {}.{}: {}", foundMappings.size(), edmClassName, propertyName,
					foundMappings);

			if (!foundMappings.isEmpty()) {
				String selectedMapping = selectBestPropertyMapping(foundMappings, edmClassName);
				LOGGER.debug("Selected DCAT property mapping: {}.{} -> {}", edmClassName, propertyName,
						selectedMapping);
				return selectedMapping;
			} else {
				LOGGER.debug("No DCAT property mapping found for {}.{}", edmClassName, propertyName);
			}
		} catch (Exception e) {
			LOGGER.debug("Error finding DCAT property for {}.{}: {}", edmClassName, propertyName,
					e.getLocalizedMessage());
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Selects the best property mapping from a list of candidates based on the EDM
	 * class.
	 * Prioritizes vocabulary-specific properties over generic schema.org
	 * properties.
	 */
	private static String selectBestPropertyMapping(List<String> mappings, String edmClassName) {
		// Define priority mappings for specific classes
		Map<String, List<String>> priorityPrefixes = new HashMap<>();

		// For SKOS Concepts, prefer skos: properties
		priorityPrefixes.put("Category", Arrays.asList(
				"http://www.w3.org/2004/02/skos/core#",
				"http://purl.org/dc/terms/",
				"http://schema.org/"));

		// For Hydra classes, prefer hydra: properties
		priorityPrefixes.put("Documentation", Arrays.asList(
				"http://www.w3.org/ns/hydra/core#",
				"http://purl.org/dc/terms/",
				"http://schema.org/"));

		// For DCAT classes, prefer dcat: and dct: properties
		priorityPrefixes.put("DataProduct", Arrays.asList(
				"http://www.w3.org/ns/dcat#",
				"http://purl.org/dc/terms/",
				"http://schema.org/"));

		priorityPrefixes.put("Distribution", Arrays.asList(
				"http://www.w3.org/ns/dcat#",
				"http://purl.org/dc/terms/",
				"http://schema.org/"));

		// For FOAF Agents, prefer foaf: properties
		priorityPrefixes.put("Organization", Arrays.asList(
				"http://xmlns.com/foaf/0.1/",
				"http://schema.org/"));

		// Get priority list for this class
		List<String> priorities = priorityPrefixes.get(edmClassName);

		if (priorities != null) {
			// Try each priority prefix in order
			for (String prefix : priorities) {
				for (String mapping : mappings) {
					if (mapping.startsWith(prefix)) {
						return mapping;
					}
				}
			}
		}

		// Fallback to first mapping if no priority match
		return mappings.get(0);
	}

	private static String resolveDCATProperty(String edmClassName, String propertyName, String dcatClassUri,
			Model mappingModel) {
		// Context-aware property mapping
		if ("http://schema.org/PropertyValue".equals(dcatClassUri)) {
			if ("type".equals(propertyName)) {
				return "http://schema.org/propertyID";
			} else if ("identifier".equals(propertyName)) {
				return "http://schema.org/value";
			}
		}

		// Default mapping
		return findDCATPropertyUri(edmClassName, propertyName, mappingModel);
	}

	private static void addPropertyToRDF(
			Resource subject,
			String propertyUri,
			Object value,
			Model rdfModel,
			Model mappingModel,
			Model shaclModel,
			Set<String> processedEntities,
			Map<String, EPOSDataModelEntity> entityMap) {
		Property property = ResourceFactory.createProperty(propertyUri);
		LOGGER.debug("Adding property {} with value type: {}", propertyUri, value.getClass().getSimpleName());

		String classUri = getSubjectClassUri(subject);
		RDFDatatype expectedDatatype = findDatatypeConstraint(classUri, propertyUri, shaclModel);
		if (expectedDatatype != null) {
			try {
				rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, expectedDatatype));
				LOGGER.debug("Added typed literal with expected datatype {}: {}", expectedDatatype.getURI(), value);
				return;
			} catch (Exception e) {
				throw new IllegalArgumentException("Value '" + value + "' does not match expected datatype "
						+ expectedDatatype.getURI() + " for property " + propertyUri, e);
			}
		}

		if (value instanceof String) {
			String stringValue = (String) value;
			XSDDatatype datatype = XSDDatatype.XSDstring;
			if (isUriLike(stringValue)) {
				datatype = XSDDatatype.XSDanyURI;
			}
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(stringValue, datatype));
			LOGGER.debug("Added string literal with datatype {}: {}", datatype.getURI(), value);
		} else if (value instanceof Integer) {
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, XSDDatatype.XSDinteger));
			LOGGER.debug("Added integer literal: {}", value);
		} else if (value instanceof Boolean) {
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, XSDDatatype.XSDboolean));
			LOGGER.debug("Added boolean literal: {}", value);
		} else if (value instanceof java.time.LocalDateTime) {
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, XSDDatatype.XSDdateTime));
			LOGGER.debug("Added dateTime literal: {}", value);
		} else if (value instanceof LinkedEntity) {
			LinkedEntity linkedEntity = (LinkedEntity) value;
			EPOSDataModelEntity resolvedEntity = entityMap != null ? entityMap.get(linkedEntity.getUid()) : null;

			if (resolvedEntity != null) {
				// Treat as nested entity
				String inferredClassUri = null;
				// Inline node: infer type from SHACL
				List<String> allowedClasses = findClassConstraints(classUri, propertyUri, shaclModel);
				if (allowedClasses != null && !allowedClasses.isEmpty()) {
					inferredClassUri = selectBestClass(allowedClasses, resolvedEntity);
					LOGGER.debug("Inferred class {} for inline identifier on property {}", inferredClassUri,
							propertyUri);
				}

				// DCAT-AP compliance: If this is an Identifier object and property is
				// dct:identifier,
				// change to adms:identifier (dct:identifier should be simple literals only)
				Property actualProperty = property;
				if ("http://purl.org/dc/terms/identifier".equals(propertyUri) &&
						resolvedEntity.getClass().getSimpleName().equals("Identifier")) {
					actualProperty = rdfModel.createProperty("http://www.w3.org/ns/adms#identifier");
					LOGGER.debug("Changed dct:identifier to adms:identifier for complex Identifier object");
				}

				Resource objectResource;
				if (resolvedEntity.getUid().startsWith("_:")) {
					objectResource = rdfModel.createResource(); // Anonymous node for inline
				} else {
					objectResource = rdfModel.createResource(resolvedEntity.getUid());
				}
				rdfModel.add(subject, actualProperty, objectResource);
				convertEntityToRDF(resolvedEntity, objectResource, rdfModel, mappingModel, shaclModel,
						processedEntities, inferredClassUri, entityMap);
			} else {
				// Fallback to reference
				Resource objectResource = rdfModel.createResource(linkedEntity.getUid());
				rdfModel.add(subject, property, objectResource);
				LOGGER.debug("Added linked entity reference: {}", linkedEntity.getUid());
			}
		} else if (value instanceof EPOSDataModelEntity) {
			// Handle nested EPOS entities recursively
			EPOSDataModelEntity nestedEntity = (EPOSDataModelEntity) value;
			String inferredClassUri = null;
			// Inline node: infer type from SHACL
			List<String> allowedClasses = findClassConstraints(classUri, propertyUri, shaclModel);
			if (allowedClasses != null && !allowedClasses.isEmpty()) {
				inferredClassUri = selectBestClass(allowedClasses, nestedEntity);
				LOGGER.debug("Inferred class {} for inline identifier on property {}", inferredClassUri,
						propertyUri);
			}
			Resource objectResource;
			if (nestedEntity.getUid().startsWith("_:")) {
				objectResource = rdfModel.createResource(); // Anonymous node for inline
			} else {
				objectResource = rdfModel.createResource(nestedEntity.getUid());
			}
			rdfModel.add(subject, property, objectResource);
			LOGGER.debug("Added nested entity reference: {} (will process recursively)", nestedEntity.getUid());
			// Recursively convert the nested entity
			convertEntityToRDF(nestedEntity, objectResource, rdfModel, mappingModel, shaclModel, processedEntities,
					inferredClassUri, entityMap);
		} else if (value instanceof List) {
			// Handle lists - recursively process each item
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) value;
			LOGGER.debug("Processing list with {} items", list.size());
			for (Object item : list) {
				addPropertyToRDF(subject, propertyUri, item, rdfModel, mappingModel, shaclModel, processedEntities,
						entityMap);
			}
		} else {
			// Default to string representation
			rdfModel.add(subject, property, rdfModel.createLiteral(value.toString()));
			LOGGER.debug("Added default string literal: {}", value.toString());
		}
	}

	private static List<EPOSDataModelEntity> collectAllLinkedEntities(List<EPOSDataModelEntity> startingEntities) {
		Set<EPOSDataModelEntity> allEntities = new LinkedHashSet<>(startingEntities);
		Set<String> visitedUids = new HashSet<>();
		Queue<EPOSDataModelEntity> queue = new LinkedList<>();

		// Initialize with starting entities
		for (EPOSDataModelEntity entity : startingEntities) {
			visitedUids.add(entity.getUid());
			queue.add(entity);
		}

		int currentDepth = 0;

		while (!queue.isEmpty() && currentDepth < MAX_DEPTH && allEntities.size() < MAX_ENTITIES) {
			int levelSize = queue.size();
			currentDepth++;

			for (int i = 0; i < levelSize; i++) {
				EPOSDataModelEntity currentEntity = queue.poll();
				LOGGER.debug("Processing entity {} at depth {}", currentEntity.getUid(), currentDepth);

				// Scan all getter methods for LinkedEntity properties
				Method[] methods = currentEntity.getClass().getMethods();
				for (Method method : methods) {
					if (method.getName().startsWith("get") && method.getParameterCount() == 0 &&
							!method.getName().equals("getClass") && !method.getName().equals("getUid")) {

						try {
							Object value = method.invoke(currentEntity);
							if (value != null) {
								if (value instanceof LinkedEntity) {
									EPOSDataModelEntity linkedEntity = resolveLinkedEntity((LinkedEntity) value);
									if (linkedEntity != null && !visitedUids.contains(linkedEntity.getUid())) {
										visitedUids.add(linkedEntity.getUid());
										allEntities.add(linkedEntity);
										queue.add(linkedEntity);
										LOGGER.debug("Added linked entity: {}", linkedEntity.getUid());
									}
								} else if (value instanceof List) {
									@SuppressWarnings("unchecked")
									List<Object> list = (List<Object>) value;
									for (Object item : list) {
										if (item instanceof LinkedEntity) {
											EPOSDataModelEntity linkedEntity = resolveLinkedEntity((LinkedEntity) item);
											if (linkedEntity != null && !visitedUids.contains(linkedEntity.getUid())) {
												visitedUids.add(linkedEntity.getUid());
												allEntities.add(linkedEntity);
												queue.add(linkedEntity);
												LOGGER.debug("Added linked entity from list: {}",
														linkedEntity.getUid());
											}
										}
									}
								}
							}
						} catch (Exception e) {
							LOGGER.warn("Error processing property {} for entity traversal: {}", method.getName(),
									e.getLocalizedMessage());
							e.printStackTrace();
						}
					}
				}
			}
		}

		if (allEntities.size() >= MAX_ENTITIES) {
			LOGGER.warn("Reached maximum entity limit ({}) during linked entity collection", MAX_ENTITIES);
		}
		if (currentDepth >= MAX_DEPTH) {
			LOGGER.warn("Reached maximum depth limit ({}) during linked entity collection", MAX_DEPTH);
		}

		return new ArrayList<>(allEntities);
	}

	private static EPOSDataModelEntity resolveLinkedEntity(LinkedEntity linkedEntity) {
		try {
			String entityTypeStr = linkedEntity.getEntityType();
			EntityNames entityType = EntityNames.valueOf(entityTypeStr);
			@SuppressWarnings("rawtypes")
			AbstractAPI api = AbstractAPI.retrieveAPI(entityType.name());
			EPOSDataModelEntity entity = (EPOSDataModelEntity) api.retrieve(linkedEntity.getInstanceId());
			if (entity == null) {
				LOGGER.warn("Linked entity not found: {} of type {}", linkedEntity.getInstanceId(), entityTypeStr);
			}
			return entity;
		} catch (Exception e) {
			LOGGER.warn("Error resolving linked entity {}: {}", linkedEntity.getInstanceId(), e.getLocalizedMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static Lang getLangForFormat(String format) {
		String lowerFormat = format.toLowerCase();
		if ("json-ld".equals(lowerFormat)) {
			return Lang.JSONLD;
		} else {
			return Lang.TTL;
		}
	}

	private static String cleanupPrefixes(String content) {
		// Only apply prefix cleanup for Turtle format
		if (content.contains("@prefix") || content.contains("PREFIX")) {
			StringBuilder sb = new StringBuilder();
			boolean inPrefixes = true;
			for (String line : content.split("\n")) {
				if (inPrefixes && line.startsWith("PREFIX")) {
					line = line.replace("PREFIX", "@prefix") + " .";
				} else {
					inPrefixes = false;
				}
				sb.append(line).append("\n");
			}
			return sb.toString();
		}
		return content;
	}
}
