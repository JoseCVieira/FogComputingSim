package org.fog.test;

import java.util.LinkedHashSet;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Util;
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
public class StaticRandomTest extends Topology {
	/** Number of fog node gateways which are responsible to connect the clients to the fog network */
	private static final int numOfDepts = 4;
	
	/** Number of mobile users connected to each fog node gateway */
	private static final int numOfMobilesPerDept = 4;
	
	/** Parameter which defines the time interval between tuples sent by the sensors */
	private static final double EEG_TRANSMISSION_TIME = 5;
	
	/**
	 * Creates a new topology.
	 */
	public StaticRandomTest() {
		super("Generating Test Static topology...");
	}
	
	/**
	 * Creates the fog nodes which compose the physical topology. Note that the connections at this point
	 * should only be created between fixed nodes once, mobile connections are updated during the simulation.
	 */
	@Override
	protected void createFogDevices() {
		// Create the movement for the cloud
		Location l = new Location(Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE), Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE));
		Movement movement = new StaticMovement(l);
		
		// Create the cloud device (cloud is seen as a single node)
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 1000000, 16*103, 16*83.25, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
		
		// Add the cloud to the physical topology
		fogDevices.add(cloud);
		
		// Create the movement for the proxy
		l = new Location(Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE), Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE));
		movement = new StaticMovement(l);
		
		// Create the proxy device
		FogDevice proxy = createFogDevice("proxy-server", 4800, 4000, 1000000, 107.339, 83.4333, 1E-5, 1E-5, 1E-5, 1E-5, 1E-5, movement);
		
		// Add the proxy to the physical topology
		fogDevices.add(proxy);
		
		// Create a connection (link) between the cloud and the proxy and vice versa
		connectFogDevices(cloud, proxy, 100, 100, Config.FIXED_COMMUNICATION_BW, Config.FIXED_COMMUNICATION_BW);
		
		// Repeat the process for the next nodes
		for(int i = 0; i < numOfDepts; i++) {
			l = new Location(Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE), Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE));
			movement = new StaticMovement(l);
			
			FogDevice f;
			if(i == 0) {
				f = createFogDevice("d-"+i, 75, 4000, 1000000, 0, 0, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
			}else if(i != numOfDepts-1) {
				f = createFogDevice("d-"+i, 75, 4000, 1000000, 107.339, 83.4333, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
			}else {
				f = createFogDevice("d-"+i, 1000, 4000, 1000000, 107.339, 83.4333, 0.01, 0.05, 0.001, 0.05, 0.05, movement);
			}
			
			if(i != numOfDepts-1)
				connectFogDevices(proxy, f, 10, 10, 10000, 10000);
			
			
			fogDevices.add(f);
		}
		
		for(int j = 0; j < numOfMobilesPerDept; j++) {
			l = new Location(Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE), Util.rand(-Movement.SQUARE_SIDE, Movement.SQUARE_SIDE));
			movement = new StaticMovement(l);
			FogDevice mobile = createClientDevice("m-"+j, 65, 1000, 1000000, movement);
			fogDevices.add(mobile);
		}
		
	}
	
	/**
	 * Creates the clients which compose the physical topology along with their sensors and actuators and applications.
	 * Note that each user application requires a pair of sensor and actuator.
	 */
	@Override
	protected void createClients() {
		Application app = ApplicationsExample.getAppExampleByName("DCNS_TEST");
		String sensorName = "EEG:";
		String actuatorName = "DISPLAY:";
		
		// For each fog node
		for(FogDevice fogDevice : fogDevices) {
			
			// Which, in this example, needs to start with the character "m"
			if(fogDevice.getName().startsWith("m")) {
				
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
