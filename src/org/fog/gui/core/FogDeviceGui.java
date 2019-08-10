package org.fog.gui.core;

import org.fog.core.Constants;
import org.fog.utils.Movement;
import org.fog.utils.distribution.Distribution;

/**
 * The model that represents virtual machine node for the graph. Nota: apenas uma aplicação
 * 
 */
public class FogDeviceGui extends Node {
	private static final long serialVersionUID = -8635044061126993668L;
	private static final double BANDWIDTH = 1000; // Currently, bandwidth is defined at the links instead at the nodes, thus is a dummy value
	
	private String name;
	private int level;
	private double mips;
	private int ram;
	private long storage;
	private double bw;
	private double rateMips;
	private double rateRam;
	private double rateStorage;
	private double rateBw;
	private double rateEnergy;
	private double idlePower;
	private double busyPower;
	private Movement movement;
	
	private String application; // Application name
	
	private Distribution distribution; // Sensor distribution

	public FogDeviceGui(String name, int level, double mips, int ram, long storage, double rateMips, double rateRam, double rateStorage,
			double rateBw, double rateEnergy, double idlePower, double busyPower, Movement movement, String appId, Distribution distribution) {
		super(name, Constants.FOG_TYPE);
		
		this.setName(name);
		this.setLevel(level);
		this.setMips(mips);
		this.setRam(ram);
		this.setStorage(storage);
		this.setBw(BANDWIDTH);
		this.setRateMips(rateMips);
		this.setRateRam(rateRam);
		this.setRateStorage(rateStorage);
		this.setRateBw(rateBw);
		this.setRateEnergy(rateEnergy);
		this.setIdlePower(idlePower);
		this.setBusyPower(busyPower);
		this.setMovement(movement);
		this.setApplication(appId);
		this.setDistribution(distribution);
	}
	
	public void setValues(String name, int level, double mips, int ram, long storage, double rateMips, double rateRam, double rateStorage,
			double rateBw, double rateEnergy, double idlePower, double busyPower, Movement movement, String appId, Distribution distribution) {
		this.setName(name);
		this.setLevel(level);
		this.setMips(mips);
		this.setRam(ram);
		this.setStorage(storage);
		this.setBw(BANDWIDTH);
		this.setRateMips(rateMips);
		this.setRateRam(rateRam);
		this.setRateStorage(rateStorage);
		this.setRateBw(rateBw);
		this.setRateEnergy(rateEnergy);
		this.setIdlePower(idlePower);
		this.setBusyPower(busyPower);
		this.setMovement(movement);
		this.setApplication(appId);
		this.setDistribution(distribution);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getMips() {
		return mips;
	}

	public void setMips(double mips) {
		this.mips = mips;
	}

	public int getRam() {
		return ram;
	}

	public void setRam(int ram) {
		this.ram = ram;
	}
	
	public long getStorage() {
		return storage;
	}

	public void setStorage(long storage) {
		this.storage = storage;
	}

	public double getBw() {
		return bw;
	}

	public void setBw(double bw) {
		this.bw = bw;
	}

	public double getRateMips() {
		return rateMips;
	}

	public void setRateMips(double rateMips) {
		this.rateMips = rateMips;
	}

	public double getRateRam() {
		return rateRam;
	}

	public void setRateRam(double rateRam) {
		this.rateRam = rateRam;
	}

	public double getRateStorage() {
		return rateStorage;
	}

	public void setRateStorage(double rateStorage) {
		this.rateStorage = rateStorage;
	}

	public double getRateBw() {
		return rateBw;
	}

	public void setRateBw(double rateBw) {
		this.rateBw = rateBw;
	}
	
	public double getRateEnergy() {
		return rateEnergy;
	}

	public void setRateEnergy(double rateEnergy) {
		this.rateEnergy = rateEnergy;
	}

	public double getIdlePower() {
		return idlePower;
	}

	public void setIdlePower(double idlePower) {
		this.idlePower = idlePower;
	}

	public double getBusyPower() {
		return busyPower;
	}

	public void setBusyPower(double busyPower) {
		this.busyPower = busyPower;
	}
	
	public Movement getMovement() {
		return movement;
	}

	public void setMovement(Movement movement) {
		this.movement = movement;
	}
	
	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}
	
	public Distribution getDistribution() {
		return this.distribution;
	}
	
	public void setDistribution(Distribution distribution) {
		this.distribution = distribution;
	}
	
	public int getDistributionType(){
		return distribution.getDistributionType();
	}	
	
}
