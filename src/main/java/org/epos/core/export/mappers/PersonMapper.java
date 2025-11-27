package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for Person entities to Schema.org Person.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class PersonMapper implements EntityMapper<Person> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersonMapper.class);

    @Override
    public Resource mapToRDF(Person entity, Model model, Map<String, EPOSDataModelEntity> entityMap,
            Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v3 model required fields
        if (entity.getIdentifier() == null || entity.getIdentifier().isEmpty()) {
            LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (identifier)", entity.getUid());
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_PERSON);

        // schema:identifier, literal or schema:PropertyValue, 1..n
        if (entity.getIdentifier() != null && !entity.getIdentifier().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getIdentifier()) {
                EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
                if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
                    // Create blank node for PropertyValue
                    Resource identifierResource = RDFHelper.createBlankNode(model);
                    RDFHelper.addType(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_VALUE);
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_ID, ((org.epos.eposdatamodel.Identifier) identifierEntity).getType());
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_VALUE, ((org.epos.eposdatamodel.Identifier) identifierEntity).getIdentifier());
                    model.add(subject, RDFConstants.SCHEMA_IDENTIFIER, identifierResource);
                }
            }
        }

        // schema:address, literal or schema:PostalAddress, 0..1
        if (entity.getAddress() != null) {
            EPOSDataModelEntity addressEntity = entityMap.get(entity.getAddress().getUid());
            if (addressEntity instanceof org.epos.eposdatamodel.Address) {
                AddressMapper addressMapper = new AddressMapper();
                Resource addressResource = addressMapper.mapToRDF((org.epos.eposdatamodel.Address) addressEntity, model, entityMap, resourceCache);
                if (addressResource != null) {
                    model.add(subject, RDFConstants.SCHEMA_ADDRESS, addressResource);
                } else {
                    LOGGER.warn("Invalid address for person uid={}: missing required fields for address with street={}, locality={}, postalCode={}, country={}",
                            entity.getUid(), ((org.epos.eposdatamodel.Address) addressEntity).getStreet(),
                            ((org.epos.eposdatamodel.Address) addressEntity).getLocality(),
                            ((org.epos.eposdatamodel.Address) addressEntity).getPostalCode(),
                            ((org.epos.eposdatamodel.Address) addressEntity).getCountry());
                }
            }
        }

        // schema:affiliation, schema:Organization, 0..n
        if (entity.getAffiliation() != null && !entity.getAffiliation().isEmpty()) {
            for (LinkedEntity linked : entity.getAffiliation()) {
                model.add(subject, RDFConstants.SCHEMA_AFFILIATION, model.createResource(linked.getUid()));
            }
        }

        // dcat:contactPoint or schema:contactPoint, schema:ContactPoint, 0..n
        if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
            for (LinkedEntity linked : entity.getContactPoint()) {
                model.add(subject, RDFConstants.DCAT_CONTACT_POINT, model.createResource(linked.getUid()));
            }
        }

        // schema:email, literal, 0..n
        if (entity.getEmail() != null && !entity.getEmail().isEmpty()) {
            for (String email : entity.getEmail()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_EMAIL, email);
            }
        }

        // schema:familyName, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_FAMILY_NAME, entity.getFamilyName());

        // schema:givenName, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_GIVEN_NAME, entity.getGivenName());

        // schema:qualifications, literal, 0..1
        // Note: v3 spec says 0..1, but entity has list - we take only the first value
        if (entity.getQualifications() != null && !entity.getQualifications().isEmpty()) {
            RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_QUALIFICATIONS,
                    entity.getQualifications().get(0));
        }

        // schema:telephone, literal, 0..n
        if (entity.getTelephone() != null && !entity.getTelephone().isEmpty()) {
            for (String telephone : entity.getTelephone()) {
                RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_TELEPHONE, telephone);
            }
        }

        // schema:url, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_URL, entity.getCVURL());

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "Person";
    }
}
