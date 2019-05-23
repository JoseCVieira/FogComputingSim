package org.fog.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class NewVRGameFog extends FogTest {	
	private static int numOfDepts = 4;
	private static int numOfMobilesPerDept = 6;
	private static double EEG_TRANSMISSION_TIME = 5.1;
	
	public NewVRGameFog() {
		System.out.println("Generating VRGame topology...");
		
		FogBroker broker = null;
		try {
			broker = new FogBroker("broker");
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unwanted errors happen\nFogComputingSim will terminate abruptally.\n");
			System.exit(0);
		}
		
		createExampleApplication();
		createFogDevices();
		createClients(broker);
		createController();
		
		/********************************************************************************************************************/
		
		Application application = new Application("VRGame", broker.getId());
		
		Application appExample = null;
		for(Application app : exampleApplications) {
			if(app.getAppId().equals("VRGame")) {
				appExample = app;
			}
		}
		if(appExample == null) {
			System.err.println("Unwanted errors happen\nFogComputingSim will terminate abruptally.\n");
			System.exit(0);
		}
		
		List<AppModule> globalModules = new ArrayList<AppModule>();
		for(FogDevice fogDevice : fogDevices) {
			if(appToFogMap.containsKey(fogDevice.getName())) {
				for(AppModule appModule : appExample.getModules()) {
					
					boolean found = false;
					for(AppModule alreadyPlacedModule : application.getModules()) {
						if(alreadyPlacedModule.getName().equals(appModule.getName())) {
							found = true;
						}
					}
					
					if(found) continue;
					application.addAppModule(appModule, fogDevice.getId());
					
					if(appModule.isGlobalModule()) {
						globalModules.add(appModule);
					}
				}
				
				for(AppEdge appEdge : appExample.getEdges()) {
					application.addAppEdge(appEdge, globalModules, fogDevice.getId());
				}
				
				for(AppModule appModule : appExample.getModules()) {
					for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
						FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)appModule.getSelectivityMap().get(pair));
						application.addTupleMapping(appModule.getName(), pair, fractionalSelectivity.getSelectivity(), fogDevice.getId());
					}
				}
				
				List<AppLoop> loops = new ArrayList<AppLoop>();
				for(AppLoop loop : appExample.getLoops()) {
					ArrayList<String> l = new ArrayList<String>();
					for(String name : loop.getModules()) {
						
						boolean found = false;
						for(AppModule gModule : globalModules) {
							if(gModule.getName().equals(name)) {
								found = true;
								break;
							}
						}
						
						if(found) {
							l.add(name);
						}else {
							l.add(name + "_" + fogDevice.getId());
						}
						
					}
					loops.add(new AppLoop(l));
				}
				
				if(application.getLoops() == null)
					application.setLoops(loops);
				else
					application.getLoops().addAll(loops);
			}
		}
		
		applications.add(application);
	}

	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 1000000, 10000, 16*103, 16*83.25, 0.01, 0.05, 0.001, 0.0);
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		connectFogDevices(cloud, proxy, 100.0, 100.0, 10000.0, 10000.0);
		
		for(int i = 0; i < numOfDepts; i++){
			FogDevice dept = createFogDevice("d-"+i, 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
			
			fogDevices.add(dept);
			
			connectFogDevices(proxy, dept, 4.0, 4.0, 10000.0, 10000.0);
			
			
			for(int j = 0; j < numOfMobilesPerDept; j++){
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1000, 1000000, 10000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 2.0, 2.0, 10000.0, 10000.0);
			}
		}
	}
	
	private static void createClients(FogBroker broker) {
		for(FogDevice fogDevice : fogDevices) {
			if(fogDevice.getName().startsWith("m")) {
				//createClient(/*broker, */fogDevice, "EEG:", new DeterministicDistribution(EEG_TRANSMISSION_TIME), 6.0, "DISPLAY:", 1.0);
				
				Distribution distribution =  new DeterministicDistribution(EEG_TRANSMISSION_TIME);
				double sensorLat = 6.0;
				double actuatorLat = 1.0;
				String sensorName = "EEG:";
				String actuatorName = "DISPLAY:";
				
				int appIndex = new Random().nextInt(exampleApplications.size());
				int gatewayDeviceId = fogDevice.getId();
				String clientName = fogDevice.getName();
				int userId = fogDevice.getId();
				
				String appName = exampleApplications.get(appIndex).getAppId();
				String sensorType = "", actuatorType = "";
				
				for(AppEdge appEdge : exampleApplications.get(appIndex).getEdges()) {
					if(appEdge.getEdgeType() == AppEdge.SENSOR)
						sensorType = appEdge.getSource();
					else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
						actuatorType = appEdge.getDestination();
				}
				
				sensors.add(new Sensor(sensorName + clientName, sensorType + "_" + userId, userId, appName,
						distribution, gatewayDeviceId, sensorLat));

				actuators.add(new Actuator(actuatorName + clientName, userId, appName,
						gatewayDeviceId, actuatorLat, actuatorType + "_" + userId));
				
				if(!appToFogMap.containsKey(fogDevice.getName())) {
					List<String> appList = new ArrayList<String>();
					appList.add(appName);
					appToFogMap.put(fogDevice.getName(), appList);
				}else {
					appToFogMap.get(fogDevice.getName()).add(appName);
				}
				
			}
		}
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplication() {		
		Application application = new Application("VRGame", -1);
		application.addAppModule("client", 100, true, false);
		application.addAppModule("calculator", 100, false, false);
		application.addAppModule("connector", 100, false, false);
		
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
		
		final AppLoop loop = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
	
}
