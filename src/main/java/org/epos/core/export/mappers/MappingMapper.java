package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Mapping;

import java.util.Map;

/**
 * Stub mapper for Mapping entities.
 */
public class MappingMapper implements EntityMapper<Mapping> {

    @Override
    public Resource mapToRDF(Mapping entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_IRI_TEMPLATE_MAPPING);
        RDFHelper.addLiteral(model, subject, RDFConstants.HYDRA_VARIABLE, entity.getVariable());
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.HYDRA_NS + "IriTemplateMapping";
    }
}