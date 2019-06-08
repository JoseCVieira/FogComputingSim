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
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.Coverage;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Location;
import org.fog.utils.Movement;

public class RunGUI extends FogTest {
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
			if(node.getType().equals(Constants.FOG_TYPE)) {
				fogDevices.add(createFogDevice((FogDeviceGui)node));
			}
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
		List<Pe> processingElementsList = new ArrayList<Pe>();
		processingElementsList.add(new Pe(0, new PeProvisioner(fog.getMips())));

		PowerHost host = new PowerHost(
				FogUtils.generateEntityId(),
				new RamProvisioner(fog.getRam()),
				new BwProvisioner((long) fog.getBw()),
				fog.getStorage(),
				processingElementsList,
				new VmSchedulerTimeSharedOverbookingEnergy(processingElementsList),
				new FogLinearPowerModel(fog.getBusyPower(), fog.getIdlePower())
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(Constants.FOG_DEVICE_ARCH,
				Constants.FOG_DEVICE_OS, Constants.FOG_DEVICE_VMM, host, Constants.FOG_DEVICE_TIMEZONE,
				fog.getCostPerSec(), fog.getRateMips(), fog.getRateRam(), fog.getRateStorage(), fog.getRateBw());
		
		try {
			Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
			Coverage coverage = new Coverage(500);
			return new FogDevice(fog.getName(), characteristics, new AppModuleAllocationPolicy(hostList),
					new LinkedList<Storage>(), Constants.SCHEDULING_INTERVAL, movement, coverage);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
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
			SensorGui sensor = null;
			ActuatorGui actuator = null;
			String tupleType = "";
			String actuatorType = "";
			double sensorLat = -1;
			double actuatorLat = -1;
			
			for (Entry<Node, List<Link>> entry : graph.getDevicesList().entrySet()) {
				for (Link edge : entry.getValue()) {
					if(entry.getKey().equals(client)){
						if(edge.getNode().getType().equals(Constants.SENSOR_TYPE)) {
							sensor = (SensorGui)edge.getNode();
							sensorLat = edge.getLatency();
						}else if(edge.getNode().getType().equals(Constants.ACTUATOR_TYPE)) {
							actuator = (ActuatorGui)edge.getNode();
							actuatorLat = edge.getLatency();
						}
					}else if(entry.getKey().getType().equals(Constants.SENSOR_TYPE) && edge.getNode().equals(client)) {
						sensor = (SensorGui)entry.getKey();
						sensorLat = edge.getLatency();
					}else if(entry.getKey().getType().equals(Constants.ACTUATOR_TYPE) && edge.getNode().equals(client)) {
						actuator = (ActuatorGui)entry.getKey();
						actuatorLat = edge.getLatency();
					}
					
					if(sensor != null && actuator != null)
						break;
				}
				if(sensor != null && actuator != null)
					break;
			}
			
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
			
			sensors.add(new Sensor(sensor.getName() + client.getName(), tupleType + "_" + userId, userId, appName,
					sensor.getDistribution(), userId, sensorLat));
	
			actuators.add(new Actuator(actuator.getName() + client.getName(), userId, appName, userId, actuatorLat,
					actuatorType + "_" + userId));
			
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
			exampleApplications.add(new Application(app));
		}
	}
	
}
