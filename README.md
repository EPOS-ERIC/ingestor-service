# EPOS Ingestor Service

## Introduction

The **Ingestor Service** is a core component of the EPOS (European Plate Observing System) ICS-C infrastructure. It provides comprehensive metadata management capabilities including ingestion, export, SPARQL querying, and OAI-PMH harvesting for the EPOS Metadata Catalogue.

Built with Spring Boot 3.5 and Java 21, the service acts as a bridge between RDF-based metadata (EPOS-DCAT-AP) and the relational EPOS Data Model, enabling seamless interoperability with external systems and data harvesters.

## Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [API Reference](#api-reference)
  - [Metadata Ingestion](#metadata-ingestion)
  - [Metadata Export](#metadata-export)
  - [SPARQL Endpoint](#sparql-endpoint)
  - [OAI-PMH Endpoint](#oai-pmh-endpoint)
  - [Ontology Management](#ontology-management)
  - [Cache Management](#cache-management)
- [Configuration](#configuration)
- [Deployment](#deployment)
- [Entity Types](#entity-types)
- [License](#license)

## Features

- **Metadata Ingestion**: Parse and ingest TTL (Turtle) files conforming to EPOS-DCAT-AP into the relational database
- **Metadata Export**: Export entities to RDF format (Turtle or JSON-LD) supporting EPOS-DCAT-AP v1 and v3
- **SPARQL Endpoint**: In-memory SPARQL query service powered by Apache Jena Fuseki
- **OAI-PMH 2.0**: Full protocol implementation for metadata harvesting interoperability
- **Ontology Management**: Store and manage base and mapping ontologies
- **OpenAPI Documentation**: Interactive Swagger UI for API exploration
- **Health Monitoring**: Spring Actuator endpoints for liveness and readiness probes

## Architecture

```
                                    +-------------------+
                                    |   External TTL    |
                                    |   Files (URLs)    |
                                    +--------+----------+
                                             |
                                             v
+------------------+              +----------+----------+
|                  |   Ingest    |                      |
|   EPOS-DCAT-AP   +------------>+   Ingestor Service   |
|   (RDF/Turtle)   |             |                      |
|                  |<------------+   - Ingestion        |
+------------------+   Export    |   - Export           |
                                 |   - SPARQL           |
+------------------+             |   - OAI-PMH          |
|                  |             |                      |
|   OAI Harvesters +------------>+                      |
|                  |   Harvest   +----------+-----------+
+------------------+                        |
                                            v
                                 +----------+----------+
                                 |                     |
                                 |   PostgreSQL DB     |
                                 |   (EPOS Data Model) |
                                 |                     |
                                 +---------------------+
```

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven 3.8+
- PostgreSQL database with EPOS Data Model schema
- Docker (optional, for containerized deployment)

### Building from Source

```bash
# Clone the repository
git clone <repository-url>
cd ingestor-service

# Build the project
mvn clean package -DskipTests

# Run the application
java -jar target/ingestor-service-*.jar
```

### Running with Docker

```bash
# Build the Docker image
docker build -t ingestor-service .

# Run the container
docker run -p 8080:8080 \
  -e POSTGRESQL_HOST=your-db-host \
  -e POSTGRESQL_DBNAME=cerif \
  -e POSTGRESQL_USERNAME=postgres \
  -e POSTGRESQL_PASSWORD=your-password \
  ingestor-service
```

### Accessing the Service

Once running, the service is available at:

- **Base URL**: `http://localhost:8080/api/ingestor-service/v1`
- **Swagger UI**: `http://localhost:8080/api/ingestor-service/v1/swagger-ui`
- **OpenAPI Spec**: `http://localhost:8080/api/ingestor-service/v1/api-docs`
- **Health Check**: `http://localhost:8080/api/ingestor-service/v1/actuator/health`

## API Reference

### Metadata Ingestion

Ingest EPOS-DCAT-AP metadata from TTL files into the database.

#### Endpoint

```
POST /populate
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | Query | Yes | Population type: `single` or `multiple` |
| `path` | Query | No | URL to the TTL file (alternative to request body) |
| `model` | Query | Yes | Metadata model name |
| `mapping` | Query | Yes | Mapping model name for transformation |
| `metadataGroup` | Query | No | Target group for resources (default: `ALL`, use `*` for all groups) |
| `status` | Query | No | Status for ingested entities (default: `PUBLISHED`) |
| `editorId` | Query | No | Editor identifier (default: `ingestor`) |
| Request Body | TTL | No | Raw Turtle content (alternative to path) |

#### Example

```bash
# Ingest from URL
curl -X POST "http://localhost:8080/api/ingestor-service/v1/populate?type=single&path=https://example.org/metadata.ttl&model=epos&mapping=epos-mapping"

# Ingest from request body
curl -X POST "http://localhost:8080/api/ingestor-service/v1/populate?type=single&model=epos&mapping=epos-mapping" \
  -H "Content-Type: text/turtle" \
  -d "@metadata.ttl"
```

#### Response

```json
{
  "status": "SUCCESS",
  "message": "Ingestion completed successfully",
  "ingestedPath": "https://example.org/metadata.ttl",
  "ingestedEntities": {
    "https://example.org/dataset/001": {
      "entityType": "DATAPRODUCT",
      "instanceId": "uuid-here",
      "metaId": "meta-uuid",
      "uid": "https://example.org/dataset/001"
    }
  }
}
```

---

### Metadata Export

Export EPOS Data Model entities to RDF format (EPOS-DCAT-AP).

#### Endpoint

```
GET /export
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `entityType` | Query | No | Entity type to export (e.g., `DATAPRODUCT`, `DISTRIBUTION`) |
| `format` | Query | No | Output format: `turtle` (default) or `json-ld` |
| `ids` | Query | No | Specific entity IDs to export (requires `entityType`) |
| `version` | Query | No | EPOS-DCAT-AP version: `V1` (default) or `V3` |

#### Example

```bash
# Export all entities as Turtle
curl "http://localhost:8080/api/ingestor-service/v1/export"

# Export specific DataProducts as JSON-LD
curl "http://localhost:8080/api/ingestor-service/v1/export?entityType=DATAPRODUCT&format=json-ld&version=V3"

# Export specific entities by ID
curl "http://localhost:8080/api/ingestor-service/v1/export?entityType=DATAPRODUCT&ids=https://example.org/dataset/001,https://example.org/dataset/002"
```

---

### SPARQL Endpoint

Execute SPARQL queries against the in-memory RDF dataset.

#### Endpoint

```
POST /api/sparql
```

#### Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| Request Body | SPARQL | Yes | SPARQL query string |
| `version` | Query | No | Dataset version: `V1` only (`V3` returns `400`) |

#### Supported Query Types

- **SELECT**: Returns JSON results
- **ASK**: Returns boolean result
- **CONSTRUCT**: Returns JSON-LD model

#### Example

```bash
# SELECT query
curl -X POST "http://localhost:8080/api/ingestor-service/v1/api/sparql" \
  -H "Content-Type: application/sparql-query" \
  -d "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 10"

# ASK query
curl -X POST "http://localhost:8080/api/ingestor-service/v1/api/sparql" \
  -H "Content-Type: application/sparql-query" \
  -d "ASK { ?s a <http://www.w3.org/ns/dcat#Dataset> }"

# CONSTRUCT query
curl -X POST "http://localhost:8080/api/ingestor-service/v1/api/sparql" \
  -H "Content-Type: application/sparql-query" \
  -d "CONSTRUCT { ?s ?p ?o } WHERE { ?s a <http://www.w3.org/ns/dcat#Dataset> . ?s ?p ?o }"
```

---

### OAI-PMH Endpoint

The Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) 2.0 endpoint enables external harvesters to collect structured metadata from the EPOS repository. The implementation uses the in-memory triplestore (SPARQL service) to expose **all entity types** with support for full EPOS-DCAT-AP RDF metadata.

#### Endpoint

```
GET /oai
```

#### Supported Verbs

| Verb | Description | Required Parameters |
|------|-------------|---------------------|
| `Identify` | Returns repository information | None |
| `ListMetadataFormats` | Lists available metadata formats | `identifier` (optional) |
| `ListSets` | Lists available sets (entity types and categories) | None |
| `ListIdentifiers` | Returns record headers only | `metadataPrefix` |
| `ListRecords` | Returns complete records with metadata | `metadataPrefix` |
| `GetRecord` | Returns a single record | `identifier`, `metadataPrefix` |

#### Supported Metadata Formats

| Prefix | Description | Use Case |
|--------|-------------|----------|
| `oai_dc` | Dublin Core (unqualified) | Basic interoperability, simple harvesters |
| `dcat` | DCAT vocabulary | Data catalog applications |
| `epos_dcat_ap` | Full EPOS-DCAT-AP RDF | Complete metadata with all relationships |

#### Harvestable Entity Types

The OAI-PMH endpoint exposes all entity types from the triplestore:

| Entity Type | RDF Class | Set Specification |
|-------------|-----------|-------------------|
| Dataset | dcat:Dataset | `type:Dataset` |
| Distribution | dcat:Distribution | `type:Distribution` |
| Organization | schema:Organization | `type:Organization` |
| Person | schema:Person | `type:Person` |
| Web Service | epos:WebService | `type:WebService` |
| Equipment | epos:Equipment | `type:Equipment` |
| Facility | epos:Facility | `type:Facility` |
| Operation | hydra:Operation | `type:Operation` |
| Category | skos:Concept | `type:Concept` |
| Category Scheme | skos:ConceptScheme | `type:ConceptScheme` |
| Contact Point | schema:ContactPoint | `type:ContactPoint` |
| Software | schema:SoftwareApplication | `type:SoftwareApplication` |

#### Hierarchical Sets

Sets are organized in two hierarchies:

1. **Entity Type Sets** (`type:<EntityType>`): Filter by RDF class
   - `type:Dataset` - All datasets
   - `type:Organization` - All organizations
   - `type:WebService` - All web services

2. **Category Sets** (`category:<encoded-uri>`): Filter by thematic category
   - Records linked via `dcat:theme` property
   - Category URI is Base64-encoded in the set specification

#### Optional Filtering Parameters

| Parameter | Description |
|-----------|-------------|
| `set` | Filter by entity type (`type:Dataset`) or category (`category:<encoded-uri>`) |
| `from` | Lower bound for datestamp-based selective harvesting (ISO 8601) |
| `until` | Upper bound for datestamp-based selective harvesting (ISO 8601) |
| `resumptionToken` | Token for resuming an incomplete list request |

#### Examples

```bash
# Get repository information
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=Identify"

# List supported metadata formats
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListMetadataFormats"

# List available sets (entity types and categories)
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListSets"

# List all record identifiers
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListIdentifiers&metadataPrefix=oai_dc"

# List all records in Dublin Core format
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=oai_dc"

# List only Dataset records
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=oai_dc&set=type:Dataset"

# List only WebService records
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=dcat&set=type:WebService"

# Filter records by date range
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=ListRecords&metadataPrefix=oai_dc&from=2024-01-01&until=2024-12-31"

# Get a specific record in Dublin Core format
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&identifier=https://example.org/dataset/001&metadataPrefix=oai_dc"

# Get a record in DCAT format
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&identifier=https://example.org/dataset/001&metadataPrefix=dcat"

# Get a record with full EPOS-DCAT-AP RDF (all properties and relationships)
curl "http://localhost:8080/api/ingestor-service/v1/oai?verb=GetRecord&identifier=https://example.org/dataset/001&metadataPrefix=epos_dcat_ap"
```

#### Dublin Core Mapping

The `oai_dc` format maps RDF properties to Dublin Core elements based on entity type:

| Dublin Core Element | RDF Properties |
|---------------------|----------------|
| `dc:title` | dct:title, schema:name, rdfs:label |
| `dc:creator` | dct:creator, schema:provider, schema:manufacturer |
| `dc:subject` | dcat:theme, dcat:keyword, schema:keywords |
| `dc:description` | dct:description, schema:description |
| `dc:publisher` | dct:publisher |
| `dc:date` | dct:issued, dct:modified, dct:created, schema:datePublished |
| `dc:type` | Derived from rdf:type (e.g., "Dataset", "Organization") |
| `dc:identifier` | Resource URI, dct:identifier, schema:identifier |
| `dc:rights` | dct:rights, dct:accessRights, dct:license |
| `dc:format` | dct:format |
| `dc:language` | dct:language |

#### EPOS-DCAT-AP Format

The `epos_dcat_ap` metadata format returns the complete RDF subgraph for each record, including:
- All direct properties of the resource
- Nested blank nodes (e.g., Location, PeriodOfTime)
- Full EPOS-DCAT-AP vocabulary compliance

This format is ideal for harvesters that need complete metadata with all relationships preserved.

#### Architecture

The OAI-PMH implementation leverages the existing in-memory triplestore:

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  OAI Harvester  │────▶│  OAI-PMH Service │────▶│  SPARQL Service │
│                 │◀────│                  │◀────│  (Triplestore)  │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                                          │
                                                          ▼
                                                 ┌─────────────────┐
                                                 │  RDF Dataset    │
                                                 │     (V1)        │
                                                 └─────────────────┘
```

The triplestore is refreshed hourly (configurable via `sparql.refresh.rate`), ensuring OAI-PMH responses reflect recent changes.

---

### Ontology Management

Manage base and mapping ontologies used for metadata transformation.

#### Endpoints

```
POST /ontology    # Add or update an ontology
GET /ontology     # Retrieve all ontologies
```

#### POST Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `path` | Query | Yes | URL to ontology file |
| `name` | Query | Yes | Ontology name |
| `type` | Query | Yes | Ontology type: `BASE` or `MAPPING` |

#### GET Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `encoded` | Query | No | Return base64 encoded content (default: `true`) |
| `nobody` | Query | No | Return only name/type without content |

#### Example

```bash
# Add an ontology
curl -X POST "http://localhost:8080/api/ingestor-service/v1/ontology?path=https://example.org/ontology.ttl&name=epos-mapping&type=MAPPING"

# Get all ontologies
curl "http://localhost:8080/api/ingestor-service/v1/ontology"
```

---

### Cache Management

Invalidate database caches.

#### Endpoint

```
POST /invalidate
```

#### Example

```bash
curl -X POST "http://localhost:8080/api/ingestor-service/v1/invalidate"
```

---

## Configuration

### Application Properties

Configure the service in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `server.port` | `8080` | HTTP server port |
| `server.servlet.contextPath` | `/api/ingestor-service/v1` | Base context path |
| `springdoc.api-docs.path` | `/api-docs` | OpenAPI documentation path |
| `springdoc.swagger-ui.path` | `/swagger-ui` | Swagger UI path |
| `sparql.refresh.rate` | `3600000` | SPARQL dataset refresh interval (ms) |
| `oaipmh.repository.name` | `EPOS Metadata Repository` | OAI-PMH repository name |
| `oaipmh.admin.email` | `info@epos-eu.org` | OAI-PMH admin email |
| `oaipmh.base.url` | (auto-detected) | Base URL for OAI-PMH responses |
| `management.endpoints.web.exposure.include` | `health,liveness` | Exposed actuator endpoints |

### Environment Variables

| Variable | Description |
|----------|-------------|
| `VERSION` | Application version displayed in Swagger UI |
| `INGESTOR_HASH` | SHA1 hash for security phrase validation |
| `POSTGRESQL_HOST` | Database host |
| `POSTGRESQL_DBNAME` | Database name |
| `POSTGRESQL_USERNAME` | Database username |
| `POSTGRESQL_PASSWORD` | Database password |
| `POSTGRESQL_CONNECTION_STRING` | Full JDBC connection URL (alternative) |

---

## Deployment

### Docker

The service includes a production-ready Dockerfile:

```dockerfile
FROM amazoncorretto:21-alpine
# Non-root user (1001:1001)
# Health check via /actuator/health
# Exposes port 8080
```

Build and run:

```bash
docker build -t ingestor-service .
docker run -p 8080:8080 -e POSTGRESQL_HOST=db ingestor-service
```

### Health Checks

The service exposes health endpoints for container orchestration:

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Combined health status |
| `/actuator/liveness` | Liveness probe for Kubernetes |

### CI/CD

The repository includes:

- **GitLab CI** (`.gitlab-ci.yml`): Build, package, security scanning
- **GitHub Actions** (`.github/workflows/`): CodeQL analysis, OWASP dependency check, multi-arch Docker builds

---

## Entity Types

The service supports the following EPOS Data Model entities:

| Entity | RDF Class | Description |
|--------|-----------|-------------|
| DataProduct | dcat:Dataset | Scientific data products |
| Distribution | dcat:Distribution | Data access endpoints |
| Organization | schema:Organization | Research organizations |
| Person | schema:Person | Researchers and contacts |
| ContactPoint | schema:ContactPoint | Contact information |
| Address | schema:PostalAddress | Physical addresses |
| Category | skos:Concept | Taxonomy categories |
| CategoryScheme | skos:ConceptScheme | Category hierarchies |
| Identifier | adms:Identifier | DOIs, ORCIDs, PICs |
| Operation | hydra:Operation | API operations |
| WebService | epos:WebService | Web service definitions |
| Location | dct:Location | Spatial extents (WKT) |
| PeriodOfTime | dct:PeriodOfTime | Temporal extents |
| Equipment | epos:Equipment | Scientific equipment |
| Facility | epos:Facility | Research facilities |
| SoftwareApplication | schema:SoftwareApplication | Software tools |
| SoftwareSourceCode | schema:SoftwareSourceCode | Source code repositories |
| Attribution | prov:Attribution | Data attribution |
| Documentation | foaf:Document | Related documentation |

---

## RDF Vocabularies

The service uses these RDF vocabularies for export:

- **DCAT** - Data Catalog Vocabulary
- **DCT** - Dublin Core Terms
- **ADMS** - Asset Description Metadata Schema
- **FOAF** - Friend of a Friend
- **Schema.org** - Web schema vocabulary
- **SKOS** - Simple Knowledge Organization System
- **VCard** - Contact information
- **Hydra** - Web API vocabulary
- **PROV** - Provenance ontology
- **DQV** - Data Quality Vocabulary
- **LOCN** - Location vocabulary
- **GeoSPARQL** - Geographic query vocabulary

---

## License

Copyright 2021 EPOS ERIC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
