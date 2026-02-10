package org.epos.api;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * OAI-PMH 2.0 Protocol Interface.
 * Implements the Open Archives Initiative Protocol for Metadata Harvesting.
 * 
 * This endpoint exposes all entity types from the EPOS triplestore, including:
 * - Datasets (dcat:Dataset)
 * - Distributions (dcat:Distribution)
 * - Organizations (schema:Organization)
 * - Persons (schema:Person)
 * - Web Services (epos:WebService)
 * - Equipment (epos:Equipment)
 * - Facilities (epos:Facility)
 * - Categories (skos:Concept, skos:ConceptScheme)
 * - And more...
 * 
 * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html">OAI-PMH 2.0 Specification</a>
 * @see <a href="https://github.com/epos-eu/EPOS-DCAT-AP">EPOS-DCAT-AP Specification</a>
 */
@Validated
public interface OaiPmhApi {

	@Operation(
		summary = "OAI-PMH endpoint",
		description = "Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) 2.0 endpoint. " +
			"Supports all six OAI-PMH verbs: Identify, ListMetadataFormats, ListSets, ListIdentifiers, ListRecords, and GetRecord. " +
			"Exposes all entity types from the EPOS triplestore with support for selective harvesting by entity type or category. " +
			"Supported metadata formats: oai_dc (Dublin Core), dcat (DCAT vocabulary), epos_dcat_ap (full EPOS-DCAT-AP RDF).",
		tags = { "OAI-PMH" }
	)
	@ApiResponses(value = {
		@ApiResponse(
			responseCode = "200",
			description = "OAI-PMH XML response",
			content = @Content(mediaType = "text/xml", schema = @Schema(type = "string"))
		),
		@ApiResponse(responseCode = "400", description = "Bad request - invalid verb or parameters"),
		@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	@RequestMapping(value = "/oai", produces = { "text/xml" }, method = RequestMethod.GET)
	ResponseEntity<String> handleOaiRequest(
		@Parameter(
			in = ParameterIn.QUERY,
			description = "OAI-PMH verb. Required. One of: Identify, ListMetadataFormats, ListSets, ListIdentifiers, ListRecords, GetRecord",
			required = true,
			schema = @Schema(allowableValues = {"Identify", "ListMetadataFormats", "ListSets", "ListIdentifiers", "ListRecords", "GetRecord"})
		)
		@RequestParam(value = "verb", required = true) String verb,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Unique identifier of a record. Required for GetRecord verb.",
			required = false
		)
		@RequestParam(value = "identifier", required = false) String identifier,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Metadata format prefix. Required for GetRecord, ListIdentifiers, and ListRecords verbs. " +
				"Supported formats: 'oai_dc' (Dublin Core), 'dcat' (DCAT vocabulary), 'epos_dcat_ap' (full EPOS-DCAT-AP RDF).",
			required = false,
			schema = @Schema(allowableValues = {"oai_dc", "dcat", "epos_dcat_ap"})
		)
		@RequestParam(value = "metadataPrefix", required = false) String metadataPrefix,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Set specification for selective harvesting. Use 'type:<EntityType>' for entity types (e.g., 'type:Dataset', 'type:WebService') " +
				"or 'category:<encoded-uri>' for categories. Optional for ListIdentifiers and ListRecords.",
			required = false
		)
		@RequestParam(value = "set", required = false) String set,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Lower bound for datestamp-based selective harvesting (ISO 8601 format). Optional for ListIdentifiers and ListRecords.",
			required = false
		)
		@RequestParam(value = "from", required = false) String from,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Upper bound for datestamp-based selective harvesting (ISO 8601 format). Optional for ListIdentifiers and ListRecords.",
			required = false
		)
		@RequestParam(value = "until", required = false) String until,

		@Parameter(
			in = ParameterIn.QUERY,
			description = "Flow control token for resuming an incomplete list request.",
			required = false
		)
		@RequestParam(value = "resumptionToken", required = false) String resumptionToken
	);
}
