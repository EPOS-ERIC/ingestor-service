package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.PeriodOfTime;

import java.util.Map;

/**
 * Mapper for PeriodOfTime entities to DCT PeriodOfTime.
 */
public class PeriodOfTimeMapper implements EntityMapper<PeriodOfTime> {

    @Override
    public Resource mapToRDF(PeriodOfTime entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCT_PERIOD_OF_TIME);

        // Add start date
        if (entity.getStartDate() != null) {
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.SCHEMA_START_DATE, entity.getStartDate().toString(), null);
        }

        // Add end date
        if (entity.getEndDate() != null) {
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.SCHEMA_END_DATE, entity.getEndDate().toString(), null);
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCT_NS + "PeriodOfTime";
    }
}