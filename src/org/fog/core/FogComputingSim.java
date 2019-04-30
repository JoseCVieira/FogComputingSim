package org.fog.core;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
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
import org.fog.test.RandomTopology;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

public class FogComputingSim {
	private static List<Application> applications;
	private static List<FogBroker> fogBrokers;
	private static List<FogDevice> fogDevices;
	private static List<Sensor> sensors;
	private static List<Actuator> actuators;
	private static Controller controller;
	
	public static void main(String[] args) {
		if(Config.DEBUG_MODE) {
			Logger.setLogLevel(Logger.DEBUG);
			Logger.setEnabled(true);
		}else
			Log.disable();
		
		CloudSim.init(Calendar.getInstance());
		
		menu();
		
		if(applications == null || applications.isEmpty() || fogBrokers == null || fogBrokers.isEmpty() ||
				fogDevices == null || fogDevices.isEmpty() || actuators == null || actuators.isEmpty() ||
				sensors == null || sensors.isEmpty() || controller == null)
			throw new IllegalArgumentException("Some of the received arguments are null or empty.");
		
		Job solution = null;
		Algorithm algorithm = null;
		
		System.out.println("Running the optimization algorithm: Brute Force.");
		algorithm = new BF(fogBrokers, fogDevices, applications, sensors, actuators);
		solution = algorithm.execute();
		
		switch (Config.OPTIMIZATION_ALGORITHM) {
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
		
		if(Config.COMPARE_WITH_LP) {
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
	
	@SuppressWarnings("resource")
	private static void menu() {
		System.out.println("——————————————————————————————————————————");
		System.out.println("|       FOG COMPUTING SIMULATOR MENU      |");
		System.out.println("|                                         |");
	    System.out.println("| Options:                                |");
	    System.out.println("|         1. GUI                          |");
	    System.out.println("|         2. Random Topology              |");
	    System.out.println("|         3. VRGameFog - iFogSim Example  |");
	    System.out.println("|         4. TEMPFog   - iFogSim Example  |");
	    System.out.println("|         5. DCNSFog   - iFogSim Example  |");
	    System.out.println("|         6. TwoApps   - iFogSim Example  |");
	    System.out.println("|         0. Exit                         |");
	    System.out.println("|                                         |");
	    System.out.println("——————————————————————————————————————————");
	    System.out.print("\n Option: ");
	    
	    FogTest fogTest = null;
	    while(fogTest == null) {		    
		    int option = -1;
		    
		    try {
		    	option = new Scanner(System.in).nextInt();
			} catch (Exception e) {
				option = -1;
			}
	    
		    switch (option) {
			    case 0:
					System.exit(0);
					break;
				case 1:
					System.out.println("Not working yet. Option: ");
					//fogTest = new Gui().getRunGUI();
					break;
				case 2:
					fogTest = new RandomTopology();
					break;
				case 3:
				case 4:
				case 5:
				case 6:
					System.out.println("Not implemented yet. Option: ");
					break;
				default:
					System.out.print("Invalid input. Option: ");
					break;
			}
	    }
	    
	    applications = fogTest.getApplications();
		fogBrokers = fogTest.getFogBrokers();
		fogDevices = fogTest.getFogDevices();
		controller = fogTest.getController();
		sensors = fogTest.getSensors();
		actuators = fogTest.getActuators();
	}
	
	private static void deployApplications(Map<String, List<String>> modulePlacementMap) {		
		for(FogDevice fogDevice : fogDevices) {
			FogBroker broker = getFogBrokerByName(fogDevice.getName());
			
			List<String> apps = fogDevice.getActiveApplications();
			fogDevice.setActiveApplications(new ArrayList<String>());
			
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
	
	private static void createRoutingTables(Algorithm algorithm, int[][] routingMatrix) {
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
	
	private static FogBroker getFogBrokerByName(String name) {
		for(FogBroker fogBroker : fogBrokers)
			if(fogBroker.getName().equals(name))
				return fogBroker;
		return null;
	}
	
	private static FogDevice getFogDeviceById(int id) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getId() == id)
				return fogDevice;
		return null;
	}
	
}