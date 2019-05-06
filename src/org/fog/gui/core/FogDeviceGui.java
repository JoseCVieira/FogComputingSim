package org.fog.gui.core;

import org.fog.core.Constants;

/**
 * The model that represents virtual machine node for the graph.
 * 
 */
public class FogDeviceGui extends Node {
	private static final long serialVersionUID = -8635044061126993668L;
	
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
	private double idlePower;
	private double busyPower;
	private double costPerSec;
	private String application;

	public FogDeviceGui(String name, int level, double mips, int ram, long storage, double bw,
			double rateMips, double rateRam, double rateStorage, double rateBw, double idlePower,
			double busyPower, double costPerSec, String appId) {
		super(name, Constants.FOG_TYPE);
		
		this.setName(name);
		this.setLevel(level);
		this.setMips(mips);
		this.setRam(ram);
		this.setStorage(storage);
		this.setBw(bw);
		this.setRateMips(rateMips);
		this.setRateRam(rateRam);
		this.setRateStorage(rateStorage);
		this.setRateBw(rateBw);
		this.setIdlePower(idlePower);
		this.setBusyPower(busyPower);
		this.setCostPerSec(costPerSec);
		this.setApplication(appId);
	}
	
	public void setValues(String name, int level, double mips, int ram, long storage, double bw,
			double rateMips, double rateRam, double rateStorage, double rateBw, double idlePower,
			double busyPower, double costPerSec, String appId) {
		this.setName(name);
		this.setLevel(level);
		this.setMips(mips);
		this.setRam(ram);
		this.setStorage(storage);
		this.setBw(bw);
		this.setRateMips(rateMips);
		this.setRateRam(rateRam);
		this.setRateStorage(rateStorage);
		this.setRateBw(rateBw);
		this.setIdlePower(idlePower);
		this.setBusyPower(busyPower);
		this.setCostPerSec(costPerSec);
		this.setApplication(appId);
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

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
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
	
	public double getCostPerSec() {
		return costPerSec;
	}

	public void setCostPerSec(double costPerSec) {
		this.costPerSec = costPerSec;
	}

	@Override
	public String toString() {
		return "FogDeviceGui [name=" + name + ", level=" + level + ", mips=" + mips + ", ram=" + ram + ", storage="
				+ storage + ", bw=" + bw + ", rateMips=" + rateMips + ", rateRam=" + rateRam + ", rateStorage="
				+ rateStorage + ", rateBw=" + rateBw + ", idlePower=" + idlePower + ", busyPower=" + busyPower
				+ ", costPerSec=" + costPerSec + ", application=" + application + "]";
	}
	
}
