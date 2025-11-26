package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Location;

import java.util.Map;

/**
 * Mapper for Location entities to dct:Location.
 * Follows EPOS-DCAT-AP v3 specification.
 */
public class LocationMapper implements EntityMapper<Location> {

    @Override
    public Resource mapToRDF(Location entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache) {
        if (resourceCache.containsKey(entity.getUid())) {
            return resourceCache.get(entity.getUid());
        }
        // Create blank node for Location
        Resource subject = RDFHelper.createBlankNode(model);
        resourceCache.put(entity.getUid(), subject);

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCT_LOCATION);

        // dcat:bbox, gsp:wktLiteral, 0..1
        // WKT literal representing the geographic bounding box
        if (entity.getLocation() != null && !entity.getLocation().isEmpty()) {
            RDFHelper.addTypedLiteral(model, subject, RDFConstants.DCAT_BBOX, entity.getLocation(), RDFConstants.GSP_WKT_LITERAL_DATATYPE);
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCT_NS + "Location";
    }
}
