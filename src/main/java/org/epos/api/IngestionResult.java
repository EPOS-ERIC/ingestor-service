package org.epos.api;

import java.util.Map;

import org.epos.eposdatamodel.LinkedEntity;

public class IngestionResult {
	private final String status;
	private final String message;
	private final String ingestedPath;
	private final Map<String, LinkedEntity> ingestedEntities;

	public IngestionResult(
			String status,
			String message,
			String ingestedPath,
			Map<String, LinkedEntity> ingestedEntities) {
		this.status = status;
		this.message = message;
		this.ingestedPath = ingestedPath;
		this.ingestedEntities = ingestedEntities;
	}

	public String getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getIngestedPath() {
		return ingestedPath;
	}

	public Map<String, LinkedEntity> getIngestedEntities() {
		return ingestedEntities;
	}
}
