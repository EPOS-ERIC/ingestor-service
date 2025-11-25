package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for WebService entities to EPOS WebService.
 */
public class WebServiceMapper implements EntityMapper<WebService> {

    @Override
    public Resource mapToRDF(WebService entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.EPOS_WEBSERVICE);

        // Add identifier
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getUid());

        // Add basic properties
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicense());
        RDFHelper.addURILiteral(model, subject, RDFConstants.HYDRA_ENTRYPOINT, entity.getEntryPoint());

        // Add published/modified dates
        if (entity.getDatePublished() != null) {
            String dateString = ((LocalDateTime) entity.getDatePublished()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.SCHEMA_DATE_PUBLISHED, dateString, XSDDatatype.XSDdateTime);
        }
        if (entity.getDateModified() != null) {
            String dateString = ((LocalDateTime) entity.getDateModified()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.SCHEMA_DATE_MODIFIED, dateString, XSDDatatype.XSDdateTime);
        }

        // Add keywords
        if (entity.getKeywords() != null) {
            String[] keywords = entity.getKeywords().split(",");
            for (String keyword : keywords) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_KEYWORDS, keyword.trim());
            }
        }

        // Add provider (inline Organization)
        if (entity.getProvider() != null) {
            EPOSDataModelEntity providerEntity = entityMap.get(entity.getProvider().getUid());
            if (providerEntity instanceof org.epos.eposdatamodel.Organization) {
                OrganizationMapper organizationMapper = new OrganizationMapper();
                Resource providerResource = organizationMapper.mapToRDF((org.epos.eposdatamodel.Organization) providerEntity, model, entityMap, resourceCache);
                model.add(subject, RDFConstants.SCHEMA_PROVIDER, providerResource);
            }
        }

        // Add supported operations (inline)
        if (entity.getSupportedOperation() != null) {
            for (LinkedEntity linkedEntity : entity.getSupportedOperation()) {
                EPOSDataModelEntity operationEntity = entityMap.get(linkedEntity.getUid());
                if (operationEntity instanceof org.epos.eposdatamodel.Operation) {
                    OperationMapper operationMapper = new OperationMapper();
                    Resource operationResource = operationMapper.mapToRDF((org.epos.eposdatamodel.Operation) operationEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.HYDRA_SUPPORTED_OPERATION, operationResource);
                }
            }
        }

        // Add spatial extent (inline Location)
        if (entity.getSpatialExtent() != null) {
            for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
                EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
                if (locationEntity instanceof org.epos.eposdatamodel.Location) {
                    org.epos.eposdatamodel.Location loc = (org.epos.eposdatamodel.Location) locationEntity;
                    Resource blankNode = model.createResource(); // blank node
                    RDFHelper.addType(model, blankNode, RDFConstants.DCT_LOCATION);
                    if (loc.getLocation() != null) {
                        RDFHelper.addLiteral(model, blankNode, RDFConstants.LOCN_GEOMETRY, loc.getLocation());
                    }
                    model.add(subject, RDFConstants.DCT_SPATIAL, blankNode);
                }
            }
        }

        // Add temporal extent (inline PeriodOfTime)
        if (entity.getTemporalExtent() != null) {
            for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
                EPOSDataModelEntity periodEntity = entityMap.get(linkedEntity.getUid());
                if (periodEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
                    org.epos.eposdatamodel.PeriodOfTime pot = (org.epos.eposdatamodel.PeriodOfTime) periodEntity;
                    Resource blankNode = model.createResource(); // blank node
                    RDFHelper.addType(model, blankNode, RDFConstants.DCT_PERIOD_OF_TIME);
                    if (pot.getStartDate() != null) {
                        String dateString = ((LocalDateTime) pot.getStartDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                        RDFHelper.addTypedLiteral(model, blankNode, RDFConstants.SCHEMA_START_DATE, dateString, XSDDatatype.XSDdateTime);
                    }
                    if (pot.getEndDate() != null) {
                        String dateString = ((LocalDateTime) pot.getEndDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                        RDFHelper.addTypedLiteral(model, blankNode, RDFConstants.SCHEMA_END_DATE, dateString, XSDDatatype.XSDdateTime);
                    }
                    model.add(subject, RDFConstants.DCT_TEMPORAL, blankNode);
                }
            }
        }

        // Add categories
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

        // Add contact points
        if (entity.getContactPoint() != null) {
            for (LinkedEntity linkedEntity : entity.getContactPoint()) {
                EPOSDataModelEntity contactEntity = entityMap.get(linkedEntity.getUid());
                if (contactEntity instanceof org.epos.eposdatamodel.ContactPoint) {
                    ContactPointMapper contactMapper = new ContactPointMapper();
                    Resource contactResource = contactMapper.mapToRDF((org.epos.eposdatamodel.ContactPoint) contactEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactResource);
                }
            }
        }

        // Add identifiers (inline)
        if (entity.getIdentifier() != null) {
            for (LinkedEntity linkedEntity : entity.getIdentifier()) {
                EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
                if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
                    Resource identifierResource = model.createResource(); // blank node
                    RDFHelper.addType(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_VALUE);
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_ID, ((org.epos.eposdatamodel.Identifier) identifierEntity).getType());
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_VALUE, ((org.epos.eposdatamodel.Identifier) identifierEntity).getIdentifier());
                    model.add(subject, RDFConstants.SCHEMA_IDENTIFIER, identifierResource);
                }
            }
        }

        // Add conformsTo for documentation
        if (entity.getDocumentation() != null) {
            for (LinkedEntity linked : entity.getDocumentation()) {
                model.add(subject, RDFConstants.DCT_CONFORMS_TO, model.createResource(linked.getUid()));
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.EPOS_NS + "WebService";
    }
}