package org.epos.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import model.Ontology;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@Validated
public interface OntologiesManagementApi {

    @Operation(summary = "ontology population operation", description = "API for internal use only!.", tags={ "Ontologies Management Service" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request."),
        
        @ApiResponse(responseCode = "401", description = "Token is missing or invalid"),
        
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        
        @ApiResponse(responseCode = "404", description = "Not Found") })
    @RequestMapping(value = "/ontology",
        produces = { "*/*" }, 
        method = RequestMethod.POST)
    ResponseEntity<ApiResponseMessage> ontologyPopulate(
    		@Parameter(in = ParameterIn.QUERY, description = "path to the ontology file" ,required=true,schema=@Schema()) @RequestParam(value="path", required=true) String path,
    		@Parameter(in = ParameterIn.QUERY, description = "ontology name" ,required=true,schema=@Schema()) @RequestParam(value="name", required=true) String name,
            @Parameter(in = ParameterIn.QUERY, description = "ontology type" ,required=true,schema=@Schema(allowableValues = {"BASE", "MAPPING"})) @RequestParam(value="type", required=true) String type);

    @Operation(summary = "retrieve ontologies operation", description = "API for internal use only!.", tags={ "Ontologies Management Service" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),

            @ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),

            @ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),

            @ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),

            @ApiResponse(responseCode = "400", description = "Bad request."),

            @ApiResponse(responseCode = "401", description = "Token is missing or invalid"),

            @ApiResponse(responseCode = "403", description = "Forbidden"),

            @ApiResponse(responseCode = "404", description = "Not Found") })
    @RequestMapping(value = "/ontology",
            produces = { "*/*" },
            method = RequestMethod.GET)
    ResponseEntity<List<Ontology>> ontologyRetrieve(
                    @Parameter(in = ParameterIn.QUERY, description = "plain content, not encoded" ,required=false,schema=@Schema()) @RequestParam(value="encoded", required=false) Boolean encoded,
                    @Parameter(in = ParameterIn.QUERY, description = "retrieve only name and type, without schema" ,required=false,schema=@Schema()) @RequestParam(value="nobody", required=false) Boolean nobody);

}

