package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.OutputMapping;

import java.util.Map;

/**
 * Stub mapper for OutputMapping entities.
 */
public class OutputMappingMapper implements EntityMapper<OutputMapping> {

    @Override
    public Resource exportToV1(OutputMapping entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.SCHEMA_PROPERTY_VALUE);

        return subject;
    }

    @Override
    public Resource exportToV3(OutputMapping entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.RDFS_RESOURCE);
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.RDFS_NS + "Resource";
    }
}