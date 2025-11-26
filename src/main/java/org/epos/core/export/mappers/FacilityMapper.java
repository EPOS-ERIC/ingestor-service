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
 * Mapper for Facility entities to epos:Facility.
 * Follows EPOS-DCAT-AP v3 specification.
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
                    Resource addressResource = addressMapper.mapToRDF((org.epos.eposdatamodel.Address) addressEntity, model, entityMap, resourceCache);
                    if (addressResource != null) {
                        model.add(subject, RDFConstants.SCHEMA_ADDRESS, addressResource);
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
                    Resource locationResource = locationMapper.mapToRDF((org.epos.eposdatamodel.Location) locationEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.DCT_SPATIAL, locationResource);
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
                    model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
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
                    model.add(subject, RDFConstants.DCAT_CONTACT_POINT, contactResource);
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
