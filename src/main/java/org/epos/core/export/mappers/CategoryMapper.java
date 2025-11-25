package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for Category entities to SKOS Concept or ConceptScheme.
 */
public class CategoryMapper implements EntityMapper<Category> {

    @Override
    public Resource mapToRDF(Category entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
		System.err.println("DEBUGPRINT: CategoryMapper.java:22: entity=" + entity);
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Determine if this is a ConceptScheme or Concept
        Resource rdfType =  RDFConstants.SKOS_CONCEPT;

        // Check if already mapped to prevent recursion
        if (model.contains(subject, RDFConstants.RDF_TYPE, rdfType)) {
            return subject;
        }

        // Add type
        RDFHelper.addType(model, subject, rdfType);

		// Add Concept properties
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_PREF_LABEL, entity.getName());
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_DEFINITION, entity.getDescription());

		// Add inScheme
		if (entity.getInScheme() != null) {
			EPOSDataModelEntity relatedEntity = entityMap.get(entity.getInScheme().getUid());
			if (relatedEntity instanceof org.epos.eposdatamodel.CategoryScheme) {
				CategorySchemeMapper categorySchemeMapper = new CategorySchemeMapper();
				Resource schemeResource = categorySchemeMapper.mapToRDF((org.epos.eposdatamodel.CategoryScheme) relatedEntity, model, entityMap, resourceCache);
				model.add(subject, RDFConstants.SKOS_IN_SCHEME, schemeResource);
			}
		}

		// Add broader
		if (entity.getBroader() != null) {
			for (LinkedEntity linkedEntity : entity.getBroader()) {
				EPOSDataModelEntity relatedEntity = entityMap.get(linkedEntity.getUid());
				if (relatedEntity instanceof Category) {
					CategoryMapper categoryMapper = new CategoryMapper();
					Resource broaderResource = categoryMapper.mapToRDF((Category) relatedEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.SKOS_BROADER, broaderResource);
				}
			}
		}

		// Add narrower
		if (entity.getNarrower() != null) {
			for (LinkedEntity linkedEntity : entity.getNarrower()) {
				EPOSDataModelEntity relatedEntity = entityMap.get(linkedEntity.getUid());
				if (relatedEntity instanceof Category) {
					CategoryMapper categoryMapper = new CategoryMapper();
					Resource narrowerResource = categoryMapper.mapToRDF((Category) relatedEntity, model, entityMap, resourceCache);
					model.add(subject, RDFConstants.SKOS_NARROWER, narrowerResource);
				}
			}
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.SKOS_NS + "Concept";
    }
}
