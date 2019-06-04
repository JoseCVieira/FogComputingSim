package org.fog.test;

import java.util.LinkedHashSet;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class VRGameFog extends FogTest {
	private static int numOfDepts = 1;//4;
	private static int numOfMobilesPerDept = 1;//6;
	private static double EEG_TRANSMISSION_TIME = 5.1;
	
	public VRGameFog() {
		super("Generating VRGame topology...");
	}
	
	@Override
	protected void createFogDevices() {		
		// Does not matter what direction because velocity is 0
		Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 1000000, 10000, 16*103, 16*83.25, 0.01, 0.05, 0.001, 0.0, movement, false);
		
		movement = new Movement(0.0, Movement.EAST, new Location(0, 250));
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0, movement, false);
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		connectFogDevices(cloud, proxy, 100.0, 100.0, 10000.0, 10000.0);
		
		for(int i = 0; i < numOfDepts; i++){
			double posx = Util.rand(-500, 500);
			double posy = Util.rand(250, 500);
			
			movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
			FogDevice dept = createFogDevice("d-"+i, 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0, movement, false);
			
			fogDevices.add(dept);
			
			connectFogDevices(proxy, dept, 4.0, 4.0, 10000.0, 10000.0);
			
			
			for(int j = 0; j < numOfMobilesPerDept; j++){
				posx = Util.rand(-500, 500);
				posy = Util.rand(400, 600);
				int direction = Util.rand(Movement.EAST, Movement.SOUTHEAST);
				
				movement = new Movement(1.0, direction, new Location(posx, posy));
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1000, 1000000, 10000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0, movement, true);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 2.0, 2.0, 10000.0, 10000.0);
			}
		}
	}
	
	@Override
	protected void createClients() {
		Application app = getAppExampleByName("VRGame_MP");
		double sensorLat = 6.0;
		double actuatorLat = 1.0;
		String sensorName = "EEG:";
		String actuatorName = "DISPLAY:";
		
		for(FogDevice fogDevice : fogDevices) {
			if(fogDevice.getName().startsWith("m")) {
				Distribution distribution =  new DeterministicDistribution(EEG_TRANSMISSION_TIME);
				
				String clientName = fogDevice.getName();
				int userId = fogDevice.getId();
				
				String appName = app.getAppId();
				String sensorType = "", actuatorType = "";
				
				for(AppEdge appEdge : app.getEdges()) {
					if(appEdge.getEdgeType() == AppEdge.SENSOR)
						sensorType = appEdge.getSource();
					else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
						actuatorType = appEdge.getDestination();
				}
				
				sensors.add(new Sensor(sensorName + clientName, sensorType + "_" + userId, userId, appName,
						distribution, userId, sensorLat));

				actuators.add(new Actuator(actuatorName + clientName, userId, appName,
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
	
}
