package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.ContactPoint;

import java.util.Map;

/**
 * Mapper for ContactPoint entities to Schema.org ContactPoint.
 */
public class ContactPointMapper implements EntityMapper<ContactPoint> {

    @Override
    public Resource mapToRDF(ContactPoint entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        // Create resource
        Resource subject = model.createResource(entity.getUid());

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_CONTACT_POINT);

        // Add email (multiple)
        if (entity.getEmail() != null) {
            for (String email : entity.getEmail()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_EMAIL, email);
            }
        }

        // Add available language (multiple)
        if (entity.getLanguage() != null) {
            for (String language : entity.getLanguage()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_AVAILABLE_LANGUAGE, language);
            }
        }

        // Add contact type
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_CONTACT_TYPE, entity.getRole());

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "ContactPoint";
    }
}