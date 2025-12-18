package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.Map;

/**
 * Mapper for IriTemplate entities to Hydra IriTemplate.
 * Maps to blank nodes (anonymous resources) as per EPOS-DCAT-AP v3.
 */
public class IriTemplateMapper implements EntityMapper<org.epos.eposdatamodel.IriTemplate> {

    @Override
    public Resource exportToV1(org.epos.eposdatamodel.IriTemplate entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // IriTemplate uses blank nodes, so we don't use resource cache
        // Create blank node for IriTemplate
        Resource subject = RDFHelper.createBlankNode(model);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_IRI_TEMPLATE);

        // hydra:template, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_TEMPLATE, entity.getTemplate());

        // hydra:mapping, hydra:IriTemplateMapping, 0..n
        if (entity.getMappings() != null && !entity.getMappings().isEmpty()) {
            MappingMapper mappingMapper = new MappingMapper();
            for (LinkedEntity mappingLinked : entity.getMappings()) {
                EPOSDataModelEntity mappingEntity = entityMap.get(mappingLinked.getUid());
                if (mappingEntity instanceof org.epos.eposdatamodel.Mapping) {
                    org.epos.eposdatamodel.Mapping mapping = (org.epos.eposdatamodel.Mapping) mappingEntity;
                    Resource mappingResource = mappingMapper.exportToV1(mapping, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.HYDRA_MAPPING, mappingResource);
                }
            }
        }

        return subject;
    }

    @Override
    public Resource exportToV3(org.epos.eposdatamodel.IriTemplate entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        // IriTemplate uses blank nodes, so we don't use resource cache
        // Create blank node for IriTemplate
        Resource subject = RDFHelper.createBlankNode(model);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_IRI_TEMPLATE);

        // hydra:template, literal, 1..1
        RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_TEMPLATE, entity.getTemplate());

        // hydra:mapping, hydra:IriTemplateMapping, 0..n
        if (entity.getMappings() != null && !entity.getMappings().isEmpty()) {
            MappingMapper mappingMapper = new MappingMapper();
            for (LinkedEntity mappingLinked : entity.getMappings()) {
                EPOSDataModelEntity mappingEntity = entityMap.get(mappingLinked.getUid());
                if (mappingEntity instanceof org.epos.eposdatamodel.Mapping) {
                    org.epos.eposdatamodel.Mapping mapping = (org.epos.eposdatamodel.Mapping) mappingEntity;
                    Resource mappingResource = mappingMapper.exportToV3(mapping, model, entityMap, resourceCache);
                    model.add(subject, RDFConstants.HYDRA_MAPPING, mappingResource);
                }
            }
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.HYDRA_NS + "IriTemplate";
    }
}
