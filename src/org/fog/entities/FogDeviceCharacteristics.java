package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;

/**
 * Class representing fog device characteristics.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class FogDeviceCharacteristics extends DatacenterCharacteristics {
	
	/** Monetary cost per processing units [€] */
	private double costPerMips;
	
	/** Monetary cost per power [€] */
	private double costPerPower;
	
	/**
	 * Creates a new fog device characteristics.
	 * 
	 * @param architecture the architecture of a resource
	 * @param os the operating system used
	 * @param vmm the virtual machine monitor used
	 * @param host object which executes actions related to management of virtual machines (e.g., creation and destruction)
	 * @param timeZone local time zone of a user that owns this reservation
	 * @param costPerMips the monetary cost per processing resource units [€]
	 * @param costPerMem the monetary cost per memory resource units [€]
	 * @param costPerStorage the monetary cost per storage resource units [€]
	 * @param costPerBw the monetary cost per network resource units [€]
	 * @param costPerEnergy the monetary cost per energy units [€]
	 */	
	@SuppressWarnings("serial")
	public FogDeviceCharacteristics(String architecture, String os, String vmm, Host host, double timeZone, double costPerMips,
			double costPerMem, double costPerStorage, double costPerBw, double costPerEnergy) {
		super(architecture, os, vmm, new ArrayList<Host>(){{add(host);}}, timeZone, 0, costPerMem, costPerStorage, costPerBw);
		
		setCostPerMips(costPerMips);
		setCostPerPower(costPerEnergy);
	}
	
	/**
	 * Gets the monetary cost per processing units [€].
	 * 
	 * @return the monetary cost per processing units
	 */
	public double getCostPerMips() {
		return costPerMips;
	}
	
	/**
	 * Sets the monetary cost per processing units [€].
	 * 
	 * @param costPerMips the monetary cost per processing units
	 */
	public void setCostPerMips(double costPerMips) {
		this.costPerMips = costPerMips;
	}
	
	/**
	 * Gets the monetary cost per power [€].
	 * 
	 * @return the monetary cost per power
	 */
	public double getCostPerPower() {
		return costPerPower;
	}
	
	/**
	 * Sets the monetary cost per power [€].
	 * 
	 * @param costPerEnergy the monetary cost per power
	 */
	public void setCostPerPower(double costPerPower) {
		this.costPerPower = costPerPower;
	}

}
