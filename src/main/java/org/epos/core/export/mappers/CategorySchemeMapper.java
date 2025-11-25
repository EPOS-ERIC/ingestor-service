package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for CategoryScheme entities to SKOS ConceptScheme.
 */
public class CategorySchemeMapper implements EntityMapper<CategoryScheme> {

    @Override
    public Resource mapToRDF(CategoryScheme entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.SKOS_CONCEPT_SCHEME);

        // Add simple properties
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_PREF_LABEL, entity.getTitle());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getTitle());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_LOGO, entity.getLogo());
        RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_HOMEPAGE, entity.getHomepage());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_COLOR, entity.getColor());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.SCHEMA_ORDER_ITEM_NUMBER, entity.getOrderitemnumber());

        // Add top concepts
        if (entity.getTopConcepts() != null) {
            for (LinkedEntity linkedEntity : entity.getTopConcepts()) {
                EPOSDataModelEntity relatedEntity = entityMap.get(linkedEntity.getUid());
                if (relatedEntity instanceof org.epos.eposdatamodel.Category) {
                    CategoryMapper categoryMapper = new CategoryMapper();
                    Resource categoryResource = categoryMapper.mapToRDF((org.epos.eposdatamodel.Category) relatedEntity, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.SKOS_HAS_TOP_CONCEPT, categoryResource);
                }
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SKOS_NS + "ConceptScheme";
    }
}
