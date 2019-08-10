package org.fog.gui.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;
import org.fog.application.AppEdge;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.core.Topology;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.test.ApplicationsExample;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Location;
import org.fog.utils.Movement;

public class RunGUI extends Topology {
	private final Graph graph;
	
	public RunGUI(final Graph graph){
		super();
		this.graph = graph;
		
		createExampleApplications();
		createFogDevices();
		createClients();
		deployApplications();
	}
	
	@Override
	protected void createFogDevices() {
		for(Node node : graph.getDevicesList().keySet()) {
			fogDevices.add(createFogDevice((FogDeviceGui)node));
		}
		
		for (Entry<Node, List<Link>> entry : graph.getDevicesList().entrySet()) {
			if(!entry.getKey().getType().equals(Constants.FOG_TYPE))
				continue;
			
			FogDeviceGui fog1 = (FogDeviceGui)entry.getKey();
			FogDevice f1 = getFogDeviceByName(fog1.getName());
			
			for (Link edge : entry.getValue()) {
				if(!edge.getNode().getType().equals(Constants.FOG_TYPE))
					continue;						
					
				FogDeviceGui fog2 = (FogDeviceGui)edge.getNode();
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
	private FogDevice createFogDevice(FogDeviceGui fog) {
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
		
		return createFogDevice(name, mips, ram, strg, bw, bPw, iPw, costPerMips, costPerMem, costPerStorage, costPerBw, costPerEnergy, movement, client);
	}
	
	private ArrayList<FogDeviceGui> getClients(){
		ArrayList<FogDeviceGui> clients = new ArrayList<FogDeviceGui>();
		
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Constants.FOG_TYPE))
    			if(((FogDeviceGui)node).getApplication().length() > 0)
    				clients.add((FogDeviceGui)node);
		
		return clients;
	}
	
	@Override
	protected void createClients() {
		for(FogDeviceGui client : getClients()) {
			String tupleType = "";
			String actuatorType = "";
			
			for(ApplicationGui applicationGui : graph.getAppList()) {
				if(applicationGui.getAppId().equals(client.getApplication())) {
					for(AppEdge appEdge : applicationGui.getEdges()) {
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
	
	protected void createExampleApplications(){
		for(ApplicationGui app : graph.getAppList()) {
			ApplicationsExample.exampleApplications.add(new Application(app));
		}
	}
	
}
