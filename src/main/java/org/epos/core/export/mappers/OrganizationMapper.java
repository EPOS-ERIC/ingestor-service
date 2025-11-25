package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;

import java.util.Map;

/**
 * Mapper for Organization entities to FOAF Agent.
 * Nested: Address (inline), Identifier (inline).
 */
public class OrganizationMapper implements EntityMapper<Organization> {

	@Override
	public Resource mapToRDF(Organization entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.SCHEMA_ORGANIZATION);

		// Add name
		if (entity.getLegalName() != null) {
			for (String name : entity.getLegalName()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_LEGAL_NAME, name);
				// Max one name
				break;
			}
		}
		// Add logo
		RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_LOGO, entity.getLogo());

		// Add URL
		RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_URL, entity.getURL());

		// Add email 
		if (entity.getEmail() != null) {
			for (String email : entity.getEmail()) {
				RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_EMAIL, email);
			}
		}

		// Add address (inline as blank node)
		if (entity.getAddress() != null && !model.contains(subject, RDFConstants.SCHEMA_ADDRESS, (RDFNode) null)) {
			EPOSDataModelEntity addressEntity = entityMap.get(entity.getAddress().getUid());
			if (addressEntity instanceof org.epos.eposdatamodel.Address) {
				Resource addressResource = model.createResource(); // blank node
				RDFHelper.addType(model, addressResource, RDFConstants.SCHEMA_POSTAL_ADDRESS);
				RDFHelper.addStringLiteral(model, addressResource, RDFConstants.SCHEMA_STREET_ADDRESS, ((org.epos.eposdatamodel.Address) addressEntity).getStreet());
				RDFHelper.addStringLiteral(model, addressResource, RDFConstants.SCHEMA_ADDRESS_LOCALITY, ((org.epos.eposdatamodel.Address) addressEntity).getLocality());
				RDFHelper.addStringLiteral(model, addressResource, RDFConstants.SCHEMA_POSTAL_CODE, ((org.epos.eposdatamodel.Address) addressEntity).getPostalCode());
				RDFHelper.addStringLiteral(model, addressResource, RDFConstants.SCHEMA_ADDRESS_COUNTRY, ((org.epos.eposdatamodel.Address) addressEntity).getCountry());
				model.add(subject, RDFConstants.SCHEMA_ADDRESS, addressResource);
			}
		}


		// Add identifiers (inline)
		if (entity.getIdentifier() != null && !model.contains(subject, RDFConstants.SCHEMA_IDENTIFIER, (RDFNode) null)) {
			for (LinkedEntity linkedEntity : entity.getIdentifier()) {
				EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
				if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
					Resource identifierResource = model.createResource(); // blank node
					RDFHelper.addType(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_VALUE);
					RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_ID, ((org.epos.eposdatamodel.Identifier) identifierEntity).getType());
					RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_VALUE, ((org.epos.eposdatamodel.Identifier) identifierEntity).getIdentifier());
					model.add(subject, RDFConstants.SCHEMA_IDENTIFIER, identifierResource);
				}
			}
		}

		// Add owns
		if (entity.getOwns() != null) {
			for (LinkedEntity linked : entity.getOwns()) {
				model.add(subject, RDFConstants.SCHEMA_OWNS, model.createResource(linked.getUid()));
			}
		}

		// Add memberOf
		if (entity.getMemberOf() != null) {
			for (LinkedEntity linked : entity.getMemberOf()) {
				model.add(subject, RDFConstants.SCHEMA_MEMBER_OF, model.createResource(linked.getUid()));
			}
		}

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.SCHEMA_ORGANIZATION + "Organization";
	}
}
