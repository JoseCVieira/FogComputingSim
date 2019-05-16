package org.fog.gui.core;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.Constants;
import org.fog.core.FogTest;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

public class RunGUI extends FogTest {
	public RunGUI(final Graph graph){
		try {			
			createFogDevices(graph);
			
			ArrayList<FogDeviceGui> clients = getClients(graph);
			
			for(FogDeviceGui fog : clients) {
				FogBroker broker = new FogBroker(fog.getName());
				createSensorActuator(graph, fog.getName(), broker.getId(), fog.getApplication());
				fogBrokers.add(broker);
			}
			
			controller = new Controller("master-controller", fogDevices, sensors, actuators);
			
			for(FogDevice fogDevice : fogDevices)
				fogDevice.setController(controller);
			
			for(FogDeviceGui fog : clients) {
				FogBroker broker = getFogBrokerByName(fog.getName());
				Application application = createApplication(graph, fog.getApplication(), broker.getId());
				application.setClientId(getFogDeviceByName(fog.getName()).getId());
				
				for(FogDevice fogDevice : fogDevices) {
					if(fogDevice.getName().equals(fog.getName())) {
						if(appToFogMap.containsKey(fogDevice.getName()))
							appToFogMap.get(fogDevice.getName()).add(fog.getApplication());
						else {
							List<String> appList = new ArrayList<String>();
							appList.add(fog.getApplication());
							appToFogMap.put(fogDevice.getName(), appList);
						}
					}
				}
				
				applications.add(application);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	private ArrayList<FogDeviceGui> getClients(Graph graph){
		ArrayList<FogDeviceGui> clients = new ArrayList<FogDeviceGui>();
		
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Constants.FOG_TYPE))
    			if(((FogDeviceGui)node).getApplication().length() > 0)
    				clients.add((FogDeviceGui)node);
		
		return clients;
	}
	
	private void createFogDevices(Graph graph) {
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Constants.FOG_TYPE))
				fogDevices.add(createFogDevice((FogDeviceGui)node));
		
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
				
				f2.getNeighborsIds().add(f1.getId());
				f1.getNeighborsIds().add(f2.getId());
				
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
			return new FogDevice(fog.getName(), characteristics, new AppModuleAllocationPolicy(hostList),
					new LinkedList<Storage>(), Constants.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Both MIPS and Bandwidth are 0
	private Application createApplication(Graph graph, String appId, int userId){
		ApplicationGui applicationGui = null;
		
		for(ApplicationGui app : graph.getAppList())
			if(app.getAppId().equals(appId))
				applicationGui = app;
		
		if(applicationGui == null) return null;
		
		Application application = new Application(appId + "_" + userId, userId);

		for(AppModule appModule : applicationGui.getModules())
			application.addAppModule(appModule);
		
		for(AppEdge appEdge : applicationGui.getEdges())
			application.addAppEdge(appEdge, null);
			
		for(AppModule appModule : applicationGui.getModules()) {
			for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
				FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)appModule.getSelectivityMap().get(pair));
				application.addTupleMapping(appModule.getName(), pair, fractionalSelectivity.getSelectivity());
			}
		}
		
		List<AppLoop> loops = new ArrayList<AppLoop>();
		for(List<String> loop : applicationGui.getLoops()) {
			ArrayList<String> l = new ArrayList<String>();
			for(String name : loop)
				l.add(name + "_" + userId);
			loops.add(new AppLoop(l));
		}
		
		application.setLoops(loops);
		
		return application;
	}
	
	private void createSensorActuator(Graph graph, String clientName, int userId, String appId) {
		FogDeviceGui client = null;
		SensorGui sensor = null;
		ActuatorGui actuator = null;
		String tupleType = "";
		String actuatorType = "";
		double sensorLat = -1;
		double actuatorLat = -1;
		
		for(Node node : graph.getDevicesList().keySet())
			if(node.getName().equals(clientName))
				client = (FogDeviceGui) node;
		
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
			if(applicationGui.getAppId().equals(appId)) {
				for(AppEdge appEdge : applicationGui.getEdges()) {
					if(appEdge.getEdgeType() == AppEdge.SENSOR)
						tupleType = appEdge.getSource();
					else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
						actuatorType = appEdge.getDestination();
				}
			}
		}
		
		sensors.add(new Sensor(sensor.getName(), tupleType + "_" + userId, userId, appId + "_" + userId,
				sensor.getDistribution(), getFogDeviceByName(clientName).getId(), sensorLat));

		actuators.add(new Actuator(actuator.getName(), userId, appId + "_" + userId,
				getFogDeviceByName(clientName).getId(), actuatorLat, actuatorType + "_" + userId));
	}
	
	private FogDevice getFogDeviceByName(String name) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().equals(name))
				return fogDevice;
		return null;
	}
	
}
