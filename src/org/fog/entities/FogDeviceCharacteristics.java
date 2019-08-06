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
	
	/** Monetary cost per processing units (€/MIPS). */
	private double costPerMips;
	
	/** Monetary cost per energy spent (€/W). */
	private double costPerEnergy;
	
	/**
	 * Creates a new fog device characteristics.
	 * 
	 * @param architecture the architecture of a resource
	 * @param os the operating system used
	 * @param vmm the virtual machine monitor used
	 * @param host object which executes actions related to management of virtual machines (e.g., creation and destruction)
	 * @param timeZone local time zone of a user that owns this reservation
	 * @param costPerMips the monetary cost per processing resource units (€/MIPS)
	 * @param costPerMem the monetary cost per memory resource units (€/kB)
	 * @param costPerStorage the monetary cost per storage resource units (€/kB)
	 * @param costPerBw the monetary cost per network resource units (€/kB)
	 * @param costPerEnergy the monetary cost per energy units (€/W)
	 */	
	@SuppressWarnings("serial")
	public FogDeviceCharacteristics(String architecture, String os, String vmm, Host host, double timeZone, double costPerMips,
			double costPerMem, double costPerStorage, double costPerBw, double costPerEnergy) {
		super(architecture, os, vmm, new ArrayList<Host>(){{add(host);}}, timeZone, 0, costPerMem, costPerStorage, costPerBw);
		
		setCostPerMips(costPerMips);
		setCostPerEnergy(costPerEnergy);
	}
	
	/**
	 * Gets the monetary cost per processing units (€/MIPS).
	 * 
	 * @return the monetary cost per processing units
	 */
	public double getCostPerMips() {
		return costPerMips;
	}
	
	/**
	 * Sets the monetary cost per processing units (€/MIPS).
	 * 
	 * @param costPerMips the monetary cost per processing units
	 */
	public void setCostPerMips(double costPerMips) {
		this.costPerMips = costPerMips;
	}
	
	/**
	 * Gets the monetary cost per energy spent (€/W).
	 * 
	 * @return the monetary cost per energy spent
	 */
	public double getCostPerEnergy() {
		return costPerEnergy;
	}
	
	/**
	 * Sets the monetary cost per energy spent (€/W).
	 * 
	 * @param costPerEnergy the monetary cost per energy spent
	 */
	public void setCostPerEnergy(double costPerEnergy) {
		this.costPerEnergy = costPerEnergy;
	}

}
