package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.core.export.util.RDFConstants;
import org.epos.core.export.util.RDFHelper;
import org.epos.eposdatamodel.Location;

import java.util.Map;

/**
 * Mapper for Location entities to DCT Location.
 */
public class LocationMapper implements EntityMapper<Location> {

    @Override
    public Resource mapToRDF(Location entity, Model model, Map<String, org.epos.eposdatamodel.EPOSDataModelEntity> entityMap) {
        // Create resource
        Resource subject = model.createResource(entity.getUid());

        // Add type
        RDFHelper.addType(model, subject, RDFConstants.DCT_LOCATION);

        // Add geometry
        if (entity.getLocation() != null) {
            RDFHelper.addLiteral(model, subject, RDFConstants.LOCN_GEOMETRY, entity.getLocation());
        }

        return subject;
    }

    @Override
    public String getDCATClassURI() {
        return RDFConstants.DCT_NS + "Location";
    }
}