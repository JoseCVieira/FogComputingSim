package org.fog.gui.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.test.ApplicationsExample;
import org.fog.utils.Movement;

/**
 * Class which converts the topology defined in the GUI to the one to be tested within the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class RunGUI extends Topology {
	private final Graph graph;
	
	/**
	 * Converts the topology defined in the GUI.
	 * 
	 * @param graph the object which holds the topology
	 */
	public RunGUI(final Graph graph){
		super();
		this.graph = graph;
		
		createExampleApplications();
		createFogDevices();
		createClients();
		deployApplications();
	}
	
	/**
	 * Creates the fog devices defined in the GUI.
	 */
	@Override
	protected void createFogDevices() {
		for(Node node : graph.getDevicesList().keySet()) {
			fogDevices.add(createFogDevice(node));
		}
		
		for (Entry<Node, List<Link>> entry : graph.getDevicesList().entrySet()) {			
			Node fog1 = entry.getKey();
			FogDevice f1 = getFogDeviceByName(fog1.getName());
			
			for (Link edge : entry.getValue()) {
				Node fog2 = edge.getNode();
				FogDevice f2 = getFogDeviceByName(fog2.getName());
				
				f2.getLatencyMap().put(f1.getId(), edge.getLatency());
				f1.getLatencyMap().put(f2.getId(), edge.getLatency());
				
				f2.getBandwidthMap().put(f1.getId(), edge.getBandwidth());
				f1.getBandwidthMap().put(f2.getId(), edge.getBandwidth());
				
				f1.getTupleQueue().put(f2.getId(), new LinkedList<Pair<Tuple, Integer>>());
				f2.getTupleQueue().put(f1.getId(), new LinkedList<Pair<Tuple, Integer>>());
				
				f1.getTupleLinkBusy().put(f2.getId(), false);
				f2.getTupleLinkBusy().put(f1.getId(), false);
			}
		}
	}
	
	/**
	 * Creates the fog device itself.
	 * 
	 * @param fog the GUI fog device
	 * @return the device to be used within the simulation
	 */
	private FogDevice createFogDevice(Node fog) {
		String name = fog.getName();
		double mips = fog.getMips();
		int ram = fog.getRam();
		long strg = fog.getStorage();
		long bw = (long) fog.getBw();
		double bPw = fog.getBusyPower();
		double iPw = fog.getIdlePower();
		double costPerMips = fog.getRateMips();
		double costPerMem = fog.getRateRam();
		double costPerStorage = fog.getRateStorage();
		double costPerBw = fog.getRateBw();
		double costPerEnergy = fog.getRateEnergy();
		Movement movement = fog.getMovement();
		boolean client = fog.getApplication().equals("") ? false : true;
		
		if(!client)
			return createFogDevice(name, mips, ram, strg, bw, bPw, iPw, costPerMips,
					costPerMem,costPerStorage, costPerBw, costPerEnergy, movement);
		else
			return createClientDevice(name, mips, ram, strg, bw, movement);
	}
	
	/**
	 * Creates a list containing all the clients nodes (i.e., the fog devices with some applications to be deployed).
	 * 
	 * @return the list containing all the clients nodes
	 */
	private ArrayList<Node> getClients(){
		ArrayList<Node> clients = new ArrayList<Node>();
		
		for(Node node : graph.getDevicesList().keySet())
			if(node.getApplication().length() > 0)
				clients.add(node);
		
		return clients;
	}
	
	/**
	 * Creates the sensors and actuators to be added to the clients (one sensor and actuator per application). Also,
	 * it adds the applications to the map which contains the fog devices and the corresponding applications which
	 * they want to deploy.
	 */
	@Override
	protected void createClients() {
		for(Node client : getClients()) {
			String tupleType = "";
			String actuatorType = "";
			
			for(Application application : graph.getAppList()) {
				if(application.getAppId().equals(client.getApplication())){
					for(AppEdge appEdge : application.getEdges()) {
						if(appEdge.getEdgeType() == AppEdge.SENSOR)
							tupleType = appEdge.getSource();
						else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
							actuatorType = appEdge.getDestination();
					}
				}
			}
			
			String appName = client.getApplication();
			FogDevice fogDevice = getFogDeviceByName(client.getName());
			int userId = fogDevice.getId();
			
			sensors.add(new Sensor(client.getName() + "_sensor", tupleType + "_" + userId, userId, appName, client.getDistribution(), userId));
			actuators.add(new Actuator(client.getName() + "_actuator", userId, appName, userId, actuatorType + "_" + userId));
			
			if(!appToFogMap.containsKey(fogDevice.getName())) {
				LinkedHashSet<String> appList = new LinkedHashSet<String>();
				appList.add(appName);
				appToFogMap.put(fogDevice.getName(), appList);
			}else {
				appToFogMap.get(fogDevice.getName()).add(appName);
			}
		}
	}
	
	/**
	 * Adds the defined applications inside the GUI to the example applications list.
	 */
	protected void createExampleApplications(){
		for(Application app : graph.getAppList()) {
			ApplicationsExample.addApplicationExample(app);
		}
	}
	
}
