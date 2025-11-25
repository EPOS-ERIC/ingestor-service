package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Equipment;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for Equipment entities to Schema.org Device.
 */
public class EquipmentMapper implements EntityMapper<Equipment> {

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

        // Add basic properties
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getIdentifier());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SERIAL_NUMBER, entity.getSerialNumber());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_FILTER, entity.getFilter());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_RESOLUTION, entity.getResolution());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_ORIENTATION, entity.getOrientation());
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_PAGE, entity.getPageURL());

        // Add manufacturer (inline Organization)
        if (entity.getManufacturer() != null) {
            EPOSDataModelEntity manufacturerEntity = entityMap.get(entity.getManufacturer().getUid());
            if (manufacturerEntity instanceof org.epos.eposdatamodel.Organization) {
                OrganizationMapper organizationMapper = new OrganizationMapper();
                Resource manufacturerResource = organizationMapper.mapToRDF((org.epos.eposdatamodel.Organization) manufacturerEntity, model, entityMap, resourceCache);
                model.add(subject, RDFConstants.SCHEMA_MANUFACTURER, manufacturerResource);
            }
        }

        // Add isPartOf
        if (entity.getIsPartOf() != null) {
            for (LinkedEntity linked : entity.getIsPartOf()) {
                model.add(subject, RDFConstants.DCT_IS_PART_OF, model.createResource(linked.getUid()));
            }
        }

        // Add spatial extent (inline Location)
        if (entity.getSpatialExtent() != null && !entity.getSpatialExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
                EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
                if (locationEntity instanceof org.epos.eposdatamodel.Location) {
                    LocationMapper locationMapper = new LocationMapper();
                    Resource locationResource = locationMapper.mapToRDF((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.DCT_SPATIAL, locationResource);
                }
            }
        }

        // Add temporal extent (inline PeriodOfTime)
        if (entity.getTemporalExtent() != null && !entity.getTemporalExtent().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getTemporalExtent()) {
                EPOSDataModelEntity periodEntity = entityMap.get(linkedEntity.getUid());
                if (periodEntity instanceof org.epos.eposdatamodel.PeriodOfTime) {
                    PeriodOfTimeMapper periodMapper = new PeriodOfTimeMapper();
                    Resource periodResource = periodMapper.mapToRDF((org.epos.eposdatamodel.PeriodOfTime) periodEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.DCT_TEMPORAL, periodResource);
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

        // Add dynamicRange
        if (entity.getDynamicRange() != null) {
            RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_DYNAMIC_RANGE, entity.getDynamicRange());
        }

        // Add samplePeriod
        if (entity.getSamplePeriod() != null) {
            RDFHelper.addStringLiteral(model, subject, RDFConstants.EPOS_SAMPLE_PERIOD, entity.getSamplePeriod());
        }

        // Add relation
        if (entity.getRelation() != null) {
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