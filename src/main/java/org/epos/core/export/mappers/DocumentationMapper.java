package org.epos.core.export.mappers;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Documentation;

import java.util.Map;

/**
 * Mapper for Documentation entities to DCT Standard.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class DocumentationMapper implements EntityMapper<Documentation> {

    @Override
    public Resource exportToV1(Documentation entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create resource
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.HYDRA_API_DOCUMENTATION);

		// hydra:description, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_DESCRIPTION, entity.getDescription());

		// hydra:title, literal, 1..1
		RDFHelper.addStringLiteral(model, subject, RDFConstants.HYDRA_TITLE, entity.getTitle());

		// hydra:entrypoint, literal, 1..1
		RDFHelper.addURILiteral(model, subject, RDFConstants.HYDRA_ENTRYPOINT, entity.getUri());

        return subject;
    }

    @Override
    public Resource exportToV3(Documentation entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        Resource subject = model.createResource(entity.getUid());
        resourceCache.put(entity.getUid(), subject);
        RDFHelper.addType(model, subject, RDFConstants.DCT_STANDARD);
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_TITLE, entity.getTitle());
        RDFHelper.addStringLiteral(model, subject, RDFConstants.DCT_DESCRIPTION, entity.getDescription());
        if (entity.getUri() != null && !entity.getUri().isEmpty()) {
            RDFHelper.addURILiteral(model, subject, RDFConstants.FOAF_PAGE, entity.getUri());
        }
        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCT_NS + "Standard";
    }
}
