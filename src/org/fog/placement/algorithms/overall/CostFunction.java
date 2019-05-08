package org.fog.placement.algorithms.overall;

import java.util.ArrayList;
import java.util.List;

import org.fog.core.Config;
import org.fog.core.Constants;

public class CostFunction {	
	public static double computeCost(Job job, Algorithm algorithm) {
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		int[][] modulePlacementMap = job.getModulePlacementMap();
		int[][] routingMap = job.getRoutingMap();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for (int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		double cost = 0;
		cost += isPossibleCombination(algorithm, modulePlacementMap);
		cost += calculateOperationalCost(algorithm, modulePlacementMap, routingMap, initialModules, finalModules);
		cost += calculatePowerCost(algorithm, modulePlacementMap);
		cost += calculateProcessingCost(algorithm, modulePlacementMap);
		cost += calculateTransmittingCost(algorithm, routingMap, initialModules, finalModules);
		
		return cost;
	}
	
	// Sums minimum possible value when it is not possible to allow GA to converge easier than return infinity
	private static double isPossibleCombination(Algorithm algorithm, int[][] modulePlacementMap) {
		double cost = 0;
		
		// If some module is not placed or placed in more than one fog node (never happens but is also verified)
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++)
				if(modulePlacementMap[i][j] == 1)
					sum++;
			
			if(sum != 1)
				cost += Constants.MIN_SOLUTION;
		}
		
		// If fog node's resources are exceeded
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalMem += modulePlacementMap[i][j] * algorithm.getmMem()[j];
			}
			
			if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] || totalMem > algorithm.getfMem()[i])
				cost += Constants.MIN_SOLUTION;
		}
		
		return cost;
	}
	
	/*private static boolean isPossibleCombination(Algorithm algorithm, int[][] modulePlacementMap) {
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
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalMem += modulePlacementMap[i][j] * algorithm.getmMem()[j];
			}
			
			if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] || totalMem > algorithm.getfMem()[i])
				return false;
		}
		
		return true;
	}*/
	
	private static double calculateOperationalCost(Algorithm algorithm, int[][] modulePlacementMap,
			int[][] routingMap, List<Integer> initialModules, List<Integer> finalModules) {
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
	
	private static double calculatePowerCost(Algorithm algorithm, int[][] modulePlacementMap) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++){
				cost += Config.EN_W * modulePlacementMap[i][j] * (algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i]) *
						(algorithm.getmMips()[j]/algorithm.getfMips()[i]) * algorithm.getfPwWeight()[i];
			}
		}
		
		return cost;
	}
	
	private static double calculateProcessingCost(Algorithm algorithm, int[][] modulePlacementMap) {
		double cost = 0;
		/*double numerator = 0;
		double denominator = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				double mipsPercentage = modulePlacementMap[i][j] * algorithm.getmMips()[j] / algorithm.getfMips()[i];
				numerator += mipsPercentage;
				denominator += Math.pow(mipsPercentage, 2);
			}
		}
		
		cost = Config.PR_W * Math.pow(numerator, 2)/(algorithm.getNumberOfNodes()*denominator);*/
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				cost += Config.PR_W*(modulePlacementMap[i][j] * algorithm.getmMips()[j] / algorithm.getfMips()[i]);
			}
		}
		
		return cost;
	}
	
	private static double calculateTransmittingCost(Algorithm algorithm, int[][] routingMap,
			List<Integer> initialModules, List<Integer> finalModules) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(routingMap[i][j] != routingMap[i][j-1]) {
					cost += Config.LT_W*(algorithm.getfLatencyMap()[routingMap[i][j-1]][routingMap[i][j]] * dependencies);
					cost += Config.BW_W*(bwNeeded/(algorithm.getfBandwidthMap()[routingMap[i][j-1]][routingMap[i][j]] + Constants.EPSILON));
				}
			}
		}
		
		return cost;
	}
}
