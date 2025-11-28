package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.util.Map;

/**
 * Mapper for Address entities to Schema.org PostalAddress.
 * Follows EPOS-DCAT-AP v3 specification.
 * Uses blank nodes (anonymous resources) as per v3 spec.
 */
public class AddressMapper implements EntityMapper<Address> {

	@Override
	public Resource exportToV1(Address entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		// Address uses blank nodes in v1, so we don't use resource cache

		// In v1, all 4 address properties are MANDATORY (1..1)
		if (entity.getStreet() == null || entity.getStreet().trim().isEmpty() ||
				entity.getLocality() == null || entity.getLocality().trim().isEmpty() ||
				entity.getPostalCode() == null || entity.getPostalCode().trim().isEmpty() ||
				entity.getCountry() == null || entity.getCountry().trim().isEmpty()) {
			return null;
		}

		// Create blank node for Address
		Resource subject = RDFHelper.createBlankNode(model);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.VCARD_ADDRESS);

		// vcard:street-address, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.VCARD_STREET_ADDRESS, entity.getStreet());

		// vcard:locality, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.VCARD_LOCALITY, entity.getLocality());

		// vcard:postal-code, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.VCARD_POSTAL_CODE, entity.getPostalCode());

		// vcard:country-name, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.VCARD_COUNTRY_NAME, entity.getCountry());

		return subject;
	}

	@Override
	public Resource exportToV3(Address entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		// PostalAddress uses blank nodes in v3, so we don't use resource cache

		// In v3, all 4 address properties are MANDATORY (1..1)
		if (entity.getStreet() == null || entity.getStreet().trim().isEmpty() ||
				entity.getLocality() == null || entity.getLocality().trim().isEmpty() ||
				entity.getPostalCode() == null || entity.getPostalCode().trim().isEmpty() ||
				entity.getCountry() == null || entity.getCountry().trim().isEmpty()) {
			return null;
		}

		// Create blank node for PostalAddress
		Resource subject = RDFHelper.createBlankNode(model);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.SCHEMA_POSTAL_ADDRESS);

		// schema:streetAddress, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_STREET_ADDRESS, entity.getStreet());

		// schema:addressLocality, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ADDRESS_LOCALITY, entity.getLocality());

		// schema:postalCode, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_POSTAL_CODE, entity.getPostalCode());

		// schema:addressCountry, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ADDRESS_COUNTRY, entity.getCountry());

		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.SCHEMA_NS + "PostalAddress";
	}
}
