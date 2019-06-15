package org.fog.placement;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.overall.util.AlgorithmMathUtils;
import org.fog.utils.FogEvents;
import org.fog.utils.Latency;
import org.fog.utils.Location;
import org.fog.utils.Util;

public class Controller extends SimEntity {
	private Map<String, ModulePlacement> appModulePlacementPolicy;
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;
	
	private List<Application> appList;
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	private Map<String, LinkedHashSet<String>> appToFogMap;
	
	private ControllerAlgorithm controllerAlgorithm;
	protected int[][] currentPlacement;
	
	public Controller(String name, List<Application> applications, List<FogDevice> fogDevices, List<Sensor> sensors,
			List<Actuator> actuators, Map<String, LinkedHashSet<String>> appToFogMap, int algorithmOp) {
		super(name);
		
		setApplications(new HashMap<String, Application>());
		appLaunchDelays = new HashMap<String, Integer>();
		appModulePlacementPolicy = new HashMap<String, ModulePlacement>();
		
		setFogDevices(fogDevices);
		this.appList = applications;
		this.sensors = sensors;
		this.actuators = actuators;
		this.appToFogMap = appToFogMap;
		
		controllerAlgorithm = new ControllerAlgorithm(fogDevices, appList, sensors, actuators, algorithmOp);
	}
	
	@Override
	public void startEntity() {
		for(String appId : getApplications().keySet()) {
			if(appLaunchDelays.get(appId) == 0)
				processAppSubmit(getApplications().get(appId));
			else
				send(getId(), appLaunchDelays.get(appId), FogEvents.APP_SUBMIT, getApplications().get(appId));
		}
		
		send(getId(), Config.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		sendNow(getId(), FogEvents.UPDATE_TOPOLOGY);
		
		for(FogDevice dev : getFogDevices()) {
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
			
			if(Config.DYNAMIC_SIMULATION) {
				sendNow(dev.getId(), FogEvents.UPDATE_PERIODIC_MOVEMENT);
			}
		}
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_TOPOLOGY:
			updateTopology(false);
			break;
		case FogEvents.UPDATE_VM_POSITION:
			updateVmPosition(ev);
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			new OutputControllerResults(this);
			
			if(OutputControllerResults.isDisplayingPlot)
				Util.promptEnterKey("Press \"ENTER\" to exit...");
			
			System.exit(0);
			break;
		default:
			break;
		}
	}
	
	@Override
	public void shutdownEntity() {
		
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement) {
		getApplications().put(application.getAppId(), application);
		appLaunchDelays.put(application.getAppId(), delay);
		appModulePlacementPolicy.put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors)
			sensor.setApp(getApplications().get(sensor.getAppId()));
		
		for(Actuator ac : actuators)
			ac.setApp(getApplications().get(ac.getAppId()));
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement) {
		submitApplication(application, 0, modulePlacement);
	}
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application) {
		FogComputingSim.print("Submitted application " + application.getAppId());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = appModulePlacementPolicy.get(application.getAppId());
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
				
				Map<AppModule, Integer> vmPosition = new HashMap<AppModule, Integer>();
				vmPosition.put(module, deviceId);
				sendNow(getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
			}
		}
	}
	
	// Mobile nodes always connect to the closest fixed fog device (which offer the best received signal strength)
	// Similarly to what happens in mobile communications, handover has a threshold in order to avoid
	// abuse of handovers in the border areas
	public void updateTopology(boolean first) {
		Map<FogDevice, Map<FogDevice, FogDevice>> handovers = new HashMap<FogDevice, Map<FogDevice,FogDevice>>();
		
		for(FogDevice f1 : fogDevices) {
			
			// If f1 is a fixed node do nothing
			if(!f1.getFixedNeighborsIds().isEmpty())
				continue;
			
			FogDevice best = null;
			FogDevice bestNeighbor = null;
			
			double bestDistance = Constants.INF;
			for(int neighborId : f1.getLatencyMap().keySet()) {
				FogDevice neighbor = getFogDeviceById(neighborId);
				
				if(bestDistance > Latency.computeConnectionLatency(f1, neighbor)) {
					best = neighbor;
					bestNeighbor = neighbor;
					bestDistance = Location.computeDistance(f1, best);
				}
			}
			
			for(FogDevice f2 : fogDevices) {
				if(f1.getId() == f2.getId())
					continue;
				
				// If f2 is a mobile node do nothing
				if(f2.getFixedNeighborsIds().isEmpty())
					continue;
				
				if(f1.getCoverage().covers(f1, f2) && f2.getCoverage().covers(f2, f1)) {
					double distance = Location.computeDistance(f1, f2);
					if(distance  + Config.HANDOVER_THRESHOLD < bestDistance) {
						bestDistance = distance;
						best = f2;
					}
				}
			}
			
			// Mobile nodes need to be connected to a fixed node
			if(best == null)
				FogComputingSim.err("There are some mobile devices with no possible communications");
			
			if(!f1.getLatencyMap().isEmpty()) {
				
				// If its not the same node which it is already connected
				// If already has a connection, remove it because there is a better one
				if(bestNeighbor.getId() != best.getId()) {					
					controllerAlgorithm.getAlgorithm().changeConnectionMap(f1, bestNeighbor, best);
					Map<FogDevice, FogDevice> handover = new HashMap<FogDevice, FogDevice>();
					handover.put(bestNeighbor, best);
					handovers.put(f1, handover);
				}
			}else {
				Map<FogDevice, FogDevice> handover = new HashMap<FogDevice, FogDevice>();
				handover.put(best, best);
				handovers.put(f1, handover);
			}
		}
		
		if(first) {
			for(FogDevice mobile : handovers.keySet()) {
				Map<FogDevice, FogDevice> handover = handovers.get(mobile);
				FogDevice from = handover.entrySet().iterator().next().getKey();
				FogDevice to = handover.get(from);
				
				createConnection(mobile, from, to);
			}
			
			controllerAlgorithm.computeAlgorithm();
			currentPlacement = new int[controllerAlgorithm.getAlgorithm().getNumberOfNodes()][controllerAlgorithm.getAlgorithm().getNumberOfModules()];
			
			deployApplications(controllerAlgorithm.getAlgorithm().extractPlacementMap(controllerAlgorithm.getSolution().getModulePlacementMap()));
			createRoutingTables(controllerAlgorithm.getAlgorithm(), controllerAlgorithm.getSolution().getTupleRoutingMap());
			
		}else if(!handovers.isEmpty() && Config.DYNAMIC_SIMULATION){
			int[][] previousModulePlacement = controllerAlgorithm.getSolution().getModulePlacementMap();
			
			if(!Config.ALLOW_MIGRATION) {
				controllerAlgorithm.getAlgorithm().setPossibleDeployment(AlgorithmMathUtils.toDouble(currentPlacement/*controllerAlgorithm.getSolution().getModulePlacementMap()*/));
			}
			
			controllerAlgorithm.recomputeAlgorithm();
			
			createRoutingTables(controllerAlgorithm.getAlgorithm(), controllerAlgorithm.getSolution().getTupleRoutingMap());
			createMigrationTables(controllerAlgorithm.getAlgorithm(), controllerAlgorithm.getSolution().getMigrationRoutingMap());
		
			// Update connections
			for(FogDevice mobile : handovers.keySet()) {
				Map<FogDevice, FogDevice> handover = handovers.get(mobile);
				FogDevice from = handover.entrySet().iterator().next().getKey();
				FogDevice to = handover.get(from);
				
				createConnection(mobile, from, to);
				removeConnection(mobile, from);
			}
			
			// Migrate modules
			migrateModules(controllerAlgorithm.getSolution().getModulePlacementMap(), previousModulePlacement);
		}
		
		send(getId(), 1, FogEvents.UPDATE_TOPOLOGY);
	}
	
	@SuppressWarnings("unchecked")
	private void updateVmPosition(SimEvent ev) {
		Map<AppModule, Integer> vmPosition = (Map<AppModule, Integer>)ev.getData();
		int fogId = 0;
		AppModule vm = null;
		
		for(AppModule appModule : vmPosition.keySet()) {
			vm = appModule;
			fogId = vmPosition.get(appModule);
		}
		
		int fogIndex = controllerAlgorithm.getAlgorithm().getNodeIndexByNodeId(fogId);
		int vmIndex = controllerAlgorithm.getAlgorithm().getModuleIndexByModuleName(vm.getName());
		
		for(int i  = 0; i < controllerAlgorithm.getAlgorithm().getNumberOfNodes(); i++) {
			if(i != fogIndex) {
				currentPlacement[i][vmIndex] = 0;
			}else {
				currentPlacement[i][vmIndex] = 1;
			}
		}
	}

	private void createConnection(FogDevice mobile, FogDevice from, FogDevice to) {
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("Creating connection between: " + mobile.getName() + " <-> " + to.getName());
		
		double latency = Latency.computeConnectionLatency(mobile, to);
		mobile.getLatencyMap().put(to.getId(), latency);
		to.getLatencyMap().put(mobile.getId(), latency);
		
		mobile.getBandwidthMap().put(to.getId(), Config.MOBILE_COMMUNICATION_BW);
		to.getBandwidthMap().put(mobile.getId(), Config.MOBILE_COMMUNICATION_BW);
		
		mobile.getTupleQueue().put(to.getId(), new LinkedList<Pair<Tuple, Integer>>());
		to.getTupleQueue().put(mobile.getId(), new LinkedList<Pair<Tuple, Integer>>());
		
		mobile.getTupleLinkBusy().put(to.getId(), false);
		to.getTupleLinkBusy().put(mobile.getId(), false);
	}
	
	private void removeConnection(FogDevice mobile, FogDevice from) {
		// Then, remove the old connections
		sendNow(mobile.getId(), FogEvents.CONNECTION_LOST, from.getId());
		sendNow(from.getId(), FogEvents.CONNECTION_LOST, mobile.getId());
	}
	
	private void deployApplications(Map<String, List<String>> modulePlacementMap) {
		for(FogDevice fogDevice : fogDevices) {
			if(appToFogMap.containsKey(fogDevice.getName())) {
				for(Application application : appList) {
					ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
					
					for(AppModule appModule : application.getModules()) {
						for(String fogName : modulePlacementMap.keySet()) {
							if(modulePlacementMap.get(fogName).contains(appModule.getName())) {
								moduleMapping.addModuleToDevice(appModule.getName(), fogName);
							}
						}
					}
					
					ModulePlacement modulePlacement = new ModulePlacementMapping(fogDevices, application, moduleMapping);
					submitApplication(application, modulePlacement);
				}
			}
		}
	}
	
	private void migrateModules(int[][] currentModulePlacement, int[][] previousModulePlacement) {
		appModulePlacementPolicy.clear();
		
		for(int j = 0; j < controllerAlgorithm.getAlgorithm().getNumberOfModules(); j++) {
			int previousPlacement = -1;
			int currentPlacement = -1;
			
			for(int i = 0; i < controllerAlgorithm.getAlgorithm().getNumberOfNodes(); i++) {
				if(currentModulePlacement[i][j] == 1) {
					currentPlacement = i;
				}
				
				if(previousModulePlacement[i][j] == 1) {
					previousPlacement = i;
				}
			}
			
			if(previousPlacement == -1 || previousPlacement == -1)
				FogComputingSim.err("Should not happen");
			
			if(currentPlacement != previousPlacement) {
				AppModule module = getModuleByName(controllerAlgorithm.getAlgorithm().getmName()[j]);
				
				if(module.isInMigration())
					continue;
				
				FogDevice from = getFogDeviceByName(controllerAlgorithm.getAlgorithm().getfName()[previousPlacement]);
				FogDevice to = getFogDeviceByName(controllerAlgorithm.getAlgorithm().getfName()[currentPlacement]);
				
				if(Config.PRINT_DETAILS)
					FogComputingSim.print("Migratig module: " + module.getName() +  " from: " + from.getName() + " to: " + to.getName());
				
				Application application = getApplicationByModule(module);
				
				if(application == null)
					FogComputingSim.err("Should not happen");
				
				Map<FogDevice, Map<Application, AppModule>> map = new HashMap<FogDevice, Map<Application,AppModule>>();
				Map<Application, AppModule> appMap = new HashMap<Application, AppModule>();
				appMap.put(application, module);
				map.put(to, appMap);
				sendNow(from.getId(), FogEvents.MIGRATION, map);
			}
		}
	}
	
	private void createRoutingTables(Algorithm algorithm, int[][] routingMatrix) {
		// Clear the current routing tables
		for(FogDevice fogDevice : fogDevices)
			fogDevice.getTupleRoutingTable().clear();
		
		Map<Map<Integer, Map<String, String>>, Integer> routingMap = algorithm.extractRoutingMap(routingMatrix);
		
		
		// Update routing tables afterwards
		for(Map<Integer, Map<String, String>> hop : routingMap.keySet()) {
			for(Integer node : hop.keySet()) {

				FogDevice fogDevice = getFogDeviceById(algorithm.getfId()[node]);
				
				// Sensors and actuators do not need routing map
				if(fogDevice == null)
					continue;
				
				fogDevice.getTupleRoutingTable().put(hop.get(node), algorithm.getfId()[routingMap.get(hop)]);
			}
		}
		
		if(Config.PRINT_DETAILS) {
			System.out.println("\nRouting Tuple tables:");
			for(FogDevice f : fogDevices) {
				System.out.println(f.getName() + " : " + f.getTupleRoutingTable());
			}
			System.out.println();
		}
	}
	
	private void createMigrationTables(Algorithm algorithm, int[][] migrationMatrix) {
		for(FogDevice fogDevice : fogDevices)
			fogDevice.getVmRoutingTable().clear();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 1; j <  algorithm.getNumberOfNodes(); j++) {				
				if(migrationMatrix[i][j-1] != migrationMatrix[i][j]) {
					int fogId = algorithm.getfId()[migrationMatrix[i][j-1]];
					int nextHopId = algorithm.getfId()[migrationMatrix[i][j]];
					
					FogDevice fogDevice = getFogDeviceById(fogId);
					String vmName = algorithm.getmName()[i];
					
					fogDevice.getVmRoutingTable().put(vmName, nextHopId);
				}
			}
		}
		
		if(Config.PRINT_DETAILS) {
			System.out.println("\nRouting VM tables:");
			for(FogDevice f : fogDevices) {
				System.out.println(f.getName() + " : " + f.getVmRoutingTable());
			}
			System.out.println();
		}
	}
	
	public FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices())
			if(id==fogDevice.getId())
				return fogDevice;
		return null;
	}
	
	private FogDevice getFogDeviceByName(String name){
		for(FogDevice fogDevice : getFogDevices())
			if(name.equals(fogDevice.getName()))
				return fogDevice;
		return null;
	}
	
	private AppModule getModuleByName(String name){
		for(Application application : appList)
			for(AppModule appModule : application.getModules())
				if(appModule.getName().equals(name))
					return appModule;
		return null;
	}
	
	private Application getApplicationByModule(AppModule appModule){		
		for(String appId : applications.keySet()) {
			Application application = applications.get(appId);
			if(application.getModules().contains(appModule))
				return application;
		}
		return null;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}
	
}
