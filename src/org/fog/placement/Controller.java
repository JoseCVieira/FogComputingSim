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
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.bf.BruteForce;
import org.fog.placement.algorithm.overall.ga.GeneticAlgorithm;
import org.fog.placement.algorithm.overall.lp.LinearProgramming;
import org.fog.placement.algorithm.overall.lp.MultiObjectiveLinearProgramming;
import org.fog.placement.algorithm.overall.random.RandomAlgorithm;
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
	
	private Algorithm algorithm;
	private Job solution;
	
	private int algorithmOp;
	
	public Controller(String name, List<Application> applications, List<FogDevice> fogDevices, List<Sensor> sensors,
			List<Actuator> actuators, Map<String, LinkedHashSet<String>> appToFogMap, int algorithmOp) {
		super(name);
		
		setApplications(new HashMap<String, Application>());
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		
		setFogDevices(fogDevices);
		this.appList = applications;
		this.sensors = sensors;
		this.actuators = actuators;
		this.algorithmOp = algorithmOp;
		this.appToFogMap = appToFogMap;
	}
	
	@Override
	public void startEntity() {
		for(String appId : getApplications().keySet()) {
			if(getAppLaunchDelays().get(appId) == 0)
				processAppSubmit(getApplications().get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, getApplications().get(appId));
		}
		
		send(getId(), Constants.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		sendNow(getId(), FogEvents.UPDATE_TOPOLOGY);
		
		for(FogDevice dev : getFogDevices()) {
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
			sendNow(dev.getId(), FogEvents.UPDATE_PERIODIC_MOVEMENT);
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
	
	public void runAlgorithm() {
		switch (algorithmOp) {
			case FogComputingSim.MOLP:
				Config.SINGLE_OBJECTIVE = false;
				System.out.println("Running the optimization algorithm: Multiobjective Linear Programming.");
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				break;
			case FogComputingSim.LP:
				System.out.println("Running the optimization algorithm: Linear Programming.");
				algorithm = new LinearProgramming(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				break;
			case FogComputingSim.GA:
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GeneticAlgorithm(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Genetic Algorithm");
				break;
			case FogComputingSim.RAND:
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new RandomAlgorithm(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Random Algorithm");
				break;
			case FogComputingSim.BF:
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BruteForce(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Brute Force");
				break;
			case FogComputingSim.MDP:
				FogComputingSim.err("MDP is not implemented yet");
				break;
			case FogComputingSim.ALL:
				System.out.println("Running the optimization algorithm: Multiobjective Linear Programming.");
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				
				System.out.println("Running the optimization algorithm: Linear programming.");
				algorithm = new LinearProgramming(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GeneticAlgorithm(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Genetic Algorithm");
				
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new RandomAlgorithm(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Random Algorithm");
				
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BruteForce(fogDevices, appList, sensors, actuators);
				solution = algorithm.execute();
				OutputControllerResults.plotResult(algorithm, "Brute Force");
				break;
			default:
				FogComputingSim.err("Unknown algorithm");
		}
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getRoutingMap() == null || !solution.isValid()) {
			FogComputingSim.err("There is no possible combination to deploy all applications");
		}
		
		deployApplications(algorithm.extractPlacementMap(solution.getModulePlacementMap()));
		createRoutingTables(algorithm, solution.getRoutingMap());
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors)
			sensor.setApp(getApplications().get(sensor.getAppId()));
		
		for(Actuator ac : actuators)
			ac.setApp(getApplications().get(ac.getAppId()));
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println("Submitted application " + application.getAppId() + " at time= " + CloudSim.clock());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
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
			double bestDistance = Constants.INF;
			if(!f1.getLatencyMap().isEmpty()) {
				best = getFogDeviceById(f1.getLatencyMap().entrySet().iterator().next().getKey());
				bestDistance = Location.computeDistance(f1, best);
			}
			
			for(FogDevice f2 : fogDevices) {
				if(f1.getId() == f2.getId())
					continue;
				
				// If f2 is a mobile node do nothing
				if(f2.getFixedNeighborsIds().isEmpty())
					continue;
				
				if(f1.getCoverage().covers(f1, f2, Config.CONNECTION_RANGE_LIMIT) && f2.getCoverage().covers(f2, f1, Config.CONNECTION_RANGE_LIMIT)){
					double distance = Location.computeDistance(f1, f2);
					if(distance  + Config.HANDOFF_THRESHOLD < bestDistance) {
						bestDistance = distance;
						best = f2;
					}
				}
			}
			
			// Mobile nodes need to be connected to a fixed node
			if(best == null)
				FogComputingSim.err("There are some mobile devices with no possible communications");
			
			if(!f1.getLatencyMap().isEmpty()) {
				int neighborhoodId = f1.getLatencyMap().entrySet().iterator().next().getKey();
				
				// If its not the same node which it is already connected
				// If already has a connection, remove it because there is a better one
				if(neighborhoodId != best.getId()) {
					FogDevice from = getFogDeviceById(neighborhoodId);
					
					algorithm.changeConnectionMap(f1, from, best);
					Map<FogDevice, FogDevice> handover = new HashMap<FogDevice, FogDevice>();
					handover.put(from, best);
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
				
				System.out.println("[" + CloudSim.clock() + "] Creating connection between "+mobile.getName()+" and " + to.getName());
				System.out.println("[" + CloudSim.clock() + "] Creating connection between "+to.getName()+" and " + mobile.getName());
				
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
				
			runAlgorithm();
		}else {
			algorithm.setPossibleDeployment(AlgorithmMathUtils.toDouble(solution.getModulePlacementMap()));
			solution = algorithm.execute();
		
			for(FogDevice mobile : handovers.keySet()) {
				Map<FogDevice, FogDevice> handover = handovers.get(mobile);
				FogDevice from = handover.entrySet().iterator().next().getKey();
				FogDevice to = handover.get(from);
				
				System.out.println("[" + CloudSim.clock() + "] Creating connection between "+mobile.getName()+" and " + to.getName());
				System.out.println("[" + CloudSim.clock() + "] Creating connection between "+to.getName()+" and " + mobile.getName());
				
				double latency = Latency.computeConnectionLatency(mobile, to);
				mobile.getLatencyMap().put(to.getId(), latency);
				to.getLatencyMap().put(mobile.getId(), latency);
				
				mobile.getBandwidthMap().put(to.getId(), Config.MOBILE_COMMUNICATION_BW);
				to.getBandwidthMap().put(mobile.getId(), Config.MOBILE_COMMUNICATION_BW);
				
				mobile.getTupleQueue().put(to.getId(), new LinkedList<Pair<Tuple, Integer>>());
				to.getTupleQueue().put(mobile.getId(), new LinkedList<Pair<Tuple, Integer>>());
				
				mobile.getTupleLinkBusy().put(to.getId(), false);
				to.getTupleLinkBusy().put(mobile.getId(), false);
				
				
				// Then, remove the old connections
				sendNow(mobile.getId(), FogEvents.CONNECTION_LOST, from.getId());
				sendNow(from.getId(), FogEvents.CONNECTION_LOST, mobile.getId());
			}
			
			createRoutingTables(algorithm, solution.getRoutingMap());
		}
		
		send(getId(), 1, FogEvents.UPDATE_TOPOLOGY);
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
		for(int j = 0; j < currentModulePlacement[0].length; j++) {
			int previousPlacement = -1;
			int currentPlacement = -1;
			
			for(int i = 0; i < currentModulePlacement.length; i++) {
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
				AppModule module = getModuleByName(algorithm.getmName()[j]);
				FogDevice from = getFogDeviceByName(algorithm.getfName()[previousPlacement]);
				FogDevice to = getFogDeviceByName(algorithm.getfName()[currentPlacement]);
				
				Map<AppModule, FogDevice> map = new HashMap<AppModule, FogDevice>();
				map.put(module, to);
				
				sendNow(from.getId(), FogEvents.START_MIGRATION, map);
			}
		}
	}
	
	private void createRoutingTables(Algorithm algorithm, int[][] routingMatrix) {
		Map<Map<Integer, Map<String, String>>, Integer> routingMap = algorithm.extractRoutingMap(routingMatrix);
		
		// Clear the current routing tables
		for(FogDevice fogDevice : fogDevices)
			fogDevice.getRoutingTable().clear();
		
		// Update routing tables afterwards
		for(Map<Integer, Map<String, String>> hop : routingMap.keySet()) {
			for(Integer node : hop.keySet()) {

				FogDevice fogDevice = getFogDeviceById(algorithm.getfId()[node]);
				
				 // Sensors and actuators do not need routing map
				if(fogDevice == null)
					continue;
				
				fogDevice.getRoutingTable().put(hop.get(node), algorithm.getfId()[routingMap.get(hop)]);
			}
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
		for(Application application : appList) {
			for(AppModule appModule : application.getModules()) {
				if(appModule.getName().equals(name)) {
					return appModule;
				}
			}
		}
		return null;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}
	
}