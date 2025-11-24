package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Identifier;

import java.util.Map;

/**
 * Mapper for Identifier entities to ADMS Identifier.
 * Special rule: Always use adms:identifier as parent property, never dct:identifier.
 */
public class IdentifierMapper implements EntityMapper<Identifier> {

    @Override
    public Resource mapToRDF(Identifier entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        // Create resource
        Resource subject = model.createResource();

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.ADMS_IDENTIFIER_CLASS);

        // Add properties
        RDFHelper.addLiteral(model, subject, RDFConstants.ADMS_SCHEME_AGENCY, entity.getType());
        RDFHelper.addLiteral(model, subject, RDFConstants.SKOS_NOTATION, entity.getIdentifier());

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.ADMS_NS + "Identifier";
    }
}