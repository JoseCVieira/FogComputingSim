package org.fog.placement;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.Client;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.overall.Algorithm;
import org.fog.placement.algorithms.overall.Job;
import org.fog.placement.algorithms.overall.bf.BruteForce;
import org.fog.placement.algorithms.overall.ga.GeneticAlgorithm;
import org.fog.placement.algorithms.overall.lp.LinearProgramming;
import org.fog.placement.algorithms.overall.lp.MultiObjectiveLinearProgramming;
import org.fog.placement.algorithms.overall.random.Random;
import org.fog.placement.algorithms.overall.util.AlgorithmMathUtils;
import org.fog.placement.algorithms.overall.util.AlgorithmUtils;
import org.fog.utils.FogEvents;
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
	private boolean handoverInProgress;
	
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
		sendNow(getId(), FogEvents.VERIFY_HANDOVER);
		
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
		case FogEvents.VERIFY_HANDOVER:
			verifyHandover();
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
	
	// Clients always connect to the closest fog device (which offer the best received signal strength)
	// However, similarly to what happens in mobile communications, handover has a threshold in order to avoid
	// abuse of handover in the border areas
	private void verifyHandover() {
		if(handoverInProgress)
			return;
		
		Map<Client, FogDevice> handovers = new HashMap<Client, FogDevice>();
		
		for(FogDevice client : fogDevices) {
			if(client instanceof Client) {
				Client clientTmp = (Client) client;
					
				FogDevice firstHop = getFogDeviceById(clientTmp.getBandwidthMap().entrySet().iterator().next().getKey());
				
				// Current distance
				double distance = Location.computeDistance(clientTmp.getMovement().getLocation(), firstHop.getMovement().getLocation());
				double bestDistance = distance;
				FogDevice bestFogNode = firstHop;
				
				// Check if there is a better fog node for that client
				for(FogDevice fogNode : fogDevices) {
					if(!(fogNode instanceof Client)) {
						double tmpDistance = Location.computeDistance(clientTmp.getMovement().getLocation(), fogNode.getMovement().getLocation());
						if(bestDistance > tmpDistance) {
							bestDistance = tmpDistance;
							bestFogNode = fogNode;
						}
					}
				}
				
				// If the current distance is better than the old one, than change its connection
				if(distance > bestDistance + Config.HANDOFF_THRESHOLD) {
					handovers.put(clientTmp, bestFogNode);
				}
			}
		}
		
		if(!handovers.isEmpty()) {
			handoverInProgress = true;
			
			if(Config.PRINT_HANDOVER_DETAILS)
				printHandoverDetails(handovers);
			
			reconfigure();
			
			handoverInProgress = false;
		}
		
		send(getId(), 1, FogEvents.VERIFY_HANDOVER);
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
				algorithm = new Random(fogDevices, appList, sensors, actuators);
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
				algorithm = new Random(fogDevices, appList, sensors, actuators);
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
	
	public void reconfigure() {
		algorithm.setPossibleDeployment(AlgorithmMathUtils.toDouble(solution.getModulePlacementMap()));
		solution = algorithm.execute();
		
		for(FogDevice fogDevice : fogDevices) {
			fogDevice.getRoutingTable().clear();
		}
		
		createRoutingTables(algorithm, solution.getRoutingMap());
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
	
	private void createRoutingTables(Algorithm algorithm, int[][] routingMatrix) {
		Map<Map<Integer, Map<String, String>>, Integer> routingMap = algorithm.extractRoutingMap(routingMatrix);
		
		for(Map<Integer, Map<String, String>> hop : routingMap.keySet()) {
			for(Integer node : hop.keySet()) {

				FogDevice fogDevice = getFogDeviceById(algorithm.getfId()[node]);
				if(fogDevice == null) //sensor and actuators do not need routing map
					continue;
				
				fogDevice.getRoutingTable().put(hop.get(node), algorithm.getfId()[routingMap.get(hop)]);
			}
		}
	}
	
	private FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices())
			if(id==fogDevice.getId())
				return fogDevice;
		return null;
	}
	
	private void printHandoverDetails(Map<Client, FogDevice> handovers) {
		System.out.println("Performing handover over the following devices:\n");
		
		for(Client client : handovers.keySet()) {
			FogDevice from = getFogDeviceById(client.getBandwidthMap().entrySet().iterator().next().getKey());
			FogDevice to = handovers.get(client);
			
			System.out.println(String.format("%-8s", "Client: ") + AlgorithmUtils.centerString(20, client.getName()) + locationToString(client));
			System.out.println(String.format("%-8s", "From: ") + AlgorithmUtils.centerString(20, from.getName()) + locationToString(from));
			System.out.println(String.format("%-8s", "To: ") + AlgorithmUtils.centerString(20, to.getName()) + locationToString(to) + "\n");
		}
	}
	
	private String locationToString(FogDevice fogDevice) {
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		
		return " at position (X=" + Float.valueOf(decimalFormat.format(fogDevice.getMovement().getLocation().getX()))
				+ ", Y=" + Float.valueOf(decimalFormat.format(fogDevice.getMovement().getLocation().getY())) + ")";
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