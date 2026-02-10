package org.epos.api;

import io.swagger.v3.oas.annotations.Operation;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.epos.core.export.EPOSVersion;
import org.epos.core.sparql.SparqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;

@RestController
@RequestMapping("/api/sparql")
public class SparqlController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlController.class);

    @Autowired
    private SparqlService sparqlService;

    @Operation(summary = "SPARQL endpoint operation", description = "SPARQL endpoint to access data.", tags={ "Ontologies Management Service" })
    @PostMapping(consumes = "application/sparql-query", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> executeQuery(@RequestBody String queryString,
            @Parameter(in = ParameterIn.QUERY, description = "EPOS-DCAT-AP version (optional, default: V1)", required = false, schema = @Schema()) @RequestParam(value = "version", required = false, defaultValue = "V1") EPOSVersion version) {
        try {
            LOGGER.debug("Received SPARQL query: {} for version {}", queryString, version);
            Query query = QueryFactory.create(queryString);
            try (QueryExecution qexec = QueryExecutionFactory.create(query, sparqlService.getDataset(version))) {
                if (query.isSelectType()) {
                    ResultSet results = qexec.execSelect();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ResultSetFormatter.outputAsJSON(baos, results);
                    String result = baos.toString("UTF-8");
                    return ResponseEntity.ok(result);
                } else if (query.isAskType()) {
                    boolean result = qexec.execAsk();
                    return ResponseEntity.ok("{\"boolean\": " + result + "}");
                } else if (query.isConstructType()) {
                    Model model = qexec.execConstruct();
                    StringWriter sw = new StringWriter();
                    RDFDataMgr.write(sw, model, Lang.JSONLD);
                    return ResponseEntity.ok(sw.toString());
                } else {
                    return ResponseEntity.badRequest().body("{\"error\": \"Unsupported query type\"}");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error executing SPARQL query: {}", queryString, e);
            String errorMessage = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                    e.getClass().getSimpleName(),
                    e.getMessage().replace("\"", "\\\"").replace("\n", " "));
            return ResponseEntity.badRequest().body(errorMessage);
        }
    }
}
