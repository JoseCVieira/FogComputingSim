package org.fog.test;

import java.util.LinkedHashSet;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.movement.Location;
import org.fog.utils.movement.Movement;
import org.fog.utils.movement.StaticMovement;

/**
 * Class which defines an example topology to test the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ValidationTest2 extends Topology {	
	/** Parameter which defines the time interval between tuples sent by the sensors */
	private static final double EEG_TRANSMISSION_TIME = 5;
	
	/**
	 * Creates a new topology.
	 */
	public ValidationTest2() {
		super("Generating Test2 topology...");
	}
	
	/**
	 * Creates the fog nodes which compose the physical topology. Note that the connections at this point
	 * should only be created between fixed nodes once, mobile connections are updated during the simulation.
	 */	
	@Override
	protected void createFogDevices() {
		// Create the movement for the cloud
		Movement movement = new StaticMovement(new Location(0, 0));
		
		// Create the cloud device (cloud is seen as a single node)
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 1000000, 1.648, 1.332, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
		
		// Add the cloud to the physical topology
		fogDevices.add(cloud);
		
		// Create the proxy device
		FogDevice proxy = createFogDevice("proxy-server", 1000, 40000, 1000000, 0.107339, 0.0834333, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5, movement);
		
		// Add the proxy to the physical topology
		fogDevices.add(proxy);
		
		// Create a connection (link) between the cloud and the proxy and vice versa
		connectFogDevices(cloud, proxy, 100, 100, 10000, 10000);
		
		
		FogDevice f1 = createFogDevice("f-1", 75, 28000, 1000000, 0.05, 0.038, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
		FogDevice f2 = createFogDevice("f-2", 75, 28000, 1000000, 0, 0, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
		
		fogDevices.add(f1);
		fogDevices.add(f2);
		
		connectFogDevices(proxy, f1, 1, 1, 10000, 10000);
		connectFogDevices(proxy, f2, 1, 1, 10000, 10000);
		connectFogDevices(f1, f2, 6, 6, 10000, 10000);
		
		FogDevice c = createClientDevice("m-1", 64, 1000, 1000000, movement);
		connectFogDevices(f1, c, 6, 6, 10000, 10000);
		fogDevices.add(c);
		
		c = createClientDevice("m-2", 1, 100000, 1000000, movement);
		connectFogDevices(f2, c, 3, 3, 10000, 10000);
		fogDevices.add(c);
	}
	
	/**
	 * Creates the clients which compose the physical topology along with their sensors and actuators and applications.
	 * Note that each user application requires a pair of sensor and actuator.
	 */
	@Override
	protected void createClients() {		
		// For each fog node
		for(FogDevice fogDevice : fogDevices) {
			
			// Which, in this example, needs to start with the character "m"
			if(fogDevice.getName().startsWith("m")) {
				
				Application app;
				String sensorName;
				String actuatorName;
				if(fogDevice.getName().startsWith("m-1")) {
					app = ApplicationsExample.getAppExampleByName("VRGame_TEST");
					sensorName = "EEG:";
					actuatorName = "DISPLAY:";
				}else {
					app = ApplicationsExample.getAppExampleByName("DCNS_TEST");
					sensorName = "CAMERA:";
					actuatorName = "PTZ_CONTROL:";
				}
				
				// Create a deterministic distribution for its sensor
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
				
				// Add its sensor to the list of sensors
				sensors.add(new Sensor(sensorName + clientName, sensorType + "_" + userId, userId, appName, distribution, userId));
				
				// Add its actuator to the list of actuators
				actuators.add(new Actuator(actuatorName + clientName, userId, appName, userId, actuatorType + "_" + userId));
				
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
	
}
