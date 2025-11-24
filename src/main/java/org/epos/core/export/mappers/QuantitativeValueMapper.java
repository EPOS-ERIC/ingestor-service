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
    public Resource mapToRDF(QuantitativeValue entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        Resource subject = model.createResource(entity.getUid());
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