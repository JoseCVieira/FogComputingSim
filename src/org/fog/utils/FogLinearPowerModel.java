package org.fog.utils;

import org.cloudbus.cloudsim.power.models.PowerModel;

public class FogLinearPowerModel implements PowerModel {

	private double maxPower;
	private double staticPower;

	public FogLinearPowerModel(double maxPower, double staticPower) {
		this.maxPower = maxPower;
		this.staticPower = staticPower;
	}
	
	@Override
	public double getPower(double utilization) throws IllegalArgumentException {
		if (utilization < 0 || utilization > 1) {
			throw new IllegalArgumentException("Utilization value must be between 0 and 1");
		}
		return staticPower + (maxPower - staticPower) * utilization;
	}

	public double getStaticPower() {
		return staticPower;
	}
	
	public double getBusyPower() {
		return maxPower;
	}
}
