package org.fog.placement.algorithms.placement;

import java.util.List;
import java.util.Map;

import org.fog.application.Application;
import org.fog.entities.FogDevice;

public abstract class Algorithm {
	private final List<FogDevice> fogDevices;
	private final List<Application> applications;
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications) {
		this.fogDevices = fogDevices;
		this.applications = applications;
	}
	
	public abstract Map<String, List<String>> execute();

	List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	List<Application> getApplications() {
		return applications;
	}
	
}
