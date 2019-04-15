package org.fog.entities;

import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenterBroker;

public class FogBroker extends PowerDatacenterBroker{

	public FogBroker(String name) throws Exception {
		super(name);
	}

	@Override
	public void startEntity() {
		
	}

	@Override
	public void processEvent(SimEvent ev) {
		
	}

	@Override
	public void shutdownEntity() {
		
	}
}