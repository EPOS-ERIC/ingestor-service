package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Mapper for Category entities to SKOS Concept.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class CategoryMapper implements EntityMapper<Category> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryMapper.class);

	@Override
	public Resource mapToRDF(Category entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
		// In v3, description and name are mandatory (1..1)
		if (entity.getDescription() == null ||
				entity.getDescription().trim().isEmpty() ||
				entity.getName() == null ||
				entity.getName().trim().isEmpty()) {
			LOGGER.warn("Entity {} not compliant with v3 model: missing required fields (description or name)", entity.getUid());
			return null;
		}

		if (resourceCache.containsKey(entity.getUid())) {
			return resourceCache.get(entity.getUid());
		}
		// Create resource
		Resource subject = model.createResource(entity.getUid());
		resourceCache.put(entity.getUid(), subject);

		// Add type
		RDFHelper.addType(model, subject, RDFConstants.SKOS_CONCEPT);

		// skos:definition, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_DEFINITION, entity.getDescription());

		// skos:inScheme, skos:ConceptScheme, 1..1
		if (entity.getInScheme() != null) {
			model.add(subject, RDFConstants.SKOS_IN_SCHEME, model.createResource(entity.getInScheme().getUid()));
		}

		// skos:prefLabel, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.SKOS_PREF_LABEL, entity.getName());

		// skos:broader, skos:Concept, 0..1
		// Note: v3 spec says 0..1, but entity has list. We take only the first value
		if (entity.getBroader() != null && !entity.getBroader().isEmpty()) {
			model.add(subject, RDFConstants.SKOS_BROADER, model.createResource(entity.getBroader().get(0).getUid()));
		}

		// skos:narrower, skos:Concept, 0..n
		if (entity.getNarrower() != null && !entity.getNarrower().isEmpty()) {
			for (LinkedEntity linked : entity.getNarrower()) {
				model.add(subject, RDFConstants.SKOS_NARROWER, model.createResource(linked.getUid()));
			}
		}
		return subject;
	}

	@Override
	public String getDCATClassURI() {
		return RDFConstants.SKOS_NS + "Concept";
	}
}
