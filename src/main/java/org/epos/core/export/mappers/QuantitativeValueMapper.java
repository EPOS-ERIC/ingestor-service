package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.QuantitativeValue;

import java.util.Map;

/**
 * Stub mapper for QuantitativeValue entities.
 */
public class QuantitativeValueMapper implements EntityMapper<QuantitativeValue> {

    @Override
    public Resource exportToV1(QuantitativeValue entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v1 model required fields
        if (entity.getValue() == null || entity.getUnit() == null) {
            return null;
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_QUANTITATIVE_VALUE);
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_VALUE, entity.getValue());
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_UNIT_CODE, entity.getUnit());
        return subject;
    }

    @Override
    public Resource exportToV3(QuantitativeValue entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_QUANTITATIVE_VALUE);
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_VALUE, entity.getValue());
        RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_UNIT_TEXT, entity.getUnit());
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "QuantitativeValue";
    }
}