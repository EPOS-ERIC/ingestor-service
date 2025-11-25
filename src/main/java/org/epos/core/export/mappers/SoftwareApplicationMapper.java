package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplication;

import java.util.Map;

/**
 * Mapper for SoftwareApplication entities to Schema.org SoftwareApplication.
 */
public class SoftwareApplicationMapper implements EntityMapper<SoftwareApplication> {

    @Override
    public Resource mapToRDF(SoftwareApplication entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_SOFTWARE_APPLICATION);

        // Add identifier
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getUid());

        // Add basic properties
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_DOWNLOAD_URL, entity.getDownloadURL());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_REQUIREMENTS, entity.getRequirements());

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

        // Add identifiers
        if (entity.getIdentifier() != null) {
            for (LinkedEntity linkedEntity : entity.getIdentifier()) {
                EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
                if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
                    IdentifierMapper identifierMapper = new IdentifierMapper();
                    Resource identifierResource = identifierMapper.mapToRDF((org.epos.eposdatamodel.Identifier) identifierEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.ADMS_IDENTIFIER, identifierResource);
                }
            }
        }

        // Add relation
        if (entity.getRelatedOperation() != null) {
            for (LinkedEntity linked : entity.getRelatedOperation()) {
                model.add(subject, RDFConstants.DCT_RELATION, model.createResource(linked.getUid()));
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "SoftwareApplication";
    }
}