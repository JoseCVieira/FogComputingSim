package org.fog.core;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.gui.Gui;
import org.fog.gui.core.Bridge;
import org.fog.gui.core.Graph;
import org.fog.gui.core.RunGUI;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacement;
import org.fog.placement.ModulePlacementMapping;
import org.fog.placement.algorithms.overall.Algorithm;
import org.fog.placement.algorithms.overall.Job;
import org.fog.placement.algorithms.overall.bf.BruteForce;
import org.fog.placement.algorithms.overall.ga.GeneticAlgorithm;
import org.fog.placement.algorithms.overall.lp.LinearProgramming;
import org.fog.placement.algorithms.overall.lp.MultiObjectiveLinearProgramming;
import org.fog.placement.algorithms.overall.random.Random;
import org.fog.placement.algorithms.overall.util.MatlabChartUtils;
import org.fog.test.DCNSFog;
import org.fog.test.RandomTopology;
import org.fog.test.TEMPFog;
import org.fog.test.VRGameFog;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class FogComputingSim {
	private static final int MOLP = 1;
	private static final int LP = 2;
	private static final int GA = 3;
	private static final int RAND = 4;
	private static final int BF = 5;
	private static final int MDP = 6;
	private static final int ALL = 7;

	private static final int EXIT = 0;
	private static final int GUI = 1;
	private static final int RANDOM = 2;
	private static final int VRGAME = 3;
	private static final int DCNS = 4;
	private static final int TEMP = 5;
	private static final int FILE = 6;
	
	private static List<Application> applications;
	private static List<FogDevice> fogDevices;
	private static List<Sensor> sensors;
	private static List<Actuator> actuators;
	private static Controller controller;
	private static Map<String, LinkedHashSet<String>> appToFogMap;
	
	public static boolean isDisplayingPlot = false;
	
	public static void main(String[] args) {
		if(Config.DEBUG_MODE) {
			Logger.setLogLevel(Logger.DEBUG);
			Logger.setEnabled(true);
		}else
			Log.disable();
		
		CloudSim.init(Calendar.getInstance());
		
		int option = -1;
		while(option == -1 || applications == null) {
			option = menuAlgorithm();
			menuTopology();
		}
		
		if(applications == null || applications.isEmpty() || fogDevices == null || fogDevices.isEmpty() ||
				actuators == null || actuators.isEmpty() || sensors == null || sensors.isEmpty() || controller == null)
			throw new IllegalArgumentException("Some of the received arguments are null or empty.");
		
		Job solution = null;
		Algorithm algorithm = null;
		switch (option) {
			case MOLP:
				Config.SINGLE_OBJECTIVE = false;
				System.out.println("Running the optimization algorithm: Multiobjective Linear Programming.");
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				break;
			case LP:
				System.out.println("Running the optimization algorithm: Linear Programming.");
				algorithm = new LinearProgramming(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				break;
			case GA:
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GeneticAlgorithm(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Genetic Algorithm");
				break;
			case RAND:
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new Random(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Random Algorithm");
				break;
			case BF:
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BruteForce(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Brute Force");
				break;
			case MDP:
				System.err.println("MDP is not implemented yet.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
				break;
			case ALL:
				System.out.println("Running the optimization algorithm: Multiobjective Linear Programming.");
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				
				System.out.println("Running the optimization algorithm: Linear programming.");
				algorithm = new LinearProgramming(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GeneticAlgorithm(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Genetic Algorithm");
				
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new Random(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Random Algorithm");
				
				/*System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BruteForce(fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Brute Force");*/
				break;
			default:
				System.err.println("Unknown algorithm.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
		}
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getRoutingMap() == null || !solution.isValid()) {
			System.err.println("There is no possible combination to deploy all applications.\n");
			System.err.println("FogComputingSim will terminate abruptally.\n");
			System.exit(-1);
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
	private static int menuAlgorithm() {
		System.out.println("—————————————————————————————————————————————————");
		System.out.println("|    FOG COMPUTING SIMULATOR MENU - ALGORITHM   |");
		System.out.println("|                                               |");
	    System.out.println("| Options:                                      |");
	    System.out.println("|       1. Multiobjective Linear Programming    |");
	    System.out.println("|       2. Linear Programming                   |");
	    System.out.println("|       3. Genetic Algorithm                    |");
	    System.out.println("|       4. Random Algorithm                     |");
	    System.out.println("|       5. Brute Force                          |");
	    System.out.println("|       6. Markov Decision Process              |");
	    System.out.println("|       7. Compare all algorithms               |");
	    System.out.println("|       0. Exit                                 |");
	    System.out.println("|                                               |");
	    System.out.println("—————————————————————————————————————————————————");
	    System.out.print("\n Algorithm: ");
	    
	    int option = -1;
	    while(option == -1) {
		    
		    try {
		    	option = new Scanner(System.in).nextInt();
		    	
		    	if(option == EXIT) {
		    		System.exit(0);
					break;
		    	}
		    	
		    	if(option < 0 || option > ALL) {
		    		option = -1;
		    	}
		    	
			} catch (Exception e) {
				option = -1;
			}
		    
		    if(option == -1)
		    	System.out.print("Invalid input. Algorithm: ");
	    }
	    
	    return option;
	}
	
	@SuppressWarnings("resource")
	private static void menuTopology() {
		System.out.println("————————————————————————————————————————————————————————");
		System.out.println("|       FOG COMPUTING SIMULATOR MENU - TOPOLOGY        |");
		System.out.println("|                                                      |");
	    System.out.println("| Options:                                             |");
	    System.out.println("|       1. GUI                                         |");
	    System.out.println("|       2. Random Topology                             |");
	    System.out.println("|       3. VRGameFog - iFogSim Example                 |");
	    System.out.println("|       4. DCNSFog   - iFogSim Example                 |");
	    System.out.println("|       5. TEMPFog   - iFogSim Example                 |");
	    System.out.println("|       6. Read JSON file (/topologies/<file_name>)    |");
	    System.out.println("|       0. Back                                        |");
	    System.out.println("|                                                      |");
	    System.out.println("————————————————————————————————————————————————————————");
	    System.out.print("\n Topology: ");
	    
	    FogTest fogTest = null;
	    while(fogTest == null) {
		    int option = -1;
		    
		    try {
		    	option = new Scanner(System.in).nextInt();
			} catch (Exception e) {
				option = -1;
			}
		    
		    boolean returning = false;
		    switch (option) {
			    case EXIT:
			    	return;
				case GUI:
					Gui gui = new Gui();
					
					while(fogTest == null) {
						Util.promptEnterKey("Press \"ENTER\" to continue...");
						fogTest = gui.getRunGUI(); // fogTest = gui.getFogTest();
					}
					break;
				case RANDOM:
					fogTest = new RandomTopology();
					break;
				case VRGAME:
					fogTest = new VRGameFog();
					break;
				case DCNS:
					fogTest = new DCNSFog();
					break;
				case TEMP:
					fogTest = new TEMPFog();
					break;
				case FILE:
					String filePath = menuFile();
					if(filePath != null) {
						Graph graph= Bridge.jsonToGraph(filePath);
				    	fogTest = new RunGUI(graph);
					}else {
						returning = true;
					}
			    	break;
				default:
					break;
			}
		    
		    if(fogTest == null) {
		    	if(returning) {
		    		returning = false;
		    		System.out.print("Topology: ");
		    		continue;
		    	}
		    	System.out.print("Invalid input. Topology: ");
		    }
	    }
	    
	    applications = fogTest.getApplications();
		fogDevices = fogTest.getFogDevices();
		controller = fogTest.getController();
		sensors = fogTest.getSensors();
		actuators = fogTest.getActuators();
		appToFogMap = fogTest.getAppToFogMap();
	}
	
	@SuppressWarnings("resource")
	private static String menuFile() {
		Path path = FileSystems.getDefault().getPath(".");
		String dir = path + "/topologies/";
		
		System.out.println("Topologies found inside " + dir + ":");
		
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles.length == 0) {
			System.out.println("There are no available topologies in this folder.\n");
			return null;
		}
		
		
		
		System.out.println("————————————————————————————————————————————————————————");
		System.out.println("|          FOG COMPUTING SIMULATOR MENU - FILE         |");
		System.out.println("|                                                      |");
		System.out.println(String.format("%-54s |", "|    Topologies found inside " + dir + ":"));
		System.out.println("|                                                      |");
	    System.out.println("| Options:                                             |");
	    
	    for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println(String.format("%-54s |", "|       " + (i+1) + ". " + listOfFiles[i].getName()));
			}
		}
	    
	    System.out.println("|       0. Back                                        |");
	    System.out.println("|                                                      |");
	    System.out.println("————————————————————————————————————————————————————————");
	    System.out.print("\n File: ");
	    

		
		int fileIndex = -1;
	    
		while(fileIndex == -1) {
		    try {
		    	fileIndex = new Scanner(System.in).nextInt();
		    	
		    	if(fileIndex == 0)
		    		return null;
		    	
		    	fileIndex--;
		    	
		    	if(fileIndex < 0 || fileIndex > listOfFiles.length - 1) {
		    		fileIndex = -1;
		    	}
			} catch (Exception e) {
				fileIndex = -1;
			}
		    
		    if(fileIndex == -1) {
		    	System.out.println("Invalid number. File: ");
		    }
		}			        
        
        return path + "/topologies/" + listOfFiles[fileIndex].getName();
	}
	
	private static void deployApplications(Map<String, List<String>> modulePlacementMap) {
		for(FogDevice fogDevice : fogDevices) {			
			if(appToFogMap.containsKey(fogDevice.getName())) {
				for(Application application : applications) {
					ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
					
					for(AppModule appModule : application.getModules()) {
						for(String fogName : modulePlacementMap.keySet()) {
							if(modulePlacementMap.get(fogName).contains(appModule.getName())) {
								moduleMapping.addModuleToDevice(appModule.getName(), fogName);
							}
						}
					}
					
					ModulePlacement modulePlacement = new ModulePlacementMapping(fogDevices, application, moduleMapping);
					controller.submitApplication(application, modulePlacement);
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
	
	private static void plotResult(Algorithm algorithm, String title) {
		isDisplayingPlot = true;
		MatlabChartUtils matlabChartUtils = new MatlabChartUtils(algorithm, title);
    	matlabChartUtils.setVisible(true);
	}
	
	private static FogDevice getFogDeviceById(int id) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getId() == id)
				return fogDevice;
		return null;
	}
	
}