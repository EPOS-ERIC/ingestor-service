package org.epos.api;

import java.util.List;

import org.epos.core.MetadataExporter;
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

	@RequestMapping(value = "/export", produces = { "text/turtle" }, method = RequestMethod.GET)
	public ResponseEntity<String> metadataExport(
			@Parameter(in = ParameterIn.QUERY, description = "entity type to export", required = true, schema = @Schema()) @RequestParam(value = "entityType", required = true) EntityNames entityType,
			@Parameter(in = ParameterIn.QUERY, description = "metadata mapping model", required = true, schema = @Schema()) @RequestParam(value = "mapping", required = true) String mapping,
			@Parameter(in = ParameterIn.QUERY, description = "specific entity IDs to export (optional)", required = false, schema = @Schema()) @RequestParam(value = "ids", required = false) List<String> ids) {

		// Validation
		if (entityType == null) {
			return ResponseEntity.badRequest()
					.contentType(MediaType.TEXT_PLAIN)
					.body("Parameter 'entityType' cannot be empty");
		}

		if (mapping == null || mapping.trim().isEmpty()) {
			return ResponseEntity.badRequest()
					.contentType(MediaType.TEXT_PLAIN)
					.body("Parameter 'mapping' cannot be empty");
		}

		try {
			LOGGER.info("[Export initialized] Exporting {} entities using mapping '{}', IDs: {}",
					entityType, mapping, ids != null ? ids : "all");
			String turtleContent = MetadataExporter.exportToTurtle(entityType, mapping, ids);
			LOGGER.info("[Export finished] Successfully exported {} entities to {} characters of Turtle content",
					entityType, turtleContent != null ? turtleContent.length() : 0);

			if (turtleContent == null || turtleContent.trim().isEmpty()) {
				LOGGER.warn("[Export result] No content generated for export request");
				return ResponseEntity.noContent().build();
			}

			return ResponseEntity.ok()
					.contentType(MediaType.parseMediaType("text/turtle; charset=utf-8"))
					.body(turtleContent);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("[VALIDATION ERROR] Export failed for entity type {}: {}", entityType, e.getLocalizedMessage());
			return ResponseEntity.badRequest()
					.contentType(MediaType.TEXT_PLAIN)
					.body(("Validation error: " + e.getLocalizedMessage()));
		} catch (Exception e) {
			LOGGER.error("[ERROR] Export failed for entity type {}: {}", entityType, e.getLocalizedMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.contentType(MediaType.TEXT_PLAIN)
					.body(("Export failed: " + e.getLocalizedMessage()));
		}
	}
}
