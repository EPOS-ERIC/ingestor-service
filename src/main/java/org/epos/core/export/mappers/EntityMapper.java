package org.epos.core.export.mappers;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.util.Map;

/**
 * Interface for mapping EPOS Data Model entities to RDF.
 * Each entity type has its own mapper implementation.
 *
 * @param <T> The EPOS Data Model entity type
 */
public interface EntityMapper<T extends EPOSDataModelEntity> {

    /**
     * Maps an entity to RDF triples in the model.
     *
     * @param entity    The entity to map
     * @param model     The RDF model to add triples to
     * @param entityMap Map of all entities (UID -> Entity) for resolving references
     * @param resourceCache Cache of processed resources (UID -> Resource) to avoid duplicates
     * @return The RDF Resource representing this entity
     */
    Resource mapToRDF(T entity, Model model, Map<String, EPOSDataModelEntity> entityMap, Map<String, Resource> resourceCache);

    /**
     * Returns the DCAT-AP class URI for this entity type.
     *
     * @return The class URI as a string
     */
    String getDCATClassURI();
}
