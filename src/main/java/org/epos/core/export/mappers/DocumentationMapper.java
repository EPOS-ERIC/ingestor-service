package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Documentation;

import java.util.Map;

/**
 * Mapper for Documentation entities.
 */
public class DocumentationMapper implements EntityMapper<Documentation> {

    @Override
    public Resource mapToRDF(Documentation entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_API_DOCUMENTATION);
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_TITLE, entity.getTitle());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_DESCRIPTION, entity.getDescription());
        RDFHelper.addURILiteral(model, subject, RDFConstants.HYDRA_ENTRYPOINT, entity.getUri());
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.HYDRA_NS + "ApiDocumentation";
    }
}