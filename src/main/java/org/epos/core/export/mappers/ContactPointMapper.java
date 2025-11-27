package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.util.Map;

/**
 * Mapper for ContactPoint entities to Schema.org ContactPoint.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class ContactPointMapper implements EntityMapper<ContactPoint> {

    @Override
    public Resource mapToRDF(ContactPoint entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_CONTACT_POINT);

        // schema:contactType, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_CONTACT_TYPE, entity.getRole());

        // schema:availableLanguage, literal, 0..n
        if (entity.getLanguage() != null && !entity.getLanguage().isEmpty()) {
            for (String language : entity.getLanguage()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_AVAILABLE_LANGUAGE, language);
            }
        }

        // schema:email, literal, 0..n
        if (entity.getEmail() != null && !entity.getEmail().isEmpty()) {
            for (String email : entity.getEmail()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_EMAIL, email);
            }
        }

        // schema:telephone, literal, 0..n
        if (entity.getTelephone() != null && !entity.getTelephone().isEmpty()) {
            for (String telephone : entity.getTelephone()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_TELEPHONE, telephone);
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "ContactPoint";
    }
}
