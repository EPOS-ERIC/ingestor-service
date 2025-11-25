package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for Distribution entities to DCAT Distribution.
 * Conditional logic.
 */
public class DistributionMapper implements EntityMapper<Distribution> {

    @Override
    public Resource mapToRDF(Distribution entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCAT_DISTRIBUTION_CLASS);

        // Auto-add dct:identifier from UID
        RDFHelper.addLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

        // Add title (multiple)
        if (entity.getTitle() != null) {
            for (String title : entity.getTitle()) {
                RDFHelper.addLiteral(model, subject, RDFConstants.DCT_TITLE, title);
            }
        }

        // Add description (multiple)
        if (entity.getDescription() != null) {
            for (String desc : entity.getDescription()) {
                RDFHelper.addLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, desc);
            }
        }

        // Add issued
        if (entity.getIssued() != null) {
            String issuedDate = entity.getIssued().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_ISSUED, issuedDate);
        }

        // Add modified
        if (entity.getModified() != null) {
            String modifiedDate = entity.getModified().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            RDFHelper.addDateLiteral(model, subject, RDFConstants.DCT_MODIFIED, modifiedDate);
        }

        // Add type
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

        // Add format
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_FORMAT, entity.getFormat());

        // Add license
        RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicence());

        // Add conformsTo
        if (entity.getAccessService() != null && !entity.getAccessService().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getAccessService()) {
                RDFHelper.addURI(model, subject, RDFConstants.DCT_CONFORMS_TO, linkedEntity.getUid());
            }
        }

        // Add accessURL
        if (entity.getSupportedOperation() != null && !entity.getSupportedOperation().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getSupportedOperation()) {
                RDFHelper.addURI(model, subject, RDFConstants.DCAT_ACCESS_URL, linkedEntity.getUid());
            }
        } else {
            if (entity.getAccessURL() != null) {
                for (String url : entity.getAccessURL()) {
                    RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_ACCESS_URL, url);
                }
            }
        }

        // Add downloadURL
        if (entity.getDownloadURL() != null) {
            for (String url : entity.getDownloadURL()) {
                RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_DOWNLOAD_URL, url);
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCAT_NS + "Distribution";
    }
}
