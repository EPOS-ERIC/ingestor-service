package org.epos.core.sparql;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.epos.core.export.EPOSVersion;
import org.epos.core.export.MetadataExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.HashMap;
import java.util.Map;

@Service
public class SparqlService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SparqlService.class);

    private FusekiServer fusekiServer;
    private Map<EPOSVersion, Dataset> datasets = new HashMap<>();
    private EPOSVersion defaultVersion = EPOSVersion.V1;
    private volatile boolean ready = false;
    private volatile String initializationError = null;

    @PostConstruct
    public void init() {
        LOGGER.info("Initializing SPARQL service");
        try {
            buildModel(EPOSVersion.V1);
            startFusekiServer();
            ready = true;
            LOGGER.info("SPARQL service initialized successfully");
        } catch (Exception e) {
            initializationError = e.getMessage();
            LOGGER.error("SPARQL service initialization failed - service will start with empty models. Error: {}", e.getMessage());
            // Initialize with empty datasets so the application can still start
            initializeEmptyDatasets();
        }
    }

    private void initializeEmptyDatasets() {
        if (!datasets.containsKey(defaultVersion)) {
            Dataset emptyDataset = DatasetFactory.create(ModelFactory.createDefaultModel());
            datasets.put(defaultVersion, emptyDataset);
            LOGGER.warn("Created empty dataset for version {}", defaultVersion);
        }
        try {
            startFusekiServer();
        } catch (Exception e) {
            LOGGER.error("Failed to start Fuseki server even with empty datasets", e);
        }
    }

    /**
     * Check if the SPARQL service is ready with populated data.
     * @return true if the service initialized successfully with data, false otherwise
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Get the initialization error message if initialization failed.
     * @return the error message, or null if initialization succeeded
     */
    public String getInitializationError() {
        return initializationError;
    }

    @PreDestroy
    public void destroy() {
        LOGGER.info("Shutting down SPARQL service");
        if (fusekiServer != null) {
            fusekiServer.stop();
        }
    }

    private boolean buildModel(EPOSVersion version) {
        LOGGER.info("Building RDF model for version {}", version);
        try {
            String rdfContent = MetadataExporter.exportToRDF(null, "turtle", null, version);
            Model rdfModel = ModelFactory.createDefaultModel();
            rdfModel.read(new java.io.StringReader(rdfContent), null, "TURTLE");
            Dataset dataset = DatasetFactory.create(rdfModel);
            datasets.put(version, dataset);
            LOGGER.info("RDF model built for version {} with {} statements", version, rdfModel.size());
            return true;
        } catch (Exception e) {
            LOGGER.error("Error building RDF model for version {}", version, e);
            throw new RuntimeException("Failed to build RDF model for version " + version, e);
        }
    }

    private void startFusekiServer() {
        if (fusekiServer != null) {
            fusekiServer.stop();
        }
        fusekiServer = FusekiServer.create()
                .add("/sparql", datasets.get(defaultVersion))
                .build();
        fusekiServer.start();
        LOGGER.info("Fuseki server started on port {}", fusekiServer.getPort());
    }

    @Scheduled(fixedRateString = "${sparql.refresh.rate:3600000}") // Refresh every hour
    public void refreshModel() {
        LOGGER.info("Refreshing RDF models");
        try {
            buildModel(EPOSVersion.V1);
            startFusekiServer();
            ready = true;
            initializationError = null;
            LOGGER.info("RDF models refreshed successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to refresh RDF models: {}", e.getMessage());
            // Keep the existing datasets if refresh fails
        }
    }

    public Dataset getDataset(EPOSVersion version) {
        return datasets.get(version);
    }
}
