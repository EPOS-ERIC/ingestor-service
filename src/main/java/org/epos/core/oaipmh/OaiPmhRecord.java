package org.epos.core.oaipmh;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;

/**
 * Represents an OAI-PMH record extracted from the triplestore.
 * Contains the record's identifier, datestamp, set memberships, and RDF metadata.
 */
public class OaiPmhRecord {

	private final String identifier;
	private final String datestamp;
	private final List<String> setSpecs;
	private final String rdfType;
	private final Model metadata;

	private OaiPmhRecord(Builder builder) {
		this.identifier = builder.identifier;
		this.datestamp = builder.datestamp;
		this.setSpecs = builder.setSpecs;
		this.rdfType = builder.rdfType;
		this.metadata = builder.metadata;
	}

	/**
	 * Returns the unique identifier for this record (resource URI).
	 */
	public String getIdentifier() {
		return identifier;
	}

	/**
	 * Returns the datestamp for this record (dct:modified or dct:issued).
	 */
	public String getDatestamp() {
		return datestamp;
	}

	/**
	 * Returns the list of set specifications this record belongs to.
	 * Includes both entity type sets (e.g., "type:Dataset") and category sets.
	 */
	public List<String> getSetSpecs() {
		return setSpecs;
	}

	/**
	 * Returns the RDF type URI for this record (e.g., "http://www.w3.org/ns/dcat#Dataset").
	 */
	public String getRdfType() {
		return rdfType;
	}

	/**
	 * Returns the Jena Model containing the full RDF metadata for this record.
	 * Used for the epos_dcat_ap metadata format.
	 */
	public Model getMetadata() {
		return metadata;
	}

	/**
	 * Returns the local name of the RDF type (e.g., "Dataset" from "http://www.w3.org/ns/dcat#Dataset").
	 */
	public String getTypeLocalName() {
		if (rdfType == null) {
			return "Resource";
		}
		int hashIndex = rdfType.lastIndexOf('#');
		int slashIndex = rdfType.lastIndexOf('/');
		int index = Math.max(hashIndex, slashIndex);
		if (index >= 0 && index < rdfType.length() - 1) {
			return rdfType.substring(index + 1);
		}
		return rdfType;
	}

	@Override
	public String toString() {
		return "OaiPmhRecord{" +
				"identifier='" + identifier + '\'' +
				", datestamp='" + datestamp + '\'' +
				", rdfType='" + rdfType + '\'' +
				", setSpecs=" + setSpecs +
				'}';
	}

	/**
	 * Builder for OaiPmhRecord.
	 */
	public static class Builder {
		private String identifier;
		private String datestamp;
		private List<String> setSpecs = new ArrayList<>();
		private String rdfType;
		private Model metadata;

		public Builder identifier(String identifier) {
			this.identifier = identifier;
			return this;
		}

		public Builder datestamp(String datestamp) {
			this.datestamp = datestamp;
			return this;
		}

		public Builder setSpecs(List<String> setSpecs) {
			this.setSpecs = setSpecs != null ? new ArrayList<>(setSpecs) : new ArrayList<>();
			return this;
		}

		public Builder addSetSpec(String setSpec) {
			this.setSpecs.add(setSpec);
			return this;
		}

		public Builder rdfType(String rdfType) {
			this.rdfType = rdfType;
			return this;
		}

		public Builder metadata(Model metadata) {
			this.metadata = metadata;
			return this;
		}

		public OaiPmhRecord build() {
			if (identifier == null || identifier.isEmpty()) {
				throw new IllegalArgumentException("Identifier is required");
			}
			if (datestamp == null || datestamp.isEmpty()) {
				throw new IllegalArgumentException("Datestamp is required");
			}
			return new OaiPmhRecord(this);
		}
	}

	public static Builder builder() {
		return new Builder();
	}
}
