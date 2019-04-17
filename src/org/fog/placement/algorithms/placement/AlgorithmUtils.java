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
			//System.out.println("fBw: " + al.getfBw()[i]);
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
			//System.out.println("mBw: " + al.getmBw()[i]);
			System.out.println("mCpuSize: " + al.getmCpuSize()[i]);
			
			if(i < al.getmName().length -1)
				System.out.println();
		}
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBW CAPACITY MAP:");
		System.out.println("*******************************************************\n");
		String[][] table = new String[al.getfName().length+1][al.getfName().length+1];
		
		table[0][0] = " ";
		for(int i = 0; i < al.getfName().length; i++)
			table[0][i+1] = al.getfName()[i];
		
		for(int i = 0; i < al.getfName().length; i++) {
			table[i+1][0] = al.getfName()[i];
			
			for(int j = 0; j < al.getfName().length; j++)
				table[i+1][j+1] = Double.toString(al.getBwCapacityMap()[i][j]);
		}
		
		String repeated = repeate(al.getfName().length, " %25s ");
		
		for (final Object[] row : table)
		    System.out.format("%23s" + repeated + "\n", row);
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tMANDATORY POSITIONING:");
		System.out.println("*******************************************************\n");
		
		table = new String[al.getfName().length+1][al.getmName().length+1];
		
		table[0][0] = " ";
		for(int i = 0; i < al.getmName().length; i++)
			table[0][i+1] = al.getmName()[i];
		
		for(int i = 0; i < al.getfName().length; i++) {
			table[i+1][0] = al.getfName()[i];
			
			for(int j = 0; j < al.getmName().length; j++)
				table[i+1][j+1] = Double.toString(al.getMandatoryMap()[i][j]);
		}
		
		repeated = repeate(al.getmName().length, "%17s");
		
		for (final Object[] row : table)
		    System.out.format("%23s" + repeated + "\n", row);
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tLATENCY MAP:");
		System.out.println("*******************************************************\n");
		
		table = new String[al.getfId().length+1][al.getfId().length+1];
		
		table[0][0] = " ";
		for(int i = 0; i < al.getfId().length; i++)
			table[0][i+1] = al.getfName()[i];
		
		for(int i = 0; i < al.getfId().length; i++) {
			table[i+1][0] = al.getfName()[i];
			
			for(int j = 0; j < al.getfId().length; j++)
				table[i+1][j+1] = Double.toString(al.getLatencyMap()[i][j]);
		}
		
		repeated = repeate(al.getfId().length, "%13s");
		
		for (final Object[] r : table)
		    System.out.format("%23s" + repeated + "\n", r);
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tBANDWIDTH MAP:");
		System.out.println("*******************************************************\n");
		
		for(int iter = 0; iter < al.getfId().length-1; iter++) {
			table = new String[al.getfId().length+1][al.getfId().length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < al.getfId().length; i++)
				table[0][i+1] = al.getfName()[i];
			
			for(int i = 0; i < al.getfId().length; i++) {
				table[i+1][0] = al.getfName()[i];
				
				for(int j = 0; j < al.getfId().length; j++)
					table[i+1][j+1] = Double.toString(al.getBandwidthMap(iter)[i][j]);
			}
			
			repeated = repeate(al.getfId().length, "%15s ");
			
			for (final Object[] r : table)
			    System.out.format("%25s" + repeated + "\n", r);
			
			if(iter < al.getfId().length-2)
				System.out.println();
		}
		
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tDEPENDENCY MAP:");
		System.out.println("*******************************************************\n");
		
		table = new String[al.getmName().length+1][al.getmName().length+1];
		
		table[0][0] = " ";
		for(int i = 0; i < al.getmName().length; i++)
			table[0][i+1] = al.getmName()[i];
		
		for(int i = 0; i < al.getmName().length; i++) {
			table[i+1][0] = al.getmName()[i];
			
			for(int j = 0; j < al.getmName().length; j++)
				table[i+1][j+1] = Double.toString(al.getDependencyMap()[i][j]);
		}
		
		repeated = repeate(al.getmName().length, "%17s");
		
		for (final Object[] row : table)
		    System.out.format("%23s" + repeated + "\n", row);
		
		System.out.println("\n*******************************************************");
		System.out.println("\t\tNW SIZE MAP:");
		System.out.println("*******************************************************\n");
		
		table = new String[al.getmName().length+1][al.getmName().length+1];
		
		table[0][0] = " ";
		for(int i = 0; i < al.getmName().length; i++)
			table[0][i+1] = al.getmName()[i];
		
		for(int i = 0; i < al.getmName().length; i++) {
			table[i+1][0] = al.getmName()[i];
			
			for(int j = 0; j < al.getmName().length; j++)
				table[i+1][j+1] = Double.toString(al.getNwSizeMap()[i][j]);
		}
		
		repeated = repeate(al.getmName().length, "%17s");
		
		for (final Object[] row : table)
		    System.out.format("%23s" + repeated + "\n", row);
	}
	
	public static String repeate(int i, String s) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < i; j++)
			sb.append(s);
		return sb.toString();
    }
	
}
