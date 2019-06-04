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
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;

public class TEMPFog extends FogTest {
	static int numOfRouters = 1;
	static int numOfMobilesPerRouter = 2;
	private static double TEMP_TRANSMISSION_TIME_MEAN = 10;
	private static double TEMP_TRANSMISSION_TIME_DEV = 2;
	
	public TEMPFog() {
		super("Generating DCNS topology...");
	}
	
	@Override
	protected void createFogDevices() {
		Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
		FogDevice cloud = createFogDevice("cloud", 100000, 10240, 1000000, 1000, 16*103, 16*83.25, 10, 0.05, 0.001, 0.0, movement, false);
		
		fogDevices.add(cloud);
		
		for(int i = 0; i < numOfRouters; i++){
			double posx = Util.rand(-500, 500);
			double posy = Util.rand(250, 500);
			
			movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
			FogDevice dept = createFogDevice("d-"+i, 1000, 1024, 1000000, 1000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0, movement, false);
			
			fogDevices.add(dept);
			
			connectFogDevices(cloud, dept, 50.0, 50.0, 1000.0, 1000.0);
			
			
			for(int j = 0; j < numOfMobilesPerRouter; j++){
				posx = Util.rand(-500, 500);
				posy = Util.rand(400, 600);
				int direction = Util.rand(Movement.EAST, Movement.SOUTHEAST);
				
				movement = new Movement(1.0, direction, new Location(posx, posy));
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1024, 1000000, 1000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0, movement, true);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 10.0, 10.0, 1000.0, 1000.0);
			}
		}
	}
	
	@Override
	protected void createClients() {
		Application app = getAppExampleByName("TEMP");
		double sensorLat = 2.0;
		double actuatorLat = 2.0;
		String sensorName = "TEMP:";
		String actuatorName = "MOTOR:";
		
		for(FogDevice fogDevice : fogDevices) {
			if(fogDevice.getName().startsWith("m")) {
				Distribution distribution =  new NormalDistribution(TEMP_TRANSMISSION_TIME_MEAN, TEMP_TRANSMISSION_TIME_DEV);
				
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