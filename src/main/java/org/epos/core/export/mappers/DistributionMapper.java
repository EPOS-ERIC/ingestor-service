package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Mapper for Distribution entities to DCAT Distribution.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class DistributionMapper implements EntityMapper<Distribution> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DistributionMapper.class);

	@Override
	public Resource exportToV1(Distribution entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Compliance check for v1 model required fields
		if ((entity.getSupportedOperation() == null || entity.getSupportedOperation().isEmpty()) && (entity.getDownloadURL() == null || entity.getDownloadURL().isEmpty())) {
			LOGGER.warn("Entity {} not compliant with v1 model: missing required fields (accessURL)", entity.getUid());
			return null;
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.DCAT_DISTRIBUTION_CLASS);

		// dcat:accessURL, resource, 1..n
		if (entity.getSupportedOperation() != null && !entity.getSupportedOperation().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getSupportedOperation()) {
				model.add(subject, RDFConstants.DCAT_ACCESS_URL, model.createResource(linkedEntity.getUid()));
			}
		} else {
			for (var downloadURL : entity.getDownloadURL()) {
				if (!downloadURL.isEmpty()) {
					RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_ACCESS_URL, downloadURL);
				}
			}
		}

		// dct:conformsTo, resource, 0..n
		if (entity.getAccessService() != null && !entity.getAccessService().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getAccessService()) {
				model.add(subject, RDFConstants.DCT_CONFORMS_TO, model.createResource(linkedEntity.getUid()));
			}
		}

		// dct:identifier, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// dct:description, literal, 0..n
		if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
			for (var description : entity.getDescription()) {
				if (!description.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, description);
				}
			}
		}

		// dct:format, dct:MediaTypeOrExtent, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_FORMAT, entity.getFormat());

		// dct:license, dct:LicenseDocument, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicence());

		// dct:title, literal, 0..n
		if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
			for (var title : entity.getTitle()) {
				if (!title.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, title);
				}
			}
		}

		// dct:type, skos:Concept, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_TYPE, entity.getType());

		// dcat:byteSize, xsd:nonNegativeInteger, 0..1
		if (entity.getByteSize() != null) {
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCAT_BYTE_SIZE, entity.getByteSize().toString(), XSDDatatype.XSDnonNegativeInteger);
		}

		// dcat:downloadURL, rdfs:Resource, 0..n
		if (entity.getDownloadURL() != null && !entity.getDownloadURL().isEmpty()) {
			for (var downloadURL : entity.getDownloadURL()) {
				if (!downloadURL.isEmpty()) {
					RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_DOWNLOAD_URL, downloadURL);
				}
			}
		}

		// dcat:mediaType, dct:MediaType, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_MEDIA_TYPE, entity.getMediaType());

		// dct:issued, literal typed as xsd:date or xsd:date, 0..1
		if (entity.getIssued() != null) {
			String dateString = entity.getIssued().format(DateTimeFormatter.ISO_DATE);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_ISSUED, dateString, XSDDatatype.XSDdate);
		}

		// dct:modified, literal typed as xsd:date or xsd:date, 0..1
		if (entity.getModified() != null) {
			String dateString = entity.getModified().format(DateTimeFormatter.ISO_DATE);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_MODIFIED, dateString, XSDDatatype.XSDdate);
		}

		return subject;
	}

	@Override
	public Resource exportToV3(Distribution entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Compliance check for v3 model required fields
		if (entity.getAccessURL() == null || entity.getAccessURL().isEmpty()) {
			LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (accessURL)", entity.getUid());
			return null;
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.DCAT_DISTRIBUTION_CLASS);

		// dcat:accessURL, resource, 1..n
		if (entity.getAccessURL() != null && !entity.getAccessURL().isEmpty()) {
			for (var accessURL : entity.getAccessURL()) {
				RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_ACCESS_URL, accessURL);
			}
		} 

		// dct:identifier, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_IDENTIFIER, entity.getUid());

		// dcat:accessService, dcat:DataService, 0..n
		if (entity.getAccessService() != null && !entity.getAccessService().isEmpty()) {
			for (LinkedEntity linkedEntity : entity.getAccessService()) {
				model.add(subject, RDFConstants.DCAT_ACCESS_SERVICE, model.createResource(linkedEntity.getUid()));
			}
		}

		// dct:description, literal, 0..n
		if (entity.getDescription() != null && !entity.getDescription().isEmpty()) {
			for (var description : entity.getDescription()) {
				if (!description.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, description);
				}
			}
		}

		// dct:format, dct:MediaTypeOrExtent, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_FORMAT, entity.getFormat());

		// dct:license, dct:LicenseDocument, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCT_LICENSE, entity.getLicence());

		// dct:title, literal, 0..n
		if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
			for (var title : entity.getTitle()) {
				if (!title.isEmpty()) {
					RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, title);
				}
			}
		}

		// dcat:byteSize, xsd:nonNegativeInteger, 0..1
		if (entity.getByteSize() != null) {
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCAT_BYTE_SIZE, entity.getByteSize().toString(), XSDDatatype.XSDnonNegativeInteger);
		}

		// dcat:downloadURL, rdfs:Resource, 0..n
		if (entity.getDownloadURL() != null && !entity.getDownloadURL().isEmpty()) {
			for (var downloadURL : entity.getDownloadURL()) {
				if (!downloadURL.isEmpty()) {
					RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_DOWNLOAD_URL, downloadURL);
				}
			}
		}

		// dcat:mediaType, dct:MediaType, 0..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.DCAT_MEDIA_TYPE, entity.getMediaType());

		// dct:issued, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getIssued() != null) {
			String dateString = ((LocalDateTime) entity.getIssued()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_ISSUED, dateString, XSDDatatype.XSDdateTime);
		}

		// dct:modified, literal typed as xsd:date or xsd:dateTime, 0..1
		if (entity.getModified() != null) {
			String dateString = ((LocalDateTime) entity.getModified()).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ISO_INSTANT);
			RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCT_MODIFIED, dateString, XSDDatatype.XSDdateTime);
		}

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.DCAT_NS + "Distribution";
	}
}
