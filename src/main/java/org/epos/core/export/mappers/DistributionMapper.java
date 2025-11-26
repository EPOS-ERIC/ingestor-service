package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
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

		// dcat:accessURL, resource, 1..n
		if (entity.getAccessURL() == null || entity.getAccessURL().isEmpty()) {
			//TODO: invalid, throw an exception maype? if so we need to handle it correctly upstream
		} else {
			for (var accessURL: entity.getAccessURL()) {
				RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_ACCESS_URL, accessURL);
			}
		}

		// identifier, literal, 1
        RDFHelper.addLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// dct:description, literal, 0..n
		if (entity.getDescription() != null) {
			for (var description: entity.getDescription()) {
				if (!description.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, description);
				}
			}
		}

		// dct:format, dct:MediaTypeOrExtent, 0..1
		if (entity.getFormat() != null && !entity.getFormat().isEmpty()) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_FORMAT, entity.getFormat());
		}

		// dct:license, dct:LicenseDocument, 0..1
		if (entity.getLicence() != null && !entity.getLicence().isEmpty()) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicence());
		}

		// dcat:accessService, dcat:DataService, 0..n
		if (entity.getAccessService() != null && !entity.getAccessService().isEmpty()) {
			for (var linkedEntity : entity.getAccessService()) {
				Resource accessServiceResource;
				EPOSDataModelEntity accessServiceEntity = entityMap.get(linkedEntity.getUid());
				if (accessServiceEntity instanceof org.epos.eposdatamodel.WebService) {
					WebServiceMapper accessServiceMapper = new WebServiceMapper();
					accessServiceResource = accessServiceMapper.mapToRDF((org.epos.eposdatamodel.WebService) accessServiceEntity, model, entityMap, resourceCache);
				} else {
					accessServiceResource = model.createResource(linkedEntity.getUid());
				}
				model.add(subject, RDFConstants.DCAT_ACCESS_SERVICE, accessServiceResource);
			}
		}

		// dcat:byteSize, xsd:nonNegativeInteger, 0..1
		if (entity.getByteSize() != null && !entity.getByteSize().isEmpty()) {
			try {
				Integer byteSizeInt = Integer.parseInt(entity.getByteSize());
				RDFHelper.addIntLiteral(model, subject, RDFConstants.DCAT_BYTE_SIZE, byteSizeInt);
			} catch (NumberFormatException e) {
				// skip invalid byteSize
			}
		}

		// dcat:downloadURL, rdfs:Resource, 0..n
		if (entity.getDownloadURL() != null && !entity.getDownloadURL().isEmpty()) {
			for (var downloadURL : entity.getDownloadURL()) {
				RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_DOWNLOAD_URL, downloadURL);
			}
		}

		// dct:issued, (rdfs:Literal typed as xsd:date or xsd:dateTime), 0..1
		if (entity.getIssued() != null) {
			String issuedStr = entity.getIssued().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_ISSUED, issuedStr, XSDDatatype.XSDdateTime);
		}

		// dcat:mediaType, dct:MediaType, 0..1
		if (entity.getMediaType() != null && !entity.getMediaType().isEmpty()) {
			RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_MEDIA_TYPE, entity.getMediaType());
		}

		// dct:modified, (rdfs:Literal typed as xsd:date or xsd:dateTime), 0..1
		if (entity.getModified() != null) {
			String modifiedStr = entity.getModified().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_MODIFIED, modifiedStr, XSDDatatype.XSDdateTime);
		}

		// dct:title, rdfs:Literal, 0..n
		if (entity.getTitle() != null) {
			for (var title : entity.getTitle()) {
				if (!title.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, title);
				}
			}
		}

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCAT_NS + "Distribution";
    }
}
