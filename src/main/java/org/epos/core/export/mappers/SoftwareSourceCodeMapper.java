package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareSourceCode;

import java.util.Map;

/**
 * Stub mapper for SoftwareSourceCode entities.
 */
public class SoftwareSourceCodeMapper implements EntityMapper<SoftwareSourceCode> {

    @Override
    public Resource mapToRDF(SoftwareSourceCode entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        Resource subject = model.createResource(entity.getUid());
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_SOFTWARE_SOURCE_CODE);
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getUid());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_VERSION, entity.getSoftwareVersion());
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_CODE_REPOSITORY, entity.getCodeRepository());
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_LICENSE, entity.getLicenseURL());
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_MAIN_ENTITY_OF_PAGE, entity.getMainEntityofPage());
        if (entity.getProgrammingLanguage() != null && !entity.getProgrammingLanguage().isEmpty()) {
            RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_PROGRAMMING_LANGUAGE, entity.getProgrammingLanguage().get(0));
        }
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_RUNTIME_PLATFORM, entity.getRuntimePlatform());
        if (entity.getRelation() != null) {
            for (LinkedEntity linked : entity.getRelation()) {
                model.add(subject, RDFConstants.SCHEMA_TARGET_PRODUCT, model.createResource(linked.getUid()));
            }
        }
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "SoftwareSourceCode";
    }
}