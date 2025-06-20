package org.epos.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@Validated
public interface MetadataPopulationApi {

    @Operation(summary = "metadata population operation", description = "API for internal use only!.", tags={ "Metadata Management Service" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request."),
        
        @ApiResponse(responseCode = "401", description = "Token is missing or invalid"),
        
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        
        @ApiResponse(responseCode = "404", description = "Not Found") })
    @RequestMapping(value = "/populate",
        produces = { "*/*" }, 
        method = RequestMethod.POST)
    ResponseEntity<ApiResponseMessage> metadataPopulate(
    		@Parameter(in = ParameterIn.QUERY, description = "population type (single file or multiple lines file)" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" })) @RequestParam(value="type", required=true) String type,
    		@Parameter(in = ParameterIn.QUERY, description = "path of the file to use" ,required=true,schema=@Schema()) @RequestParam(value="path", required=true) String path,
    		@Parameter(in = ParameterIn.QUERY, description = "metadata model" ,required=true,schema=@Schema()) @RequestParam(value="model", required=true) String model,
            @Parameter(in = ParameterIn.QUERY, description = "metadata mapping model" ,required=true,schema=@Schema()) @RequestParam(value="mapping", required=true) String mapping,
    		@Parameter(in = ParameterIn.QUERY, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestParam(value="securityCode", required=true) String securityCode,
            @Parameter(in = ParameterIn.QUERY, description = "metadata group where the resource should be placed" ,required=true,schema=@Schema()) @RequestParam(value="metadataGroup", required=false) String metadataGroup);

}

