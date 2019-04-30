package org.fog.placement.algorithms.placement;

import java.util.ArrayList;
import java.util.List;

public class CostFunction {
	
	public static double computeCost(Job job, Algorithm algorithm) {
		if(isPossibleCombination(job, algorithm) == false)
			return Double.MAX_VALUE - 1;
		
		double cost = calculateOperationalCost(job, algorithm);
		cost += calculateEnergyConsumption(job, algorithm);
		cost += calculateProcessingCost(job, algorithm);
		cost += calculateTransmittingCost(job, algorithm);
		
		return cost;
	}
	
	private static boolean isPossibleCombination(Job job, Algorithm algorithm) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++)
				if(modulePlacementMap[i][j] == 1)
					sum++;
			
			if(sum != 1)
				return false;
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			double totalBw = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalMem += modulePlacementMap[i][j] * algorithm.getmMem()[j];
				totalBw += modulePlacementMap[i][j] * algorithm.getmBw()[j];
			}
			
			if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] ||
					totalMem > algorithm.getfMem()[i] || totalBw > algorithm.getfBw()[i])
				return false;
		}
		return true;
	}
	
	private static double calculateOperationalCost(Job job, Algorithm algorithm) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				
				cost += modulePlacementMap[i][j]*(
						algorithm.getfMipsPrice()[i] * algorithm.getmMips()[j] +
						algorithm.getfRamPrice()[i] * algorithm.getmRam()[j] +
						algorithm.getfMemPrice()[i] * algorithm.getmMem()[j]);
			}
		}
		
		return cost;
	}
	
	private static double calculateEnergyConsumption(Job job, Algorithm algorithm) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		double energy = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++)
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
			
			energy += (algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i])*(totalMips/algorithm.getfMips()[i]);
		}
		
		return energy;
	}
	
	private static double calculateProcessingCost(Job job, Algorithm algorithm) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		double cost = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			int nrModules = 0;
			double totalMips = 0;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				nrModules += modulePlacementMap[i][j];
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
			}
			
			double unnusedMips = algorithm.getfMips()[i] - totalMips;
			cost += nrModules / (1E-9 + unnusedMips);
		}
		
		return cost;
	}
	
	private static double calculateTransmittingCost(Job job, Algorithm algorithm) {
		double[][] bwMap = new double[algorithm.getNumberOfNodes()][algorithm.getNumberOfNodes()];
		int[][] routingMap = job.getRoutingMap();
		
		double transmittingCost = 0;
		
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for (int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		for(int i = 0; i < routingMap.length; i++) {
			for (int j = 1; j < routingMap[0].length; j++) {
				int from = (int) routingMap[i][j-1];
				int to = (int) routingMap[i][j];
				
				double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				bwMap[from][to] += bwNeeded;
				transmittingCost += algorithm.getfLatencyMap()[from][to]*dependencies;
				transmittingCost += algorithm.getfBwPrice()[from]*bwNeeded;
				
				if(algorithm.getfBandwidthMap()[from][to] == 0)
					transmittingCost += Short.MAX_VALUE;
				else
					transmittingCost += bwNeeded/algorithm.getfBandwidthMap()[from][to];
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++)
			for (int j = 0; j < algorithm.getNumberOfNodes(); j++)
				if(bwMap[i][j] > algorithm.getfBandwidthMap()[i][j])
					transmittingCost += Short.MAX_VALUE;
		
		return transmittingCost;
	}
}
