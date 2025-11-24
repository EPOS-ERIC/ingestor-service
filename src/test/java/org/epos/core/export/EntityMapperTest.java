package org.epos.core.export;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.io.StringWriter;
import org.epos.core.export.mappers.*;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for entity mappers.
 * Tests each mapper independently with mock data.
 */
public class EntityMapperTest {

    @Test
    public void testCategoryMapper() {
        // Create test Category entity
        Category category = new Category();
        category.setUid("test-category-uid");
        category.setName("Test Category");
        category.setDescription("Test description");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        CategoryMapper mapper = new CategoryMapper();
        Resource resource = mapper.mapToRDF(category, model, entityMap);

        // Verify
        assertEquals("test-category-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/2004/02/skos/core#Concept")));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel"), model.createTypedLiteral("Test Category", XSDDatatype.XSDstring)));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2004/02/skos/core#definition"), model.createTypedLiteral("Test description", XSDDatatype.XSDstring)));
    }

    @Test
    public void testAddressMapper() {
        // Create test Address entity
        Address address = new Address();
        address.setUid("test-address-uid");
        address.setStreet("Test Street");
        address.setLocality("Test City");
        address.setPostalCode("12345");
        address.setCountry("Test Country");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        AddressMapper mapper = new AddressMapper();
        Resource resource = mapper.mapToRDF(address, model, entityMap);

        // Verify
        assertEquals("test-address-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/2006/vcard/ns#Address")));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2006/vcard/ns#street-address"), "Test Street"));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2006/vcard/ns#locality"), "Test City"));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2006/vcard/ns#postal-code"), "12345"));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2006/vcard/ns#country-name"), "Test Country"));
    }

    @Test
    public void testContactPointMapper() {
        // Create test ContactPoint entity
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setUid("test-contact-uid");
        contactPoint.addEmail("test@example.com");
        contactPoint.addLanguage("en");
        contactPoint.setRole("manager");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        ContactPointMapper mapper = new ContactPointMapper();
        Resource resource = mapper.mapToRDF(contactPoint, model, entityMap);

        // Verify
        assertEquals("test-contact-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://schema.org/ContactPoint")));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/email"), "test@example.com"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/availableLanguage"), "en"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/contactType"), "manager"));
    }

    @Test
    public void testIdentifierMapper() {
        // Create test Identifier entity
        Identifier identifier = new Identifier();
        identifier.setUid("test-identifier-uid");
        identifier.setType("DOI");
        identifier.setIdentifier("10.1234/test");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        IdentifierMapper mapper = new IdentifierMapper();
        Resource resource = mapper.mapToRDF(identifier, model, entityMap);

        // Verify
        assertEquals("test-identifier-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/ns/adms#Identifier")));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/ns/adms#schemeAgency"), "DOI"));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/2004/02/skos/core#notation"), "10.1234/test"));
    }

    @Test
    public void testOrganizationMapper() {
        // Create test Organization entity
        Organization organization = new Organization();
        organization.setUid("test-org-uid");
        organization.addLegalName("Test Organization");
        organization.addEmail("info@test.org");

        // Create test Address
        Address address = new Address();
        address.setUid("test-address-uid");
        address.setStreet("Test Street");

        // Create LinkedEntity for address
        LinkedEntity addressLink = new LinkedEntity();
        addressLink.setUid("test-address-uid");
        addressLink.setEntityType("ADDRESS");
        organization.setAddress(addressLink);

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();
        entityMap.put("test-address-uid", address);

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        OrganizationMapper mapper = new OrganizationMapper();
        Resource resource = mapper.mapToRDF(organization, model, entityMap);

        // Verify
        assertEquals("test-org-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://xmlns.com/foaf/0.1/Agent")));
        assertTrue(model.contains(resource, model.createProperty("http://xmlns.com/foaf/0.1/name"), "Test Organization"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/email"), "info@test.org"));
        // Address should be linked
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/address")));
    }

    @Test
    public void testDataProductMapper() {
        // Create test DataProduct entity
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("test-dataproduct-uid");
        dataProduct.addTitle("Test Dataset");
        dataProduct.addDescription("Test description");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        DataProductMapper mapper = new DataProductMapper();
        Resource resource = mapper.mapToRDF(dataProduct, model, entityMap);

        // Verify
        assertEquals("test-dataproduct-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/ns/dcat#Dataset")));
        assertTrue(model.contains(resource, model.createProperty("http://purl.org/dc/terms/identifier"), "test-dataproduct-uid"));
        assertTrue(model.contains(resource, model.createProperty("http://purl.org/dc/terms/title"), "Test Dataset"));
        assertTrue(model.contains(resource, model.createProperty("http://purl.org/dc/terms/description"), "Test description"));
    }

    @Test
    public void testCategoryMapperWithNullValues() {
        // Create test Category entity with null values
        Category category = new Category();
        category.setUid("test-category-uid");
        category.setName(null); // null name
        category.setDescription(""); // empty description

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        CategoryMapper mapper = new CategoryMapper();
        Resource resource = mapper.mapToRDF(category, model, entityMap);

        // Verify
        assertEquals("test-category-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/2004/02/skos/core#Concept")));
        // Should not contain null or empty properties
        assertFalse(model.contains(resource, model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel")));
        assertFalse(model.contains(resource, model.createProperty("http://www.w3.org/2004/02/skos/core#definition")));
    }

    @Test
    public void testContactPointMapperWithLists() {
        // Create test ContactPoint entity with multiple emails and languages
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setUid("test-contact-uid");
        contactPoint.addEmail("test1@example.com");
        contactPoint.addEmail("test2@example.com");
        contactPoint.addLanguage("en");
        contactPoint.addLanguage("fr");
        contactPoint.setRole("manager");

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        ContactPointMapper mapper = new ContactPointMapper();
        Resource resource = mapper.mapToRDF(contactPoint, model, entityMap);

        // Verify
        assertEquals("test-contact-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://schema.org/ContactPoint")));
        // Should contain both emails
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/email"), "test1@example.com"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/email"), "test2@example.com"));
        // Should contain both languages
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/availableLanguage"), "en"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/availableLanguage"), "fr"));
        assertTrue(model.contains(resource, model.createProperty("http://schema.org/contactType"), "manager"));
    }

    @Test
    public void testOperationMapper() {
        // Create test Operation entity
        Operation operation = new Operation();
        operation.setUid("test-operation-uid");
        operation.setMethod("GET");
        operation.addReturns("application/gml+xml");

        // Create test IriTemplate
        IriTemplate iriTemplate = new IriTemplate();
        iriTemplate.setUid("test-iritemplate-uid");
        iriTemplate.setTemplate("https://example.com/api{?param1,param2}");

        // Create test Mappings
        Mapping mapping1 = new Mapping();
        mapping1.setUid("test-mapping1-uid");
        mapping1.setVariable("param1");
        mapping1.setProperty("schema:startDate");
        mapping1.setRange("xsd:dateTime");
        mapping1.setLabel("Start Date");
        mapping1.setValuePattern("YYYY-MM-DDThh:mm:ss");
        mapping1.setDefaultValue("2010-01-01T00:00:00");
        mapping1.setRequired("false");
        mapping1.setMultipleValues("false");
        mapping1.setReadOnlyValue("false");

        Mapping mapping2 = new Mapping();
        mapping2.setUid("test-mapping2-uid");
        mapping2.setVariable("param2");
        mapping2.setProperty("schema:endDate");
        mapping2.setRange("xsd:dateTime");
        mapping2.setLabel("End Date");
        mapping2.setRequired("true");
        mapping2.setMultipleValues("false");
        mapping2.setReadOnlyValue("true");
        mapping2.addParamValue("value1");
        mapping2.addParamValue("value2");

        // Link mappings to iriTemplate
        LinkedEntity mapping1Link = new LinkedEntity();
        mapping1Link.setUid("test-mapping1-uid");
        mapping1Link.setEntityType("MAPPING");
        iriTemplate.addMapping(mapping1Link);

        LinkedEntity mapping2Link = new LinkedEntity();
        mapping2Link.setUid("test-mapping2-uid");
        mapping2Link.setEntityType("MAPPING");
        iriTemplate.addMapping(mapping2Link);

        // Set iriTemplate object directly
        operation.setIriTemplateObject(iriTemplate);

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();
        entityMap.put("test-iritemplate-uid", iriTemplate);
        entityMap.put("test-mapping1-uid", mapping1);
        entityMap.put("test-mapping2-uid", mapping2);

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        OperationMapper mapper = new OperationMapper();
        Resource resource = mapper.mapToRDF(operation, model, entityMap);

        // Verify
        assertEquals("test-operation-uid", resource.getURI());
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
                model.createResource("http://www.w3.org/ns/hydra/core#Operation")));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/ns/hydra/core#method"), "GET"));
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/ns/hydra/core#returns"), "application/gml+xml"));

        // Check hydra:property points to IriTemplate
        assertTrue(model.contains(resource, model.createProperty("http://www.w3.org/ns/hydra/core#property")));

        // Find the IriTemplate resource
        Resource iriTemplateResource = model.listResourcesWithProperty(model.createProperty("http://www.w3.org/ns/hydra/core#template"),
                "https://example.com/api{?param1,param2}").nextResource();

        // Verify IriTemplate has mappings
        assertTrue(model.contains(iriTemplateResource, model.createProperty("http://www.w3.org/ns/hydra/core#mapping")));

        // Find mapping resources
        // This is a bit tricky, but we can check for specific triples
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/ns/hydra/core#variable"), model.createTypedLiteral("param1", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/ns/hydra/core#variable"), model.createTypedLiteral("param2", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/2000/01/rdf-schema#range"), model.createTypedLiteral("xsd:dateTime", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), model.createTypedLiteral("Start Date", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://schema.org/valuePattern"), model.createTypedLiteral("YYYY-MM-DDThh:mm:ss", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://schema.org/defaultValue"), model.createTypedLiteral("2010-01-01T00:00:00", XSDDatatype.XSDstring)));
        // Required true for mapping2
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/ns/hydra/core#required"), model.createTypedLiteral(true)));
        // ReadonlyValue true for mapping2
        assertTrue(model.contains(null, model.createProperty("http://schema.org/readonlyValue"), model.createTypedLiteral(true)));
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/2006/http#paramValue"), model.createTypedLiteral("value1", XSDDatatype.XSDstring)));
        assertTrue(model.contains(null, model.createProperty("http://www.w3.org/2006/http#paramValue"), model.createTypedLiteral("value2", XSDDatatype.XSDstring)));
    }

    @Test
    public void testTypedLiteralsInModel() {
        // Create test Operation entity
        Operation operation = new Operation();
        operation.setUid("test-operation-uid");
        operation.setMethod("GET");
        operation.addReturns("application/gml+xml");

        // Create test IriTemplate
        IriTemplate iriTemplate = new IriTemplate();
        iriTemplate.setUid("test-iritemplate-uid");
        iriTemplate.setTemplate("https://example.com/api{?param1}");

        // Create test Mapping
        Mapping mapping = new Mapping();
        mapping.setUid("test-mapping-uid");
        mapping.setVariable("param1");
        mapping.setLabel("Test Parameter");
        mapping.setRequired("true");

        // Link
        LinkedEntity mappingLink = new LinkedEntity();
        mappingLink.setUid("test-mapping-uid");
        mappingLink.setEntityType("MAPPING");
        iriTemplate.addMapping(mappingLink);

        operation.setIriTemplateObject(iriTemplate);

        // Create entity map
        Map<String, EPOSDataModelEntity> entityMap = new HashMap<>();
        entityMap.put("test-mapping-uid", mapping);

        // Create RDF model
        Model model = ModelFactory.createDefaultModel();

        // Create mapper and map
        OperationMapper mapper = new OperationMapper();
        mapper.mapToRDF(operation, model, entityMap);

        // Check that literals have correct datatypes
        // Find the literals
        Statement methodStmt = model.getProperty(model.createResource("test-operation-uid"), model.createProperty("http://www.w3.org/ns/hydra/core#method"));
        assertNotNull(methodStmt);
        assertEquals(XSDDatatype.XSDstring, methodStmt.getLiteral().getDatatype());

        Statement variableStmt = model.listStatements(null, model.createProperty("http://www.w3.org/ns/hydra/core#variable"), (RDFNode) null).nextStatement();
        assertEquals(XSDDatatype.XSDstring, variableStmt.getLiteral().getDatatype());

        Statement labelStmt = model.listStatements(null, model.createProperty("http://www.w3.org/2000/01/rdf-schema#label"), (RDFNode) null).nextStatement();
        assertEquals(XSDDatatype.XSDstring, labelStmt.getLiteral().getDatatype());

        Statement requiredStmt = model.listStatements(null, model.createProperty("http://www.w3.org/ns/hydra/core#required"), (RDFNode) null).nextStatement();
        assertEquals(XSDDatatype.XSDboolean, requiredStmt.getLiteral().getDatatype());
    }
}