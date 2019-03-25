package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;

public class FogDeviceCharacteristics extends DatacenterCharacteristics {
	
	/** The cost per mips. */
	private double costPerMips;

	@SuppressWarnings("serial")
	public FogDeviceCharacteristics(String architecture, String os, String vmm, Host host, double timeZone,
			double costPerSec, double costPerMips, double costPerMem, double costPerStorage, double costPerBw) {
		super(architecture, os, vmm, new ArrayList<Host>(){{add(host);}}, timeZone, costPerSec, costPerMem, costPerStorage, costPerBw);
		
		setCostPerMips(costPerMips);
	}
	
	public double getCostPerMips() {
		return costPerMips;
	}

	public void setCostPerMips(double costPerMips) {
		this.costPerMips = costPerMips;
	}

}
