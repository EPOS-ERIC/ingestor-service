package org.epos.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.epos.core.OntologiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import model.Ontology;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
public class OntologiesManagementApiController implements OntologiesManagementApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologiesManagementApiController.class);

	private final String code = System.getenv("INGESTOR_HASH");

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Autowired
	private Gson gsonSingleton;

	@Autowired
	public OntologiesManagementApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<ApiResponseMessage> ontologyPopulate(
			@Parameter(in = ParameterIn.QUERY, description = "path to the ontology file" ,required=true,schema=@Schema()) @RequestParam(value="path", required=true) String path,
			@Parameter(in = ParameterIn.QUERY, description = "ontology name" ,required=true,schema=@Schema()) @RequestParam(value="name", required=true) String name,
			@Parameter(in = ParameterIn.QUERY, description = "ontology type" ,required=true,schema=@Schema(allowableValues = {"BASE", "MAPPING"})) @RequestParam(value="type", required=true) String type) {

		try {
			OntologiesManager.createOntology(name, type, path);
		}catch(IOException e){
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(ApiResponseMessage.ERROR,"ERROR on adding ontology from: "+path));
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(ApiResponseMessage.OK,"DONE, correcly added ontology from: "+path));
	}

	public ResponseEntity<List<Ontology>> ontologyRetrieve(
			@Parameter(in = ParameterIn.QUERY, description = "plain content, not encoded" ,required=false,schema=@Schema()) @RequestParam(value="encoded", required=false) Boolean encoded,
			@Parameter(in = ParameterIn.QUERY, description = "retrieve only name and type, without schema" ,required=false,schema=@Schema()) @RequestParam(value="nobody", required=false) Boolean nobody) {

		if(nobody == null) nobody = false;
		if(encoded == null) encoded = true;


		List<Ontology> ontologiesList = new ArrayList<>();
		ontologiesList = OntologiesManager.retrieveOntologies();
		if(nobody.booleanValue()){
			ontologiesList.stream().map(Ontology::getContent).forEach(content -> {content = null;});
		}
		if(!encoded.booleanValue()){
			ontologiesList.stream().map(Ontology::getContent).filter(content -> content!=null).forEach(content -> {content = Base64.getDecoder().decode(content).toString();});
		}

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ontologiesList);
	}

	private boolean validSecurityPhrase( String phrase ) {

		if(!DigestUtils.sha1Hex(phrase).toUpperCase().equals(code)) {
			LOGGER.error("Security phrase incorrect. Input code is {}, encodes to {}", phrase, DigestUtils.sha1Hex(phrase));
			if ( code == null || code.trim().equals("") )
				LOGGER.error("No hash of security phrase provided in this environment");
			return false;
		}
		return true;
	}
}
