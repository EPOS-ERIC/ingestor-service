package org.epos.core.export.mappers;

import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper for Operation entities to Hydra Operation.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class OperationMapper implements EntityMapper<Operation> {

	private static final Logger LOGGER = LoggerFactory.getLogger(OperationMapper.class);

	@Override
	public Resource exportToV1(Operation entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.HYDRA_OPERATION);

		// hydra:method, literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_METHOD, entity.getMethod());

		// hydra:returns, literal, 0..n
		if (entity.getReturns() != null) {
			for (String ret : entity.getReturns()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_RETURNS, ret);
			}
		}

		// hydra:property, hydra:IriTemplate, 0..1
		if (entity.getIriTemplateObject() != null) {
			IriTemplateMapper iriTemplateMapper = new IriTemplateMapper();
			Resource iriTemplateResource = iriTemplateMapper.exportToV1(entity.getIriTemplateObject(), model, entityMap, resourceCache);
			model.add(subject, RDFConstants.HYDRA_PROPERTY, iriTemplateResource);
		}
		
		if (entity.getPayload() != null && !entity.getPayload().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getPayload()) {
				EPOSDataModelEntity payloadEntity = entityMap.get(linkedEntity.getUid());
				if (payloadEntity instanceof org.epos.eposdatamodel.Payload) {
					PayloadMapper payloadMapper = new PayloadMapper();
					Resource payloadResource = payloadMapper.exportToV1((org.epos.eposdatamodel.Payload)payloadEntity, model, entityMap, resourceCache);
					if (payloadResource != null) {
						model.add(subject, RDFConstants.HYDRA_EXPECTS, payloadResource);
					} else {
						LOGGER.warn("Skipping invalid publisher for DataProduct {}", entity.getUid());
					}
				}
			}
		}

		return subject;
	}

	@Override
	public Resource exportToV3(Operation entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.HYDRA_OPERATION);

		// hydra:method, literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_METHOD, entity.getMethod());

		// hydra:returns, literal, 0..n
		if (entity.getReturns() != null) {
			for (String ret : entity.getReturns()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_RETURNS, ret);
			}
		}

		// hydra:property, hydra:IriTemplate, 0..1
		if (entity.getIriTemplateObject() != null) {
			IriTemplateMapper iriTemplateMapper = new IriTemplateMapper();
			Resource iriTemplateResource = iriTemplateMapper.exportToV3(entity.getIriTemplateObject(), model, entityMap, resourceCache);
			model.add(subject, RDFConstants.HYDRA_PROPERTY, iriTemplateResource);
		}

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.HYDRA_NS + "Operation";
	}
}
