package org.fog.placement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.fog.utils.ExcelUtils;
import org.fog.utils.FogEvents;
import org.fog.utils.Location;
import org.fog.utils.MobileBandwidthModel;
import org.fog.utils.MobilePathLossModel;
import org.fog.utils.SimulationResults;
import org.fog.utils.Util;

/**
 * Class representing the controller of the fog network. It supervises and manages the network connections and runs the optimization algorithm.
 * Based on the solution found by the optimization algorithm, it performs the deployment of the application modules, creates and updates
 * both the routing tuple and routing migration tables. Also, it sends some events (control messages) to the fog nodes so that they perform some
 * actions (e.g., start migrating some virtual machine).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Controller extends SimEntity {
	/** Map between the name of the application and it's module placement within the fog network */
	private Map<String, ModulePlacement> appModulePlacementPolicy;
	
	/** Map between the name of the application and the application itself */
	private Map<String, Application> applications;
	
	/** Map between the name of the application to be deployed and the delay until being launched */
	private Map<String, Integer> appLaunchDelays;
	
	/** List containing all applications which it needs to supervise and manage */
	private List<Application> appList;
	
	/** List containing all fog devices which it needs to supervise and manage */
	private List<FogDevice> fogDevices;
	
	/** List containing all sensors which it needs to supervise and manage */
	private List<Sensor> sensors;
	
	/** List containing all actuators which it needs to supervise and manage */
	private List<Actuator> actuators;
	
	/** Object responsible for running the optimization algorithm and hold it's solution */
	private ControllerAlgorithm controllerAlgorithm;
	
	/** Object which holds all the information needed to run the optimization algorithm */
	private Algorithm algorithm;
	
	/** Object which holds the results of the optimization algorithm */
	private Job solution;
	
	/** Number of migrations performed during the whole simulation */
	private int nrMigrations;
	
	/** Number of handovers performed during the whole simulation */
	private int nrHandovers;

	/**
	 * Creates a new controller.
	 * 
	 * @param name the name of the controller
	 * @param applications the list containing all applications which it needs to supervise and manage
	 * @param fogDevices the list containing all fog devices which it needs to supervise and manage
	 * @param sensors the list containing all sensors which it needs to supervise and manage
	 * @param actuators the list containing all actuators which it needs to supervise and manage
	 * @param algorithmOp the id of the optimization algorithm chosen to be executed
	 */
	public Controller(String name, List<Application> applications, List<FogDevice> fogDevices, List<Sensor> sensors,
			List<Actuator> actuators, int algorithmOp) {
		super(name);
		
		setApplications(new HashMap<String, Application>());
		appLaunchDelays = new HashMap<String, Integer>();
		appModulePlacementPolicy = new HashMap<String, ModulePlacement>();
		
		this.fogDevices = fogDevices;
		this.appList = applications;
		this.sensors = sensors;
		this.actuators = actuators;
		
		controllerAlgorithm = new ControllerAlgorithm(algorithmOp);
	}
	
	/**
	 * At the beginning send the events to: launch the applications with the respective delays, to start the resource
	 * management at the fog nodes, and to periodically update the mobile nodes position.
	 */
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
	
	/**
	 * Processes the events that can occur in the controller.
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_TOPOLOGY:
			updateTopology();
			break;
		case FogEvents.UPDATE_VM_POSITION:
			updateVmPosition(ev);
			break;
		case FogEvents.STOP_SIMULATION:
			for(FogDevice fogDevice : fogDevices) {
				sendNow(fogDevice.getId(), FogEvents.RESOURCE_MGMT);
			}
			
			CloudSim.stopSimulation();
			new SimulationResults(this);
			
			if(Config.EXPORT_RESULTS_EXCEL) {
				try {
					ExcelUtils.writeExcel(this);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(Config.PLOT_ALGORITHM_RESULTS)
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
	
	/**
	 * Submits one application to the controller with a given delay.
	 * 
	 * @param application the application itself
	 * @param delay the delay after which it will be launched
	 * @param modulePlacement the module placement of the application
	 */
	private void submitApplication(Application application, int delay, ModulePlacement modulePlacement) {
		getApplications().put(application.getAppId(), application);
		appLaunchDelays.put(application.getAppId(), delay);
		appModulePlacementPolicy.put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors)
			sensor.setApp(getApplications().get(sensor.getAppId()));
		
		for(Actuator ac : actuators)
			ac.setApp(getApplications().get(ac.getAppId()));
	}
	
	/**
	 * Submits one application to the controller with no delay.
	 * 
	 * @param application the application itself
	 * @param modulePlacement the module placement of the application
	 */
	private void submitApplication(Application application, ModulePlacement modulePlacement) {
		submitApplication(application, 0, modulePlacement);
	}
	
	/**
	 * The defined delay to launch the application its over and now it is submitted according to it's application module placement.
	 * 
	 * @param ev the event that just occurred containing the application to be submitted
	 */
	private void processAppSubmit(SimEvent ev) {
		processAppSubmit((Application) ev.getData());
	}
	
	/**
	 * The defined delay to launch the application its over and now it is submitted according to it's application module placement.
	 * 
	 * @param application the application to be submitted
	 */
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
	
	/**
	 * Computes the new connections for the mobile nodes, and executes the optimization algorithm in order to reconfigure
	 * the module placement, tuple routing and migration routing tables if needed.
	 */
	public void updateTopology() {
		Map<FogDevice, Map<FogDevice, FogDevice>> handovers;
		int[][] previousModulePlacement = null;
		boolean first = false;
		
		// Updates both the algorithm bandwidth and latency map
		if(algorithm != null)
			algorithm.updateConnectionCharacteristcs(fogDevices);
		
		// Computes the handovers which occurred since the previous algorithm execution
		handovers = computeHandovers();
		
		// Schedules the next reconfiguration of the topology
		send(getId(), Config.RECONFIG_PERIOD, FogEvents.UPDATE_TOPOLOGY);
		
		// If it's the first execution
		if(algorithm == null) {
			first = true;
			
			for(FogDevice mobile : handovers.keySet()) {
				Map<FogDevice, FogDevice> handover = handovers.get(mobile);
				FogDevice from = handover.entrySet().iterator().next().getKey();
				FogDevice to = handover.get(from);
				
				// Create the connections for the mobile nodes
				createConnection(mobile, to);
			}
			
		// Else, if it's not the first execution check if there were some handovers and if the users selected a dynamic simulation
		}else if(!handovers.isEmpty() && Config.DYNAMIC_SIMULATION) {
			previousModulePlacement = solution.getModulePlacementMap();
			
			// If the user choose to allow to perform migrations
			if(!Config.ALLOW_MIGRATION)
				algorithm.setPossibleDeployment(algorithm.getCurrentPlacement());
			
		// Otherwise, do nothing
		}else
			return;
		
		// Execute the selected optimization algorithm and extract both the solution and the algorithm from it
		controllerAlgorithm.computeAlgorithm(fogDevices, appList, sensors, actuators);
		algorithm = controllerAlgorithm.getAlgorithm();
		solution = controllerAlgorithm.getSolution();
		
		// If it's the first execution, just deploy the applications into the respective nodes and create the tuple routing tables
		if(first) {
			deployApplications(algorithm.extractPlacementMap(solution.getModulePlacementMap()));
			updateTupleRoutingTables(algorithm, solution.getTupleRoutingMap());
			
		// Otherwise, notify the fog nodes to change their connections, update the routing tables and migrate modules if needed
		}else {
			updateTupleRoutingTables(algorithm, solution.getTupleRoutingMap());
			updateMigrationTables(algorithm, solution.getMigrationRoutingMap());
		
			// Update connections
			for(FogDevice mobile : handovers.keySet()) {
				Map<FogDevice, FogDevice> handover = handovers.get(mobile);
				FogDevice from = handover.entrySet().iterator().next().getKey();
				FogDevice to = handover.get(from);
				
				createConnection(mobile, to);
				removeConnection(mobile, from);
				
				nrHandovers++;
			}
			
			// Migrate modules
			migrateModules(solution.getModulePlacementMap(), previousModulePlacement);
		}
	}
	
	/**
	 *  Computes the new connections for the mobile nodes. Mobile nodes always connect to the closest fixed fog device
	 *  (which offer the best received signal strength; similarly to what happens in mobile communications). The
	 *  handover has a threshold in order to avoid abuse of swaps in the border areas.
	 * 
	 * @return the list containing the handovers which occurred
	 */
	private Map<FogDevice, Map<FogDevice, FogDevice>> computeHandovers() {
		Map<FogDevice, Map<FogDevice, FogDevice>> handovers = new HashMap<FogDevice, Map<FogDevice,FogDevice>>();
		
		for(FogDevice f1 : fogDevices) {
			// If f1 is a fixed node do nothing
			if(f1.isStaticNode())
				continue;
			
			FogDevice best = null;
			FogDevice bestNeighbor = null;
			
			double bestDistance = Constants.INF;
			for(int neighborId : f1.getLatencyMap().keySet()) {
				FogDevice neighbor = getFogDeviceById(neighborId);
				
				if(bestDistance > Location.computeDistance(f1, neighbor)) {
					best = neighbor;
					bestNeighbor = neighbor;
					bestDistance = Location.computeDistance(f1, best);
				}
			}
			
			for(FogDevice f2 : fogDevices) {
				if(f1.getId() == f2.getId())
					continue;
				
				// If f2 is a mobile node do nothing
				if(!f2.isStaticNode()) continue;
				
				double distance = Location.computeDistance(f1, f2);
				if(distance  + Config.HANDOVER_THRESHOLD < bestDistance) {
					bestDistance = distance;
					best = f2;
				}
			}
			
			// Mobile nodes need to be connected to a fixed node
			if(best == null)
				FogComputingSim.err("There are some mobile devices with no possible communications");
			
			if(!f1.getLatencyMap().isEmpty()) {				
				// If its not the same node which it is already connected
				// If already has a connection, remove it because there is a better one
				if(bestNeighbor.getId() != best.getId()) {					
					algorithm.changeConnectionMap(f1, bestNeighbor, best);
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
		
		return handovers;
	}
	
	/**
	 * Updates the position of a given virtual machine (application module). It's required to keep track of their current positions
	 * so that if the topology changes during one migration, the system is capable of forward it to the correct destination.
	 * 
	 * @param ev the event that just occurred containing the virtual machine and its current position (fog device id)
	 */
	@SuppressWarnings("unchecked")
	private void updateVmPosition(SimEvent ev) {
		Map<AppModule, Integer> vmPosition = (Map<AppModule, Integer>)ev.getData();
		int fogId = 0;
		AppModule vm = null;
		
		for(AppModule appModule : vmPosition.keySet()) {
			vm = appModule;
			fogId = vmPosition.get(appModule);
		}
		
		int fogIndex = algorithm.getNodeIndexByNodeId(fogId);
		int vmIndex = algorithm.getModuleIndexByModuleName(vm.getName());
		
		algorithm.setCurrentPlacement(vmIndex, fogIndex);
	}
	
	/**
	 * Creates a new connection between two nodes. This is used for mobile communications, hence both the mobile path loss and the mobile
	 * bandwidth models are used to compute the connection characteristics.
	 * 
	 * @param mobile the mobile fog device
	 * @param to the new fixed node where the mobile one will be connected
	 */
	private void createConnection(FogDevice mobile, FogDevice to) {
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("Creating connection between: " + mobile.getName() + " <-> " + to.getName());
			
		mobile.getLatencyMap().put(to.getId(), MobilePathLossModel.LATENCY);
		to.getLatencyMap().put(mobile.getId(), MobilePathLossModel.LATENCY);
		
		double distance = Location.computeDistance(mobile, to);
		double rxPower = MobilePathLossModel.computeReceivedPower(distance);
		Map<String, Double> map = MobileBandwidthModel.computeCommunicationBandwidth(1, rxPower);
		
		
		String modulation = map.entrySet().iterator().next().getKey();
		double bandwidth = map.entrySet().iterator().next().getValue();
		
		if(Config.PRINT_DETAILS) {
			FogComputingSim.print("Communication between " + mobile.getName() + " and " + to.getName() + " is using " +
					modulation + " modulation" + " w/ bandwidth = "  + String.format("%.2f", bandwidth/1024/1024) + " MB/s");
		}
		
		mobile.getBandwidthMap().put(to.getId(), bandwidth);
		to.getBandwidthMap().put(mobile.getId(), bandwidth);
		
		mobile.getTupleQueue().put(to.getId(), new LinkedList<Pair<Tuple, Integer>>());
		to.getTupleQueue().put(mobile.getId(), new LinkedList<Pair<Tuple, Integer>>());
		
		mobile.getTupleLinkBusy().put(to.getId(), false);
		to.getTupleLinkBusy().put(mobile.getId(), false);
	}
	
	/**
	 * Notifies the fog nodes to remove their connections between them.
	 * 
	 * @param mobile one of the fog devices
	 * @param from another fog device
	 */
	private void removeConnection(FogDevice mobile, FogDevice from) {
		// Then, remove the old connections
		sendNow(mobile.getId(), FogEvents.CONNECTION_LOST, from.getId());
		sendNow(from.getId(), FogEvents.CONNECTION_LOST, mobile.getId());
	}
	
	/**
	 * Deploys the application application modules according a given module placement map.
	 * 
	 * @param modulePlacementMap the map between the node names and the application modules names
	 */
	private void deployApplications(Map<String, List<String>> modulePlacementMap) {
		for(Application application : appList) {
			Map<String, List<String>> moduleMapping = new HashMap<String, List<String>>();
			
			for(AppModule appModule : application.getModules()) {
				for(String deviceName : modulePlacementMap.keySet()) {
					if(modulePlacementMap.get(deviceName).contains(appModule.getName())) {
						if(!moduleMapping.containsKey(deviceName))
							moduleMapping.put(deviceName, new ArrayList<String>());
						if(!moduleMapping.get(deviceName).contains(appModule.getName()))
							moduleMapping.get(deviceName).add(appModule.getName());
					}
				}
			}
			
			ModulePlacement modulePlacement = new ModulePlacement(fogDevices, application, moduleMapping);
			submitApplication(application, modulePlacement);
		}
	}
	
	/**
	 * Verifies the differences between the current module placement and the one obtained from the optimization algorithm and notifies
	 * the corresponding nodes to perform migration if needed.
	 * 
	 * @param currentModulePlacement the current module placement
	 * @param previousModulePlacement the module placement obtained from the optimization algorithm
	 */
	private void migrateModules(int[][] currentModulePlacement, int[][] previousModulePlacement) {
		appModulePlacementPolicy.clear();
		
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int previousPlacement = -1;
			int currentPlacement = -1;
			
			for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
				if(currentModulePlacement[i][j] == 1) {
					currentPlacement = i;
				}
				
				if(previousModulePlacement[i][j] == 1) {
					previousPlacement = i;
				}
			}
			
			if(previousPlacement == -1 || previousPlacement == -1)
				FogComputingSim.err("Should not happen (Controller)");
			
			if(currentPlacement != previousPlacement) {
				AppModule module = getModuleByName(algorithm.getmName()[j]);
				
				if(module.isInMigration())
					continue;
				
				FogDevice from = getFogDeviceByName(algorithm.getfName()[previousPlacement]);
				FogDevice to = getFogDeviceByName(algorithm.getfName()[currentPlacement]);
				
				Application application = getApplicationByModule(module);
				
				if(application == null)
					FogComputingSim.err("Should not happen (Controller)");
				
				if(Config.PRINT_DETAILS)
					FogComputingSim.print("Migratig module: " + module.getName() +  " from: " + from.getName() + " to: " + to.getName());
				
				Map<FogDevice, Map<Application, AppModule>> map = new HashMap<FogDevice, Map<Application,AppModule>>();
				Map<Application, AppModule> appMap = new HashMap<Application, AppModule>();
				appMap.put(application, module);
				map.put(to, appMap);
				
				// notifies the fog device to perform migration
				sendNow(from.getId(), FogEvents.MIGRATION, map);
				
				nrMigrations++;
			}
		}
	}
	
	/**
	 * Updates the tuple routing tables for all fog devices.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param routingMatrix the tuple routing matrix obtained from the optimization algorithm
	 */
	private void updateTupleRoutingTables(Algorithm algorithm, int[][] routingMatrix) {
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
	}
	
	/**
	 * Updates the migration of virtual machines routing tables for all fog devices.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param migrationMatrix the virtual machine routing matrix obtained from the optimization algorithm
	 */
	private void updateMigrationTables(Algorithm algorithm, int[][] migrationMatrix) {
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
	}
	
	/**
	 * Gets a given fog device from its id.
	 * 
	 * @param id the id of the fog device
	 * @return the fog device itself; can be full if it was not found
	 */
	public FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices())
			if(id==fogDevice.getId())
				return fogDevice;
		return null;
	}
	
	/**
	 * Gets a given fog device from its name.
	 * 
	 * @param name the name of the fog device
	 * @return the fog device itself; can be full if it was not found
	 */
	private FogDevice getFogDeviceByName(String name){
		for(FogDevice fogDevice : getFogDevices())
			if(name.equals(fogDevice.getName()))
				return fogDevice;
		return null;
	}
	
	/**
	 * Gets a given application module from its name.
	 * 
	 * @param name the name of the application module
	 * @return the application module itself; can be full if it was not found
	 */
	private AppModule getModuleByName(String name){
		for(Application application : appList)
			for(AppModule appModule : application.getModules())
				if(appModule.getName().equals(name))
					return appModule;
		return null;
	}
	
	/**
	 * Gets a given application from one of its application modules names.
	 * 
	 * @param name the name of the application module name
	 * @return the application itself; can be full if it was not found
	 */
	private Application getApplicationByModule(AppModule appModule){		
		for(String appId : applications.keySet()) {
			Application application = applications.get(appId);
			if(application.getModules().contains(appModule))
				return application;
		}
		return null;
	}
	
	/**
	 * Gets the list containing all fog devices which it needs to supervise and manage.
	 * 
	 * @return the list containing all fog devices which it needs to supervise and manage
	 */
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}
	
	/**
	 * Sets the list containing all fog devices which it needs to supervise and manage.
	 * 
	 * @param fogDevices the list containing all fog devices which it needs to supervise and manage
	 */
	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}
	
	/**
	 * Gets the map between the name of the application and the application itself.
	 * 
	 * @return the map between the name of the application and the application itself
	 */
	public Map<String, Application> getApplications() {
		return applications;
	}
	
	/**
	 * Sets the map between the name of the application and the application itself.
	 * 
	 * @param applications the map between the name of the application and the application itself
	 */
	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}
	
	/**
	 * Gets the number of migrations performed during the whole simulation.
	 * 
	 * @return the number of migrations performed during the whole simulation
	 */
	public int getNrMigrations() {
		return nrMigrations;
	}
	
	/**
	 * Gets the number of nrHandovers performed during the whole simulation.
	 * 
	 * @return the number of nrHandovers performed during the whole simulation
	 */
	public int getNrHandovers() {
		return nrHandovers;
	}
	
}
