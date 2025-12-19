package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.PeriodOfTime;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for PeriodOfTime entities to dct:PeriodOfTime.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class PeriodOfTimeMapper implements EntityMapper<PeriodOfTime> {

    @Override
    public Resource exportToV1(PeriodOfTime entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create blank node for PeriodOfTime
        Resource subject = RDFHelper.createBlankNode(model);
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCT_PERIOD_OF_TIME);

        // schema:startDate, xsd:date or xsd:dateTime, 0..1
        if (entity.getStartDate() != null) {
            String dateString = ((LocalDateTime) entity.getStartDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            model.add(subject, RDFConstants.SCHEMA_START_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
        }

        // schema:endDate, xsd:date or xsd:dateTime, 0..1
        if (entity.getEndDate() != null) {
            String dateString = ((LocalDateTime) entity.getEndDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            model.add(subject, RDFConstants.SCHEMA_END_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
        }

        return subject;
    }

    @Override
    public Resource exportToV3(PeriodOfTime entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create blank node for PeriodOfTime
        Resource subject = RDFHelper.createBlankNode(model);
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCT_PERIOD_OF_TIME);

        // dcat:startDate, xsd:date or xsd:dateTime, 0..1
        if (entity.getStartDate() != null) {
            String dateString = ((LocalDateTime) entity.getStartDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            model.add(subject, RDFConstants.DCAT_START_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
        }

        // dcat:endDate, xsd:date or xsd:dateTime, 0..1
        if (entity.getEndDate() != null) {
            String dateString = ((LocalDateTime) entity.getEndDate()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
            model.add(subject, RDFConstants.DCAT_END_DATE, model.createTypedLiteral(dateString, XSDDatatype.XSDdateTime));
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCT_NS + "PeriodOfTime";
    }
}
