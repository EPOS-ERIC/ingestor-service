package org.epos.core;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;

public class MetadataExporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExporter.class);

	public static String exportToTurtle(EntityNames entityType, String mappingName, List<String> ids) {
		if (entityType == null) {
			throw new IllegalArgumentException("Entity type cannot be null or empty");
		}

		if (mappingName == null || mappingName.trim().isEmpty()) {
			throw new IllegalArgumentException("Mapping name cannot be null or empty");
		}

		try {
			LOGGER.info("Starting export for entity type '{}' using mapping '{}'", entityType, mappingName);

			// 1. Retrieve mapping model (reverse of current)
			LOGGER.debug("Retrieving reverse mapping model for '{}'", mappingName);
			Model mappingModel = retrieveReverseMapping(mappingName);
			LOGGER.info("Retrieved reverse mapping with {} triples", mappingModel.size());

			// 2. Retrieve entities from DB
			List<EPOSDataModelEntity> entities = retrieveEntities(entityType, ids);
			LOGGER.info("Retrieved {} entities of type '{}' from database", entities.size(), entityType);

			if (entities.isEmpty()) {
				LOGGER.info("No entities found for type: {}", entityType);
				return ""; // Return empty string for no content
			}

			// 3. Create RDF model
			Model rdfModel = ModelFactory.createDefaultModel();

			// Set prefixes for readable Turtle output
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

			LOGGER.debug("Created empty RDF model for conversion");

			// 4. Convert entities to RDF triples (recursive for nested entities)
			int processedCount = 0;
			for (EPOSDataModelEntity entity : entities) {
				LOGGER.debug("Converting entity {} of {}: {}", ++processedCount, entities.size(), entity.getUid());
				convertEntityToRDF(entity, rdfModel, mappingModel, null);
			}
			LOGGER.info("Converted {} entities to RDF triples", entities.size());
			LOGGER.info("RDF model now has {} statements", rdfModel.size());

			// 5. Serialize to Turtle
			StringWriter writer = new StringWriter();
			RDFDataMgr.write(writer, rdfModel, RDFFormat.TURTLE);
			String turtleContent = writer.toString();
			LOGGER.info("Serialized RDF model to {} characters of Turtle content", turtleContent.length());
			LOGGER.debug("First 500 characters of Turtle content: {}",
					turtleContent.substring(0, Math.min(500, turtleContent.length())));
			if (turtleContent.trim().equals("{}") || turtleContent.trim().isEmpty()) {
				LOGGER.error("Turtle content is empty or just '{}', RDF model has {} statements", rdfModel.size());
				rdfModel.listStatements().forEachRemaining(stmt -> LOGGER.debug("Triple: {} {} {}", stmt.getSubject(),
						stmt.getPredicate(), stmt.getObject()));
			}

			return turtleContent;

		} catch (Exception e) {
			LOGGER.error("Error during export: {}", e.getLocalizedMessage());
			throw new RuntimeException("Export failed", e);
		}
	}

	private static Model retrieveReverseMapping(String mappingName) {
		LOGGER.debug("Retrieving original mapping model '{}'", mappingName);
		Model originalMapping = MetadataPopulator.retrieveModelMapping(mappingName);
		if (originalMapping == null) {
			LOGGER.error("Original mapping model not found: {}", mappingName);
			throw new RuntimeException("Mapping model not found: " + mappingName);
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

	@SuppressWarnings("unchecked")
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
							return (EPOSDataModelEntity) api.retrieve(id);
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

	private static void convertEntityToRDF(
			EPOSDataModelEntity entity,
			Model rdfModel,
			Model mappingModel,
			java.util.Set<String> processedEntities) {
		if (processedEntities == null) {
			processedEntities = new java.util.HashSet<>();
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
					}
				}
			}

			LOGGER.debug("Processed {} properties for entity {}, {} were mapped to DCAT", processedProperties,
					entity.getUid(), mappedProperties);

		} catch (Exception e) {
			LOGGER.error("Error converting entity {} to RDF: {}", entity.getUid(), e.getLocalizedMessage());
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
			var query = org.apache.jena.query.QueryFactory.create(queryString);
			var qexec = org.apache.jena.query.QueryExecutionFactory.create(query, mappingModel);
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
			var query = org.apache.jena.query.QueryFactory.create(queryString);
			var qexec = org.apache.jena.query.QueryExecutionFactory.create(query, mappingModel);
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
		}

		return null;
	}

	private static void addPropertyToRDF(Resource subject, String propertyUri, Object value, Model rdfModel,
			Model mappingModel, java.util.Set<String> processedEntities) {
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
}
