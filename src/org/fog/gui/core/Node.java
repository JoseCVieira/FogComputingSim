package org.fog.gui.core;

import java.io.Serializable;

import org.fog.utils.distribution.Distribution;
import org.fog.utils.movement.Location;
import org.fog.utils.movement.Movement;

/**
 * The model that represents virtual machine node for the graph. Note that in the current version of the GUI it's only possible
 * to deploy one application per client. In order to use more than one it's necessary to define it programmatically (check the examples package).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Node implements Serializable {
	private static final long serialVersionUID = -8635044061126993668L;
	private static final double BANDWIDTH = 100000; // Currently, bandwidth is defined at the links instead at the nodes, thus is a dummy value
	
	/** Name of the fog device */
	private String name;
	
	/** Defines the position at the graphical interface (it is not used by the simulation itself) */
	private int level;
	
	/** Available MIPS of the machine */
	private double mips;
	
	/** Available ram in the machine */
	private int ram;
	
	/** Available storage in the machine */
	private long storage;
	
	/** Available bandwidth in the machine */
	private double bw;
	
	 /** Price that will be charged by using processing resources */
	private double rateMips;
	
	/** Price that will be charged by using memory resources */
	private double rateRam;
	
	/** Price that will be charged by using storage resources */
	private double rateStorage;
	
	/** Price that will be charged by bandwidth resources */
	private double rateBw;
	
	/** Price that will be charged by spending energy */
	private double rateEnergy;
	
	/** Power value while using the full processing capacity of the machine */
	private double idlePower;
	
	/** Power value while using no processing resources in the machine */
	private double busyPower;
	
	/** Movement of the machine (i.e., contains the velocity, position and direction) */
	private Movement movement;
	
	/** Application name (its empty for non client nodes) */
	private String application; // 
	
	/** Distribution of the sensor (its null for non client nodes) */
	private Distribution distribution;
	
	/** GUI coordinates */
	private Location coord;
	
	/**
	 * Creates a new virtual machine node for the graph.
	 * 
	 * @param name the name of the fog device
	 * @param level the position at the graphical interface
	 * @param mips the available processing resources of the machine
	 * @param ram the available memory in the machine
	 * @param storage the available storage in the machine
	 * @param rateMips the price that will be charged by using processing resources
	 * @param rateRam the price that will be charged by using memory resources
	 * @param rateStorage the price that will be charged by using storage resources
	 * @param rateBw the price that will be charged by bandwidth resources
	 * @param rateEnergy the price that will be charged by spending energy
	 * @param idlePower the power value while using the full processing capacity of the machine
	 * @param busyPower the power value while using no processing resources in the machine
	 * @param movement the movement of the machine
	 * @param appId the application name; can be empty
	 * @param distribution the distribution of the sensor; can be null
	 */
	public Node(String name, int level, double mips, int ram, long storage, double rateMips, double rateRam, double rateStorage,
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
		
		coord = new Location();
	}
	
	/**
	 * Changes the values of the virtual machine node for the graph.
	 * 
	 * @param name the name of the fog device
	 * @param level the position at the graphical interface
	 * @param mips the available processing resources of the machine
	 * @param ram the available memory in the machine
	 * @param storage the available storage in the machine
	 * @param rateMips the price that will be charged by using processing resources
	 * @param rateRam the price that will be charged by using memory resources
	 * @param rateStorage the price that will be charged by using storage resources
	 * @param rateBw the price that will be charged by bandwidth resources
	 * @param rateEnergy the price that will be charged by spending energy
	 * @param idlePower the power value while using the full processing capacity of the machine
	 * @param busyPower the power value while using no processing resources in the machine
	 * @param movement the movement of the machine
	 * @param appId the application name; can be empty
	 * @param distribution the distribution of the sensor; can be null
	 */
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
	
	/**
	 * Gets the name of the fog device.
	 * 
	 * @return the name of the fog device
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the fog device.
	 * 
	 * @param name the name of the fog device
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the position at the graphical interface (it is not used by the simulation itself).
	 * 
	 * @return the position at the graphical interface
	 */
	public int getLevel() {
		return level;
	}
	
	/**
	 * Sets the position at the graphical interface (it is not used by the simulation itself).
	 *  
	 * @param level the position at the graphical interface
	 */
	public void setLevel(int level) {
		this.level = level;
	}
	
	/**
	 * Gets the available processing resources of the machine.
	 * 
	 * @return the available processing resources of the machine
	 */
	public double getMips() {
		return mips;
	}
	
	/**
	 * Sets the available processing resources of the machine.
	 * 
	 * @param mips the available processing resources of the machine
	 */
	public void setMips(double mips) {
		this.mips = mips;
	}
	
	/**
	 * Gets the available memory in the machine.
	 * 
	 * @return the available memory in the machine
	 */
	public int getRam() {
		return ram;
	}
	
	/**
	 * Sets the available memory in the machine.
	 * 
	 * @param ram the available memory in the machine
	 */
	public void setRam(int ram) {
		this.ram = ram;
	}
	
	/**
	 * Gets the available storage in the machine.
	 * 
	 * @return the available storage in the machine
	 */
	public long getStorage() {
		return storage;
	}
	
	/**
	 * Sets the available storage in the machine.
	 * 
	 * @param storage the available storage in the machine
	 */
	public void setStorage(long storage) {
		this.storage = storage;
	}
	
	/**
	 * Gets the available bandwidth in the machine.
	 * 
	 * @return the available bandwidth in the machine
	 */
	public double getBw() {
		return bw;
	}
	
	/**
	 * Sets the available bandwidth in the machine.
	 * 
	 * @param bw the available bandwidth in the machine
	 */
	public void setBw(double bw) {
		this.bw = bw;
	}
	
	/**
	 * Gets the price that will be charged by using processing resources.
	 * 
	 * @return the price that will be charged by using processing resources
	 */
	public double getRateMips() {
		return rateMips;
	}
	
	/**
	 * Sets the price that will be charged by using processing resources.
	 * 
	 * @param rateMips the price that will be charged by using processing resources
	 */
	public void setRateMips(double rateMips) {
		this.rateMips = rateMips;
	}
	
	/**
	 * Gets the price that will be charged by using memory resources.
	 * 
	 * @return the price that will be charged by using memory resources
	 */
	public double getRateRam() {
		return rateRam;
	}
	
	/**
	 * Sets the price that will be charged by using memory resources.
	 * 
	 * @param rateRam the price that will be charged by using memory resources
	 */
	public void setRateRam(double rateRam) {
		this.rateRam = rateRam;
	}
	
	/**
	 * Gets the price that will be charged by using storage resources.
	 * 
	 * @return the price that will be charged by using storage resources
	 */
	public double getRateStorage() {
		return rateStorage;
	}
	
	/**
	 * Sets the price that will be charged by using storage resources.
	 * 
	 * @param rateStorage the price that will be charged by using storage resources
	 */
	public void setRateStorage(double rateStorage) {
		this.rateStorage = rateStorage;
	}
	
	/**
	 * Gets the price that will be charged by bandwidth resources.
	 * 
	 * @return the price that will be charged by bandwidth resources
	 */
	public double getRateBw() {
		return rateBw;
	}

	/**
	 * Sets the price that will be charged by bandwidth resources.
	 * 
	 * @param rateBw the price that will be charged by bandwidth resources
	 */
	public void setRateBw(double rateBw) {
		this.rateBw = rateBw;
	}
	
	/**
	 * Gets the price that will be charged by spending energy.
	 * 
	 * @return the price that will be charged by spending energy
	 */
	public double getRateEnergy() {
		return rateEnergy;
	}
	
	/**
	 * Sets the price that will be charged by spending energy.
	 * 
	 * @param rateEnergy the price that will be charged by spending energy
	 */
	public void setRateEnergy(double rateEnergy) {
		this.rateEnergy = rateEnergy;
	}
	
	/**
	 * Gets the power value while using the full processing capacity of the machine.
	 * 
	 * @return the power value while using the full processing capacity of the machine
	 */
	public double getIdlePower() {
		return idlePower;
	}
	
	/**
	 * Sets the power value while using the full processing capacity of the machine.
	 * 
	 * @param idlePower the power value while using the full processing capacity of the machine
	 */
	public void setIdlePower(double idlePower) {
		this.idlePower = idlePower;
	}
	
	/**
	 * Gets the power value while using no processing resources in the machine.
	 * 
	 * @return the power value while using no processing resources in the machine
	 */
	public double getBusyPower() {
		return busyPower;
	}
	
	/**
	 * Sets the power value while using no processing resources in the machine.
	 * 
	 * @param busyPower the power value while using no processing resources in the machine
	 */
	public void setBusyPower(double busyPower) {
		this.busyPower = busyPower;
	}
	
	/**
	 * Gets the the movement of the machine.
	 * 
	 * @return the movement of the machine
	 */
	public Movement getMovement() {
		return movement;
	}
	
	/**
	 * Sets the movement of the machine.
	 * 
	 * @param movement the movement of the machine
	 */
	public void setMovement(Movement movement) {
		this.movement = movement;
	}
	
	/**
	 * Gets the application name.
	 * 
	 * @return the application name; can be empty for non clients
	 */
	public String getApplication() {
		return application;
	}
	
	/**
	 * Sets the application name.
	 * 
	 * @param application the application name; can be empty for non clients
	 */
	public void setApplication(String application) {
		this.application = application;
	}
	
	/**
	 * Gets the distribution of the sensor.
	 * 
	 * @return the distribution of the sensor; can be null for non clients
	 */
	public Distribution getDistribution() {
		return this.distribution;
	}
	
	/**
	 * Sets the distribution of the sensor.
	 * 
	 * @param distribution the distribution of the sensor
	 */
	public void setDistribution(Distribution distribution) {
		this.distribution = distribution;
	}
	
	/**
	 * Gets the sensor distribution type.
	 * 
	 * @return the sensor distribution type
	 */
	public int getDistributionType(){
		return distribution.getDistributionType();
	}
	
	/**
	 * Gets the GUI coordinates of the node
	 * 
	 * @return the coordinates of the node
	 */
	public Location getCoordinate() {
		return coord;
	}
	
	/**
	 * Sets the GUI coordinates of the node.
	 * 
	 * @param coord the coordinates of the node
	 */
	public void setCoordinate(Location coord) {
		this.coord.setX(coord.getX());
		this.coord.setY(coord.getY());
	}
	
}
