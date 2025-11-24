package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;

import java.util.Arrays;
import java.util.Map;

/**
 * Mapper for Operation entities to Hydra Operation.
 * Very Complex - nested structure.
 */
public class OperationMapper implements EntityMapper<Operation> {

	@Override
	public Resource mapToRDF(Operation entity, Model model, Map<String, EPOSDataModelEntity> entityMap) {
		// Create resource
		Resource subject = model.createResource(entity.getUid());

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.HYDRA_OPERATION);

		// Add properties
		RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_METHOD, entity.getMethod());

		// Add returns (multiple)
		if (entity.getReturns() != null) {
			for (String ret : entity.getReturns()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_RETURNS, ret);
			}
		}

		// Add expects (payload)
		if (entity.getPayload() != null) {
			for (LinkedEntity linked : entity.getPayload()) {
				model.add(subject, RDFConstants.HYDRA_EXPECTS, model.createResource(linked.getUid()));
			}
		}

		// Add IriTemplate (inline)
		if (entity.getIriTemplateObject() != null && !model.contains(subject, RDFConstants.HYDRA_PROPERTY, (RDFNode) null)) {
			// Create blank node for IriTemplate
			Resource iriTemplateResource = RDFHelper.createBlankNode(model);
			RDFHelper.addType(model, iriTemplateResource, RDFConstants.HYDRA_IRI_TEMPLATE);

			org.epos.eposdatamodel.IriTemplate iriTemplate = entity.getIriTemplateObject();
			RDFHelper.addStringLiteral(model, iriTemplateResource, RDFConstants.HYDRA_TEMPLATE,iriTemplate.getTemplate());

			// Add mappings
			if (iriTemplate.getMappings() != null) {
				for (LinkedEntity mappingLinked : iriTemplate.getMappings()) {
					EPOSDataModelEntity mappingEntity = entityMap.get(mappingLinked.getUid());
					if (mappingEntity instanceof org.epos.eposdatamodel.Mapping) {
						// Create blank node for IriTemplateMapping
						Resource mappingResource = RDFHelper.createBlankNode(model);
						RDFHelper.addType(model, mappingResource, RDFConstants.HYDRA_IRI_TEMPLATE_MAPPING);

						org.epos.eposdatamodel.Mapping mapping = (org.epos.eposdatamodel.Mapping) mappingEntity;
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.HYDRA_VARIABLE,mapping.getVariable());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.HYDRA_PROPERTY,mapping.getProperty());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.RDFS_RANGE, mapping.getRange());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.RDFS_LABEL, mapping.getLabel());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.SCHEMA_VALUE_PATTERN,mapping.getValuePattern());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.SCHEMA_DEFAULT_VALUE,mapping.getDefaultValue());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.SCHEMA_MIN_VALUE,mapping.getMinValue());
						RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.SCHEMA_MAX_VALUE,mapping.getMaxValue());
						boolean readonly = "true".equals(mapping.getReadOnlyValue());
						if (Arrays.asList("type", "organisationName", "individualName", "purpose", "status", "distributionFormat").contains(mapping.getVariable())) {
							readonly = true;
						}
						model.add(mappingResource, RDFConstants.SCHEMA_READONLY_VALUE, model.createTypedLiteral(readonly));
					    boolean isRequired = "true".equals(mapping.getRequired());
					    model.add(mappingResource, RDFConstants.HYDRA_REQUIRED, model.createTypedLiteral(isRequired));

						// Add multiple parameter values
						if (mapping.getParamValue() != null) {
							for (String paramValue : mapping.getParamValue()) {
								RDFHelper.addStringLiteral(model, mappingResource, RDFConstants.HTTP_PARAM_VALUE,
										paramValue);
							}
						}

						// Add to IriTemplate
						model.add(iriTemplateResource, RDFConstants.HYDRA_MAPPING, mappingResource);
					}
				}
			}

			// Add IriTemplate to Operation
			model.add(subject, RDFConstants.HYDRA_PROPERTY, iriTemplateResource);
		}

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.HYDRA_NS + "Operation";
	}
}
