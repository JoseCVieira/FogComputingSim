package org.fog.core;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
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
import org.fog.placement.algorithms.overall.BF.BF;
import org.fog.placement.algorithms.overall.GA.GA;
import org.fog.placement.algorithms.overall.LP.LP;
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
	private static final int LP = 1;
	private static final int GA = 2;
	private static final int RAND = 3;
	private static final int BF = 4;
	private static final int MDP = 5;
	private static final int ALL = 6;

	private static final int EXIT = 0;
	private static final int GUI = 1;
	private static final int RANDOM = 2;
	private static final int VRGAME = 3;
	private static final int DCNS = 4;
	private static final int TEMP = 5;
	private static final int FILE = 6;
	
	private static List<Application> applications;
	private static List<FogBroker> fogBrokers;
	private static List<FogDevice> fogDevices;
	private static List<Sensor> sensors;
	private static List<Actuator> actuators;
	private static Controller controller;
	
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
		
		if(applications == null || applications.isEmpty() || fogBrokers == null || fogBrokers.isEmpty() ||
				fogDevices == null || fogDevices.isEmpty() || actuators == null || actuators.isEmpty() ||
				sensors == null || sensors.isEmpty() || controller == null)
			throw new IllegalArgumentException("Some of the received arguments are null or empty.");
		
		Job solution = null;
		Algorithm algorithm = null;
		switch (option) {
			case LP:
				System.out.println("Running the optimization algorithm: Linear programming.");
				algorithm = new LP(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();				
				break;
			case GA:
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GA(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Genetic Algorithm");
				break;
			case RAND:
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new Random(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Random Algorithm");
				break;
			case BF:
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BF(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Brute Force");
				break;
			case MDP:
				System.err.println("MDP is not implemented yet.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
				break;
			case ALL:
				System.out.println("Running the optimization algorithm: Linear programming.");
				algorithm = new LP(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				
				System.out.println("Running the optimization algorithm: Genetic Algorithm.");
				algorithm = new GA(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Genetic Algorithm");
				
				System.out.println("Running the optimization algorithm: Random Algorithm.");
				algorithm = new Random(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Random Algorithm");
				
				System.out.println("Running the optimization algorithm: Brute Force.");
				algorithm = new BF(fogBrokers, fogDevices, applications, sensors, actuators);
				solution = algorithm.execute();
				plotResult(algorithm, "Brute Force");
				break;
			default:
				System.err.println("Unknown algorithm.\nFogComputingSim will terminate abruptally.\n");
				System.exit(-1);
		}
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getRoutingMap() == null || solution.getCost() >= Constants.MIN_SOLUTION) {
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
		System.out.println("——————————————————————————————————————————————");
		System.out.println("|  FOG COMPUTING SIMULATOR MENU - ALGORITHM  |");
		System.out.println("|                                            |");
	    System.out.println("| Options:                                   |");
	    System.out.println("|       1. Linear Programming                |");
	    System.out.println("|       2. Genetic Algorithm                 |");
	    System.out.println("|       3. Random Algorithm                  |");
	    System.out.println("|       4. Brute Force                       |");
	    System.out.println("|       5. Markov Decision Process           |");
	    System.out.println("|       6. Compare all algorithms            |");
	    System.out.println("|       0. Exit                              |");
	    System.out.println("|                                            |");
	    System.out.println("——————————————————————————————————————————————");
	    System.out.print("\n Option: ");
	    
	    int option = -1;
	    while(option == -1) {
		    
		    try {
		    	option = new Scanner(System.in).nextInt();
		    	
		    	if(option == EXIT) {
		    		System.exit(0);
					break;
		    	}
		    	
		    	if(option < LP || option > ALL) {
		    		option = -1;
		    	}
		    	
			} catch (Exception e) {
				option = -1;
			}
		    
		    if(option == -1)
		    	System.out.print("Invalid input. Option: ");
	    }
	    
	    return option;
	}
	
	@SuppressWarnings("resource")
	private static void menuTopology() {
		System.out.println("————————————————————————————————————————————————————————");
		System.out.println("|  FOG COMPUTING SIMULATOR MENU - TOPOLOGY             |");
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
			    case EXIT:
			    	return;
				case GUI:
					Gui gui = new Gui();
					
					while(fogTest == null) {
						Util.promptEnterKey("Press \"ENTER\" to continue...");
						fogTest = gui.getRunGUI();
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
					Path path = FileSystems.getDefault().getPath(".");
					String dir = path + "/topologies/";
					
					System.out.println("Topologies found inside " + dir + ":");
					
					File folder = new File(dir);
					File[] listOfFiles = folder.listFiles();

					for (int i = 0; i < listOfFiles.length; i++) {
						if (listOfFiles[i].isFile()) {
							System.out.println("File[" + i + "]: " + listOfFiles[i].getName());
						}
					}
					
					System.out.print("\nEnter a file name: ");
					Scanner scanner = new Scanner(System. in);
			        String fileName = scanner. nextLine();
			        
			        if(fileName == null || fileName.isEmpty())
			        	break;
			        
			        
			        String filePath = path + "/topologies/" + fileName;
			        
			        if(!new File(filePath).exists())
			        	break;
			        
			        Graph graph= Bridge.jsonToGraph(filePath);
			    	fogTest = new RunGUI(graph);
					
			    	break;
				default:
					break;
			}
		    
		    if(fogTest == null) {
		    	System.out.print("Invalid input. Option: ");
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