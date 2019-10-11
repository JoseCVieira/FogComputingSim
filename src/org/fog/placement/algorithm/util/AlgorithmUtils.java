package org.fog.placement.algorithm.util;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;
import org.fog.utils.Util;

/**
 * Class which defines some utility methods used along the optimization algorithms and to debug.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AlgorithmUtils {
	
	/**
	 * Prints a given integer matrix.
	 * 
	 * @param text the text to be displayed
	 * @param matrix the matrix to be printed
	 */
	public static void print(final String text, final int[][] matrix) {
		if(text == null || matrix == null) {
			FogComputingSim.err("AlgorithmUtils Err: Some of the received arguments are null");
		}
		
		System.out.println(text);
		
		int r = matrix.length;
		int c = matrix[0].length;
		
        for(int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++)
            	System.out.print(matrix[i][j] + " ");
            System.out.println();
        }
        
        System.out.println("\n");
	}
	
	/**
	 * Prints a given double matrix.
	 * 
	 * @param text the text to be displayed
	 * @param matrix the matrix to be printed
	 */
	public static void print(final String text, final double[][] matrix) {
		if(text == null || matrix == null) {
			FogComputingSim.err("AlgorithmUtils Err: Some of the received arguments are null");
		}
		
		System.out.println(text);
		
		int r = matrix.length;
		int c = matrix[0].length;
		
        for(int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {            	
            	if(matrix[i][j] == Constants.INF) {
					System.out.format(Util.centerString(20, "Inf"));
            	}else if(matrix[i][j] == 0) {
					System.out.format(Util.centerString(20, "-"));
            	}else {
					System.out.format(Util.centerString(20, String.format("%.3E", matrix[i][j])));
            	}
            }
            System.out.println();
        }
        
        System.out.println("\n");
	}
	
	/**
	 * Prints a given integer vector.
	 * 
	 * @param text the text to be displayed
	 * @param vector the vector to be printed
	 */
	public static void print(final String text, final int[] vector) {
		if(text == null || vector == null) {
			FogComputingSim.err("AlgorithmUtils Err: Some of the received arguments are null");
		}
		
		System.out.println(text);
		
        for(int i = 0; i < vector.length; i++)
        	System.out.print(vector[i]+ " ");
        System.out.println("\n");
	}
	
	/**
	 * Prints a given double vector.
	 * 
	 * @param text the text to be displayed
	 * @param vector the vector to be printed
	 */
	public static void print(final String text, final double[] vector) {
		if(text == null || vector == null) {
			FogComputingSim.err("AlgorithmUtils Err: Some of the received arguments are null");
		}
		
		System.out.println(text);
		
        for(int i = 0; i < vector.length; i++)
        	System.out.print(vector[i]+ " ");
        System.out.println("\n");
	}
	
	/**
	 * Prints the parameters previously parsed which are used along the optimization algorithm.
	 * 
	 * @param al the algorithm object
	 * @param fogDevices list containing all fog devices
	 * @param applications list containing all applications
	 * @param sensors list containing all sensors
	 * @param actuators list containing all actuators
	 */
	public static void printAlgorithmDetails(final Algorithm al, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		System.out.println("\n\n---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------");
		System.out.println("                                                                                 ALG. DETAILS START");
		System.out.println("---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------\n");
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tFOG NODES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		for(int i = 0; i < fogDevices.size(); i++) {
			FogDevice fDevice = null;
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(al.getfName()[i]))
					fDevice = fogDevice;
			
			System.out.println(Util.leftString(24, "Id: ") + fDevice.getId());
			System.out.println(Util.leftString(24, "Name: ") + al.getfName()[i]);
			System.out.println(Util.leftString(24, "Client: ") + (al.getfIsFogDevice()[i] == 1 ? "False" : "True"));
			System.out.println(Util.leftString(24, "Mips [MIPS]: ") + al.getfMips()[i]);
			System.out.println(Util.leftString(24, "Ram [Byte]: ") + al.getfRam()[i]);
			System.out.println(Util.leftString(24, "Storage [Byte]: ") + al.getfStrg()[i]);
			System.out.println(Util.leftString(24, "Mips price [€]: ") + al.getfMipsPrice()[i]);
			System.out.println(Util.leftString(24, "Ram price [€]: ") + al.getfRamPrice()[i]);
			System.out.println(Util.leftString(24, "Storage price [€]: ") + al.getfStrgPrice()[i]);
			System.out.println(Util.leftString(24, "Bandwidth price [€]: ") + al.getfBwPrice()[i]);
			System.out.println(Util.leftString(24, "Energy price [€]: ") + al.getfPwPrice()[i]);
			System.out.println(Util.leftString(24, "Busy power [W]: ") + al.getfBusyPw()[i]);
			System.out.println(Util.leftString(24, "Idle power [W]: ") + al.getfIdlePw()[i]);
			System.out.println(Util.leftString(24, "Transmission power [W]: ") + al.getfTxPw()[i]);
			
			if(i < fogDevices.size() -1)
				System.out.println();
		}
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tAPP MODULES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		
		for(int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.println(Util.leftString(17, "Name: ") + al.getmName()[i]);
			System.out.println(Util.leftString(17, "Mips [MIPS]: ") + al.getmMips()[i]);
			System.out.println(Util.leftString(17, "Ram [Byte]: ") + al.getmRam()[i]);
			System.out.println(Util.leftString(17, "Strorage [Byte]: ") + al.getmStrg()[i]);
			
			if(i < al.getNumberOfModules() -1)
				System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBANDWIDTH MAP (Between modules) [B/s]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(Util.centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++) {
				if(al.getmBandwidthMap()[i][j] != 0)
					System.out.format(Util.centerString(20, String.format("%.5f", al.getmBandwidthMap()[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			}
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tDEPENDENCY MAP (Between modules) [s^-1]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(Util.centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getmDependencyMap()[i][j] != 0.0)
					System.out.format(Util.centerString(20, String.format("%.5f", al.getmDependencyMap()[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tCPU MAP (Between modules) [MI]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(Util.centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getmCPUMap()[i][j] != 0.0)
					System.out.format(Util.centerString(20, String.format("%.2f", al.getmCPUMap()[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tNETWORK MAP (Between modules) [Byte]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(Util.centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getmNWMap()[i][j] != 0.0)
					System.out.format(Util.centerString(20, String.format("%.5f", al.getmNWMap()[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tPOSSIBLE POSITIONING:");
		System.out.println("*******************************************************\n");

		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(Util.centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getPossibleDeployment()[i][j] != 0.0)
					System.out.format(Util.centerString(20, Integer.toString((int) al.getPossibleDeployment()[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBANDWIDTH MAP (Between nodes) [MByte/s]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfNodes(); i++)
			System.out.format(Util.centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(Util.centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfNodes(); j++) {
				if(al.getfBandwidthMap()[i][j] == Constants.INF)
					System.out.format(Util.centerString(20, "Inf"));
				else if(al.getfBandwidthMap()[i][j] == 0)
					System.out.format(Util.centerString(20, "-"));
				else
					System.out.format(Util.centerString(20, String.format("%.2f", al.getfBandwidthMap()[i][j]/1024/1024)));
			}
			System.out.println();
		}		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tLATENCY MAP (Between nodes) [s]:");
		System.out.println("*******************************************************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfNodes(); i++)
			System.out.format(Util.centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(Util.centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfNodes(); j++) {
				if(al.getfLatencyMap()[i][j] == Constants.INF)
					System.out.format(Util.centerString(20, "Inf"));
				else if(al.getfLatencyMap()[i][j] == 0)
					System.out.format(Util.centerString(20, "-"));
				else
					System.out.format(Util.centerString(20, String.format("%.2f", al.getfLatencyMap()[i][j])));
			}
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tLOOPS DEADLINE [s]:");
		System.out.println("*******************************************************\n");
		
		for (int i = 0; i < al.getNumberOfLoops(); i++) {
			for (int j = 0; j < al.getNumberOfModules(); j++) {
				if(al.getLoops()[i][j] == -1) break;
				
				if(j != 0) System.out.print(" -> ");
				System.out.format(Util.centerString(20, al.getmName()[al.getLoops()[i][j]]));
			}
			
			if(al.getLoopsDeadline()[i] != Constants.INF)
				System.out.format(":::: " + Util.centerString(25, ("Deadline: " + al.getLoopsDeadline()[i])) + "\n");
			else
				System.out.format(":::: " + Util.centerString(25, ("Deadline: INF" )) + "\n");
		}
		
		System.out.println("\n---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------");
		System.out.println("                                                                                 ALG. DETAILS END");
		System.out.println("---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------");
	}
	
	/**
	 * Prints the algorithm results.
	 * 
	 * @param al the algorithm object
	 * @param solution the final solution
	 */
	public static void printAlgorithmResults(final Algorithm al, final Solution solution) {
		System.out.println("\n\n*******************************************************");
		System.out.println("\t\tALGORITHM OUTPUT:");
		System.out.println("*******************************************************\n");
		
		printSolution(al, solution, -1);
		
		System.out.println("\n**Algorithm Elapsed time: " + al.getElapsedTime() + " ms**\n\n");		
	}
	
	/**
	 * Prints a given solution.
	 * 
	 * @param al the algorithm object
	 * @param solution the final solution
	 */
	public static void printSolution(final Algorithm al, final Solution solution, int iteration) {
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] routingMap = solution.getTupleRoutingMap();
		int[][] migrationMap = solution.getMigrationRoutingMap();
		
		if(iteration >= 0) {
			System.out.println("\n\n---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------");
			System.out.println("                                                                                New best solution ( iteration: " + iteration + " )");
			System.out.println("---------------------------------------------------------------------------------------- '' ----------------------------------------------------------------------------------------");
		}
		
		System.out.println("\n****************** MODULE PLACEMENT MAP ********************\n");
		
		System.out.format(Util.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(Util.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(Util.centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] != 0)
					System.out.format(Util.centerString(20, Integer.toString(modulePlacementMap[i][j])));
				else
					System.out.format(Util.centerString(20, "-"));
			}
			System.out.println();
		}
		
		System.out.println("\n******************** TUPLE ROUTING MAP *********************\n");
		
		for (int i = 0; i < routingMap.length; i++) {
			String startNode = al.getmName()[al.getStartModDependency(i)];
			String finalNode = al.getmName()[al.getFinalModDependency(i)];
			
			System.out.format(Util.leftString(25, "From: " + startNode));
			System.out.format(Util.leftString(25, "To: " + finalNode) + " .......... ");
			
			for (int j = 0; j < routingMap[0].length; j++) {
				System.out.format(Util.centerString(20, al.getfName()[routingMap[i][j]]));
				
				if(j < routingMap[0].length - 1)
					System.out.print(" -> ");
			}
			System.out.println();
		}
		
		System.out.println("\n****************** MIGRATION ROUTING MAP *******************\n");
		
		for (int i = 0; i < migrationMap.length; i++) {
			System.out.format(Util.leftString(25, "VM name: " + al.getmName()[i]) + " .......... ");
			
			for (int j = 0; j < migrationMap[0].length; j++) {
				System.out.format(Util.centerString(20, al.getfName()[migrationMap[i][j]]));
				
				if(j < migrationMap[0].length - 1)
					System.out.print(" -> ");
			}
			System.out.println();
		}
		
		System.out.println("\n********************* COST FUNCTIONS ***********************\n");
		
		for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
			System.out.format("Function: " + Util.leftString(10, Config.objectiveNames[i]));
			System.out.println(" priority: " + Config.priorities[i] + " cost: " + solution.getDetailedCost(i));
		}
		
		System.out.println("\n********************* LOOP DEADLINES ***********************\n");
		
		System.out.println("Number of loops: " + al.getNumberOfLoops() + " | Number of ensured loops: " +
				solution.getDetailedCost(Config.QOS_COST) + "\n");
		
		for(int i = 0; i < al.getNumberOfLoops(); i++) {
			System.out.print("Loop " + i + ": [ " );
			
			for(int j = 0; j < al.getNumberOfModules(); j++) {
				if(j == al.getNumberOfModules() - 1 || al.getLoops()[i][j+1] == -1) {
					System.out.print(al.getmName()[al.getLoops()[i][j]] + " ] -> ");
					break;
				}
				System.out.print(al.getmName()[al.getLoops()[i][j]] + " -> ");
			}
			
			System.out.print("latency (worst case): " + String.format("%.5f", solution.getLoopDeadline(i)) +
					" sec, deadline: " + al.getLoopsDeadline()[i] + " sec\n");
		}
		
		System.out.println("\n******************* MIGRATION DEADLINES ********************\n");
		
		for(int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format("Module: " + Util.centerString(20, al.getmName()[i]));
			
			System.out.print(" -> latency (worst case): " + String.format("%.5f", solution.getMigrationDeadline(i)) +
					" sec, deadline: " + al.getmMigD()[i] + " sec\n");
		}
	}
	
}
