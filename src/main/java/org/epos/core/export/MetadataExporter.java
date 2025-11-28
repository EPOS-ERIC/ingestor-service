package org.epos.core.export;

import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.epos.core.export.mappers.AddressMapper;
import org.epos.core.export.mappers.AttributionMapper;
import org.epos.core.export.mappers.CategoryMapper;
import org.epos.core.export.mappers.CategorySchemeMapper;
import org.epos.core.export.mappers.ContactPointMapper;
import org.epos.core.export.mappers.DataProductMapper;
import org.epos.core.export.mappers.DistributionMapper;
import org.epos.core.export.mappers.DocumentationMapper;
import org.epos.core.export.mappers.EntityMapper;
import org.epos.core.export.mappers.EquipmentMapper;
import org.epos.core.export.mappers.FacilityMapper;
import org.epos.core.export.mappers.IdentifierMapper;
import org.epos.core.export.mappers.LocationMapper;
import org.epos.core.export.mappers.MappingMapper;
import org.epos.core.export.mappers.OperationMapper;
import org.epos.core.export.mappers.OrganizationMapper;
import org.epos.core.export.mappers.OutputMappingMapper;
import org.epos.core.export.mappers.PayloadMapper;
import org.epos.core.export.mappers.PeriodOfTimeMapper;
import org.epos.core.export.mappers.PersonMapper;
import org.epos.core.export.mappers.QuantitativeValueMapper;
import org.epos.core.export.mappers.SoftwareApplicationMapper;
import org.epos.core.export.mappers.SoftwareSourceCodeMapper;
import org.epos.core.export.mappers.WebServiceMapper;
import org.epos.core.export.EPOSVersion;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;

public class MetadataExporter {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExporter.class);

	private static final Map<Class<?>, EntityMapper<?>> MAPPERS = new HashMap<>();

	static {
		// Register all mappers
		MAPPERS.put(org.epos.eposdatamodel.DataProduct.class, new DataProductMapper());
		MAPPERS.put(org.epos.eposdatamodel.Distribution.class, new DistributionMapper());
		MAPPERS.put(org.epos.eposdatamodel.Organization.class, new OrganizationMapper());
		MAPPERS.put(org.epos.eposdatamodel.Person.class, new PersonMapper());
		MAPPERS.put(org.epos.eposdatamodel.ContactPoint.class, new ContactPointMapper());
		MAPPERS.put(org.epos.eposdatamodel.Address.class, new AddressMapper());
		MAPPERS.put(org.epos.eposdatamodel.Category.class, new CategoryMapper());
		MAPPERS.put(org.epos.eposdatamodel.CategoryScheme.class, new CategorySchemeMapper());
		MAPPERS.put(org.epos.eposdatamodel.Identifier.class, new IdentifierMapper());
		MAPPERS.put(org.epos.eposdatamodel.Operation.class, new OperationMapper());
		MAPPERS.put(org.epos.eposdatamodel.Location.class, new LocationMapper());
		MAPPERS.put(org.epos.eposdatamodel.PeriodOfTime.class, new PeriodOfTimeMapper());
		MAPPERS.put(org.epos.eposdatamodel.Equipment.class, new EquipmentMapper());
		MAPPERS.put(org.epos.eposdatamodel.Facility.class, new FacilityMapper());
		MAPPERS.put(org.epos.eposdatamodel.WebService.class, new WebServiceMapper());
		MAPPERS.put(org.epos.eposdatamodel.SoftwareApplication.class, new SoftwareApplicationMapper());
		MAPPERS.put(org.epos.eposdatamodel.Attribution.class, new AttributionMapper());
		MAPPERS.put(org.epos.eposdatamodel.Documentation.class, new DocumentationMapper());
		MAPPERS.put(org.epos.eposdatamodel.QuantitativeValue.class, new QuantitativeValueMapper());
		MAPPERS.put(org.epos.eposdatamodel.Mapping.class, new MappingMapper());
		MAPPERS.put(org.epos.eposdatamodel.OutputMapping.class, new OutputMappingMapper());
		MAPPERS.put(org.epos.eposdatamodel.Payload.class, new PayloadMapper());
		MAPPERS.put(org.epos.eposdatamodel.SoftwareSourceCode.class, new SoftwareSourceCodeMapper());
	}

	/**
	 * Exports EPOS Data Model entities to RDF in the specified format.
	 *
	 * @param entityType The type of entities to export (null for all types)
	 * @param format     The output format ("turtle" or "json-ld")
	 * @param ids        Specific entity IDs to export (null for all)
	 * @param version    The EPOS-DCAT-AP version (default V3)
	 * @return RDF content as string
	 */
	public static String exportToRDF(
			EntityNames entityType,
			String format,
			List<String> ids,
			EPOSVersion version) {

		if (format == null || format.trim().isEmpty()) {
			format = "turtle";
		}

		if (!format.matches("(?i)(turtle|json-ld)")) {
			throw new IllegalArgumentException("Format must be one of: turtle, json-ld");
		}

		if (version == null) {
			version = EPOSVersion.V3;
		}

		if (ids != null && !ids.isEmpty() && entityType == null) {
			throw new IllegalArgumentException("Entity type must be specified when providing specific IDs");
		}

		try {
			LOGGER.info("Starting new export for entity type '{}' in format '{}' and version '{}'",
					entityType != null ? entityType : "all types", format, version);

			// 1. Retrieve entities from database
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

			entities = entities.stream().filter(entity -> entity != null).collect(Collectors.toList());

			// 2. Build entity map
			if (entityType != null) {
				entities = collectAllLinkedEntities(entities);
				LOGGER.debug("After collecting linked entities: {} total entities", entities.size());
			}

			Map<String, EPOSDataModelEntity> entityMap = entities.stream()
					.collect(Collectors.toMap(EPOSDataModelEntity::getUid, e -> e, (e1, e2) -> e1));

			// 3. Create RDF model
			Model rdfModel = ModelFactory.createDefaultModel();
			setNamespacePrefixes(rdfModel);
			rdfModel.removeNsPrefix("rdf");

			// 4. Initialize resource cache
			Map<String, Resource> resourceCache = new HashMap<>();

			// 5. For each root entity, get mapper and call mapToRDF
			List<EPOSDataModelEntity> rootEntities;
			if (entityType != null) {
				rootEntities = entities.stream()
						.filter(e -> !e.getUid().startsWith("_:"))
						.filter(entity -> !(entity instanceof org.epos.eposdatamodel.IriTemplate))
						.collect(Collectors.toList());
			} else {
				rootEntities = entities.stream()
						.filter(entity -> !(entity instanceof org.epos.eposdatamodel.Element))
						.filter(e -> !e.getUid().startsWith("_:"))
						.filter(entity -> !(entity instanceof org.epos.eposdatamodel.IriTemplate))
						.collect(Collectors.toList());
			}

			int processedCount = 0;
			for (EPOSDataModelEntity entity : rootEntities) {
				LOGGER.debug("Converting entity {} of {}: {}", ++processedCount, rootEntities.size(), entity.getUid());
				@SuppressWarnings("unchecked")
				EntityMapper<EPOSDataModelEntity> mapper = (EntityMapper<EPOSDataModelEntity>) MAPPERS
						.get(entity.getClass());
				if (mapper != null) {
					switch (version) {
						case V1:
							mapper.exportToV1(entity, rdfModel, entityMap, resourceCache);
							break;
						case V3:
							mapper.exportToV3(entity, rdfModel, entityMap, resourceCache);
							break;
					}
				} else {
					LOGGER.warn("No mapper found for entity type: {}", entity.getClass().getSimpleName());
				}
			}

			LOGGER.debug("Converted {} entities to RDF triples", rootEntities.size());
			LOGGER.debug("RDF model now has {} statements", rdfModel.size());

			StringWriter writer = new StringWriter();
			Lang lang = getLangForFormat(format);
			RDFDataMgr.write(writer, rdfModel, lang);
			String content = writer.toString();

			content = cleanupPrefixes(content);

			return content;

		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error during export: {}", e.getLocalizedMessage());
			e.printStackTrace();
			throw new RuntimeException("Export failed", e);
		}
	}

	private static void setNamespacePrefixes(Model model) {
		model.setNsPrefix("adms", "http://www.w3.org/ns/adms#");
		model.setNsPrefix("dash", "http://datashapes.org/dash#");
		model.setNsPrefix("dc", "http://purl.org/dc/elements/1.1/");
		model.setNsPrefix("dcat", "http://www.w3.org/ns/dcat#");
		model.setNsPrefix("dct", "http://purl.org/dc/terms/");
		model.setNsPrefix("epos", "https://www.epos-eu.org/epos-dcat-ap#");
		model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
		model.setNsPrefix("cnt", "http://www.w3.org/2011/content#");
		model.setNsPrefix("oa", "http://www.w3.org/ns/oa#");
		model.setNsPrefix("org", "http://www.w3.org/ns/org#");
		model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
		model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		model.setNsPrefix("schema", "http://schema.org/");
		model.setNsPrefix("sh", "http://www.w3.org/ns/shacl#");
		model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
		model.setNsPrefix("spdx", "http://spdx.org/rdf/terms#");
		model.setNsPrefix("vcard", "http://www.w3.org/2006/vcard/ns#");
		model.setNsPrefix("hydra", "http://www.w3.org/ns/hydra/core#");
		model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		model.setNsPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
		model.setNsPrefix("http", "http://www.w3.org/2006/http#");
		model.setNsPrefix("locn", "http://www.w3.org/ns/locn#");
		model.setNsPrefix("gsp", "http://www.opengis.net/ont/geosparql#");
		model.setNsPrefix("dqv", "http://www.w3.org/ns/dqv#");
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
						LOGGER.debug("Retrieved {} entities for type {}", entities.size(), entityType);
					}

					allEntities.addAll(entities);
				} catch (Exception e) {
					LOGGER.warn("Error retrieving entities for type {}: {}", entityType, e.getLocalizedMessage());
				}
			}

			return allEntities;
		} catch (Exception e) {
			LOGGER.error("Error retrieving all entities: {}", e.getLocalizedMessage());
			throw new RuntimeException("Failed to retrieve all entities", e);
		}
	}

	private static final int MAX_DEPTH = 20;
	private static final int MAX_ENTITIES = 1000;

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
							LOGGER.debug("Error invoking method {} on {}: {}", method.getName(),
									currentEntity.getClass().getSimpleName(), e.getLocalizedMessage());
						}
					}
				}
			}
		}

		if (allEntities.size() >= MAX_ENTITIES) {
			LOGGER.warn("Reached maximum entity limit of {} during linked entity collection", MAX_ENTITIES);
		}

		return new ArrayList<>(allEntities);
	}

	private static EPOSDataModelEntity resolveLinkedEntity(LinkedEntity linkedEntity) {
		try {
			String entityTypeStr = linkedEntity.getEntityType();
			if (entityTypeStr == null) {
				LOGGER.warn("Linked entity has null entity type: {}", linkedEntity.getInstanceId());
				return null;
			}
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
			return null;
		}
	}
}
