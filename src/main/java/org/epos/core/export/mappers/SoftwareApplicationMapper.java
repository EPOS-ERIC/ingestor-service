package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for SoftwareApplication entities to schema:SoftwareApplication.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class SoftwareApplicationMapper implements EntityMapper<SoftwareApplication> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoftwareApplicationMapper.class);

    @Override
    public Resource exportToV1(SoftwareApplication entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v1 model required fields
        if (entity.getIdentifier() == null || entity.getIdentifier().isEmpty()) {
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_SOFTWARE_APPLICATION);

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

		// schema:name, literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());

		// schema:description, literal, 0..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());

        // dcat:contactPoint or schema:contactPoint, schema:ContactPoint, 0..n
        if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getContactPoint()) {
                EPOSDataModelEntity contactEntity = entityMap.get(linkedEntity.getUid());
                if (contactEntity instanceof org.epos.eposdatamodel.ContactPoint) {
                    ContactPointMapper contactMapper = new ContactPointMapper();
                    Resource contactResource = contactMapper.exportToV1((org.epos.eposdatamodel.ContactPoint) contactEntity, model, entityMap, resourceCache);
                    if (contactResource != null) {
                        model.add(subject, RDFConstants.SCHEMA_CONTACT_POINT, contactResource);
                    } else {
                        LOGGER.warn("Skipping invalid contactPoint for SoftwareApplication {}", entity.getUid());
                    }
                }
            }
        }

        // schema:mainEntityOfPage, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_MAIN_ENTITY_OF_PAGE, entity.getMainEntityOfPage());

        // schema:license, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_LICENSE, entity.getLicenseURL());

        // schema:softwareVersion, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_VERSION, entity.getSoftwareVersion());

		if (entity.getKeywords() != null) {
			RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_KEYWORDS, entity.getKeywords().replace("\t", " "));
		}

        // dcat:theme, skos:Concept, 0..n
        if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getCategory()) {
                EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
                if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
                    CategoryMapper categoryMapper = new CategoryMapper();
                    Resource categoryResource = categoryMapper.exportToV1((org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
                    if (categoryResource != null) {
                        model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
                    } else {
                        LOGGER.warn("Skipping invalid category for SoftwareApplication {}", entity.getUid());
                    }
                }
            }
        }

        // schema:downloadUrl, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_DOWNLOAD_URL, entity.getDownloadURL());

        // schema:softwareRequirements, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_REQUIREMENTS, entity.getRequirements());

        // schema:creator, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getCreator() != null && !entity.getCreator().isEmpty()) {
            for (LinkedEntity linked : entity.getCreator()) {
                model.add(subject, RDFConstants.SCHEMA_CREATOR, model.createResource(linked.getUid()));
            }
        }

		if (entity.getOperatingSystem() != null && !entity.getOperatingSystem().isEmpty()) {
			for (String operatingSystem : entity.getOperatingSystem()) {
				RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_OPERATING_SYSTEM, operatingSystem);
			}
		}

        return subject;
    }

    @Override
    public Resource exportToV3(SoftwareApplication entity, Model model, Map<String, EPOSDataModelEntity> entityMap,
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
        RDFHelper.addType(model, subject, RDFConstants.SCHEMA_SOFTWARE_APPLICATION);

        // schema:identifier, literal or schema:PropertyValue, 1..n
        if (entity.getIdentifier() != null && !entity.getIdentifier().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getIdentifier()) {
                EPOSDataModelEntity identifierEntity = entityMap.get(linkedEntity.getUid());
                if (identifierEntity instanceof org.epos.eposdatamodel.Identifier) {
                    Resource identifierResource = RDFHelper.createBlankNode(model);
                    RDFHelper.addType(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_VALUE);
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_PROPERTY_ID,
                            ((org.epos.eposdatamodel.Identifier) identifierEntity).getType());
                    RDFHelper.addLiteral(model, identifierResource, RDFConstants.SCHEMA_VALUE,
                            ((org.epos.eposdatamodel.Identifier) identifierEntity).getIdentifier());
                    model.add(subject, RDFConstants.SCHEMA_IDENTIFIER, identifierResource);
                }
            }
        } else {
            RDFHelper.addLiteral(model, subject, RDFConstants.SCHEMA_IDENTIFIER, entity.getUid());
        }

        // dcat:contactPoint or schema:contactPoint, schema:ContactPoint, 0..n
        if (entity.getContactPoint() != null && !entity.getContactPoint().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getContactPoint()) {
                EPOSDataModelEntity contactEntity = entityMap.get(linkedEntity.getUid());
                if (contactEntity instanceof org.epos.eposdatamodel.ContactPoint) {
                    ContactPointMapper contactMapper = new ContactPointMapper();
                    Resource contactResource = contactMapper.exportToV3(
                            (org.epos.eposdatamodel.ContactPoint) contactEntity, model, entityMap, resourceCache);
                    if (contactResource != null) {
                        model.add(subject, RDFConstants.SCHEMA_CONTACT_POINT, contactResource);
                    } else {
                        LOGGER.warn("Skipping invalid contactPoint for SoftwareApplication {}", entity.getUid());
                    }
                }
            }
        }

        // schema:name, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_NAME, entity.getName());

        // schema:description, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_DESCRIPTION, entity.getDescription());

        // schema:downloadUrl, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_DOWNLOAD_URL, entity.getDownloadURL());

        // schema:installUrl, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_INSTALL_URL, entity.getInstallURL());

        // schema:keywords, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_KEYWORDS, entity.getKeywords());

        // schema:license, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_LICENSE, entity.getLicenseURL());

        // schema:mainEntityOfPage, literal typed with URI, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.SCHEMA_MAIN_ENTITY_OF_PAGE, entity.getMainEntityOfPage());

        // dct:relation, hydra:Operation, 0..n
        if (entity.getRelatedOperation() != null && !entity.getRelatedOperation().isEmpty()) {
            for (LinkedEntity linked : entity.getRelatedOperation()) {
                model.add(subject, RDFConstants.DCT_RELATION, model.createResource(linked.getUid()));
            }
        }

        // schema:softwareRequirements, literal typed with URI, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_REQUIREMENTS, entity.getRequirements());

        // schema:softwareVersion, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_SOFTWARE_VERSION, entity.getSoftwareVersion());

        // dcat:theme, skos:Concept, 0..n
        if (entity.getCategory() != null && !entity.getCategory().isEmpty()) {
            for (LinkedEntity linkedEntity : entity.getCategory()) {
                EPOSDataModelEntity categoryEntity = entityMap.get(linkedEntity.getUid());
                if (categoryEntity instanceof org.epos.eposdatamodel.Category) {
                    CategoryMapper categoryMapper = new CategoryMapper();
                    Resource categoryResource = categoryMapper.exportToV3(
                            (org.epos.eposdatamodel.Category) categoryEntity, model, entityMap, resourceCache);
                    if (categoryResource != null) {
                        model.add(subject, RDFConstants.DCAT_THEME, categoryResource);
                    } else {
                        LOGGER.warn("Skipping invalid category for SoftwareApplication {}", entity.getUid());
                    }
                }
            }
        }

        // schema:author, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getAuthor() != null && !entity.getAuthor().isEmpty()) {
            for (LinkedEntity linked : entity.getAuthor()) {
                model.add(subject, RDFConstants.SCHEMA_AUTHOR, model.createResource(linked.getUid()));
            }
        }

        // schema:contributor, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getContributor() != null && !entity.getContributor().isEmpty()) {
            for (LinkedEntity linked : entity.getContributor()) {
                model.add(subject, RDFConstants.SCHEMA_CONTRIBUTOR, model.createResource(linked.getUid()));
            }
        }

        // schema:creator, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getCreator() != null && !entity.getCreator().isEmpty()) {
            for (LinkedEntity linked : entity.getCreator()) {
                model.add(subject, RDFConstants.SCHEMA_CREATOR, model.createResource(linked.getUid()));
            }
        }

        // schema:funder, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getFunder() != null && !entity.getFunder().isEmpty()) {
            for (LinkedEntity linked : entity.getFunder()) {
                model.add(subject, RDFConstants.SCHEMA_FUNDER, model.createResource(linked.getUid()));
            }
        }

        // schema:maintainer, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getMaintainer() != null && !entity.getMaintainer().isEmpty()) {
            for (LinkedEntity linked : entity.getMaintainer()) {
                model.add(subject, RDFConstants.SCHEMA_MAINTAINER, model.createResource(linked.getUid()));
            }
        }

        // schema:provider, schema:Organization or schema:Person or foaf:Agent, 0..n
        if (entity.getProvider() != null && !entity.getProvider().isEmpty()) {
            for (LinkedEntity linked : entity.getProvider()) {
                model.add(subject, RDFConstants.SCHEMA_PROVIDER, model.createResource(linked.getUid()));
            }
        }

        // schema:publisher, schema:Organization or schema:Person or foaf:Agent, 0..1
        if (entity.getPublisher() != null && !entity.getPublisher().isEmpty()) {
            // v3 spec says 0..1, so take first value
            model.add(subject, RDFConstants.SCHEMA_PUBLISHER,
                    model.createResource(entity.getPublisher().get(0).getUid()));
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SCHEMA_NS + "SoftwareApplication";
    }
}
