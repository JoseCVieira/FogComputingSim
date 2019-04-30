package org.fog.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.gui.core.FogDeviceGui;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementMapping;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.Job;
import org.fog.placement.algorithms.placement.BF.BF;
import org.fog.placement.algorithms.placement.GA.GA;
import org.fog.placement.algorithms.placement.LP.LP;
import org.fog.placement.algorithms.placement.PSO.PSO;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

public class FogComputingSim {
	private static final boolean DEBUG_MODE = false;
	private static final boolean COMPARE_WITH_LP = false;
	private static final String OPTIMIZATION_ALGORITHM = "LP";
	
	private List<Application> applications;
	private List<FogBroker> fogBrokers;
	private List<FogDevice> fogDevices;
	private Controller controller;
	
	public FogComputingSim(final List<Application> applications, final List<FogBroker> fogBrokers,
			final List<FogDevice> fogDevices, final List<Actuator> actuators, final List<Sensor> sensors,
			final Controller controller) throws IllegalArgumentException {
		
		if(applications == null || applications.isEmpty() || fogBrokers == null || fogBrokers.isEmpty() ||
				fogDevices == null || fogDevices.isEmpty() || actuators == null || actuators.isEmpty() ||
				sensors == null || sensors.isEmpty() || controller == null)
			throw new IllegalArgumentException("Some of the received arguments are null or empty.");

		this.applications = applications;
		this.fogBrokers = fogBrokers;
		this.fogDevices = fogDevices;
		this.controller = controller;
		
		if(DEBUG_MODE) {
			Logger.setLogLevel(Logger.DEBUG);
			Logger.setEnabled(true);
		}else
			Log.disable();
		
		CloudSim.init(Calendar.getInstance());
		
		Job solution = null;
		Algorithm algorithm = null;
		switch (OPTIMIZATION_ALGORITHM) {
			case "BF":
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BF(fogBrokers, fogDevices, applications, sensors, actuators);
				break;
			case "LP":
				System.out.println("Running the optimization algorithm: Linear programming.");
				algorithm = new LP(fogBrokers, fogDevices, applications, sensors, actuators);
				break;
			case "GA":
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GA(fogBrokers, fogDevices, applications, sensors, actuators);
				break;
			case "PSO":
				System.out.println("Running the optimization algorithm: Particle Swarm Optimization.");
				algorithm = new PSO(fogBrokers, fogDevices, applications, sensors, actuators);
				break;
			case "MDP":
				System.err.println("MDP is not implemented yet.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
				break;
			default:
				System.err.println("Unknown algorithm.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
		}
		
		solution = algorithm.execute();
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getRoutingMap() == null) {
			System.err.println("There is no possible combination to deploy all applications.\n");
			System.err.println("FogComputingSim will terminate abruptally.\n");
			System.exit(-1);
		}
		
		if(COMPARE_WITH_LP) {
			System.out.println("Running the optimization algorithm: Linear programming.");
			new LP(fogBrokers, fogDevices, applications, sensors, actuators).execute();
		}
		
		deployApplications(algorithm.extractPlacementMap(solution.getModulePlacementMap()));
		createRoutingTables(algorithm, solution.getRoutingMap());
			
		System.out.println("Starting simulation...");
		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
		CloudSim.startSimulation();
		CloudSim.stopSimulation();
		System.out.println("Simulation finished.");
		System.exit(0);
	}
	
	private void deployApplications(Map<String, List<String>> modulePlacementMap) {		
		for(FogDevice fogDevice : fogDevices) {
			FogBroker broker = getFogBrokerByName(fogDevice.getName());
			
			List<String> apps = fogDevice.getActiveApplications();
			fogDevice.setActiveApplications(new ArrayList<String>());
			
			System.out.println(apps);
			
			for(String app : apps) {
				for(Application application : applications) {
					if(application.getAppId().equals(app + "_" + broker.getId())) {
						
						ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
						
						for(AppModule appModule : application.getModules())
							for(String fogName : modulePlacementMap.keySet())
								if(modulePlacementMap.get(fogName).contains(appModule.getName()))
									moduleMapping.addModuleToDevice(appModule.getName(), fogName);
						
						ModulePlacement modulePlacement = new ModulePlacementMapping(fogDevices, application, moduleMapping);
						controller.submitApplication(application, modulePlacement);
					}
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
	
	private FogBroker getFogBrokerByName(String name) {
		for(FogBroker fogBroker : fogBrokers)
			if(fogBroker.getName().equals(name))
				return fogBroker;
		return null;
	}
	
	private FogDevice getFogDeviceById(int id) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getId() == id)
				return fogDevice;
		return null;
	}
	
}