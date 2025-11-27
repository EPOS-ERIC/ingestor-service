package org.epos.core.export.util;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Constants for RDF vocabularies and properties used in EPOS-DCAT-AP export.
 * Centralizes all vocabulary URIs to ensure consistency.
 */
public class RDFConstants {

        // Namespace URIs
        public static final String DCAT_NS = "http://www.w3.org/ns/dcat#";
        public static final String DCT_NS = "http://purl.org/dc/terms/";
        public static final String ADMS_NS = "http://www.w3.org/ns/adms#";
        public static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
        public static final String SCHEMA_NS = "http://schema.org/";
        public static final String SKOS_NS = "http://www.w3.org/2004/02/skos/core#";
        public static final String VCARD_NS = "http://www.w3.org/2006/vcard/ns#";
        public static final String HYDRA_NS = "http://www.w3.org/ns/hydra/core#";
        public static final String LOCN_NS = "http://www.w3.org/ns/locn#";
        public static final String OWL_NS = "http://www.w3.org/2002/07/owl#";
        public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        public static final String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";
        public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";
        public static final String GSP_NS = "http://www.opengis.net/ont/geosparql#";
        public static final String OA_NS = "http://www.w3.org/ns/oa#";
        public static final String HTTP_NS = "http://www.w3.org/2006/http#";
        public static final String PROV_NS = "http://www.w3.org/ns/prov#";
        public static final String DQV_NS = "http://www.w3.org/ns/dqv#";
        public static final String EPOS_NS = "https://www.epos-eu.org/epos-dcat-ap#";

        // Classes
        public static final Resource DCAT_DATASET = ResourceFactory.createResource(DCAT_NS + "Dataset");
        public static final Resource DCAT_DISTRIBUTION_CLASS = ResourceFactory.createResource(DCAT_NS + "Distribution");
        public static final Resource DCAT_DATA_SERVICE = ResourceFactory.createResource(DCAT_NS + "DataService");
        public static final Resource DCAT_CONTACT_POINT_CLASS = ResourceFactory.createResource(DCAT_NS + "ContactPoint");
        public static final Resource FOAF_AGENT = ResourceFactory.createResource(FOAF_NS + "Agent");
        public static final Resource SCHEMA_ORGANIZATION = ResourceFactory.createResource(SCHEMA_NS + "Organization");
        public static final Resource SCHEMA_PERSON = ResourceFactory.createResource(SCHEMA_NS + "Person");
        public static final Property SCHEMA_CONTACT_POINT = ResourceFactory.createProperty(SCHEMA_NS, "ContactPoint");
        public static final Resource SCHEMA_DEVICE = ResourceFactory.createResource(SCHEMA_NS + "Device");
        public static final Resource SCHEMA_SOFTWARE_APPLICATION = ResourceFactory.createResource(SCHEMA_NS + "SoftwareApplication");
        public static final Resource SCHEMA_QUANTITATIVE_VALUE = ResourceFactory.createResource(SCHEMA_NS + "QuantitativeValue");
        public static final Resource SCHEMA_SOFTWARE_SOURCE_CODE = ResourceFactory.createResource(SCHEMA_NS + "SoftwareSourceCode");
        public static final Resource SCHEMA_PLACE = ResourceFactory.createResource(SCHEMA_NS + "Place");
        public static final Resource SCHEMA_POSTAL_ADDRESS = ResourceFactory.createResource(SCHEMA_NS + "PostalAddress");
        public static final Resource SCHEMA_PROPERTY_VALUE = ResourceFactory.createResource(SCHEMA_NS + "PropertyValue");
        public static final Resource SKOS_CONCEPT = ResourceFactory.createResource(SKOS_NS + "Concept");
        public static final Resource SKOS_CONCEPT_SCHEME = ResourceFactory.createResource(SKOS_NS + "ConceptScheme");
        public static final Resource ADMS_IDENTIFIER_CLASS = ResourceFactory.createResource(ADMS_NS + "Identifier");
        public static final Resource VCARD_ADDRESS = ResourceFactory.createResource(VCARD_NS + "Address");
        public static final Resource DCT_LOCATION = ResourceFactory.createResource(DCT_NS + "Location");
        public static final Resource DCT_PERIOD_OF_TIME = ResourceFactory.createResource(DCT_NS + "PeriodOfTime");
        public static final Resource DCT_STANDARD = ResourceFactory.createResource(DCT_NS + "Standard");
        public static final Resource HYDRA_OPERATION = ResourceFactory.createResource(HYDRA_NS + "Operation");
        public static final Resource HYDRA_IRI_TEMPLATE = ResourceFactory.createResource(HYDRA_NS + "IriTemplate");
        public static final Resource HYDRA_IRI_TEMPLATE_MAPPING = ResourceFactory.createResource(HYDRA_NS + "IriTemplateMapping");
        public static final Resource HYDRA_API_DOCUMENTATION = ResourceFactory.createResource(HYDRA_NS + "ApiDocumentation");
        public static final Property HYDRA_SUPPORTED_PROPERTY = ResourceFactory.createProperty(HYDRA_NS, "supportedProperty");
        public static final Resource HYDRA_CLASS = ResourceFactory.createResource(HYDRA_NS + "Class");
        public static final Resource OA_ANNOTATION = ResourceFactory.createResource(OA_NS + "Annotation");
        public static final Resource PROV_ATTRIBUTION = ResourceFactory.createResource(PROV_NS + "Attribution");
        public static final Resource RDFS_RESOURCE = ResourceFactory.createResource(RDFS_NS + "Resource");
        public static final Resource EPOS_WEBSERVICE = ResourceFactory.createResource(EPOS_NS + "WebService");
        public static final Resource EPOS_FACILITY = ResourceFactory.createResource(EPOS_NS + "Facility");
        public static final Resource EPOS_EQUIPMENT = ResourceFactory.createResource(EPOS_NS + "Equipment");

        // DCAT Properties
        public static final Property DCAT_THEME = ResourceFactory.createProperty(DCAT_NS, "theme");
        public static final Property DCAT_KEYWORD = ResourceFactory.createProperty(DCAT_NS, "keyword");
        public static final Property DCAT_CONTACT_POINT = ResourceFactory.createProperty(DCAT_NS, "contactPoint");
        public static final Property DCAT_DISTRIBUTION = ResourceFactory.createProperty(DCAT_NS, "distribution");
        public static final Property DCAT_ACCESS_URL = ResourceFactory.createProperty(DCAT_NS, "accessURL");
        public static final Property DCAT_DOWNLOAD_URL = ResourceFactory.createProperty(DCAT_NS, "downloadURL");
        public static final Property DCAT_ENDPOINT_URL = ResourceFactory.createProperty(DCAT_NS, "endpointURL");
        public static final Property DCAT_ACCESS_SERVICE = ResourceFactory.createProperty(DCAT_NS, "accessService");
        public static final Property DCAT_BYTE_SIZE = ResourceFactory.createProperty(DCAT_NS, "byteSize");
        public static final Property DCAT_MEDIA_TYPE = ResourceFactory.createProperty(DCAT_NS, "mediaType");
        public static final Property DCAT_VERSION = ResourceFactory.createProperty(DCAT_NS, "version");
        public static final Property DCAT_BBOX = ResourceFactory.createProperty(DCAT_NS, "bbox");
        public static final Property DCAT_START_DATE = ResourceFactory.createProperty(DCAT_NS, "startDate");
        public static final Property DCAT_END_DATE = ResourceFactory.createProperty(DCAT_NS, "endDate");
        public static final Property DCAT_ENDPOINT_DESCRIPTION = ResourceFactory.createProperty(DCAT_NS,
                        "endpointDescription");

        // Dublin Core Terms Properties
        public static final Property DCT_IDENTIFIER = ResourceFactory.createProperty(DCT_NS, "identifier");
        public static final Property DCT_TITLE = ResourceFactory.createProperty(DCT_NS, "title");
        public static final Property DCT_DESCRIPTION = ResourceFactory.createProperty(DCT_NS, "description");
        public static final Property DCT_CREATED = ResourceFactory.createProperty(DCT_NS, "created");
        public static final Property DCT_ISSUED = ResourceFactory.createProperty(DCT_NS, "issued");
        public static final Property DCT_MODIFIED = ResourceFactory.createProperty(DCT_NS, "modified");
        public static final Property DCT_PUBLISHER = ResourceFactory.createProperty(DCT_NS, "publisher");
        public static final Property DCT_TYPE = ResourceFactory.createProperty(DCT_NS, "type");
        public static final Property DCT_FORMAT = ResourceFactory.createProperty(DCT_NS, "format");
        public static final Property DCT_LICENSE = ResourceFactory.createProperty(DCT_NS, "license");
        public static final Property DCT_SPATIAL = ResourceFactory.createProperty(DCT_NS, "spatial");
        public static final Property DCT_TEMPORAL = ResourceFactory.createProperty(DCT_NS, "temporal");
        public static final Property DCT_ACCRUAL_PERIODICITY = ResourceFactory.createProperty(DCT_NS, "accrualPeriodicity");
        public static final Property DCT_CONFORMS_TO = ResourceFactory.createProperty(DCT_NS, "conformsTo");
        public static final Property DCT_IS_PART_OF = ResourceFactory.createProperty(DCT_NS, "isPartOf");
        public static final Property DCT_RELATION = ResourceFactory.createProperty(DCT_NS, "relation");

        // ADMS Properties
        public static final Property ADMS_IDENTIFIER = ResourceFactory.createProperty(ADMS_NS, "identifier");
        public static final Property ADMS_SCHEME_AGENCY = ResourceFactory.createProperty(ADMS_NS, "schemeAgency");

        // FOAF Properties
        public static final Property FOAF_NAME = ResourceFactory.createProperty(FOAF_NS, "name");
        public static final Property FOAF_PAGE = ResourceFactory.createProperty(FOAF_NS, "page");
        public static final Property FOAF_HOMEPAGE = ResourceFactory.createProperty(FOAF_NS, "homepage");
        public static final Property FOAF_LOGO = ResourceFactory.createProperty(FOAF_NS, "logo");
        public static final Resource FOAF_DOCUMENT = ResourceFactory.createResource(FOAF_NS + "Document");

        // Schema.org Properties
        public static final Property SCHEMA_NAME = ResourceFactory.createProperty(SCHEMA_NS, "name");
        public static final Property SCHEMA_EMAIL = ResourceFactory.createProperty(SCHEMA_NS, "email");
        public static final Property SCHEMA_ADDRESS = ResourceFactory.createProperty(SCHEMA_NS, "address");
        public static final Property SCHEMA_LOGO = ResourceFactory.createProperty(SCHEMA_NS, "logo");
        public static final Property SCHEMA_URL = ResourceFactory.createProperty(SCHEMA_NS, "url");
        public static final Property SCHEMA_CONTACT_TYPE = ResourceFactory.createProperty(SCHEMA_NS, "contactType");
        public static final Property SCHEMA_AVAILABLE_LANGUAGE = ResourceFactory.createProperty(SCHEMA_NS, "availableLanguage");
        public static final Property SCHEMA_START_DATE = ResourceFactory.createProperty(SCHEMA_NS, "startDate");
        public static final Property SCHEMA_END_DATE = ResourceFactory.createProperty(SCHEMA_NS, "endDate");
        public static final Property SCHEMA_DEFAULT_VALUE = ResourceFactory.createProperty(SCHEMA_NS, "defaultValue");
        public static final Property SCHEMA_VALUE_PATTERN = ResourceFactory.createProperty(SCHEMA_NS, "valuePattern");
        public static final Property SCHEMA_MULTIPLE_VALUES = ResourceFactory.createProperty(SCHEMA_NS, "multipleValues");
        public static final Property SCHEMA_READONLY_VALUE = ResourceFactory.createProperty(SCHEMA_NS, "readonlyValue");
        public static final Property SCHEMA_IDENTIFIER = ResourceFactory.createProperty(SCHEMA_NS, "identifier");
        public static final Property SCHEMA_SERIAL_NUMBER = ResourceFactory.createProperty(SCHEMA_NS, "serialNumber");
        public static final Property SCHEMA_MANUFACTURER = ResourceFactory.createProperty(SCHEMA_NS, "manufacturer");
        public static final Property SCHEMA_DOWNLOAD_URL = ResourceFactory.createProperty(SCHEMA_NS, "downloadUrl");
        public static final Property SCHEMA_VALUE = ResourceFactory.createProperty(SCHEMA_NS, "value");
        public static final Property SCHEMA_UNIT_TEXT = ResourceFactory.createProperty(SCHEMA_NS, "unitText");
        public static final Property SCHEMA_MIN_VALUE = ResourceFactory.createProperty(SCHEMA_NS, "minValue");
        public static final Property SCHEMA_MAX_VALUE = ResourceFactory.createProperty(SCHEMA_NS, "maxValue");
        public static final Property SCHEMA_LEGAL_NAME = ResourceFactory.createProperty(SCHEMA_NS, "legalName");
        public static final Property SCHEMA_LEI_CODE = ResourceFactory.createProperty(SCHEMA_NS, "leiCode");
        public static final Property SCHEMA_STREET_ADDRESS = ResourceFactory.createProperty(SCHEMA_NS, "streetAddress");
        public static final Property SCHEMA_ADDRESS_LOCALITY = ResourceFactory.createProperty(SCHEMA_NS, "addressLocality");
        public static final Property SCHEMA_POSTAL_CODE = ResourceFactory.createProperty(SCHEMA_NS, "postalCode");
        public static final Property SCHEMA_ADDRESS_COUNTRY = ResourceFactory.createProperty(SCHEMA_NS, "addressCountry");
        public static final Property SCHEMA_PROPERTY_ID = ResourceFactory.createProperty(SCHEMA_NS, "propertyID");
        public static final Property SCHEMA_OWNS = ResourceFactory.createProperty(SCHEMA_NS, "owns");
        public static final Property SCHEMA_MEMBER_OF = ResourceFactory.createProperty(SCHEMA_NS, "memberOf");
        public static final Property SCHEMA_FAMILY_NAME = ResourceFactory.createProperty(SCHEMA_NS, "familyName");
        public static final Property SCHEMA_GIVEN_NAME = ResourceFactory.createProperty(SCHEMA_NS, "givenName");
        public static final Property SCHEMA_TELEPHONE = ResourceFactory.createProperty(SCHEMA_NS, "telephone");
        public static final Property SCHEMA_QUALIFICATIONS = ResourceFactory.createProperty(SCHEMA_NS, "qualifications");
        public static final Property SCHEMA_AFFILIATION = ResourceFactory.createProperty(SCHEMA_NS, "affiliation");
        public static final Property SCHEMA_KEYWORDS = ResourceFactory.createProperty(SCHEMA_NS, "keywords");
        public static final Property SCHEMA_DATE_PUBLISHED = ResourceFactory.createProperty(SCHEMA_NS, "datePublished");
        public static final Property SCHEMA_DATE_MODIFIED = ResourceFactory.createProperty(SCHEMA_NS, "dateModified");
        public static final Property SCHEMA_PROVIDER = ResourceFactory.createProperty(SCHEMA_NS, "provider");
        public static final Property SCHEMA_DESCRIPTION = ResourceFactory.createProperty(SCHEMA_NS, "description");
        public static final Property SCHEMA_AUTHOR = ResourceFactory.createProperty(SCHEMA_NS, "author");
        public static final Property SCHEMA_CONTRIBUTOR = ResourceFactory.createProperty(SCHEMA_NS, "contributor");
        public static final Property SCHEMA_CREATOR = ResourceFactory.createProperty(SCHEMA_NS, "creator");
        public static final Property SCHEMA_FUNDER = ResourceFactory.createProperty(SCHEMA_NS, "funder");
        public static final Property SCHEMA_MAINTAINER = ResourceFactory.createProperty(SCHEMA_NS, "maintainer");
        public static final Property SCHEMA_PUBLISHER = ResourceFactory.createProperty(SCHEMA_NS, "publisher");

        // SKOS Properties
        public static final Property SKOS_PREF_LABEL = ResourceFactory.createProperty(SKOS_NS, "prefLabel");
        public static final Property SKOS_DEFINITION = ResourceFactory.createProperty(SKOS_NS, "definition");
        public static final Property SKOS_IN_SCHEME = ResourceFactory.createProperty(SKOS_NS, "inScheme");
        public static final Property SKOS_BROADER = ResourceFactory.createProperty(SKOS_NS, "broader");
        public static final Property SKOS_NARROWER = ResourceFactory.createProperty(SKOS_NS, "narrower");
        public static final Property SKOS_NOTATION = ResourceFactory.createProperty(SKOS_NS, "notation");
        public static final Property SKOS_HAS_TOP_CONCEPT = ResourceFactory.createProperty(SKOS_NS, "hasTopConcept");

        // VCard Properties
        public static final Property VCARD_HAS_ADDRESS = ResourceFactory.createProperty(VCARD_NS, "hasAddress");
        public static final Property VCARD_STREET_ADDRESS = ResourceFactory.createProperty(VCARD_NS, "street-address");
        public static final Property VCARD_LOCALITY = ResourceFactory.createProperty(VCARD_NS, "locality");
        public static final Property VCARD_POSTAL_CODE = ResourceFactory.createProperty(VCARD_NS, "postal-code");
        public static final Property VCARD_COUNTRY_NAME = ResourceFactory.createProperty(VCARD_NS, "country-name");

        // Hydra Properties
        public static final Property HYDRA_SUPPORTED_OPERATION = ResourceFactory.createProperty(HYDRA_NS, "supportedOperation");
        public static final Property HYDRA_METHOD = ResourceFactory.createProperty(HYDRA_NS, "method");
        public static final Property HYDRA_RETURNS = ResourceFactory.createProperty(HYDRA_NS, "returns");
        public static final Property HYDRA_PROPERTY = ResourceFactory.createProperty(HYDRA_NS, "property");
        public static final Property HYDRA_TEMPLATE = ResourceFactory.createProperty(HYDRA_NS, "template");
        public static final Property HYDRA_MAPPING = ResourceFactory.createProperty(HYDRA_NS, "mapping");
        public static final Property HYDRA_VARIABLE = ResourceFactory.createProperty(HYDRA_NS, "variable");
        public static final Property HYDRA_REQUIRED = ResourceFactory.createProperty(HYDRA_NS, "required");
        public static final Property HYDRA_TITLE = ResourceFactory.createProperty(HYDRA_NS, "title");
        public static final Property HYDRA_DESCRIPTION = ResourceFactory.createProperty(HYDRA_NS, "description");
        public static final Property HYDRA_ENTRYPOINT = ResourceFactory.createProperty(HYDRA_NS, "entrypoint");
        public static final Property HYDRA_EXPECTS = ResourceFactory.createProperty(HYDRA_NS, "expects");

        // Location Properties
        public static final Property LOCN_GEOMETRY = ResourceFactory.createProperty(LOCN_NS, "geometry");

        // OWL Properties
        public static final Property OWL_VERSION_INFO = ResourceFactory.createProperty(OWL_NS, "versionInfo");

        // RDFS Properties
        public static final Property RDFS_LABEL = ResourceFactory.createProperty(RDFS_NS, "label");
        public static final Property RDFS_RANGE = ResourceFactory.createProperty(RDFS_NS, "range");

        // HTTP Properties
        public static final Property HTTP_PARAM_VALUE = ResourceFactory.createProperty(HTTP_NS, "paramValue");

        // OA Properties
        public static final Property OA_HAS_BODY = ResourceFactory.createProperty(OA_NS, "hasBody");
        public static final Property OA_HAS_TARGET = ResourceFactory.createProperty(OA_NS, "hasTarget");

        // PROV Properties
        public static final Property PROV_AGENT = ResourceFactory.createProperty(PROV_NS, "agent");
        public static final Property PROV_HAD_ROLE = ResourceFactory.createProperty(PROV_NS, "hadRole");

        // DQV Properties
        public static final Property DQV_HAS_QUALITY_ANNOTATION = ResourceFactory.createProperty(DQV_NS, "hasQualityAnnotation");

        // RDF Properties
        public static final Property RDF_TYPE = ResourceFactory.createProperty(RDF_NS, "type");

        // Additional Schema Properties
        public static final Property SCHEMA_RUNTIME_PLATFORM = ResourceFactory.createProperty(SCHEMA_NS, "runtimePlatform");
        public static final Property SCHEMA_SOFTWARE_VERSION = ResourceFactory.createProperty(SCHEMA_NS, "softwareVersion");
        public static final Property SCHEMA_CODE_REPOSITORY = ResourceFactory.createProperty(SCHEMA_NS, "codeRepository");
        public static final Property SCHEMA_PROGRAMMING_LANGUAGE = ResourceFactory.createProperty(SCHEMA_NS, "programmingLanguage");
        public static final Property SCHEMA_MAIN_ENTITY_OF_PAGE = ResourceFactory.createProperty(SCHEMA_NS, "mainEntityOfPage");
        public static final Property SCHEMA_INSTALL_URL = ResourceFactory.createProperty(SCHEMA_NS, "installUrl");
        public static final Property SCHEMA_TARGET_PRODUCT = ResourceFactory.createProperty(SCHEMA_NS, "targetProduct");
        public static final Property SCHEMA_LICENSE = ResourceFactory.createProperty(SCHEMA_NS, "license");
        public static final Property SCHEMA_SOFTWARE_REQUIREMENTS = ResourceFactory.createProperty(SCHEMA_NS, "softwareRequirements");

        // EPOS Properties
        public static final Property EPOS_FILTER = ResourceFactory.createProperty(EPOS_NS, "filter");
        public static final Property EPOS_DYNAMIC_RANGE = ResourceFactory.createProperty(EPOS_NS, "dynamicRange");
        public static final Property EPOS_SAMPLE_PERIOD = ResourceFactory.createProperty(EPOS_NS, "samplePeriod");
        public static final Property EPOS_RESOLUTION = ResourceFactory.createProperty(EPOS_NS, "resolution");
        public static final Property EPOS_ORIENTATION = ResourceFactory.createProperty(EPOS_NS, "orientation");
        public static final Property EPOS_ASSOCIATED_PROJECT = ResourceFactory.createProperty(EPOS_NS, "associatedProject");

        // Additional Schema Properties
        public static final Property SCHEMA_COLOR = ResourceFactory.createProperty(SCHEMA_NS, "color");
        public static final Property SCHEMA_ORDER_ITEM_NUMBER = ResourceFactory.createProperty(SCHEMA_NS, "orderItemNumber");

        // XSD Datatypes
        public static final String XSD_DATE = XSD_NS + "date";

        // GeoSPARQL Datatypes
        public static final String GSP_WKT_LITERAL = GSP_NS + "wktLiteral";
        public static final RDFDatatype GSP_WKT_LITERAL_DATATYPE = TypeMapper.getInstance().getSafeTypeByName(GSP_WKT_LITERAL);

        private RDFConstants() {
                // Utility class, no instantiation
        }
}
