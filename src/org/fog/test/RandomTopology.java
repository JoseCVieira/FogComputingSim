package org.fog.test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.Config;
import org.fog.core.FogTest;
import org.fog.entities.FogDevice;
import org.fog.entities.Tuple;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class RandomTopology extends FogTest {
	
	public RandomTopology() {
		System.out.println("Generating a new random topology...");
		
		createExampleApplications();
		createFogDevices();
		connectFogDevices();
		createClients();
		createController();
		createApplications();
	}
	
	private static void createFogDevices() {
		FogDevice cloud = createFogDevice(Config.CLOUD_NAME, (int) Config.IINF, (int) Config.IINF, (int) Config.IINF, (int) Config.IINF,
				16*Config.BUSY_POWER, 16*Config.IDLE_POWER, Config.COST_PER_SEC, Config.RATE_MIPS, Config.RATE_RAM,
				Config.RATE_MEM, Config.RATE_BW);
		
		fogDevices.add(cloud);
		
		int iter = 1;
		int nrFogNodes = Config.NR_FOG_DEVICES - 1;
		
		while(nrFogNodes > 0) {
			int nr = Util.rand(0, nrFogNodes);
			nrFogNodes -= nr;
			
			for(int i = 0; i < nr; i++) {
				double mips = Util.normalRand(Config.MIPS/iter, Config.RESOURCES_DEV/iter);
				double ram = Util.normalRand(Config.RAM/iter, Config.RESOURCES_DEV/iter);
				double strg = Util.normalRand(Config.MEM/iter, Config.RESOURCES_DEV/iter);
				double bw = Util.normalRand(Config.BW/iter,Config. RESOURCES_DEV/iter);
				
				double bPw = Util.normalRand(Config.BUSY_POWER, Config.ENERGY_DEV);
				double iPw = Util.normalRand(Config.IDLE_POWER, Config.ENERGY_DEV);
				
				double rateMips = Util.normalRand(Config.RATE_MIPS, Config.COST_DEV);
				double rateRam = Util.normalRand(Config.RATE_RAM, Config.COST_DEV);
				double rateStrg = Util.normalRand(Config.RATE_MEM, Config.COST_DEV);
				double rateBw = Util.normalRand(Config.RATE_BW, Config.COST_DEV);
				
				FogDevice fogDevice = createFogDevice("L"+iter+":F"+i, mips, (int) ram, (long) strg, (long) bw, bPw, iPw,
						Config.COST_PER_SEC, rateMips, rateRam, rateStrg, rateBw);
				
				fogDevices.add(fogDevice);			
			}
			
			iter++;
		}
	}
	
	private static void connectFogDevices() {
		List<FogDevice> notConnctedDevices = new ArrayList<FogDevice>();
		List<FogDevice> connctedDevices = new ArrayList<FogDevice>();
		
		notConnctedDevices.addAll(fogDevices);
		connctedDevices.add(notConnctedDevices.get(0));
		notConnctedDevices.remove(0);
		
		while(!notConnctedDevices.isEmpty()) {
			FogDevice fogDevice = connctedDevices.get(new Random().nextInt(connctedDevices.size()));
			List<FogDevice> toRemove = new ArrayList<FogDevice>();
			
			for(FogDevice f : notConnctedDevices) {
				if(new Random().nextFloat() < Config.CONNECTION_PROB) {
					toRemove.add(f);
					
					fogDevice.getNeighborsIds().add(f.getId());
					f.getNeighborsIds().add(fogDevice.getId());
					
					fogDevice.getLatencyMap().put(f.getId(), (double) Util.rand(Config.MAX_CONN_LAT/3, Config.MAX_CONN_LAT));
					f.getLatencyMap().put(fogDevice.getId(), (double) Util.rand(Config.MAX_CONN_LAT/3, Config.MAX_CONN_LAT));
					
					fogDevice.getBandwidthMap().put(f.getId(), (double) Util.rand(Config.MAX_CONN_BW/3, Config.MAX_CONN_BW));
					f.getBandwidthMap().put(fogDevice.getId(), (double) Util.rand(Config.MAX_CONN_BW/3, Config.MAX_CONN_BW));
					
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
	
	private static void createClients() {
		int nrApps = 0;
		
		while(nrApps == 0) {
			for(FogDevice fogDevice : fogDevices) {
				if(fogDevice.getName().equals(Config.CLOUD_NAME)) continue;
				
				if(new Random().nextFloat() < Config.DEPLOY_APP_PROB) {
					Distribution sensorDist = new DeterministicDistribution(Util.normalRand(Config.SENSOR_DESTRIBUTION, 1.0));
					double sensorLat = Util.normalRand(Config.SENSOR_LATENCY, 1);
					double actuatorLat = Util.normalRand(Config.ACTUATOR_LATENCY, 0.1);
					
					createClient(fogDevice, "Sensor:", sensorDist, sensorLat, "Actuator:", actuatorLat);
					nrApps++;
				}
			}
		}
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplications() {		
		Application application = new Application("VRGame", -1);
		application.addAppModule("client", 100, true);
		application.addAppModule("calculator", 100, false);
		application.addAppModule("connector", 100, false);
		
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", AppEdge.SENSOR);
		application.addAppEdge("client", "calculator", 3500, 500, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("calculator", "client", 14, 500, "CONCENTRATION", AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("DCNS", -1);
		application.addAppModule("object_detector", 100, false);
		application.addAppModule("motion_detector", 100, true);
		application.addAppModule("object_tracker", 100, false);
		application.addAppModule("user_interface", 100, false);
		
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", AppEdge.SENSOR);
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", AppEdge.MODULE);
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", AppEdge.MODULE);
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", AppEdge.MODULE);
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", AppEdge.ACTUATOR);
		
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05));
		
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});
		loops = new ArrayList<AppLoop>(){{add(loop2);add(loop3);}};
		exampleApplications.add(application);
		
		application = new Application("TEMP", -1);
		application.addAppModule("client", 100, false);
		application.addAppModule("classifier", 100, false);
		application.addAppModule("tuner", 100, false);
	
		application.addAppEdge("TEMP", "client", 1000, 100, "TEMP", AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 8000, 100, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000000, 100, "HISTORY", AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", AppEdge.MODULE);
		application.addAppEdge("client", "MOTOR", 1000, 100, "ACTUATOR", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "TEMP", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
		
		final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}});
		final AppLoop loop5 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		loops = new ArrayList<AppLoop>(){{add(loop4);add(loop5);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
	
}