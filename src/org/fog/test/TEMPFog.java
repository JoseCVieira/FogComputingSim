package org.fog.test;

import java.util.LinkedHashSet;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
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
		FogDevice cloud = createFogDevice("cloud", 100000, 10240, 1000000, 1000, 16*103, 16*83.25, 10, 0.05, 0.001, 0.0);
		
		fogDevices.add(cloud);
		
		for(int i = 0; i < numOfRouters; i++){
			FogDevice dept = createFogDevice("d-"+i, 1000, 1024, 1000000, 1000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
			
			fogDevices.add(dept);
			
			connectFogDevices(cloud, dept, 50.0, 50.0, 1000.0, 1000.0);
			
			
			for(int j = 0; j < numOfMobilesPerRouter; j++){
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1024, 1000000, 1000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0);
				
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