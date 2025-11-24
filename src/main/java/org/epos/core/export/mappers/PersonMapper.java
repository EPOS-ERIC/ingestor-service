package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;

import java.util.Map;

/**
 * Mapper for Person entities to Schema.org Person.
 */
public class PersonMapper implements EntityMapper<Person> {

    @Override
    public Resource mapToRDF(Person entity, Model model, Map<String, EPOSDataModelEntity> entityMap) {
        // Create resource
        Resource subject = model.createResource(entity.getUid());

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_PERSON);

        // Add names
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_GIVEN_NAME, entity.getGivenName());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_FAMILY_NAME, entity.getFamilyName());

        // Add email (multiple)
        if (entity.getEmail() != null) {
            for (String email : entity.getEmail()) {
                RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_EMAIL, email);
            }
        }

        // Add telephone (multiple)
        if (entity.getTelephone() != null) {
            for (String tel : entity.getTelephone()) {
                RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_TELEPHONE, tel);
            }
        }

        // Add address (inline)
        if (entity.getAddress() != null) {
            EPOSDataModelEntity addressEntity = entityMap.get(entity.getAddress().getUid());
            if (addressEntity instanceof org.epos.eposdatamodel.Address) {
                AddressMapper addressMapper = new AddressMapper();
                Resource addressResource = addressMapper.mapToRDF((org.epos.eposdatamodel.Address) addressEntity, model, entityMap);
                model.add(subject, RDFConstants.SCHEMA_ADDRESS, addressResource);
            }
        }

        // Add identifiers (inline)
        if (entity.getIdentifier() != null) {
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

        // Add qualifications
        if (entity.getQualifications() != null) {
            for (String qual : entity.getQualifications()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_QUALIFICATIONS, qual);
            }
        }

        // Add affiliation
        if (entity.getAffiliation() != null) {
            for (LinkedEntity linked : entity.getAffiliation()) {
                model.add(subject, RDFConstants.SCHEMA_AFFILIATION, model.createResource(linked.getUid()));
            }
        }

        // Add contactPoint
        if (entity.getContactPoint() != null) {
            for (LinkedEntity linked : entity.getContactPoint()) {
                model.add(subject, RDFConstants.SCHEMA_CONTACT_POINT, model.createResource(linked.getUid()));
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "Person";
    }
}