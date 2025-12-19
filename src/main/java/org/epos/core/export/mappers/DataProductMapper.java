package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for DataProduct entities to DCAT Dataset.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class DataProductMapper implements EntityMapper<DataProduct> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataProductMapper.class);

	@Override
	public Resource exportToV1(DataProduct entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Compliance check for v1 model required fields
		if (entity.getDescription() == null || entity.getDescription().isEmpty() ||
				entity.getTitle() == null || entity.getTitle().isEmpty()) {
			LOGGER.warn("Entity {} not compliant with v1 model: missing required fields (description or title)", entity.getUid());
			return null;
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.DCAT_DATASET);

		// dct:description, literal, 1..n
		if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
			for (String desc : entity.getDescription()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, desc);
			}
		}

		// dct:identifier, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// dct:title, literal, 1..n
		if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
			for (String title : entity.getTitle()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, title);
			}
		}

		// dcat:contactPoint, schema:ContactPoint, 0..n
		if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getContactPoint()) {
				EPOSDataModelEntity contactPointEntity = entityMap.get(linkedEntity.getUid());
				if (contactPointEntity instanceof org.epos.eposdatamodel.ContactPoint) {
					ContactPointMapper contactPointMapper = new ContactPointMapper();
					Resource contactPointResource = contactPointMapper.exportToV1((org.epos.eposdatamodel.ContactPoint) contactPointEntity, model, entityMap, resourceCache);
					if (contactPointResource != null) {
						model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactPointResource);
					} else {
						LOGGER.warn("Skipping invalid contactPoint for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dcat:distribution, dcat:Distribution, 0..n
		if (entity.getDistribution() != null && !entity.getDistribution().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getDistribution()) {
				EPOSDataModelEntity distributionEntity = entityMap.get(linkedEntity.getUid());
				if (distributionEntity instanceof org.epos.eposdatamodel.Distribution) {
					DistributionMapper distributionMapper = new DistributionMapper();
					Resource distributionResource = distributionMapper.exportToV1((org.epos.eposdatamodel.Distribution) distributionEntity, model, entityMap, resourceCache);
					if (distributionResource != null) {
						model.add(subject, RDFConstants.DCAT_DISTRIBUTION, distributionResource);
					} else {
						LOGGER.warn("Skipping invalid distribution for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dcat:keyword, literal, 0..n
		if (entity.getKeywords() != null && !entity.getKeywords().isEmpty()) {
			String[] keywords = entity.getKeywords().split(",");
			for (String keyword : keywords) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCAT_KEYWORD, keyword.trim());
			}
		}

		// dct:publisher, schema:Organization, 0..n
		if (entity.getPublisher() != null && !entity.getPublisher().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getPublisher()) {
				EPOSDataModelEntity publisherEntity = entityMap.get(linkedEntity.getUid());
				if (publisherEntity instanceof org.epos.eposdatamodel.Organization) {
					OrganizationMapper organizationMapper = new OrganizationMapper();
					Resource organizationResource = organizationMapper.exportToV1((org.epos.eposdatamodel.Organization) publisherEntity, model, entityMap, resourceCache);
					if (organizationResource != null) {
						model.add(subject, RDFConstants.DCT_PUBLISHER, organizationResource);
					} else {
						LOGGER.warn("Skipping invalid publisher for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:spatial, dct:Location, 0..n
		if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
				EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
				if (locationEntity instanceof org.epos.eposdatamodel.Location) {
					LocationMapper locationMapper = new LocationMapper();
					Resource spatialResource = locationMapper.exportToV1((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
					if (spatialResource != null) {
						model.add(subject, RDFConstants.DCT_SPATIAL, spatialResource);
					}
				}
			}
		}

		// dct:temporal, dct:PeriodOfTime, 0..n
		if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
				EPOSDataModelEntity temporalEntity = entityMap.get(linkedEntity.getUid());
				if (temporalEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
					PeriodOfTimeMapper periodOfTimeMapper = new PeriodOfTimeMapper();
					Resource temporalResource = periodOfTimeMapper.exportToV1((org.epos.eposdatamodel.PeriodOfTime) temporalEntity, model, entityMap, resourceCache);
					if (temporalResource != null) {
						model.add(subject, RDFConstants.DCT_TEMPORAL, temporalResource);
					}
				}
			}
		}

		// dcat:theme, skos:Concept, 0..n
		if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getCategory()) {
				EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
				if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
					CategoryMapper categoryMapper = new CategoryMapper();
					Resource categoryResource = categoryMapper.exportToV1((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
					if (categoryResource != null) {
						model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
					} else {
						LOGGER.warn("Skipping invalid category for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:accrualPeriodicity, dct:Frequency, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_ACCRUAL_PERIODICITY, entity.getAccrualPeriodicity());

		// dct:created, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getCreated() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_CREATED, entity.getCreated().toLocalDate().toString());
		}

		// dct:hasPart, dcat:Dataset, 0..n
		if (entity.getHasPart() != null && !entity.getHasPart().isEmpty()) {
			for (LinkedEntity linked : entity.getHasPart()) {
				model.add(subject, RDFConstants.DCT_HAS_PART, model.createResource(linked.getUid()));
			}
		}

		// dct:isPartOf, dcat:Dataset, 0..n
		if (entity.getIsPartOf() != null && !entity.getIsPartOf().isEmpty()) {
			for (LinkedEntity linked : entity.getIsPartOf()) {
				model.add(subject, RDFConstants.DCT_IS_PART_OF, model.createResource(linked.getUid()));
			}
		}

		// dct:type, skos:Concept, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

		// epos:annotation, oa:Annotation, 0..n
		if (entity.getQualityAssurance() != null && !entity.getQualityAssurance().isEmpty()) {
			Resource qualityResource = RDFHelper.createBlankNode(model);
			RDFHelper.addType(model, qualityResource, RDFConstants.OA_ANNOTATION);
			RDFHelper.addURILiteral(model, qualityResource, RDFConstants.OA_HAS_BODY, entity.getQualityAssurance());
			model.add(subject, RDFConstants.DQV_HAS_QUALITY_ANNOTATION, qualityResource);
		}

		// adms:identifier, adms:Identifier, 0..n
		if (entity.getIdentifier() != null && !entity.getIdentifier().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getIdentifier()) {
				EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
				if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
					IdentifierMapper identifierMapper = new IdentifierMapper();
					Resource identifierResource = identifierMapper.exportToV1((org.epos.eposdatamodel.Identifier) identifierEntity, model, entityMap, resourceCache);
					if (identifierResource != null) {
						model.add(subject, RDFConstants.ADMS_IDENTIFIER, identifierResource);
					} else {
						LOGGER.warn("Skipping invalid identifier for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:issued, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getIssued() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_ISSUED, entity.getIssued().toLocalDate().toString());
		}

		// dct:modified, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getModified() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_MODIFIED, entity.getModified().toLocalDate().toString());
		}

		// owl:versionInfo literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.OWL_VERSION_INFO, entity.getVersionInfo());

		// schema:variableMeasured, literal string, 0..n
		if (entity.getVariableMeasured() != null && !entity.getVariableMeasured().isEmpty()) {
			for (String variableMeasured : entity.getVariableMeasured()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_VARIABLE_MEASURED, variableMeasured);
			}
		}

		// TODO: check if this works
		// prov:qualifiedAttribution, prov:Attribution, 0..1
		if (entity.getQualifiedAttribution() != null && !entity.getQualifiedAttribution().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getQualifiedAttribution()) {
				EPOSDataModelEntity qualifiedAttributionEntity = entityMap.get(linkedEntity.getUid());
				if (qualifiedAttributionEntity instanceof org.epos.eposdatamodel.Attribution) {
					AttributionMapper qualifiedAttributionMapper = new AttributionMapper();
					Resource qualifiedAttributionResource = qualifiedAttributionMapper.exportToV1((org.epos.eposdatamodel.Attribution) qualifiedAttributionEntity, model, entityMap, resourceCache);
					if (qualifiedAttributionResource != null) {
						model.add(subject, RDFConstants.PROV_QUALIFIED_ATTRIBUTION, qualifiedAttributionResource);
					} else {
						LOGGER.warn("Skipping invalid qualifiedAttribution for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		return subject;
	}

	@Override
	public Resource exportToV3(DataProduct entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Compliance check for v3 model required fields
		if (entity.getDescription() == null || entity.getDescription().isEmpty() ||
				entity.getTitle() == null || entity.getTitle().isEmpty()) {
			LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (description or title)", entity.getUid());
			return null;
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.DCAT_DATASET);

		// dct:description, literal, 1..n
		if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
			for (String desc : entity.getDescription()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, desc);
			}
		}

		// dct:identifier, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// dct:title, literal, 1..n
		if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
			for (String title : entity.getTitle()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, title);
			}
		}

		// dcat:contactPoint, vcard:Kind or schema:ContactPoint, 0..n
		if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getContactPoint()) {
				EPOSDataModelEntity contactPointEntity = entityMap.get(linkedEntity.getUid());
				if (contactPointEntity instanceof org.epos.eposdatamodel.ContactPoint) {
					ContactPointMapper contactPointMapper = new ContactPointMapper();
					Resource contactPointResource = contactPointMapper.exportToV3((org.epos.eposdatamodel.ContactPoint) contactPointEntity, model, entityMap, resourceCache);
					if (contactPointResource != null) {
						model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactPointResource);
					} else {
						LOGGER.warn("Skipping invalid contactPoint for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dcat:distribution, dcat:Distribution, 0..n
		if (entity.getDistribution() != null && !entity.getDistribution().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getDistribution()) {
				EPOSDataModelEntity distributionEntity = entityMap.get(linkedEntity.getUid());
				if (distributionEntity instanceof org.epos.eposdatamodel.Distribution) {
					DistributionMapper distributionMapper = new DistributionMapper();
					Resource distributionResource = distributionMapper.exportToV3((org.epos.eposdatamodel.Distribution) distributionEntity, model, entityMap, resourceCache);
					if (distributionResource != null) {
						model.add(subject, RDFConstants.DCAT_DISTRIBUTION, distributionResource);
					} else {
						LOGGER.warn("Skipping invalid distribution for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dcat:keyword, literal, 0..n
		if (entity.getKeywords() != null && !entity.getKeywords().isEmpty()) {
			String[] keywords = entity.getKeywords().split(",");
			for (String keyword : keywords) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.DCAT_KEYWORD, keyword.trim());
			}
		}

		// dct:publisher, foaf:Agent or schema:Organization, 0..n
		if (entity.getPublisher() != null && !entity.getPublisher().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getPublisher()) {
				EPOSDataModelEntity publisherEntity = entityMap.get(linkedEntity.getUid());
				if (publisherEntity instanceof org.epos.eposdatamodel.Organization) {
					OrganizationMapper organizationMapper = new OrganizationMapper();
					Resource organizationResource = organizationMapper.exportToV3((org.epos.eposdatamodel.Organization) publisherEntity, model, entityMap, resourceCache);
					if (organizationResource != null) {
						model.add(subject, RDFConstants.DCT_PUBLISHER, organizationResource);
					} else {
						LOGGER.warn("Skipping invalid publisher for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:spatial, dct:Location, 0..n
		if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
				EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
				if (locationEntity instanceof org.epos.eposdatamodel.Location) {
					org.epos.eposdatamodel.Location location = (org.epos.eposdatamodel.Location) locationEntity;
					Resource spatialResource = RDFHelper.createBlankNode(model);
					RDFHelper.addType(model, spatialResource, RDFConstants.DCT_LOCATION);
					if (location.getLocation() != null && !location.getLocation().isEmpty()) {
						RDFHelper.addTypedLiteral(model, spatialResource, RDFConstants.DCAT_BBOX, location.getLocation(), RDFConstants.GSP_WKT_LITERAL_DATATYPE);
					}
					model.add(subject, RDFConstants.DCT_SPATIAL, spatialResource);
				}
			}
		}

		// dct:temporal, dct:PeriodOfTime, 0..n
		if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
				EPOSDataModelEntity temporalEntity = entityMap.get(linkedEntity.getUid());
				if (temporalEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
					org.epos.eposdatamodel.PeriodOfTime periodOfTime = (org.epos.eposdatamodel.PeriodOfTime) temporalEntity;
					Resource temporalResource = RDFHelper.createBlankNode(model);
					RDFHelper.addType(model, temporalResource, RDFConstants.DCT_PERIOD_OF_TIME);
					if (periodOfTime.getStartDate() != null) {
						String dateString = ((LocalDateTime) periodOfTime.getStartDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
						model.add(temporalResource, RDFConstants.DCAT_START_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
					}
					if (periodOfTime.getEndDate() != null) {
						String dateString = ((LocalDateTime) periodOfTime.getEndDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
						model.add(temporalResource, RDFConstants.DCAT_END_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
					}
					model.add(subject, RDFConstants.DCT_TEMPORAL, temporalResource);
				}
			}
		}

		// dcat:theme, skos:Concept, 0..n
		if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getCategory()) {
				EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
				if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
					CategoryMapper categoryMapper = new CategoryMapper();
					Resource categoryResource = categoryMapper.exportToV3((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
					if (categoryResource != null) {
						model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
					} else {
						LOGGER.warn("Skipping invalid category for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:accrualPeriodicity, dct:Frequency, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_ACCRUAL_PERIODICITY, entity.getAccrualPeriodicity());

		// dct:created, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getCreated() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_CREATED, entity.getCreated().toLocalDate().toString());
		}

		// dct:type, skos:Concept, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

		// dqv:hasQualityAnnotation, oa:Annotation, 0..n
		if (entity.getQualityAssurance() != null && !entity.getQualityAssurance().isEmpty()) {
			Resource qualityResource = RDFHelper.createBlankNode(model);
			RDFHelper.addType(model, qualityResource, RDFConstants.OA_ANNOTATION);
			RDFHelper.addURILiteral(model, qualityResource, RDFConstants.OA_HAS_BODY, entity.getQualityAssurance());
			model.add(qualityResource, RDFConstants.OA_HAS_TARGET, subject);
			model.add(subject, RDFConstants.DQV_HAS_QUALITY_ANNOTATION, qualityResource);
		}

		// adms:identifier, adms:Identifier, 0..n
		if (entity.getIdentifier() != null && !entity.getIdentifier().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getIdentifier()) {
				EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
				if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
					IdentifierMapper identifierMapper = new IdentifierMapper();
					Resource identifierResource = identifierMapper.exportToV3((org.epos.eposdatamodel.Identifier) identifierEntity, model, entityMap, resourceCache);
					if (identifierResource != null) {
						model.add(subject, RDFConstants.ADMS_IDENTIFIER, identifierResource);
					} else {
						LOGGER.warn("Skipping invalid identifier for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		// dct:issued, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getIssued() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_ISSUED,
					entity.getIssued().toLocalDate().toString());
		}

		// dct:modified, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getModified() != null) {
			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_MODIFIED,
					entity.getModified().toLocalDate().toString());
		}

		// dcat:version, literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCAT_VERSION, entity.getVersionInfo());

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.DCAT_NS + "Dataset";
	}
}
