package org.epos.api;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import jakarta.annotation.Generated;
import org.epos.core.MetadataPopulator;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import model.StatusType;
import usermanagementapis.UserGroupManagementAPI;

@Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
public class MetadataPopulationApiController implements MetadataPopulationApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataPopulationApiController.class);

	public MetadataPopulationApiController() {
	}

	private boolean isBodyValid(String body) {
		return body != null && !body.isBlank() && !body.equals("{}");
	}

	public ResponseEntity<IngestionResult> metadataPopulate(
			@Parameter(in = ParameterIn.QUERY, description = "population type (single file or multiple lines file)", required = true, schema = @Schema(allowableValues = {
					"single", "multiple" })) @RequestParam(value = "type", required = true) String type,
			@Parameter(in = ParameterIn.QUERY, description = "path of the file to use", required = false, schema = @Schema()) @RequestParam(value = "path", required = false) String path,
			@Parameter(in = ParameterIn.QUERY, description = "metadata model", required = true, schema = @Schema()) @RequestParam(value = "model", required = true) String model,
			@Parameter(in = ParameterIn.QUERY, description = "metadata mapping model", required = true, schema = @Schema()) @RequestParam(value = "mapping", required = true) String mapping,
			@Parameter(in = ParameterIn.QUERY, description = "metadata group where the resource should be placed", required = false, schema = @Schema()) @RequestParam(value = "metadataGroup", required = false) String metadataGroup,
			@Parameter(in = ParameterIn.QUERY, description = "status to ingest the file as", required = false, schema = @Schema()) @RequestParam(value = "status", required = false, defaultValue = "PUBLISHED") StatusType status,
			@Parameter(in = ParameterIn.QUERY, description = "editor id for the ingested entities", required = false, schema = @Schema()) @RequestParam(value = "editorId", required = false, defaultValue = "ingestor") String editorId,
			@RequestBody(required = false) String body) {

		if (!isBodyValid(body) && (path == null || path.isBlank())) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new IngestionResult("ERROR",
							"Request parameter 'path' cannot be blank and the request body must be valid.", path,
							new HashMap<>()));
		}

		if (metadataGroup == null || metadataGroup.isEmpty()) {
			metadataGroup = "ALL";
		}

		try {
			metadataGroup = URLDecoder.decode(metadataGroup, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("[Error] Error decoding groupname {}", e.getLocalizedMessage());
		}

		List<Group> selectedGroup = new ArrayList<>();

		if (metadataGroup.equals("*")){
			for(Group group : UserGroupManagementAPI.retrieveAllGroups()){
				selectedGroup.add(group);
			}
		}
		else selectedGroup.add(UserGroupManagementAPI.retrieveGroupByName(metadataGroup));
		
		if (selectedGroup.isEmpty()) {
			selectedGroup.add(UserGroupManagementAPI.retrieveGroupByName("ALL"));
		}

		boolean multiline = type.equals("single") ? false : true;
		Map<String, LinkedEntity> finalIngestionResult = new HashMap<>();

		if (multiline) {
			URL urlMultiline = null;
			try {
				urlMultiline = new URL(path);
				Scanner s = new Scanner(urlMultiline.openStream());
				while (s.hasNextLine()) {
					String urlsingle = s.nextLine();
					LOGGER.info("[Ingestion initialized] Ingesting file {} using mapping {} in the group {}", urlsingle,
							mapping, selectedGroup);
					Map<String, LinkedEntity> result;
					if ((path == null || path.isBlank()) && isBodyValid(body)) {
						result = MetadataPopulator.startMetadataPopulationFromContent(body, mapping, selectedGroup,
								status, editorId);
					} else {
						result = MetadataPopulator.startMetadataPopulation(urlsingle, mapping, selectedGroup, status, editorId);
					}
					finalIngestionResult.putAll(result);
					LOGGER.info("[Ingestion finished] Ingested file {}", urlsingle);
				}
				s.close();
			} catch (IOException e) {
				return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
						.body(new IngestionResult("ERROR", e.getLocalizedMessage(), path, finalIngestionResult));
			}

		} else {
			LOGGER.info("[Ingestion initialized] Ingesting file {} using mapping {} in the group {}", path, mapping,
					selectedGroup);
			if ((path == null || path.isBlank()) && isBodyValid(body)) {
				finalIngestionResult = MetadataPopulator.startMetadataPopulationFromContent(body, mapping,
						selectedGroup, status, editorId);
			} else {
				finalIngestionResult = MetadataPopulator.startMetadataPopulation(path, mapping, selectedGroup, status, editorId);
			}
			LOGGER.info("[Ingestion finished] Ingested file {}", path);
		}

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
				.body(new IngestionResult("SUCCESS", "DONE, correcly ingested " + path, path, finalIngestionResult));
	}
}
