package org.fog.placement.algorithms.placement;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

public class AlgorithmUtils {
	public static void print(String text, int[][] matrix) {
		
		if(text == null || matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Some of the received arguments are null.");
		
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
	
	public static void print(String text, double[][] matrix) {
		
		if(text == null || matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Some of the received arguments are null.");
		
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
	
	public static void print(String text, int[] vector) {
		
		if(text == null || vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Some of the received arguments are null.");
		
		System.out.println(text);
		
        for(int i = 0; i < vector.length; i++)
        	System.out.print(vector[i]+ " ");
        System.out.println("\n");
	}
	
	
	public static void print(String text, double[] vector) {
		
		if(text == null || vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Some of the received arguments are null.");
		
		System.out.println(text);
		
        for(int i = 0; i < vector.length; i++)
        	System.out.print(vector[i]+ " ");
        System.out.println("\n");
	}
	
	static void printDetails(final Algorithm al, final List<FogDevice> fogDevices,
			final List<Application> applications, final List<Sensor> sensors,
			final List<Actuator> actuators) {
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tFOG NODES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		for(int i = 0; i < fogDevices.size(); i++) {
			FogDevice fDevice = null;
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(al.getfName()[i]))
					fDevice = fogDevice;
			
			System.out.println("Id: " + fDevice.getId() + " fName: " + al.getfName()[i]);
			System.out.println("fMips: " + al.getfMips()[i]);
			System.out.println("fRam: " + al.getfRam()[i]);
			System.out.println("fMem: " + al.getfMem()[i]);
			System.out.println("fBw: " + al.getfBw()[i]);
			System.out.println("fMipsPrice: " + al.getfMipsPrice()[i]);
			System.out.println("fRamPrice: " + al.getfRamPrice()[i]);
			System.out.println("fMemPrice: " + al.getfMemPrice()[i]);
			System.out.println("fBwPrice: " + al.getfBwPrice()[i]);
			System.out.println("fBusyPw: " + al.getfBusyPw()[i]);
			System.out.println("fIdlePw: " + al.getfIdlePw()[i]);			
			System.out.println("Neighbors: " +  fDevice.getNeighborsIds());
			System.out.println("LatencyMap: " + fDevice.getLatencyMap());
			System.out.println("BandwidthMap: " + fDevice.getBandwidthMap());
			
			if(i < fogDevices.size() -1)
				System.out.println();
		}
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tAPP MODULES CHARACTERISTICS:");
		System.out.println("*******************************************************\n");
		
		for(int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.println("mName: " + al.getmName()[i]);
			System.out.println("mMips: " + al.getmMips()[i]);
			System.out.println("mRam: " + al.getmRam()[i]);
			System.out.println("mMem: " + al.getmMem()[i]);
			System.out.println("mBw: " + al.getmBw()[i]);
			
			if(i < al.getNumberOfModules() -1)
				System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBW MAP (Between modules):");
		System.out.println("*******************************************************\n");
		
		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++) {
				if(al.getmBandwidthMap()[i][j] != 0)
					System.out.format(centerString(20, String.format("%.2f", al.getmBandwidthMap()[i][j])));
				else
					System.out.format(AlgorithmUtils.centerString(20, "-"));
			}
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tDEPENDENCY MAP (Between modules):");
		System.out.println("*******************************************************\n");
		
		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfModules(); i++) {
			System.out.format(centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getmDependencyMap()[i][j] != 0.0)
					System.out.format(centerString(20, String.format("%.2f", al.getmDependencyMap()[i][j])));
				else
					System.out.format(AlgorithmUtils.centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tPOSSIBLE POSITIONING:");
		System.out.println("*******************************************************\n");

		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++)
				if(al.getPossibleDeployment()[i][j] != 0.0)
					System.out.format(centerString(20, Integer.toString((int) al.getPossibleDeployment()[i][j])));
				else
					System.out.format(centerString(20, "-"));
			System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBANDWIDTH MAP (Between directed nodes):");
		System.out.println("*******************************************************\n");
		
		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getNumberOfNodes(); i++)
			System.out.format(centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfNodes(); j++) {
				if(al.getfBandwidthMap()[i][j] == Config.INF)
					System.out.format(centerString(20, "Inf"));
				else if(al.getfBandwidthMap()[i][j] == 0)
					System.out.format(AlgorithmUtils.centerString(20, "-"));
				else
					System.out.format(centerString(20, String.format("%.2f", al.getfBandwidthMap()[i][j])));
			}
			System.out.println();
		}		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tLATENCY MAP (Between directed nodes):");
		System.out.println("*******************************************************\n");
		
		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getNumberOfNodes(); i++)
			System.out.format(centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfNodes(); j++) {
				if(al.getfLatencyMap()[i][j] == Config.INF)
					System.out.format(centerString(20, "Inf"));
				else if(al.getfLatencyMap()[i][j] == 0)
					System.out.format(AlgorithmUtils.centerString(20, "-"));
				else
					System.out.format(centerString(20, String.format("%.2f", al.getfLatencyMap()[i][j])));
			}
			System.out.println();
		}
	}
	
	public static void printResults(final Algorithm al, final Job job) {
		System.out.println("\n\n*******************************************************");
		System.out.println("\t\tALGORITHM OUTPUT (Cost = " + String.format("%.5f", job.getCost()) + "):");
		System.out.println("*******************************************************\n");
		
		System.out.println("**************** MODULE PLACEMENT MAP *****************\n");
		
		int[][] modulePlacementMap = job.getModulePlacementMap();
		
		System.out.format(AlgorithmUtils.centerString(20, " "));
		for (int i = 0; i < al.getNumberOfModules(); i++)
			System.out.format(AlgorithmUtils.centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getNumberOfNodes(); i++) {
			System.out.format(AlgorithmUtils.centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] != 0)
					System.out.format(AlgorithmUtils.centerString(20, Integer.toString(modulePlacementMap[i][j])));
				else
					System.out.format(AlgorithmUtils.centerString(20, "-"));
			}
			System.out.println();
		}
		
		System.out.println("\n******************** ROUTING MAP  *********************\n");
		
		int[][] routingMap = job.getRoutingMap();
		
		for (int i = 0; i < routingMap.length; i++) {
			for (int j = 0; j < routingMap[0].length; j++) {
				System.out.format(AlgorithmUtils.centerString(20, al.getfName()[routingMap[i][j]]));
				
				if(j < routingMap[0].length - 1)
					System.out.print(" -> ");
			}
			System.out.println();
		}
	}
	
	public static String centerString (int width, String s) {
	    return String.format("%-" + width  + "s",
	    		String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
	}
	
}
