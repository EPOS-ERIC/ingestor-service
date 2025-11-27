package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for WebService entities to DCAT DataService.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class WebServiceMapper implements EntityMapper<WebService> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebServiceMapper.class);

    @Override
    public Resource mapToRDF(WebService entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v3 model required fields
        if (entity.getEntryPoint() == null || entity.getEntryPoint().trim().isEmpty() ||
                entity.getName() == null || entity.getName().trim().isEmpty()) {
            LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (endpointURL or title)", entity.getUid());
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCAT_DATA_SERVICE);

        // dcat:endpointURL, rdfs:Resource, 1..n
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_ENDPOINT_URL, entity.getEntryPoint());

        // dct:identifier, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

        // dct:title, literal, 1..n
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getName());

        // dct:conformsTo, dct:Standard, 0..n
        if (entity.getDocumentation() != null && !entity.getDocumentation().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getDocumentation()) {
                EPOSDataModelEntity docEntity = entityMap.get(linkedEntity.getUid());
                if (docEntity instanceof org.epos.eposdatamodel.Documentation) {
                    DocumentationMapper docMapper = new DocumentationMapper();
                    Resource docResource = docMapper.mapToRDF((org.epos.eposdatamodel.Documentation) docEntity, model, entityMap, resourceCache);
                    if (docResource != null) {
                        model.add(subject, RDFConstants.DCT_CONFORMS_TO, docResource);
                    } else {
                        LOGGER.warn("Skipping invalid documentation for WebService {}", entity.getUid());
                    }
                }
            }
        }

        // dcat:contactPoint, vcard:Kind or schema:ContactPoint, 0..n
        if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getContactPoint()) {
                EPOSDataModelEntity contactEntity = entityMap.get(linkedEntity.getUid());
                if (contactEntity instanceof org.epos.eposdatamodel.ContactPoint) {
                    ContactPointMapper contactMapper = new ContactPointMapper();
                    Resource contactResource = contactMapper.mapToRDF((org.epos.eposdatamodel.ContactPoint) contactEntity, model, entityMap, resourceCache);
                    if (contactResource != null) {
                        model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactResource);
                    } else {
                        LOGGER.warn("Skipping invalid contactPoint for WebService {}", entity.getUid());
                    }
                }
            }
        }

        // dct:description, literal, 0..n
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());

        // dcat:endpointDescription, rdfs:Resource or hydra:Operation, 0..n
        if (entity.getSupportedOperation() != null && !entity.getSupportedOperation().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getSupportedOperation()) {
                EPOSDataModelEntity operationEntity = entityMap.get(linkedEntity.getUid());
                if (operationEntity instanceof org.epos.eposdatamodel.Operation) {
                    OperationMapper operationMapper = new OperationMapper();
                    Resource operationResource = operationMapper.mapToRDF((org.epos.eposdatamodel.Operation) operationEntity, model, entityMap, resourceCache);
                    if (operationResource != null) {
                        model.add(subject, RDFConstants.DCAT_ENDPOINT_DESCRIPTION, operationResource);
                    } else {
                        LOGGER.warn("Skipping invalid supportedOperation for WebService {}", entity.getUid());
                    }
                }
            }
        }

        // dct:issued, literal typed as xsd:date or xsd:dateTime, 0..1
        if (entity.getDatePublished() != null) {
            String dateString = ((LocalDateTime) entity.getDatePublished()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_ISSUED, dateString, XSDDatatype.XSDdateTime);
        }

        // dcat:keyword, literal, 0..n
        if (entity.getKeywords() != null && !entity.getKeywords().isEmpty()) {
            String[] keywords = entity.getKeywords().split(",");
            for (String keyword : keywords) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.DCAT_KEYWORD, keyword.trim());
            }
        }

        // dct:modified, literal typed as xsd:date or xsd:dateTime, 0..1
        if (entity.getDateModified() != null) {
            String dateString = ((LocalDateTime) entity.getDateModified()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_MODIFIED, dateString, XSDDatatype.XSDdateTime);
        }

        // dct:publisher, foaf:Agent or schema:Organization, 0..1
        if (entity.getProvider() != null) {
            EPOSDataModelEntity providerEntity = entityMap.get(entity.getProvider().getUid());
            if (providerEntity instanceof org.epos.eposdatamodel.Organization) {
                OrganizationMapper organizationMapper = new OrganizationMapper();
                Resource providerResource = organizationMapper.mapToRDF((org.epos.eposdatamodel.Organization) providerEntity, model, entityMap, resourceCache);
                if (providerResource != null) {
                    model.add(subject, RDFConstants.DCT_PUBLISHER, providerResource);
                } else {
                    LOGGER.warn("Skipping invalid provider for WebService {}", entity.getUid());
                }
            }
        }

        // dcat:theme, skos:Concept, 0..n
        if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getCategory()) {
                EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
                if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
                    CategoryMapper categoryMapper = new CategoryMapper();
                    Resource categoryResource = categoryMapper.mapToRDF((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
                    if (categoryResource != null) {
                        model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
                    } else {
                        LOGGER.warn("Skipping invalid category for WebService {}", entity.getUid());
                    }
                }
            }
        }

        // dct:license, dct:LicenseDocument, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicense());

        // dct:spatial, dct:Location, 0..n
        if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
                EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
                if (locationEntity instanceof org.epos.eposdatamodel.Location) {
                    org.epos.eposdatamodel.Location loc = (org.epos.eposdatamodel.Location) locationEntity;
                    Resource spatialResource = RDFHelper.createBlankNode(model);
                    RDFHelper.addType(model, spatialResource, RDFConstants.DCT_LOCATION);
                    if (loc.getLocation() != null && !loc.getLocation().isEmpty()) {
                        RDFHelper.addTypedLiteral(model, spatialResource, RDFConstants.DCAT_BBOX, loc.getLocation(), RDFConstants.GSP_WKT_LITERAL_DATATYPE);
                    }
                    model.add(subject, RDFConstants.DCT_SPATIAL, spatialResource);
                }
            }
        }

        // dct:temporal, dct:PeriodOfTime, 0..n
        if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
                EPOSDataModelEntity periodEntity = entityMap.get(linkedEntity.getUid());
                if (periodEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
                    org.epos.eposdatamodel.PeriodOfTime pot = (org.epos.eposdatamodel.PeriodOfTime) periodEntity;
                    Resource temporalResource = RDFHelper.createBlankNode(model);
                    RDFHelper.addType(model, temporalResource, RDFConstants.DCT_PERIOD_OF_TIME);
                    if (pot.getStartDate() != null) {
                        String dateString = ((LocalDateTime) pot.getStartDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                        RDFHelper.addTypedLiteral(model, temporalResource, RDFConstants.DCAT_START_DATE, dateString, XSDDatatype.XSDdateTime);
                    }
                    if (pot.getEndDate() != null) {
                        String dateString = ((LocalDateTime) pot.getEndDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
                        RDFHelper.addTypedLiteral(model, temporalResource, RDFConstants.DCAT_END_DATE, dateString, XSDDatatype.XSDdateTime);
                    }
                    model.add(subject, RDFConstants.DCT_TEMPORAL, temporalResource);
                }
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCAT_NS + "DataService";
    }
}
