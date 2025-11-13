package org.epos.core;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;

public class MetadataExporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExporter.class);
	private static final int MAX_DEPTH = 20;
	private static final int MAX_ENTITIES = 1000;

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

			Model mappingModel = retrieveReverseMapping(mappingName);
			LOGGER.debug("Retrieved reverse mapping with {} triples", mappingModel.size());

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
			rdfModel.setNsPrefix("foaf", "http://xmlns.com/foaf/spec/#term_");
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

			int processedCount = 0;
			for (EPOSDataModelEntity entity : entities) {
				LOGGER.debug("Converting entity {} of {}: {}", ++processedCount, entities.size(), entity.getUid());
				convertEntityToRDF(entity, rdfModel, mappingModel, null);
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

	private static Model retrieveReverseMapping(String mappingName) {
		LOGGER.debug("Retrieving original mapping model '{}'", mappingName);
		Model originalMapping = MetadataPopulator.retrieveModelMapping(mappingName);
		if (originalMapping == null) {
			LOGGER.error("Original mapping model not found: {}", mappingName);
			throw new IllegalArgumentException("Mapping model not found: " + mappingName);
		}
		LOGGER.debug("Original mapping has {} triples", originalMapping.size());

		// Create a reverse mapping by inverting equivalent class/property relationships
		Model reverseMapping = ModelFactory.createDefaultModel();
		reverseMapping.setNsPrefixes(originalMapping.getNsPrefixMap());

		// Copy all triples, but invert owl:equivalentClass and owl:equivalentProperty
		originalMapping.listStatements().forEachRemaining(stmt -> {
			if (stmt.getPredicate().getURI().equals("http://www.w3.org/2002/07/owl#equivalentClass") ||
					stmt.getPredicate().getURI().equals("http://www.w3.org/2002/07/owl#equivalentProperty")) {
				// Invert subject and object
				reverseMapping.add(reverseMapping.createStatement(
						stmt.getObject().asResource(),
						stmt.getPredicate(),
						stmt.getSubject()));
				LOGGER.debug("Inverted mapping: {} -> {}", stmt.getSubject(), stmt.getObject());
			} else {
				// Copy other triples as-is
				reverseMapping.add(stmt);
			}
		});

		return reverseMapping;
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
			Model rdfModel,
			Model mappingModel,
			Set<String> processedEntities) {
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
			String dcatClassUri = findDCATClassUri(edmClassName, mappingModel);

			if (dcatClassUri == null) {
				LOGGER.warn("No DCAT mapping found for EDM class: {}", edmClassName);
				return;
			}

			LOGGER.debug("Found DCAT class mapping: {} -> {}", edmClassName, dcatClassUri);
			Resource subject = rdfModel.createResource(entity.getUid());
			Resource typeResource = rdfModel.createResource(dcatClassUri);
			rdfModel.add(
					subject,
					rdfModel.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
					typeResource);

			// Get all getter methods and convert properties
			Method[] methods = entity.getClass().getMethods();
			int processedProperties = 0;
			int mappedProperties = 0;

			for (Method method : methods) {
				if (method.getName().startsWith("get") && method.getParameterCount() == 0 &&
						!method.getName().equals("getClass") && !method.getName().equals("getUid")) {

					try {
						Object value = method.invoke(entity);
						if (value != null) {
							processedProperties++;
							String propertyName = method.getName().substring(3); // Remove "get"
							propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);

							String dcatPropertyUri = findDCATPropertyUri(edmClassName, propertyName, mappingModel);
							if (dcatPropertyUri != null) {
								mappedProperties++;
								LOGGER.debug("Mapping property {}.{} -> {}", edmClassName, propertyName,
										dcatPropertyUri);
								addPropertyToRDF(subject, dcatPropertyUri, value, rdfModel, mappingModel,
										processedEntities);
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

	private static String findDCATClassUri(String edmClassName, Model mappingModel) {
		LOGGER.debug("Searching for DCAT class mapping for EDM class: {}", edmClassName);

		// Query for equivalent class in reverse mapping
		String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"SELECT ?dcatClass WHERE { " +
				"  ?dcatClass owl:equivalentClass ?edmClass . " +
				"  FILTER(STRSTARTS(str(?edmClass), \"http://www.epos-eu.org/epos-data-model#\")) " +
				"  FILTER(STRENDS(str(?edmClass), \"" + edmClassName + "\")) " +
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

		// Query for equivalent property
		String queryString = "PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
				"PREFIX edm: <http://www.epos-eu.org/epos-data-model#> " +
				"SELECT ?dcatProperty WHERE { " +
				"  ?dcatProperty owl:equivalentProperty edm:" + propertyName + " . " +
				"}";

		try {
			var query = QueryFactory.create(queryString);
			var qexec = QueryExecutionFactory.create(query, mappingModel);
			var results = qexec.execSelect();

			if (results.hasNext()) {
				var soln = results.next();
				String dcatPropertyUri = soln.get("dcatProperty").toString();
				LOGGER.debug("Found DCAT property mapping: {}.{} -> {}", edmClassName, propertyName, dcatPropertyUri);
				return dcatPropertyUri;
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

	private static void addPropertyToRDF(
			Resource subject,
			String propertyUri,
			Object value,
			Model rdfModel,
			Model mappingModel,
			Set<String> processedEntities) {
		Property property = ResourceFactory.createProperty(propertyUri);
		LOGGER.debug("Adding property {} with value type: {}", propertyUri, value.getClass().getSimpleName());

		if (value instanceof String) {
			rdfModel.add(subject, property, rdfModel.createLiteral((String) value));
			LOGGER.debug("Added string literal: {}", value);
		} else if (value instanceof Integer) {
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, XSDDatatype.XSDinteger));
			LOGGER.debug("Added integer literal: {}", value);
		} else if (value instanceof Boolean) {
			rdfModel.add(subject, property, rdfModel.createTypedLiteral(value, XSDDatatype.XSDboolean));
			LOGGER.debug("Added boolean literal: {}", value);
		} else if (value instanceof LinkedEntity) {
			LinkedEntity linkedEntity = (LinkedEntity) value;
			Resource objectResource = rdfModel.createResource(linkedEntity.getUid());
			rdfModel.add(subject, property, objectResource);
			LOGGER.debug("Added linked entity reference: {}", linkedEntity.getUid());
		} else if (value instanceof EPOSDataModelEntity) {
			// Handle nested EPOS entities recursively
			EPOSDataModelEntity nestedEntity = (EPOSDataModelEntity) value;
			Resource objectResource = rdfModel.createResource(nestedEntity.getUid());
			rdfModel.add(subject, property, objectResource);
			LOGGER.debug("Added nested entity reference: {} (will process recursively)", nestedEntity.getUid());
			// Recursively convert the nested entity
			convertEntityToRDF(nestedEntity, rdfModel, mappingModel, processedEntities);
		} else if (value instanceof List) {
			// Handle lists - recursively process each item
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) value;
			LOGGER.debug("Processing list with {} items", list.size());
			for (Object item : list) {
				addPropertyToRDF(subject, propertyUri, item, rdfModel, mappingModel, processedEntities);
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
