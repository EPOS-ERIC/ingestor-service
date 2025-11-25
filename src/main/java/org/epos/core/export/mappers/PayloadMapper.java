package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Payload;

import java.util.Map;

/**
 * Stub mapper for Payload entities.
 */
public class PayloadMapper implements EntityMapper<Payload> {

    @Override
    public Resource mapToRDF(Payload entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_CLASS);
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_TITLE, "Payload description");
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_DESCRIPTION, "Payload description");

        // Add supportedProperty from outputMapping
        if (entity.getOutputMapping() != null) {
            for (LinkedEntity linked : entity.getOutputMapping()) {
                EPOSDataModelEntity outputEntity = entityMap.get(linked.getUid());
                if (outputEntity instanceof org.epos.eposdatamodel.OutputMapping) {
                    org.epos.eposdatamodel.OutputMapping outputMapping = (org.epos.eposdatamodel.OutputMapping) outputEntity;
                    Resource supportedProperty = model.createResource();
                    RDFHelper.addType(model, supportedProperty, RDFConstants.HYDRA_SUPPORTED_PROPERTY);
                    RDFHelper.addStringLiteral(model, supportedProperty, RDFConstants.HYDRA_VARIABLE, outputMapping.getOutputVariable());
                    RDFHelper.addStringLiteral(model, supportedProperty, RDFConstants.HYDRA_PROPERTY, outputMapping.getOutputProperty());
                    RDFHelper.addStringLiteral(model, supportedProperty, RDFConstants.HYDRA_DESCRIPTION, outputMapping.getOutputLabel());
                    if ("true".equals(outputMapping.getOutputRequired())) {
                        model.add(supportedProperty, RDFConstants.HYDRA_REQUIRED, model.createTypedLiteral(true));
                    }
                    model.add(subject, RDFConstants.HYDRA_SUPPORTED_PROPERTY, supportedProperty);
                }
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.RDFS_NS + "Resource";
    }
}
