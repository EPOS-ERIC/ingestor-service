package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for Facility entities to epos:Facility.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class FacilityMapper implements EntityMapper<Facility> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FacilityMapper.class);

    @Override
    public Resource exportToV1(Facility entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v1 model required fields
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty() ||
                entity.getIdentifier() == null || entity.getIdentifier().trim().isEmpty() ||
                entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            LOGGER.warn("Entity {} not compliant with v1 model: missing required fields", entity.getUid());
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.EPOS_FACILITY);

        // dct:description, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());

        // dct:identifier, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getIdentifier());

        // dct:title, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getTitle());

		// dct:type, skos:Concept or rdfs:Literal typed with URI, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

        return subject;
    }

    @Override
    public Resource exportToV3(Facility entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.EPOS_FACILITY);

        // schema:name, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getTitle());

        // dct:description, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());

        // dct:type, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

        // schema:identifier, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getIdentifier());

        // foaf:page, resource, 0..n
        if (entity.getPageURL() != null && !entity.getPageURL().isEmpty()) {
            for (String url : entity.getPageURL()) {
                RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_PAGE, url);
            }
        }

        // schema:address, schema:PostalAddress, 0..n
        if (entity.getAddress() != null && !entity.getAddress().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getAddress()) {
                EPOSDataModelEntity addressEntity = entityMap.get(linkedEntity.getUid());
                if (addressEntity instanceof org.epos.eposdatamodel.Address) {
                    AddressMapper addressMapper = new AddressMapper();
                    Resource addressResource = addressMapper.exportToV3((org.epos.eposdatamodel.Address) addressEntity, model, entityMap, resourceCache);
                    if (addressResource != null) {
                        model.add(subject, RDFConstants.SCHEMA_ADDRESS, addressResource);
                    } else {
                        LOGGER.warn("Skipping invalid address for Facility {}", entity.getUid());
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
                    Resource locationResource = locationMapper.exportToV3((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
                    if (locationResource != null) {
                        model.add(subject, RDFConstants.DCT_SPATIAL, locationResource);
                    } else {
                        LOGGER.warn("Skipping invalid spatial extent for Facility {}", entity.getUid());
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
                    Resource categoryResource = categoryMapper.exportToV3((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
                    if (categoryResource != null) {
                        model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
                    } else {
                        LOGGER.warn("Skipping invalid category for Facility {}", entity.getUid());
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
                    Resource contactResource = contactMapper.exportToV3((org.epos.eposdatamodel.ContactPoint) contactEntity, model, entityMap, resourceCache);
                    if (contactResource != null) {
                        model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactResource);
                    } else {
                        LOGGER.warn("Skipping invalid contactPoint for Facility {}", entity.getUid());
                    }
                }
            }
        }

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
        return RDFConstants.EPOS_NS + "Facility";
    }
}
