package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for DataProduct entities to DCAT Dataset.
 * Most Complex - orchestrates everything.
 */
public class DataProductMapper implements EntityMapper<DataProduct> {

	@Override
	public Resource mapToRDF(DataProduct entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.DCAT_DATASET);

		// Add title (multiple)
		if (entity.getTitle() != null) {
			for (String title : entity.getTitle()) {
				RDFHelper.addLiteral(model, subject, RDFConstants.DCT_TITLE, title);
			}
		}

		// Auto-add dct:identifier from UID
		RDFHelper.addLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// Nested: Identifiers
		if (entity.getIdentifier() != null) {
			for (LinkedEntity linkedEntity : entity.getIdentifier()) {
				EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
				if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
					IdentifierMapper identifierMapper = new IdentifierMapper();
					Resource identifierResource = identifierMapper .mapToRDF((org.epos.eposdatamodel.Identifier) identifierEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.ADMS_IDENTIFIER, identifierResource);
				}
			}
		}

		// Add description (multiple)
		if (entity.getDescription() != null) {
			for (String desc : entity.getDescription()) {
				RDFHelper.addLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, desc);
			}
		}

		// Add accrualPeriodicity
		if (entity.getAccrualPeriodicity() != null) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_ACCRUAL_PERIODICITY,entity.getAccrualPeriodicity());
		}

 		// Add created
 		if (entity.getCreated() != null) {
 			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_CREATED, entity.getCreated().toLocalDate().toString());
 		}

 		// Add issued
 		if (entity.getIssued() != null) {
 			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_ISSUED, entity.getIssued().toLocalDate().toString());
 		}

 		// Add modified
 		if (entity.getModified() != null) {
 			RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_MODIFIED, entity.getModified().toLocalDate().toString());
 		}

		// Add versionInfo
		RDFHelper.addLiteral(model, subject, RDFConstants.OWL_VERSION_INFO, entity.getVersionInfo());

		// Add type
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

		// Add spatial
		if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
			// For now, take the first spatial extent and create inline location
			LinkedEntity firstSpatial = entity.getSpatialExtent().get(0);
			EPOSDataModelEntity locationEntity = entityMap.get(firstSpatial.getUid());
			if (locationEntity instanceof org.epos.eposdatamodel.Location) {
				org.epos.eposdatamodel.Location location = (org.epos.eposdatamodel.Location) locationEntity;
 				Resource spatialResource = model.createResource();
 				RDFHelper.addType(model, spatialResource, RDFConstants.DCT_LOCATION);
 				if (location.getLocation() != null) {
 					RDFHelper.addTypedLiteral(model, spatialResource, RDFConstants.LOCN_GEOMETRY, location.getLocation(), RDFConstants.GSP_WKT_LITERAL_DATATYPE);
 				}
				model.add(subject, RDFConstants.DCT_SPATIAL, spatialResource);
			}
		}

		// Add temporal
		if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
			// For now, take the first temporal extent and create inline periodOfTime
			LinkedEntity firstTemporal = entity.getTemporalExtent().get(0);
			EPOSDataModelEntity temporalEntity = entityMap.get(firstTemporal.getUid());
			if (temporalEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
				org.epos.eposdatamodel.PeriodOfTime periodOfTime = (org.epos.eposdatamodel.PeriodOfTime) temporalEntity;
				Resource temporalResource = model.createResource();
				RDFHelper.addType(model, temporalResource, RDFConstants.DCT_PERIOD_OF_TIME);
				if (periodOfTime.getStartDate() != null) {
					model.add(temporalResource, RDFConstants.SCHEMA_START_DATE, model.createTypedLiteral(periodOfTime.getStartDate().toString(), XSDDatatype.XSDdateTime));
				}
				if (periodOfTime.getEndDate() != null) {
					model.add(temporalResource, RDFConstants.SCHEMA_END_DATE, model.createTypedLiteral(periodOfTime.getEndDate().toString(), XSDDatatype.XSDdateTime));
				}
				model.add(subject, RDFConstants.DCT_TEMPORAL, temporalResource);
			}
		}

		// Nested: Categories
		if (entity.getCategory() != null) {
			for (LinkedEntity linkedEntity : entity.getCategory()) {
				EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
				if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
					CategoryMapper categoryMapper = new CategoryMapper();
					Resource categoryResource = categoryMapper.mapToRDF((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
				}
			}
		}

		// Add keywords
		if (entity.getKeywords() != null) {
			String[] keywords = entity.getKeywords().split(",");
			for (String keyword : keywords) {
				RDFHelper.addLiteral(model, subject, RDFConstants.DCAT_KEYWORD, keyword.trim());
			}
		}

		// Nested: ContactPoints
		if (entity.getContactPoint() != null) {
			for (LinkedEntity linkedEntity : entity.getContactPoint()) {
				EPOSDataModelEntity contactPointEntity = entityMap.get(linkedEntity.getUid());
				if (contactPointEntity instanceof org.epos.eposdatamodel.ContactPoint) {
					ContactPointMapper contactPointMapper = new ContactPointMapper();
					Resource contactPointResource = contactPointMapper.mapToRDF((org.epos.eposdatamodel.ContactPoint) contactPointEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactPointResource);
				}
			}
		}

		// Nested: Distributions
		if (entity.getDistribution() != null) {
			for (LinkedEntity linkedEntity : entity.getDistribution()) {
				EPOSDataModelEntity distributionEntity = entityMap.get(linkedEntity.getUid());
				if (distributionEntity instanceof org.epos.eposdatamodel.Distribution) {
					DistributionMapper distributionMapper = new DistributionMapper();
					Resource distributionResource = distributionMapper.mapToRDF((org.epos.eposdatamodel.Distribution) distributionEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.DCAT_DISTRIBUTION, distributionResource);
				}
			}
		}

		// Nested: Publishers (Organizations)
		if (entity.getPublisher() != null) {
			for (LinkedEntity linkedEntity : entity.getPublisher()) {
				EPOSDataModelEntity publisherEntity = entityMap.get(linkedEntity.getUid());
				if (publisherEntity instanceof org.epos.eposdatamodel.Organization) {
					OrganizationMapper organizationMapper = new OrganizationMapper();
					Resource organizationResource = organizationMapper.mapToRDF((org.epos.eposdatamodel.Organization) publisherEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.DCT_PUBLISHER, organizationResource);
				}
			}
		}

		// Add hasQualityAnnotation
		if (entity.getQualityAssurance() != null) {
			Resource qualityResource = model.createResource();
			RDFHelper.addType(model, qualityResource, RDFConstants.OA_ANNOTATION);
			RDFHelper.addURILiteral(model, qualityResource, RDFConstants.OA_HAS_BODY, entity.getQualityAssurance());
			model.add(subject, RDFConstants.DQV_HAS_QUALITY_ANNOTATION, qualityResource);
		}

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.DCAT_NS + "Dataset";
	}
}
