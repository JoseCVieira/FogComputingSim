package org.fog.test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.Client;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.gui.GuiConfig;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class RandomTopology extends FogTest {
	private static final int NR_FOG_DEVICES = 5;
	
	private static final int MAX_CONN_LAT = 100;
	private static final int MAX_CONN_BW = 10000;
	
	private static final double CONNECTION_PROB = 0.4;
	private static final double CLIENT_PROBABILITY = 0.4;
	
	private static final double RESOURCES_DEV = 100;
	private static final double ENERGY_DEV = 5;
	private static final double COST_DEV = 1E-5;
	
	public RandomTopology() {
		super("Generating a new random topology...");
	}
	
	@Override
	protected void createFogDevices() {
		Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
		FogDevice cloud = createFogDevice("Cloud", Double.MAX_VALUE, (int) Constants.INF, (int) Constants.INF, (int) Constants.INF,
				16*GuiConfig.BUSY_POWER, 16*GuiConfig.IDLE_POWER, GuiConfig.RATE_MIPS, GuiConfig.RATE_RAM,
				GuiConfig.RATE_MEM, GuiConfig.RATE_BW, movement, false);
		
		fogDevices.add(cloud);
		
		int iter = 1;
		int nrFogNodes = NR_FOG_DEVICES - 1;
		
		while(nrFogNodes > 0) {
			int nr = Util.rand(0, nrFogNodes);
			nrFogNodes -= nr;
			
			for(int i = 0; i < nr; i++) {
				double mips = Util.normalRand(GuiConfig.MIPS/iter, RESOURCES_DEV/iter);
				double ram = Util.normalRand(GuiConfig.RAM/iter, RESOURCES_DEV/iter);
				double strg = Util.normalRand(GuiConfig.MEM/iter, RESOURCES_DEV/iter);
				double bw = Util.normalRand(GuiConfig.BW/iter, RESOURCES_DEV/iter);
				
				double bPw = Util.normalRand(GuiConfig.BUSY_POWER, ENERGY_DEV);
				double iPw = Util.normalRand(GuiConfig.IDLE_POWER, ENERGY_DEV);
				
				double rateMips = Util.normalRand(GuiConfig.RATE_MIPS, COST_DEV);
				double rateRam = Util.normalRand(GuiConfig.RATE_RAM, COST_DEV);
				double rateStrg = Util.normalRand(GuiConfig.RATE_MEM, COST_DEV);
				double rateBw = Util.normalRand(GuiConfig.RATE_BW, COST_DEV);
				
				double posx = Util.rand(-500, 500);
				double posy = Util.rand(250, 500);
				
				movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
				
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
				
				FogDevice fogDevice = createFogDevice("L"+iter+":F"+i, mips, (int) ram, (long) strg, (long) bw, bPw, iPw,
						rateMips, rateRam, rateStrg, rateBw, movement, client);
				
				fogDevices.add(fogDevice);			
			}
			
			iter++;
		}
		
		connectFogDevices();
	}
	
	private void connectFogDevices() {
		List<FogDevice> notConnctedDevices = new ArrayList<FogDevice>();
		List<FogDevice> connctedDevices = new ArrayList<FogDevice>();
		
		notConnctedDevices.addAll(fogDevices);
		connctedDevices.add(notConnctedDevices.get(0));
		notConnctedDevices.remove(0);
		
		while(!notConnctedDevices.isEmpty()) {
			FogDevice fogDevice = connctedDevices.get(new Random().nextInt(connctedDevices.size()));
			List<FogDevice> toRemove = new ArrayList<FogDevice>();
			
			for(FogDevice f : notConnctedDevices) {
				if(new Random().nextFloat() < CONNECTION_PROB) {
					toRemove.add(f);
					
					fogDevice.getNeighborsIds().add(f.getId());
					f.getNeighborsIds().add(fogDevice.getId());
					
					fogDevice.getLatencyMap().put(f.getId(), (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT));
					f.getLatencyMap().put(fogDevice.getId(), (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT));
					
					fogDevice.getBandwidthMap().put(f.getId(), (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW));
					f.getBandwidthMap().put(fogDevice.getId(), (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW));
					
					fogDevice.getTupleQueue().put(f.getId(), new LinkedList<Pair<Tuple, Integer>>());
					f.getTupleQueue().put(fogDevice.getId(), new LinkedList<Pair<Tuple, Integer>>());
					
					fogDevice.getTupleLinkBusy().put(f.getId(), false);
					f.getTupleLinkBusy().put(fogDevice.getId(), false);
				}
			}
			
			for(FogDevice f : toRemove) {
				notConnctedDevices.remove(f);
				connctedDevices.add(f);
			}
		}
	}
	
	@Override
	protected void createClients() {
		for(FogDevice fogDevice : fogDevices) {
			if(!(fogDevice instanceof Client)) continue;
			
			Distribution distribution = new DeterministicDistribution(Util.normalRand(GuiConfig.SENSOR_DESTRIBUTION, 1.0));
			double sensorLat = Util.normalRand(GuiConfig.SENSOR_LATENCY, 1);
			double actuatorLat = Util.normalRand(GuiConfig.ACTUATOR_LATENCY, 0.1);
			
			String clientName = fogDevice.getName();
			int userId = fogDevice.getId();
			
			int appIndex = new Random().nextInt(exampleApplications.size());
			Application app = exampleApplications.get(appIndex);
			
			String appName = app.getAppId();
			String sensorType = "", actuatorType = "";
			
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.SENSOR)
					sensorType = appEdge.getSource();
				else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
					actuatorType = appEdge.getDestination();
			}
			
			sensors.add(new Sensor("Sensor:" + clientName + ":" + app.getAppId(), sensorType + "_" + userId, userId,
					appName, distribution, userId, sensorLat));

			actuators.add(new Actuator("Actuator:" + clientName  + ":" + app.getAppId(), userId, appName,
					userId, actuatorLat, actuatorType + "_" + userId));
			
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