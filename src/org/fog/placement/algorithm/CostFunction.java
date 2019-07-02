package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.fog.core.Config;
import org.fog.core.Constants;

public class CostFunction {
	private static final boolean PRINT_DETAILS = true;
	
	public static void computeCost(Job job, Algorithm algorithm) {
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		int[][] modulePlacementMap = job.getModulePlacementMap();
		int[][] tupleRoutingMap = job.getTupleRoutingMap();
		int[][] migrationRoutingMap = job.getMigrationRoutingMap();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for (int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		double cost = isPossibleCombination(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap, initialModules, finalModules);
		
		if(cost == 0)
			job.setValid(true);
		
		double tmp = 0;
		tmp = calculateOperationalCost(algorithm, modulePlacementMap, tupleRoutingMap, initialModules, finalModules);
		cost += tmp;
		if(PRINT_DETAILS)
			System.out.println("OP cost: " + tmp);
		
		tmp = calculatePowerCost(algorithm, modulePlacementMap);
		cost += tmp;
		if(PRINT_DETAILS)
			System.out.println("PW cost: " + tmp);
		
		tmp = calculateProcessingCost(algorithm, modulePlacementMap);
		cost += tmp;
		if(PRINT_DETAILS)
			System.out.println("PR cost: " + tmp);
		
		tmp = calculateLatencyCost(algorithm, tupleRoutingMap, initialModules, finalModules);
		cost += tmp;
		if(PRINT_DETAILS)
			System.out.println("LT cost: " + tmp);
		
		tmp = calculateBandwidthCost(algorithm, tupleRoutingMap, initialModules, finalModules);
		cost += tmp;
		if(PRINT_DETAILS)
			System.out.println("BW cost: " + tmp);
		
		//TODO
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(migrationRoutingMap[i][j-1] != migrationRoutingMap[i][j]) {
					cost += 1;
				}
			}
		}
		
		job.setCost(cost);
	}
	
	// Sums minimum possible value when it is not possible to allow GA to converge easier than return infinity
	private static double isPossibleCombination(Algorithm algorithm, int[][] modulePlacementMap, int[][] tupleRoutingMap,
			int[][] migrationRoutingMap, List<Integer> initialModules, List<Integer> finalModules) {
		double cost = 0;
		
		// If placement does not respects possible deployment matrix
		for(int i  = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] > algorithm.getPossibleDeployment()[i][j]) {
					cost += Constants.REFERENCE_COST;
					if(PRINT_DETAILS)
						System.out.println("does not respects possible deployment");
				}
			}
		}		
		
		// If some module is not placed or placed in more than one fog node (never happens but is also verified)
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++)
				if(modulePlacementMap[i][j] == 1)
					sum++;
			
			if(sum != 1) {
				cost += Constants.REFERENCE_COST;
				if(PRINT_DETAILS)
					System.out.println("module is not placed or placed in more than one fog node");
			}
		}
		
		// If dependencies are not accomplished
		int iter = 0;
		for(int i = 0; i < algorithm.getmDependencyMap().length; i++) {
			for(int j = 0; j < algorithm.getmDependencyMap()[0].length; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					if(tupleRoutingMap[iter][0] != Job.findModulePlacement(modulePlacementMap, i) ||
							tupleRoutingMap[iter][algorithm.getNumberOfNodes() - 1] != Job.findModulePlacement(modulePlacementMap, j)) {
						cost += Constants.REFERENCE_COST;
						if(PRINT_DETAILS)
							System.out.println("dependencies are not accomplished");
					}
					iter++;
				}
			}
		}
		
		// If fog node's resources are exceeded
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalStrg = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalStrg += modulePlacementMap[i][j] * algorithm.getmStrg()[j];
			}
			
			if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] || totalStrg > algorithm.getfStrg()[i]) {
				cost += Constants.REFERENCE_COST;
				if(PRINT_DETAILS)
					System.out.println("fog node's resources are exceeded");
			}
		}
		
		// If bandwidth links usage are exceeded
		double bwUsage[][] = new double[algorithm.getNumberOfNodes()][algorithm.getNumberOfNodes()];
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(tupleRoutingMap[i][j-1] != tupleRoutingMap[i][j]) {
					bwUsage[tupleRoutingMap[i][j-1]][tupleRoutingMap[i][j]] += bwNeeded;
				}
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
				if(bwUsage[i][j] > algorithm.getfBandwidthMap()[i][j]) {
					cost += Constants.REFERENCE_COST;
					if(PRINT_DETAILS)
						System.out.println("bandwidth links usage are exceeded");
				}
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(tupleRoutingMap[i][j-1] != tupleRoutingMap[i][j]) {					
					if(algorithm.getfLatencyMap()[tupleRoutingMap[i][j-1]][tupleRoutingMap[i][j]] == Constants.INF) {
						cost += Constants.REFERENCE_COST;
						if(PRINT_DETAILS)
							System.out.println("bandwidth links usage are exceeded");
					}
				}
			}
		}
		
		//TODO : VM routing table analysis
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(migrationRoutingMap[i][j-1] != migrationRoutingMap[i][j]) {
					if(algorithm.getfLatencyMap()[migrationRoutingMap[i][j-1]][migrationRoutingMap[i][j]] == Constants.INF) {
						cost += Constants.REFERENCE_COST;
						if(PRINT_DETAILS)
							System.out.println("bandwidth links usage are exceeded");
					}
				}
			}
		}
		
		return cost;
	}
	
	private static double calculateOperationalCost(Algorithm algorithm, int[][] modulePlacementMap,
			int[][] routingMap, List<Integer> initialModules, List<Integer> finalModules) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				cost += Config.OP_W * modulePlacementMap[i][j] *
						(algorithm.getfMipsPrice()[i] * algorithm.getmMips()[j] +
						 algorithm.getfRamPrice()[i] * algorithm.getmRam()[j] +
						 algorithm.getfStrgPrice()[i] * algorithm.getmStrg()[j]);
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++)
				if(routingMap[i][j] != routingMap[i][j-1]) {
					cost += Config.OP_W*(algorithm.getfBwPrice()[routingMap[i][j-1]]*bwNeeded);
				}
		}
		
		return cost;
	}
	
	private static double calculatePowerCost(Algorithm algorithm, int[][] modulePlacementMap) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++){
				cost += Config.PW_W * modulePlacementMap[i][j] * (algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i]) *
						(algorithm.getmMips()[j]/algorithm.getfMips()[i]);
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
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++){
				cost += Config.PR_W*(modulePlacementMap[i][j] * algorithm.getmMips()[j] / algorithm.getfMips()[i]);
			}
		}
		
		return cost;
	}
	
	private static double calculateLatencyCost(Algorithm algorithm, int[][] routingMap,
			List<Integer> initialModules, List<Integer> finalModules) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(routingMap[i][j] != routingMap[i][j-1]) {
					cost += Config.LT_W*(algorithm.getfLatencyMap()[routingMap[i][j-1]][routingMap[i][j]] * dependencies);
				}
			}
		}
		
		return cost;
	}
	
	private static double calculateBandwidthCost(Algorithm algorithm, int[][] routingMap,
			List<Integer> initialModules, List<Integer> finalModules) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(routingMap[i][j] != routingMap[i][j-1]) {
					cost += Config.BW_W*(bwNeeded/(algorithm.getfBandwidthMap()[routingMap[i][j-1]][routingMap[i][j]] + Constants.EPSILON));
				}
			}
		}
		
		return cost;
	}
	
}
