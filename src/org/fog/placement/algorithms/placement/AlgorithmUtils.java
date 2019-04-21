package org.fog.placement.algorithms.placement;

import java.util.List;

import org.cloudbus.cloudsim.power.models.PowerModel;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

public class AlgorithmUtils {
	
	public static double[][] vectorToHorizontalMatrix(double [] vector) throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("Invalid argument");
		
		double[][] ret = new double[1][vector.length];
        for (int i = 0; i < vector.length; i++)
        	ret[1][i] = vector[i];
        return ret;
    }
	
	public static double[][] vectorToVerticalMatrix(double [] vector) throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("Invalid argument");
		
		double[][] ret = new double[vector.length][1];
        for (int i = 0; i < vector.length; i++)
        	ret[i][1] = vector[i];
        return ret;
    }

	public static double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix)
			throws IllegalArgumentException {
		
		if(firstMatrix == null || secondMatrix == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		int r1 = firstMatrix.length;
		int c1 = firstMatrix[0].length;
		int r2 = secondMatrix.length;
		int c2 = secondMatrix[0].length;
		
		if(c1 != r2)
			throw new IllegalArgumentException("Impossible to preform the required matrix multiplication");
		
		double[][] product = new double[r1][c2];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c2; j++)
                for (int k = 0; k < c1; k++)
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
        
        return product;
    }
	
	public static double[][] transposeMatrix(double [][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("Invalid argument");
		
		double[][] temp = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[0].length; j++)
                temp[j][i] = matrix[i][j];
        return temp;
    }
	
	public static double[][] dotProductMatrices(double[][] firstMatrix, double[][] secondMatrix)
			throws IllegalArgumentException {
		
		if(firstMatrix == null || secondMatrix == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		int r1 = firstMatrix.length;
		int c1 = firstMatrix[0].length;
		int r2 = secondMatrix.length;
		int c2 = secondMatrix[0].length;
		
		if(r1 != r2 || c1 != c2)
			throw new IllegalArgumentException("Impossible to preform the required dot product");
		
		double[][] product = new double[r1][c1];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c1; j++)
                    product[i][j] = firstMatrix[i][j] * secondMatrix[i][j];
        
        return product;
	}
	
	public static double[][] dotDivisionMatrices(double[][] firstMatrix, double[][] secondMatrix)
			throws IllegalArgumentException {
		
		if(firstMatrix == null || secondMatrix == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		int r1 = firstMatrix.length;
		int c1 = firstMatrix[0].length;
		int r2 = secondMatrix.length;
		int c2 = secondMatrix[0].length;
		
		if(r1 != r2 || c1 != c2)
			throw new IllegalArgumentException("Impossible to preform the required dot product");
		
		double[][] product = new double[r1][c1];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c1; j++)
                    product[i][j] = firstMatrix[i][j] / secondMatrix[i][j];
        
        return product;
	}
	
	public static double sumAllElementsMatrix(double[][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("Invalid argument");
		
		int r = matrix.length;
		int c = matrix[0].length;
        double ret = 0;
		
        for(int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
            	ret += matrix[i][j];
        
        return ret;
	}
	
	public static void printMatrix(double[][] matrix) {		
		int r = matrix.length;
		int c = matrix[0].length;
		
        for(int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++)
            	System.out.print(matrix[i][j] + " ");
            System.out.println();
        }
        
        System.out.println("\n");
	}
	
	public static void printVector(double[] vector) {		
        for(int i = 0; i < vector.length; i++)
            	System.out.print(vector[i] + " ");        
        System.out.println("\n");
	}
	
	static double[] convertDoubles(List<Double> d) {
		double[] ret = new double[d.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = d.get(i).doubleValue();
	    
	    return ret;
	}
	
	static int[] convertIntegers(List<Integer> ints) {
		int[] ret = new int[ints.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = ints.get(i).intValue();
	    
	    return ret;
	}
	
	static PowerModel[] convertPowerModels(List<PowerModel> pwM) {
		PowerModel[] ret = new PowerModel[pwM.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = pwM.get(i);
	    
	    return ret;
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
		
		for(int i = 0; i < al.getmName().length; i++) {
			System.out.println("mName: " + al.getmName()[i]);
			System.out.println("mMips: " + al.getmMips()[i]);
			System.out.println("mRam: " + al.getmRam()[i]);
			System.out.println("mMem: " + al.getmMem()[i]);
			System.out.println("mBw: " + al.getmBw()[i]);
			
			if(i < al.getmName().length -1)
				System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBW MAP (Between modules):");
		System.out.println("*******************************************************\n");
		
		System.out.format(centerString(20, " "));
		for (int i = 0; i < al.getmName().length; i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getmName().length; i++) {
			System.out.format(centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getmName().length; j++) {
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
		for (int i = 0; i < al.getmName().length; i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getmName().length; i++) {
			System.out.format(centerString(20, al.getmName()[i]));
			for (int j = 0; j < al.getmName().length; j++)
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
		for (int i = 0; i < al.getmName().length; i++)
			System.out.format(centerString(20, al.getmName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getfName().length; i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getmName().length; j++)
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
		for (int i = 0; i < al.getfName().length; i++)
			System.out.format(centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getfName().length; i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getfName().length; j++) {
				if(al.getfBandwidthMap()[i][j] == Double.MAX_VALUE)
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
		for (int i = 0; i < al.getfName().length; i++)
			System.out.format(centerString(20, al.getfName()[i]));
		System.out.println();
		
		for (int i = 0; i < al.getfName().length; i++) {
			System.out.format(centerString(20, al.getfName()[i]));
			for (int j = 0; j < al.getfName().length; j++) {
				if(al.getfLatencyMap()[i][j] == Double.MAX_VALUE)
					System.out.format(centerString(20, "Inf"));
				else if(al.getfLatencyMap()[i][j] == 0)
					System.out.format(AlgorithmUtils.centerString(20, "-"));
				else
					System.out.format(centerString(20, String.format("%.2f", al.getfLatencyMap()[i][j])));
			}
			System.out.println();
		}
	}
	
	public static String centerString (int width, String s) {
	    return String.format("%-" + width  + "s",
	    		String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
	}
	
}
