package org.fog.placement.algorithms.placement;

import java.util.ArrayList;
import java.util.List;

import org.fog.core.Config;

public class CostFunction {
	private static List<Integer> initialModules;
	private static List<Integer> finalModules;
	private static int[][] modulePlacementMap;
	private static int[][] routingMap;
	
	public static double computeCost(Job job, Algorithm algorithm) {
		initialModules = new ArrayList<Integer>();
		finalModules = new ArrayList<Integer>();
		
		modulePlacementMap = job.getModulePlacementMap();
		routingMap = job.getRoutingMap();
		
		if(isPossibleCombination(job, algorithm) == false)
			return Config.INF;
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for (int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		double cost = 0;
		cost += calculateOperationalCost(job, algorithm);
		cost += calculateEnergyConsumption(job, algorithm);		
		cost += calculateProcessingCost(job, algorithm);		
		cost += calculateTransmittingCost(job, algorithm);
		
		return cost;
	}
	
	private static boolean isPossibleCombination(Job job, Algorithm algorithm) {
		// If some module is not placed
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++)
				if(modulePlacementMap[i][j] == 1)
					sum++;
			
			if(sum != 1)
				return false;
		}
		
		// If fog node's resources are exceeded
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
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {				
				cost += Config.OP_W * modulePlacementMap[i][j] *
						(algorithm.getfMipsPrice()[i] * algorithm.getmMips()[j] +
						 algorithm.getfRamPrice()[i] * algorithm.getmRam()[j] +
						 algorithm.getfMemPrice()[i] * algorithm.getmMem()[j]);
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++)
				if(routingMap[i][j] != routingMap[i][j-1])
					cost += Config.OP_W*(algorithm.getfBwPrice()[routingMap[i][j-1]]*bwNeeded);
		}
		
		return cost;
	}
	
	private static double calculateEnergyConsumption(Job job, Algorithm algorithm) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++)
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
			
			cost += Config.EN_W * (algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i]) *
					(totalMips/algorithm.getfMips()[i]) * algorithm.getfPwWeight()[i];
		}
		
		return cost;
	}
	
	private static double calculateProcessingCost(Job job, Algorithm algorithm) {
		double cost = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++)
			for(int j = 0; j < modulePlacementMap[i].length; j++)
				cost += Config.PR_W*(modulePlacementMap[i][j] * algorithm.getmMips()[j] / algorithm.getfMips()[i]);
		
		return cost;
	}
	
	private static double calculateTransmittingCost(Job job, Algorithm algorithm) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				cost += Config.TX_W*(algorithm.getfLatencyMap()[routingMap[i][j-1]][routingMap[i][j]] * dependencies +
					bwNeeded/(algorithm.getfBandwidthMap()[routingMap[i][j-1]][routingMap[i][j]] + Config.EPSILON));
			}
		}
		
		return cost;
	}
}
