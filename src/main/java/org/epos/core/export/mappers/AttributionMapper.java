package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Attribution;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.util.Map;

/**
 * Mapper for Attribution entities to PROV Attribution.
 */
public class AttributionMapper implements EntityMapper<Attribution> {

	@Override
	public Resource exportToV1(Attribution entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.PROV_ATTRIBUTION);

		// prov:agent, prov:Agent, 0..1
		if (entity.getAgent() != null) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.PROV_AGENT, entity.getAgent().getUid());
		}

		// prov:hadRole, prov:Role, 0..1
		if (entity.getRole() != null && !entity.getRole().isEmpty()) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.PROV_HAD_ROLE, entity.getRole().get(0));
		}

		return subject;
	}

	@Override
	public Resource exportToV3(Attribution entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.PROV_ATTRIBUTION);

        // Add roles
        if (entity.getRole() != null) {
            for (String role : entity.getRole()) {
                RDFHelper.addLiteral(model, subject, RDFConstants.PROV_HAD_ROLE, role);
            }
        }

        // Add agent (if exists)
        if (entity.getAgent() != null) {
            EPOSDataModelEntity agentEntity = entityMap.get(entity.getAgent().getUid());
            if (agentEntity != null) {
                Resource agentResource = model.createResource(agentEntity.getUid());
                model.add(subject, RDFConstants.PROV_AGENT, agentResource);
            }
        }

        return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.PROV_NS + "Attribution";
	}
}
