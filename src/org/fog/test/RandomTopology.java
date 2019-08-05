package org.fog.test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.Client;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.gui.GuiConfig;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

/**
 * Class which defines a random example topology to test the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class RandomTopology extends Topology {
	/** Number of fog devices generated in this topology */
	private static final int NR_FOG_DEVICES = 5;
	
	/** Maximum latency generated between two nodes */
	private static final int MAX_CONN_LAT = 100;
	
	/** Maximum bandwidth generated between two nodes */
	private static final int MAX_CONN_BW = 10000;
	
	/** Probability of creating a connection between two nodes */
	private static final double CONNECTION_PROB = 0.4;
	
	/** Probability of being a client (i.e., to have an application to be deployed) */
	private static final double CLIENT_PROBABILITY = 0.4;
	
	/** Deviation of the normal distribution defined to the resources */
	private static final double RESOURCES_DEV = 100;
	
	/** Deviation of the normal distribution defined to the energy */
	private static final double ENERGY_DEV = 5;
	
	/** Deviation of the normal distribution defined to the cost */
	private static final double COST_DEV = 1E-5;
	
	/**
	 * Creates a new topology.
	 */
	public RandomTopology() {
		super("Generating a new random topology...");
	}
	
	/**
	 * Creates the fog nodes which compose the physical topology. Note that the connections at this point
	 * should only be created between fixed nodes once, mobile connections are updated during the simulation.
	 */
	@Override
	protected void createFogDevices() {
		// Create the movement for the cloud
		// Does not matter what direction because velocity is 0
		Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
		
		// Create the cloud device (cloud is seen as a single node)
		FogDevice cloud = createFogDevice("Cloud", Double.MAX_VALUE, (int) Constants.INF, (int) Constants.INF, (int) Constants.INF,
				16*GuiConfig.BUSY_POWER, 16*GuiConfig.IDLE_POWER, GuiConfig.RATE_MIPS, GuiConfig.RATE_RAM,
				GuiConfig.RATE_STRG, GuiConfig.RATE_BW, GuiConfig.RATE_EN, movement, false);
		
		// Add the cloud to the physical topology
		fogDevices.add(cloud);
		
		int iter = 1;
		int nrFogNodes = NR_FOG_DEVICES - 1;
		
		// While there are some nodes which have been not created yet
		while(nrFogNodes > 0) {
			
			// Generate a random number of nodes
			int nr = Util.rand(0, nrFogNodes);
			nrFogNodes -= nr;
			
			// For each node
			for(int i = 0; i < nr; i++) {
				// Generate the quantity of resources
				double mips = Util.normalRand(GuiConfig.MIPS/iter, RESOURCES_DEV/iter);
				double ram = Util.normalRand(GuiConfig.RAM/iter, RESOURCES_DEV/iter);
				double strg = Util.normalRand(GuiConfig.STRG/iter, RESOURCES_DEV/iter);
				double bw = Util.normalRand(GuiConfig.BW/iter, RESOURCES_DEV/iter);
				
				double bPw = Util.normalRand(GuiConfig.BUSY_POWER, ENERGY_DEV);
				double iPw = Util.normalRand(GuiConfig.IDLE_POWER, ENERGY_DEV);
				
				double rateMips = Util.normalRand(GuiConfig.RATE_MIPS, COST_DEV);
				double rateRam = Util.normalRand(GuiConfig.RATE_RAM, COST_DEV);
				double rateStrg = Util.normalRand(GuiConfig.RATE_STRG, COST_DEV);
				double rateBw = Util.normalRand(GuiConfig.RATE_BW, COST_DEV);
				double rateEn = Util.normalRand(GuiConfig.RATE_EN, COST_DEV);
				
				double posx = Util.rand(-500, 500);
				double posy = Util.rand(250, 500);
				
				// Generate its movement
				movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
				
				// Define if its a client or not
				boolean client = false;
				if(new Random().nextFloat() < CLIENT_PROBABILITY) {
					client = true;
				}
				
				// If its the last one and there are no clients, then it creates a client
				boolean found = false;
				if(!client && nrFogNodes == 0 && i == nr - 1) {
					for(FogDevice fogDevice : fogDevices) {
						if(fogDevice instanceof Client) {
							found = true;
							break;
						}
					}
					
					if(!found)
						client = true;
				}
				
				// Create the device
				FogDevice fogDevice = createFogDevice("L"+iter+":F"+i, mips, (int) ram, (long) strg, (long) bw, bPw, iPw,
						rateMips, rateRam, rateStrg, rateBw, rateEn, movement, client);
				
				// Add the device to the physical topology
				fogDevices.add(fogDevice);			
			}
			
			iter++;
		}
		
		// After the creation, connect the devices
		connectDevices();
	}
	
	/**
	 * Connect the fog devices based on the defined probability. Note that its not necessary to connect all devices once
	 * the ones which are not connected are seen as mobile nodes, and the mobile connections are automatically created and
	 * removed during the simulation
	 */
	private void connectDevices() {
		List<FogDevice> notConnctedDevices = new ArrayList<FogDevice>();
		notConnctedDevices.addAll(fogDevices);
		
		int fIndex = Util.rand(1, fogDevices.size() - 1);
		
		FogDevice cloud = fogDevices.get(0);
		FogDevice f = fogDevices.get(fIndex);
		
		notConnctedDevices.remove(cloud);
		notConnctedDevices.remove(f);
		
		double latUp = (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT);
		double latDown = (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT);
		double bwUp = (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW);
		double bwDown = (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW);
		
		
		// Connect the cloud to some other node
		connectFogDevices(cloud, f, latUp, latDown, bwUp, bwDown);
		
		// Randomly connect the remaining nodes
		for(FogDevice f1 : notConnctedDevices) {
			for(FogDevice f2 : notConnctedDevices) {
				if(f1.getId() == f2.getId()) continue;
				if(new Random().nextFloat() >= CONNECTION_PROB) continue;
				
				latUp = (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT);
				latDown = (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT);
				bwUp = (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW);
				bwDown = (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW);
				
				connectFogDevices(cloud, f, latUp, latDown, bwUp, bwDown);
				
				notConnctedDevices.remove(f1);
				notConnctedDevices.remove(f2);
			}
		}
	}
	
	/**
	 * Creates the clients which compose the physical topology along with their sensors and actuators and applications.
	 * Note that each user application requires a pair of sensor and actuator.
	 */
	@Override
	protected void createClients() {
		// For each node
		for(FogDevice fogDevice : fogDevices) {
			// If it is a client
			if(!(fogDevice instanceof Client)) continue;
			
			// Create a normal distribution
			Distribution distribution = new DeterministicDistribution(Util.normalRand(GuiConfig.SENSOR_DESTRIBUTION, 1.0));
			
			String clientName = fogDevice.getName();
			int userId = fogDevice.getId();
			
			int appIndex = new Random().nextInt(ApplicationsExample.getNumberOfApplicationsExample());
			Application app = ApplicationsExample.getAppExampleByIndex(appIndex);
			
			String appName = app.getAppId();
			String sensorType = "", actuatorType = "";
			
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.SENSOR)
					sensorType = appEdge.getSource();
				else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
					actuatorType = appEdge.getDestination();
			}
			
			// Add its sensor to the list of sensors
			sensors.add(new Sensor("Sensor:" + clientName + ":" + app.getAppId(), sensorType + "_" + userId, userId, appName, distribution, userId));
			
			// Add its actuator to the list of actuators
			actuators.add(new Actuator("Actuator:" + clientName  + ":" + app.getAppId(), userId, appName, userId, actuatorType + "_" + userId));
			
			// Add the application to the map of applications to be deployed
			if(!appToFogMap.containsKey(fogDevice.getName())) {
				LinkedHashSet<String> appList = new LinkedHashSet<String>();
				appList.add(appName);
				appToFogMap.put(fogDevice.getName(), appList);
			}else {
				appToFogMap.get(fogDevice.getName()).add(appName);
			}
		}
	}
	
}