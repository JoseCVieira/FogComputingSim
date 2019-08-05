package org.fog.utils;

import org.cloudbus.cloudsim.power.models.PowerModel;

/**
 * Class which defines the model used to compute the power spent to use processing resources.
 * Original version by Anton Beloglazov
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since   July, 2019
 */
public class FogLinearPowerModel implements PowerModel {
	/** The busy power value */
	private double busyPower;
	
	/** The idle power value */
	private double idlePower;
	
	/**
	 * Creates a new fog linear power model.
	 * 
	 * @param busyPower the busy power value
	 * @param idlePower the idle power value
	 */
	public FogLinearPowerModel(final double busyPower, final double idlePower) {
		this.busyPower = busyPower;
		this.idlePower = idlePower;
	}
	
	/**
	 * Gets the power needed to support a percentage of processing units (MIPS) utilization.
	 * 
	 * @param utilization the percentage of processing units in use
	 * @return the power needed to support a percentage of processing units (MIPS) utilization
	 */
	@Override
	public double getPower(final double utilization) throws IllegalArgumentException {
		if (utilization < 0 || utilization > 1) {
			throw new IllegalArgumentException("Utilization value must be between 0 and 1");
		}
		return idlePower + (busyPower - idlePower) * utilization;
	}

	/**
	 * Gets the idle/static power value.
	 * 
	 * @return the static power value
	 */
	public double getIdlePower() {
		return idlePower;
	}
	
	/**
	 * Gets the busy power value.
	 * 
	 * @return the busy power value
	 */
	public double getBusyPower() {
		return busyPower;
	}
}
