package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for Facility entities to Schema.org Place.
 */
public class FacilityMapper implements EntityMapper<Facility> {

    @Override
    public Resource mapToRDF(Facility entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.EPOS_FACILITY);

        // Add basic properties
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getTitle());
        RDFHelper.addLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());
        RDFHelper.addLiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getIdentifier());

        // Add page URLs
        if (entity.getPageURL() != null) {
            for (String url : entity.getPageURL()) {
                RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_PAGE, url);
            }
        }

        // Add addresses (inline)
        if (entity.getAddress() != null) {
            for (LinkedEntity linkedEntity : entity.getAddress()) {
                EPOSDataModelEntity addressEntity = entityMap.get(linkedEntity.getUid());
                if (addressEntity instanceof org.epos.eposdatamodel.Address) {
                    Resource addressResource = model.createResource(); // blank node
                    RDFHelper.addType(model, addressResource, RDFConstants.VCARD_ADDRESS);
                    RDFHelper.addStringLiteral(model, addressResource, RDFConstants.VCARD_STREET_ADDRESS, ((org.epos.eposdatamodel.Address) addressEntity).getStreet());
                    RDFHelper.addStringLiteral(model, addressResource, RDFConstants.VCARD_LOCALITY, ((org.epos.eposdatamodel.Address) addressEntity).getLocality());
                    RDFHelper.addStringLiteral(model, addressResource, RDFConstants.VCARD_POSTAL_CODE, ((org.epos.eposdatamodel.Address) addressEntity).getPostalCode());
                    RDFHelper.addStringLiteral(model, addressResource, RDFConstants.VCARD_COUNTRY_NAME, ((org.epos.eposdatamodel.Address) addressEntity).getCountry());
                    model.add(subject, RDFConstants.VCARD_HAS_ADDRESS, addressResource);
                }
            }
        }

        // Add spatial extent (inline Location)
        if (entity.getSpatialExtent() != null) {
            for (LinkedEntity linkedEntity : entity.getSpatialExtent()) {
                EPOSDataModelEntity locationEntity = entityMap.get(linkedEntity.getUid());
                if (locationEntity instanceof org.epos.eposdatamodel.Location) {
                    LocationMapper locationMapper = new LocationMapper();
                    Resource locationResource = locationMapper.mapToRDF((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.DCT_SPATIAL, locationResource);
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
        return RDFConstants.EPOS_NS + "Facility";
    }
}