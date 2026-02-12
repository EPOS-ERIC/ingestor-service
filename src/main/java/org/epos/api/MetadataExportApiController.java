package org.epos.api;

import java.util.List;

import org.epos.core.export.EPOSVersion;
import org.epos.core.export.MetadataExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import metadataapis.EntityNames;

@RestController
public class MetadataExportApiController implements MetadataExportApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataExportApiController.class);

	public MetadataExportApiController() {
	}

	@RequestMapping(value = "/export", produces = { "text/turtle", "application/ld+json" }, method = RequestMethod.GET)
	public ResponseEntity<String> metadataExport(
			@Parameter(in = ParameterIn.QUERY, description = "entity type to export (optional - if not provided, exports all published entity types)", required = false, schema = @Schema()) @RequestParam(value = "entityType", required = false) EntityNames entityType,
			@Parameter(in = ParameterIn.QUERY, description = "output format (optional, default: turtle)", required = false, schema = @Schema(allowableValues = {
					"turtle",
					"json-ld" })) @RequestParam(value = "format", required = false, defaultValue = "turtle") String format,
			@Parameter(in = ParameterIn.QUERY, description = "specific entity UIDs to export (optional, only published entities are included)", required = false, schema = @Schema()) @RequestParam(value = "ids", required = false) List<String> ids,
			@Parameter(in = ParameterIn.QUERY, description = "EPOS-DCAT-AP version (optional, default: V1)", required = false, schema = @Schema()) @RequestParam(value = "version", required = false, defaultValue = "V1") EPOSVersion version) {

		// Validation
		if (format != null && !format.matches("(?i)(turtle|json-ld)")) {
			return ResponseEntity.badRequest()
					.contentType(MediaType.TEXT_PLAIN)
					.body("Parameter 'format' must be one of: turtle, json-ld");
		}

		try {
			LOGGER.info(
					"[Export initialized] Exporting {} entities in format: {}, version: {}, IDs: {}",
					entityType != null ? entityType : "all types", format, version, ids != null ? ids : "all");
			String rdfOutput = MetadataExporter.exportToRDF(entityType, format, ids, version);
			LOGGER.info("[Export finished] Successfully exported entities to {} characters of {} content",
					rdfOutput != null ? rdfOutput.length() : 0, format);

			if (rdfOutput == null || rdfOutput.trim().isEmpty()) {
				if (ids != null && !ids.isEmpty()) {
					LOGGER.warn("[Export result] No entities found for requested IDs: {}", ids);
					return ResponseEntity.notFound().build();
				} else {
					LOGGER.warn("[Export result] No content generated for export request");
					return ResponseEntity.noContent().build();
				}
			}

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType(getContentTypeForFormat(format)))
					.body(rdfOutput);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("[VALIDATION ERROR] Export failed for entity type {}: {}",
					entityType != null ? entityType : "all types", e.getLocalizedMessage());
			return ResponseEntity.badRequest()
					.contentType(MediaType.TEXT_PLAIN)
					.body("Validation error: " + e.getLocalizedMessage());
		} catch (Exception e) {
			LOGGER.error("[ERROR] Export failed for entity type {}: {}", entityType != null ? entityType : "all types",
					e.getLocalizedMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.contentType(MediaType.TEXT_PLAIN)
					.body("Export failed: " + e.getLocalizedMessage());
		}
	}

	private String getContentTypeForFormat(String format) {
		String lowerFormat = format.toLowerCase();
		if ("json-ld".equals(lowerFormat)) {
			return "application/ld+json; charset=utf-8";
		} else {
			return "text/turtle; charset=utf-8";
		}
	}
}
