package org.fog.placement.algorithm.util;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.utils.Util;

/**
 * Class which defines some utility methods used along the optimization algorithms and to debug.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since   July, 2019
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
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tFOG NODES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		for(int i = 0; i < fogDevices.size(); i++) {
			FogDevice fDevice = null;
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(al.getfName()[i]))
					fDevice = fogDevice;
			
			System.out.println(Util.leftString(26, "Id: ") + fDevice.getId());
			System.out.println(Util.leftString(26, "Name: ") + al.getfName()[i]);
			System.out.println(Util.leftString(26, "Mips [MIPS]: ") + al.getfMips()[i]);
			System.out.println(Util.leftString(26, "Ram [kB]: ") + al.getfRam()[i]);
			System.out.println(Util.leftString(26, "Storage [kB]: ") + al.getfStrg()[i]);
			System.out.println(Util.leftString(26, "Mips price [€/MIPS]: ") + al.getfMipsPrice()[i]);
			System.out.println(Util.leftString(26, "Ram price [€/kB]: ") + al.getfRamPrice()[i]);
			System.out.println(Util.leftString(26, "Storage price [€/kB]: ") + al.getfStrgPrice()[i]);
			System.out.println(Util.leftString(26, "Bandwidth price [€/kB/s]: ") + al.getfBwPrice()[i]);
			System.out.println(Util.leftString(26, "Energy price [€/W]: ") + al.getfEnPrice()[i]);
			System.out.println(Util.leftString(26, "Busy power [W]: ") + al.getfBusyPw()[i]);
			System.out.println(Util.leftString(26, "Idle power [W]: ") + al.getfIdlePw()[i]);
			System.out.println(Util.leftString(26, "Transmission power [W]: ") + al.getfTxPw()[i]);
			System.out.println(Util.leftString(26, "Latency Map [s]: ") + fDevice.getLatencyMap().toString());
			System.out.println(Util.leftString(26, "Bandwidth Map [kB/s]: ") + fDevice.getBandwidthMap().toString());
			
			if(i < fogDevices.size() -1)
				System.out.println();
		}
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tAPP MODULES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		
		for(int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.println(Util.leftString(15, "Name: ") + al.getmName()[i]);
			System.out.println(Util.leftString(15, "Mips [MIPS]: ") + al.getmMips()[i]);
			System.out.println(Util.leftString(15, "Ram [kB]: ") + al.getmRam()[i]);
			System.out.println(Util.leftString(15, "Strorage [kB]: ") + al.getmStrg()[i]);
			
			if(i < al.getNumberOfModules() -1)
				System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBANDWIDTH MAP (Between modules) [kB/s]:");
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
		System.out.println("\t\tNETWORK MAP (Between modules) [kB]:");
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
		System.out.println("\t\tBANDWIDTH MAP (Between nodes) [kB/s]:");
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
					System.out.format(Util.centerString(20, String.format("%.2f", al.getfBandwidthMap()[i][j])));
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
		
		System.out.println("\n");
	}
	
	/**
	 * Prints the algorithm results.
	 * 
	 * @param al the algorithm object
	 * @param job the final solution
	 */
	public static void printAlgorithmResults(final Algorithm al, final Job job) {
		System.out.println("\n\n*******************************************************");
		System.out.println("\t\tALGORITHM OUTPUT (Cost = " + job.getCost() + "):");
		System.out.println("*******************************************************\n");
		
		System.out.println("**************** MODULE PLACEMENT MAP *****************\n");
		
		int[][] modulePlacementMap = job.getModulePlacementMap();
		
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
		
		System.out.println("\n******************** ROUTING TUPLE MAP  *********************\n");
		
		int[][] routingMap = job.getTupleRoutingMap();
		
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
		
		System.out.println("\n******************** MIGRATION ROUTING MAP  *********************\n");
		
		int[][] migrationMap = job.getMigrationRoutingMap();
		
		for (int i = 0; i < migrationMap.length; i++) {
			System.out.format(Util.leftString(25, "VM name: " + al.getmName()[i]) + " .......... ");
			
			for (int j = 0; j < migrationMap[0].length; j++) {
				System.out.format(Util.centerString(20, al.getfName()[migrationMap[i][j]]));
				
				if(j < migrationMap[0].length - 1)
					System.out.print(" -> ");
			}
			System.out.println();
		}
		
		System.out.println("\n**Algorithm Elapsed time: " + al.getElapsedTime() + " ms**\n\n");		
	}
	
}
