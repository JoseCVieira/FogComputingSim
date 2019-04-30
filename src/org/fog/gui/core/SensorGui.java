package org.fog.gui.core;

import java.io.Serializable;

import org.fog.core.Config;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

public class SensorGui extends Node implements Serializable{
	private static final long serialVersionUID = 4087896123649020073L;

	private String name;
	private String sensorType;
	private Distribution distribution;
	
	public SensorGui(String name, Distribution distribution){
		super(name, Config.SENSOR_TYPE);
		setName(name);
		setSensorType("");
		setDistribution(distribution);
	}

	public SensorGui(String name, String selectedItem, double normalMean_, double normalStdDev_, double uniformLow_, double uniformUp_,
			double deterministicVal_) {
		super(name, Config.SENSOR_TYPE);
		
		setName(name);
		setSensorType("");
		if(normalMean_ != -1)
			setDistribution(new NormalDistribution(normalMean_, normalStdDev_));
		else if(uniformLow_ != -1)
			setDistribution(new UniformDistribution(uniformLow_, uniformUp_));
		else if(deterministicVal_ != -1)
			setDistribution(new DeterministicDistribution(deterministicVal_));
	}
	
	public void setValues(String name, String selectedItem, double normalMean_, double normalStdDev_, double uniformLow_, double uniformUp_,
			double deterministicVal_) {
		
		setName(name);
		setSensorType("");
		if(normalMean_ != -1)
			setDistribution(new NormalDistribution(normalMean_, normalStdDev_));
		else if(uniformLow_ != -1)
			setDistribution(new UniformDistribution(uniformLow_, uniformUp_));
		else if(deterministicVal_ != -1)
			setDistribution(new DeterministicDistribution(deterministicVal_));
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getSensorType() {
		return sensorType;
	}

	public void setSensorType(String sensorType) {
		this.sensorType = sensorType;
	}
	
	public int getDistributionType(){
		return distribution.getDistributionType();
	}

	public Distribution getDistribution() {
		return distribution;
	}

	public void setDistribution(Distribution distribution) {
		this.distribution = distribution;
	}
	
	@Override
	public String toString() {
		if(distribution instanceof NormalDistribution)
			return "Sensor [dist=1 mean=" + ((NormalDistribution)distribution).getMean() + " stdDev=" + ((NormalDistribution)distribution).getStdDev() + "]";
		else if(distribution instanceof UniformDistribution)
			return "Sensor [dist=2 min=" + ((UniformDistribution)distribution).getMin() + " max=" + ((UniformDistribution)distribution).getMax() + "]";
		else if(distribution instanceof DeterministicDistribution)
			return "Sensor [dist=3 value=" + ((DeterministicDistribution)distribution).getValue() + "]";
		else
			return "";
	}
}
