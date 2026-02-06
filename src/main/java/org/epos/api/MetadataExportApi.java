package org.epos.api;

import java.util.List;

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
import metadataapis.EntityNames;
import org.epos.core.export.EPOSVersion;

@Validated
public interface MetadataExportApi {

	@Operation(summary = "metadata export operation", description = "Export EPOS metadata from the relational database into EPOS-DCAT-AP RDF. When entityType is provided, the export starts from the matching entities (optionally filtered by ids) and includes only the explicitly referenced entities reachable through outgoing relationships. When entityType is not provided, the export returns all entities. Output can be Turtle or JSON-LD and is generated from the same mapping used by the SPARQL in-memory dataset.", tags = {
			"Metadata Management Service" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "text/turtle", schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "Bad request."),
			@ApiResponse(responseCode = "404", description = "Not Found")
	})
	@RequestMapping(value = "/export", produces = { "text/turtle", "application/ld+json" }, method = RequestMethod.GET)
	ResponseEntity<String> metadataExport(
			@Parameter(in = ParameterIn.QUERY, description = "Entity type to export. Required when ids is provided. If omitted, all entity types are exported.", required = false, schema = @Schema()) @RequestParam(value = "entityType", required = false) EntityNames entityType,
			@Parameter(in = ParameterIn.QUERY, description = "Output format. Defaults to turtle.", required = false, schema = @Schema(allowableValues = {"turtle", "json-ld"})) @RequestParam(value = "format", required = false, defaultValue = "turtle") String format,
			@Parameter(in = ParameterIn.QUERY, description = "Specific entity IDs to export. When provided, only entities explicitly reachable from these roots are included.", required = false, schema = @Schema()) @RequestParam(value = "ids", required = false) List<String> ids,
			@Parameter(in = ParameterIn.QUERY, description = "EPOS-DCAT-AP version. Defaults to V1.", required = false, schema = @Schema()) @RequestParam(value = "version", required = false, defaultValue = "V1") EPOSVersion version);
}
