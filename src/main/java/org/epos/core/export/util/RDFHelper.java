package org.epos.core.export.util;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Helper utilities for RDF model manipulation.
 * Provides convenience methods for adding properties while handling null values
 * gracefully.
 */
public class RDFHelper {

    /**
     * Adds a string literal property to a resource.
     * Does nothing if value is null or empty.
     */
    public static void addLiteral(Model model, Resource subject, Property property, String value) {
        if (value != null && !value.isEmpty()) {
            model.add(subject, property, value);
        }
    }

    /**
     * Adds a language-tagged literal property to a resource.
     * Does nothing if value is null or empty.
     */
    public static void addLiteralWithLang(Model model, Resource subject, Property property, String value, String lang) {
        if (value != null && !value.isEmpty()) {
            model.add(subject, property, model.createLiteral(value, lang));
        }
    }

    /**
     * Adds a typed string literal property to a resource.
     * Uses xsd:string datatype. Does nothing if value is null or empty.
     */
    public static void addStringLiteral(Model model, Resource subject, Property property, String value) {
        if (value != null && !value.isEmpty()) {
            model.add(subject, property, model.createTypedLiteral(value, XSDDatatype.XSDstring));
        }
    }

    /**
     * Adds a typed string literal property to a resource.
     * Uses xsd:string datatype. Does nothing if value is null. If the value is empty it is still added
     */
    public static void addStringLiteralEmpty(Model model, Resource subject, Property property, String value) {
        if (value != null) {
            model.add(subject, property, model.createTypedLiteral(value, XSDDatatype.XSDstring));
        }
    }

    /**
     * Adds a typed boolean literal property to a resource.
     * Does nothing if value is null.
     */
    public static void addBooleanLiteral(Model model, Resource subject, Property property, Boolean value) {
        if (value != null) {
            model.add(subject, property, model.createTypedLiteral(value));
        }
    }

    /**
     * Adds a typed date literal property to a resource.
     * Does nothing if value is null.
     */
    public static void addDateLiteral(Model model, Resource subject, Property property, String value) {
        if (value != null && !value.isEmpty()) {
            model.add(subject, property, model.createTypedLiteral(value, XSDDatatype.XSDdate));
        }
    }

    /**
     * Adds a typed int literal property to a resource.
     * Does nothing if value is null.
     */
    public static void addIntLiteral(Model model, Resource subject, Property property, Integer value) {
        if (value != null) {
            model.add(subject, property, model.createTypedLiteral(value));
        }
    }

    /**
     * Adds a typed float literal property to a resource.
     * Does nothing if value is null.
     */
    public static void addFloatLiteral(Model model, Resource subject, Property property, Float value) {
        if (value != null) {
            model.add(subject, property, model.createTypedLiteral(value));
        }
    }

    /**
     * Adds a typed literal property to a resource.
     * Does nothing if value is null.
     */
    public static void addTypedLiteral(Model model, Resource subject, Property property, Object value,
            RDFDatatype datatype) {
        if (value != null) {
            model.add(subject, property, model.createTypedLiteral(value, datatype));
        }
    }

    /**
     * Adds a URI property to a resource.
     * Does nothing if uri is null or empty.
     */
    public static void addURI(Model model, Resource subject, Property property, String uri) {
        if (uri != null && !uri.isEmpty()) {
            model.add(subject, property, model.createResource(uri));
        }
    }

    /**
     * Adds an anyURI typed literal property to a resource.
     * Does nothing if uri is null or empty.
     */
    public static void addURILiteral(Model model, Resource subject, Property property, String uri) {
        if (uri != null && !uri.isEmpty()) {
            model.add(subject, property, model.createTypedLiteral(uri, XSDDatatype.XSDanyURI));
        }
    }

    /**
     * Adds a resource property to a resource.
     * Does nothing if value is null.
     */
    public static void addResource(Model model, Resource subject, Property property, Resource value) {
        if (value != null) {
            model.add(subject, property, value);
        }
    }

    /**
     * Creates a blank node (anonymous resource).
     */
    public static Resource createBlankNode(Model model) {
        return model.createResource();
    }

    /**
     * Adds a type to a resource.
     */
    public static void addType(Model model, Resource subject, Resource type) {
        model.add(subject, RDFConstants.RDF_TYPE, type);
    }

    private RDFHelper() {
        // Utility class, no instantiation
    }
}
