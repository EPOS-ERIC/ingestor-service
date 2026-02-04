package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Mapping;

import java.util.Arrays;
import java.util.Map;

/**
 * Mapper for Mapping entities to Hydra IriTemplateMapping.
 * Maps to blank nodes (anonymous resources) as per EPOS-DCAT-AP v3.
 */
public class MappingMapper implements EntityMapper<Mapping> {

    @Override
    public Resource exportToV1(Mapping entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // IriTemplateMapping uses blank nodes, so we don't use resource cache
        // Create blank node for IriTemplateMapping
        Resource subject = RDFHelper.createBlankNode(model);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_IRI_TEMPLATE_MAPPING);

        // hydra:variable, literal, 1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_VARIABLE, entity.getVariable());

        // hydra:property, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_PROPERTY, entity.getProperty());

        // rdfs:range, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.RDFS_RANGE, entity.getRange());

        // rdfs:label, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.RDFS_LABEL, entity.getLabel());

        // schema:valuePattern, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_VALUE_PATTERN, entity.getValuePattern());

        // schema:defaultValue, literal, 0..1
        RDFHelper.addStringLiteralEmpty(model, subject, RDFConstants.SCHEMA_DEFAULT_VALUE, entity.getDefaultValue());

        // schema:minValue, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_MIN_VALUE, entity.getMinValue());

        // schema:maxValue, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_MAX_VALUE, entity.getMaxValue());

        // schema:multipleValues, literal, 0..1
		if ("true".equalsIgnoreCase(entity.getMultipleValues())) {
			RDFHelper.addBooleanLiteral(model, subject, RDFConstants.SCHEMA_MULTIPLE_VALUES, true);
		}

        // schema:readonlyValue, boolean, 0..1
        boolean readonly = "true".equals(entity.getReadOnlyValue());
        if (Arrays.asList("type", "organisationName", "individualName", "purpose", "status", "distributionFormat").contains(entity.getVariable())) {
            readonly = true;
        }
        model.add(subject, RDFConstants.SCHEMA_READONLY_VALUE, model.createTypedLiteral(readonly));

        // hydra:required, boolean, 0..1
        boolean isRequired = "true".equals(entity.getRequired());
        model.add(subject, RDFConstants.HYDRA_REQUIRED, model.createTypedLiteral(isRequired));

        // http:paramValue, literal, 0..n
        if (entity.getParamValue() != null) {
            for (String paramValue : entity.getParamValue()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.HTTP_PARAM_VALUE, paramValue);
            }
        }

        return subject;
    }

    @Override
    public Resource exportToV3(Mapping entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        // IriTemplateMapping uses blank nodes, so we don't use resource cache
        // Create blank node for IriTemplateMapping
        Resource subject = RDFHelper.createBlankNode(model);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_IRI_TEMPLATE_MAPPING);

        // hydra:variable, literal, 1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_VARIABLE, entity.getVariable());

        // hydra:property, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_PROPERTY, entity.getProperty());

        // rdfs:range, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.RDFS_RANGE, entity.getRange());

        // rdfs:label, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.RDFS_LABEL, entity.getLabel());

        // schema:valuePattern, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_VALUE_PATTERN, entity.getValuePattern());

        // schema:defaultValue, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DEFAULT_VALUE, entity.getDefaultValue());

        // schema:minValue, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_MIN_VALUE, entity.getMinValue());

        // schema:maxValue, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_MAX_VALUE, entity.getMaxValue());

        // schema:readonlyValue, boolean, 0..1
        boolean readonly = "true".equals(entity.getReadOnlyValue());
        if (Arrays.asList("type", "organisationName", "individualName", "purpose", "status", "distributionFormat").contains(entity.getVariable())) {
            readonly = true;
        }
        model.add(subject, RDFConstants.SCHEMA_READONLY_VALUE, model.createTypedLiteral(readonly));

        // hydra:required, boolean, 0..1
        boolean isRequired = "true".equals(entity.getRequired());
        model.add(subject, RDFConstants.HYDRA_REQUIRED, model.createTypedLiteral(isRequired));

        // http:paramValue, literal, 0..n
        if (entity.getParamValue() != null) {
            for (String paramValue : entity.getParamValue()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.HTTP_PARAM_VALUE, paramValue);
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.HYDRA_NS + "IriTemplateMapping";
    }
}
