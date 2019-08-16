package org.fog.core;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.gui.Gui;
import org.fog.gui.core.Bridge;
import org.fog.gui.core.Graph;
import org.fog.gui.core.RunGUI;
import org.fog.placement.Controller;
import org.fog.placement.ControllerAlgorithm;
import org.fog.test.DCNSFog;
import org.fog.test.RandomTopology;
import org.fog.test.TEMPFog;
import org.fog.test.VRGameFog;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

/**
 * Class which is responsible for running the FogComputingSim.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class FogComputingSim {
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
	
	public static void main(String[] args) {
		if(Config.DEBUG_MODE) {
			Logger.setLogLevel(Logger.DEBUG);
			Logger.setEnabled(true);
		}else
			Log.disable();
		
		// Initialize CloudSim
		CloudSim.init(Calendar.getInstance());
		
		// Create the topology to be tested and select the algorithm to be used
		int option = -1;
		while(option == -1 || applications == null) {
			option = menuAlgorithm();
			menuTopology();
		}
		
		if(applications == null || applications.isEmpty() || fogDevices == null || fogDevices.isEmpty() ||
				actuators == null || actuators.isEmpty() || sensors == null || sensors.isEmpty())
			throw new IllegalArgumentException("Some of the received arguments are null or empty.");
		
		// Create the controller object
		Controller controller = new Controller("master-controller", applications, fogDevices, sensors, actuators, option);
		
		for(FogDevice fogDevice : fogDevices) {
			fogDevice.setController(controller);
		}
		
		// Add mobile communications and run the optimization algorithm in the first place to deploy the applications' modules in the best way
		controller.updateTopology();
		
		System.out.println("Starting simulation...");
		TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
		
		// Starts the simulation
		CloudSim.startSimulation();
		
		// Ends the simulations
		CloudSim.stopSimulation();
		System.out.println("Simulation finished.");
		System.exit(0);
	}
	
	/**
	 * Prints a menu to allow choosing one of the optimization algorithms.
	 * 
	 * @return the number of the optimization algorithms
	 */
	@SuppressWarnings("resource")
	private static int menuAlgorithm() {
		System.out.println("—————————————————————————————————————————————————");
		System.out.println("|    FOG COMPUTING SIMULATOR MENU - ALGORITHM   |");
		System.out.println("|                                               |");
	    System.out.println("| Options:                                      |");
	    System.out.println("|       1. Multiobjective Linear Programming    |");
	    System.out.println("|       2. Multiobjective Genetic Algorithm     |");
	    System.out.println("|                  -------------                |");
	    System.out.println("|       3. Linear Programming                   |");
	    System.out.println("|       4. Genetic Algorithm                    |");
	    System.out.println("|       5. Random Algorithm                     |");
	    System.out.println("|       6. Brute Force                          |");
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
		    	
		    	if(option < 0 || option > ControllerAlgorithm.NR_ALGORITHMS) {
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
	
	/**
	 * Prints a menu to allow choosing one of the topology generators and generates the topology to be tested.
	 */
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
	    
	    Topology topology = null;
	    while(topology == null) {
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
					
					while(topology == null) {
						Util.promptEnterKey("Press \"ENTER\" to continue...");
						topology = gui.getRunGUI();
					}
					break;
				case RANDOM:
					topology = new RandomTopology();
					break;
				case VRGAME:
					topology = new VRGameFog();
					break;
				case DCNS:
					topology = new DCNSFog();
					break;
				case TEMP:
					topology = new TEMPFog();
					break;
				case FILE:
					String filePath = menuFile();
					if(filePath != null) {
						Graph graph= Bridge.jsonToGraph(filePath);
						topology = new RunGUI(graph);
					}else {
						returning = true;
					}
			    	break;
				default:
					break;
			}
		    
		    if(topology == null) {
		    	if(returning) {
		    		returning = false;
		    		System.out.print("Topology: ");
		    		continue;
		    	}
		    	System.out.print("Invalid input. Topology: ");
		    }
	    }
	    
	    applications = topology.getApplications();
		fogDevices = topology.getFogDevices();
		sensors = topology.getSensors();
		actuators = topology.getActuators();
	}
	
	/**
	 * Prints a menu to allow choosing one of the topology generators which is inside a JSON file.
	 * 
	 * @return the path to the file
	 */
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
		System.out.println(Util.leftString(54, "|    Topologies found inside " + dir + ":") + " |");
		System.out.println("|                                                      |");
	    System.out.println("| Options:                                             |");
	    
	    for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println(Util.leftString(54, "|       " + (i+1) + ". " + listOfFiles[i].getName()) + " |");
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
	
	/**
	 * Outputs a given error message and terminates the program.
	 * 
	 * @param err the error message to be printed
	 */
	public static void err(final String err) {
		System.err.println("err: [ " + err + " ]");
		System.err.println("FogComputingSim will terminate abruptally.\n");
		System.exit(-1);
	}
	
	/**
	 * Prints a log along with the simulation's timestamp.
	 * 
	 * @param str the message to be printed
	 */
	public static void print(final String str) {
		DecimalFormat df = new DecimalFormat("0.00");
		String clock = Util.centerString(13, df.format(CloudSim.clock()));
		System.out.println("Clock=" + clock + "-> " + str);
	}
	
}