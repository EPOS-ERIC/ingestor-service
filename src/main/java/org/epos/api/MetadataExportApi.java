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

@Validated
public interface MetadataExportApi {

	@Operation(summary = "metadata export operation", description = "Export metadata from database to RDF Turtle format.", tags = {
			"Metadata Management Service" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "text/turtle", schema = @Schema(type = "string", format = "binary"))),
			@ApiResponse(responseCode = "400", description = "Bad request."),
			@ApiResponse(responseCode = "404", description = "Not Found")
	})
	@RequestMapping(value = "/export", produces = { "text/turtle", "application/ld+json" }, method = RequestMethod.GET)
	ResponseEntity<String> metadataExport(
			@Parameter(in = ParameterIn.QUERY, description = "entity type to export (optional - if not provided, exports all entity types)", required = false, schema = @Schema()) @RequestParam(value = "entityType", required = false) EntityNames entityType,
			@Parameter(in = ParameterIn.QUERY, description = "metadata mapping model", required = true, schema = @Schema()) @RequestParam(value = "mapping", required = true) String mapping,
			@Parameter(in = ParameterIn.QUERY, description = "output format (optional, default: turtle)", required = false, schema = @Schema(allowableValues = {"turtle", "json-ld"})) @RequestParam(value = "format", required = false, defaultValue = "turtle") String format,
			@Parameter(in = ParameterIn.QUERY, description = "specific entity IDs to export (optional)", required = false, schema = @Schema()) @RequestParam(value = "ids", required = false) List<String> ids,
			@Parameter(in = ParameterIn.QUERY, description = "whether to include transitively linked entities in the export (default: false)", required = false, schema = @Schema()) @RequestParam(value = "includeLinked", required = false, defaultValue = "false") Boolean includeLinked);
}
