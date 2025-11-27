package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Equipment;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for Equipment entities to epos:Equipment.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class EquipmentMapper implements EntityMapper<Equipment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EquipmentMapper.class);

    @Override
    public Resource mapToRDF(Equipment entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.EPOS_EQUIPMENT);

        // schema:name, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());

        // schema:description, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());

        // dct:type, resource, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

        // schema:identifier, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getIdentifier());

        // schema:serialNumber, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SERIAL_NUMBER, entity.getSerialNumber());

        // foaf:page, resource, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_PAGE, entity.getPageURL());

        // schema:manufacturer, schema:Organization, 0..1
        if (entity.getManufacturer() != null) {
            EPOSDataModelEntity manufacturerEntity = entityMap.get(entity.getManufacturer().getUid());
            if (manufacturerEntity instanceof org.epos.eposdatamodel.Organization) {
                OrganizationMapper organizationMapper = new OrganizationMapper();
                Resource manufacturerResource = organizationMapper.mapToRDF((org.epos.eposdatamodel.Organization) manufacturerEntity, model, entityMap, resourceCache);
                if (manufacturerResource != null) {
                    model.add(subject, RDFConstants.SCHEMA_MANUFACTURER, manufacturerResource);
                } else {
                    LOGGER.warn("Skipping invalid manufacturer for Equipment {}", entity.getUid());
                }
            }
        }

        // dct:isPartOf, resource, 0..n
        if (entity.getIsPartOf() != null && !entity.getIsPartOf().isEmpty()) {
            for (LinkedEntity linked : entity.getIsPartOf()) {
                model.add(subject, RDFConstants.DCT_IS_PART_OF, model.createResource(linked.getUid()));
            }
        }

        // dct:spatial, dct:Location, 0..n
        if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
                EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
                if (locationEntity instanceof org.epos.eposdatamodel.Location) {
                    LocationMapper locationMapper = new LocationMapper();
                    Resource locationResource = locationMapper.mapToRDF((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
                    if (locationResource != null) {
                        model.add(subject, RDFConstants.DCT_SPATIAL, locationResource);
                    } else {
                        LOGGER.warn("Skipping invalid spatial extent for Equipment {}", entity.getUid());
                    }
                }
            }
        }

        // dct:temporal, dct:PeriodOfTime, 0..n
        if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
                EPOSDataModelEntity periodEntity = entityMap.get(linkedEntity.getUid());
                if (periodEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
                    PeriodOfTimeMapper periodMapper = new PeriodOfTimeMapper();
                    Resource periodResource = periodMapper.mapToRDF((org.epos.eposdatamodel.PeriodOfTime) periodEntity, model, entityMap, resourceCache);
                    if (periodResource != null) {
                        model.add(subject, RDFConstants.DCT_TEMPORAL, periodResource);
                    } else {
                        LOGGER.warn("Skipping invalid temporal extent for Equipment {}", entity.getUid());
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
                    Resource categoryResource = categoryMapper.mapToRDF((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
                    if (categoryResource != null) {
                        model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
                    } else {
                        LOGGER.warn("Skipping invalid category for Equipment {}", entity.getUid());
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
                        LOGGER.warn("Skipping invalid contactPoint for Equipment {}", entity.getUid());
                    }
                }
            }
        }

        // epos:dynamicRange, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_DYNAMIC_RANGE, entity.getDynamicRange());

        // epos:samplePeriod, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_SAMPLE_PERIOD, entity.getSamplePeriod());

        // epos:filter, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_FILTER, entity.getFilter());

        // epos:resolution, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_RESOLUTION, entity.getResolution());

        // epos:orientation, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_ORIENTATION, entity.getOrientation());

        // dct:relation, resource, 0..n
        if (entity.getRelation() != null && !entity.getRelation().isEmpty()) {
            for (LinkedEntity linked : entity.getRelation()) {
                model.add(subject, RDFConstants.DCT_RELATION, model.createResource(linked.getUid()));
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.EPOS_NS + "Equipment";
    }
}
