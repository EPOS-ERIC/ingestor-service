package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for CategoryScheme entities to SKOS ConceptScheme.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class CategorySchemeMapper implements EntityMapper<CategoryScheme> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CategorySchemeMapper.class);

    @Override
    public Resource exportToV1(CategoryScheme entity, Model model, Map<String, EPOSDataModelEntity> entityMap,
            Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v1 model required fields
        if (entity.getDescription() == null || entity.getDescription().trim().isEmpty()) {
            LOGGER.warn("Entity {} not compliant with v1 model: missing required fields (description)", entity.getUid());
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SKOS_CONCEPT_SCHEME);

        // dct:title, literal, 1..n
        if (entity.getTitle() != null && !entity.getTitle().isEmpty()) {
            RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getTitle());
        }

        // dct:description, literal, 1..n
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());

        // skos:prefLabel, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_PREF_LABEL, entity.getCode());

        // schema:color, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_COLOR, entity.getColor());

        // skos:hasTopConcept, skos:Concept, 0..1
        // Note: v1 spec says 0..1, but entity has list. We take only the first value
        if (entity.getTopConcepts() != null && !entity.getTopConcepts().isEmpty()) {
            model.add(subject, RDFConstants.SKOS_HAS_TOP_CONCEPT,
                    model.createResource(entity.getTopConcepts().get(0).getUid()));
        }

        // foaf:homepage, foaf:Document, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_HOMEPAGE, entity.getHomepage());

        // foaf:logo, literal, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_LOGO, entity.getLogo());

        // schema:orderItemNumber, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ORDER_ITEM_NUMBER, entity.getOrderitemnumber());

        return subject;
    }

    @Override
    public Resource exportToV3(CategoryScheme entity, Model model, Map<String, EPOSDataModelEntity> entityMap,
            Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Compliance check for v3 model required fields
        if (entity.getTitle() == null || entity.getTitle().trim().isEmpty()) {
            LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (title)", entity.getUid());
            return null;
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SKOS_CONCEPT_SCHEME);

        // dct:title, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getTitle());

        // dct:description, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());

        // skos:prefLabel, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_PREF_LABEL, entity.getTitle());

        // schema:color, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_COLOR, entity.getColor());

        // skos:hasTopConcept, skos:Concept, 0..1
        // Note: v3 spec says 0..1, but entity has list. We take only the first value
        if (entity.getTopConcepts() != null && !entity.getTopConcepts().isEmpty()) {
            model.add(subject, RDFConstants.SKOS_HAS_TOP_CONCEPT,
                    model.createResource(entity.getTopConcepts().get(0).getUid()));
        }

        // foaf:homepage, foaf:Document, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_HOMEPAGE, entity.getHomepage());

        // foaf:logo, literal, 0..1
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_LOGO, entity.getLogo());

        // schema:orderItemNumber, literal, 0..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ORDER_ITEM_NUMBER, entity.getOrderitemnumber());

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SKOS_NS + "ConceptScheme";
    }
}
