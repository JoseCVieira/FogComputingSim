package org.fog.test;

import java.util.LinkedHashSet;

import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Util;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.movement.Location;
import org.fog.utils.movement.Movement;
import org.fog.utils.movement.StaticMovement;

/**
 * Class which defines an example topology to test the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class TEMPFog extends Topology {
	/** Number of router gateways which are responsible to connect the clients to the fog network */
	private static final int numOfRouters = 1;
	
	/** Number of mobile users connected to each router gateway */
	private static final int numOfMobilesPerRouter = 2;
	
	/** Parameter (mean) which defines the distribution time interval between tuples sent by the sensors */
	private static final double TEMP_TRANSMISSION_TIME_MEAN = 10;
	
	/** Parameter (deviation) which defines the distribution time interval between tuples sent by the sensors */
	private static final double TEMP_TRANSMISSION_TIME_DEV = 2;
	
	/**
	 * Creates a new topology.
	 */
	public TEMPFog() {
		super("Generating DCNS topology...");
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
		FogDevice cloud = createFogDevice("cloud", 100000, 10240, 1000000, 1000, 16*103, 16*83.25, 10, 0.05, 0.001, 0.0, 0.05, movement);
		
		// Add the cloud to the physical topology
		fogDevices.add(cloud);
		
		for(int i = 0; i < numOfRouters; i++){
			double posx = Util.rand(-500, 500);
			double posy = Util.rand(250, 500);
			
			// Create the movement for the router
			movement = new StaticMovement(new Location(posx, posy));
			
			// Create the router device
			FogDevice dept = createFogDevice("d-"+i, 1000, 1024, 1000000, 1000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0, 0.05, movement);
			
			// Add the router to the physical topology
			fogDevices.add(dept);
			
			// Create a connection (link) between the cloud and the router and vice versa
			connectFogDevices(cloud, dept, 50.0, 50.0, 1000.0, 1000.0);
			
			// Repeat the process for the next nodes
			for(int j = 0; j < numOfMobilesPerRouter; j++){
				posx = Util.rand(-500, 500);
				posy = Util.rand(400, 600);
				movement = new StaticMovement(new Location(posx, posy));
				
				FogDevice mobile = createClientDevice("m-"+i+"-"+j, 1000, 1024, 1000000, 1000, movement);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 10.0, 10.0, 1000.0, 1000.0);
			}
		}
	}
	
	@Override
	protected void createClients() {
		Application app = ApplicationsExample.getAppExampleByName("TEMP");
		String sensorName = "TEMP:";
		String actuatorName = "MOTOR:";
		
		// For each fog node
		for(FogDevice fogDevice : fogDevices) {
			
			// Which, in this example, needs to start with the character "m"
			if(fogDevice.getName().startsWith("m")) {
				
				// Create a normal distribution for its sensor
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