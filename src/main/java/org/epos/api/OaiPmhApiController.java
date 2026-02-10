package org.epos.api;

import org.epos.core.oaipmh.OaiPmhService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controller implementing the OAI-PMH 2.0 Protocol.
 * 
 * The Open Archives Initiative Protocol for Metadata Harvesting (OAI-PMH) is a 
 * low-barrier mechanism for repository interoperability. This endpoint allows 
 * metadata harvesters to collect structured metadata from the EPOS repository.
 * 
 * @see <a href="http://www.openarchives.org/OAI/openarchivesprotocol.html">OAI-PMH 2.0 Specification</a>
 */
@RestController
public class OaiPmhApiController implements OaiPmhApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(OaiPmhApiController.class);

	private final OaiPmhService oaiPmhService;

	@Autowired
	public OaiPmhApiController(OaiPmhService oaiPmhService) {
		this.oaiPmhService = oaiPmhService;
	}

	@Override
	@RequestMapping(value = "/oai", produces = { "text/xml" }, method = RequestMethod.GET)
	public ResponseEntity<String> handleOaiRequest(
			@Parameter(in = ParameterIn.QUERY, description = "OAI-PMH verb", required = true,
				schema = @Schema(allowableValues = {"Identify", "ListMetadataFormats", "ListSets", 
					"ListIdentifiers", "ListRecords", "GetRecord"}))
			@RequestParam(value = "verb", required = true) String verb,

			@Parameter(in = ParameterIn.QUERY, description = "Unique identifier of a record", required = false)
			@RequestParam(value = "identifier", required = false) String identifier,

			@Parameter(in = ParameterIn.QUERY, description = "Metadata format prefix", required = false,
				schema = @Schema(allowableValues = {"oai_dc", "dcat"}))
			@RequestParam(value = "metadataPrefix", required = false) String metadataPrefix,

			@Parameter(in = ParameterIn.QUERY, description = "Set specification", required = false)
			@RequestParam(value = "set", required = false) String set,

			@Parameter(in = ParameterIn.QUERY, description = "Lower bound for datestamp (ISO 8601)", required = false)
			@RequestParam(value = "from", required = false) String from,

			@Parameter(in = ParameterIn.QUERY, description = "Upper bound for datestamp (ISO 8601)", required = false)
			@RequestParam(value = "until", required = false) String until,

			@Parameter(in = ParameterIn.QUERY, description = "Resumption token for pagination", required = false)
			@RequestParam(value = "resumptionToken", required = false) String resumptionToken) {

		HttpServletRequest request = getRequest();
		String requestUrl = request != null ? getFullRequestUrl(request) : "";

		LOGGER.info("[OAI-PMH] Processing request - verb: {}, identifier: {}, metadataPrefix: {}", 
			verb, identifier, metadataPrefix);

		try {
			String response = oaiPmhService.handleRequest(
				verb, identifier, metadataPrefix, set, from, until, resumptionToken, requestUrl);

			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_XML)
				.body(response);

		} catch (Exception e) {
			LOGGER.error("[OAI-PMH] Error processing request: {}", e.getMessage(), e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.contentType(MediaType.APPLICATION_XML)
				.body(buildErrorXml("Internal server error: " + e.getMessage()));
		}
	}

	/**
	 * Get the current HTTP request from the context.
	 */
	@Autowired(required = false)
	private HttpServletRequest httpServletRequest;

	private HttpServletRequest getRequest() {
		return httpServletRequest;
	}

	/**
	 * Constructs the full request URL including query parameters.
	 */
	private String getFullRequestUrl(HttpServletRequest request) {
		StringBuilder url = new StringBuilder();
		url.append(request.getScheme()).append("://");
		url.append(request.getServerName());
		if (request.getServerPort() != 80 && request.getServerPort() != 443) {
			url.append(":").append(request.getServerPort());
		}
		url.append(request.getRequestURI());
		if (request.getQueryString() != null) {
			url.append("?").append(request.getQueryString());
		}
		return url.toString();
	}

	/**
	 * Builds a simple error XML response for unexpected errors.
	 */
	private String buildErrorXml(String message) {
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<OAI-PMH xmlns=\"http://www.openarchives.org/OAI/2.0/\">\n" +
			"  <error>Internal error: " + escapeXml(message) + "</error>\n" +
			"</OAI-PMH>\n";
	}

	private String escapeXml(String text) {
		if (text == null) return "";
		return text.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\"", "&quot;")
			.replace("'", "&apos;");
	}
}
