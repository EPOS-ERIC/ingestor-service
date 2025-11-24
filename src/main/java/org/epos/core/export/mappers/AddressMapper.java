package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Address;

import java.util.Map;

/**
 * Mapper for Address entities to Schema.org PostalAddress.
 */
public class AddressMapper implements EntityMapper<Address> {

    @Override
    public Resource mapToRDF(Address entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        // Create resource
        Resource subject = model.createResource(entity.getUid());

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_POSTAL_ADDRESS);

        // Add properties
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_STREET_ADDRESS, entity.getStreet());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ADDRESS_LOCALITY, entity.getLocality());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_POSTAL_CODE, entity.getPostalCode());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ADDRESS_COUNTRY, entity.getCountry());

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_POSTAL_ADDRESS + "Address";
    }
}
