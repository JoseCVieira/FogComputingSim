package org.fog.placement.algorithm;

import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;

/**
 * Class in which constraints are defined and analyzed both for single- or multiple-objective optimization
 * problems (except for problems defined using frameworks such as CPLEX). Violated constraints
 * are multiplied by Constants.REFERENCE_COST in order to allow an easier conversion for the evolutionary
 * algorithms.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since   July, 2019
 */
public class Constraints {
	/**
	 * Verifies if all constraints are met.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param solution the solution found
	 * @return the number of violations times a constant
	 */
	public static double checkConstraints(final Algorithm algorithm, Solution solution) {
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		int[][] migrationRoutingMap = solution.getMigrationRoutingMap();
		
		Constraints.checkVariableSizeType(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		
		double constraint = checkResourcesExceeded(algorithm, modulePlacementMap);
		constraint += checkPossiblePlacement(algorithm, modulePlacementMap);
		constraint += checkMultiplePlacement(algorithm, modulePlacementMap);
		constraint += checkDependencies(algorithm, modulePlacementMap, tupleRoutingMap);
		constraint += checkBandwidth(algorithm, tupleRoutingMap);
		constraint += checkMigration(algorithm, modulePlacementMap, migrationRoutingMap);
		constraint += checkMigrationDeadlines(algorithm, solution, modulePlacementMap, migrationRoutingMap);
		
		return constraint;
	}
	
	/**
	 * Check if all variables have the correct size and type.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @param migrationRoutingMap matrix which contains the routing for each module migration
	 */
	private static void checkVariableSizeType(final Algorithm algorithm, final int[][] modulePlacementMap,
			final int[][] tupleRoutingMap, final int[][] migrationRoutingMap) {
		
		if(modulePlacementMap.length != algorithm.getNumberOfNodes() || modulePlacementMap[0].length != algorithm.getNumberOfModules())
			FogComputingSim.err("Module placement variable has the wrong size");
		
		if(tupleRoutingMap.length != algorithm.getNumberOfDependencies() || tupleRoutingMap[0].length != algorithm.getNumberOfNodes())
			FogComputingSim.err("Tuple routing variable has the wrong size");
		
		if(migrationRoutingMap.length != algorithm.getNumberOfModules() || migrationRoutingMap[0].length != algorithm.getNumberOfNodes())
			FogComputingSim.err("Migration routing variable has the wrong size");
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] != 0 && modulePlacementMap[i][j] != 1)
					FogComputingSim.err("Module placement variable needs to be binary");
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
				if(tupleRoutingMap[i][j] < 0 || tupleRoutingMap[i][j] >= algorithm.getNumberOfNodes())
					FogComputingSim.err("Tuple routing variable needs to be filled with valid node indexes");
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
				if(migrationRoutingMap[i][j] < 0 || migrationRoutingMap[i][j] >= algorithm.getNumberOfNodes())
					FogComputingSim.err("Migration routing variable needs to be filled with valid node indexes");
			}
		}
	}
	
	/**
	 * Check whether the solutions placement contains modules which are exceeding the machines' resources.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkResourcesExceeded(final Algorithm algorithm, final int[][] modulePlacementMap) {
		double violations = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalStrg = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalStrg += modulePlacementMap[i][j] * algorithm.getmStrg()[j];
			}
			
			boolean exceeded = false;
			if(totalMips > algorithm.getfMips()[i] * Config.MIPS_PERCENTAGE_UTIL) exceeded = true;
			if(totalRam > algorithm.getfRam()[i] * Config.MEM_PERCENTAGE_UTIL) exceeded = true;
			if(totalStrg > algorithm.getfStrg()[i] * Config.STRG_PERCENTAGE_UTIL) exceeded = true;
			
			if(!exceeded) continue;
			violations += Constants.REFERENCE_COST;
		}
		
		return violations;
	}
	
	/**
	 * Check whether the solutions placement respects the possible deployment matrix.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkPossiblePlacement(final Algorithm algorithm, final int[][] modulePlacementMap) {
		double violations = 0;
		
		for(int i  = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] <= algorithm.getPossibleDeployment()[i][j]) continue;
				violations += Constants.REFERENCE_COST;
			}
		}
		
		return violations;
	}
	
	/**
	 * Check whether the solutions placement contains modules which have not been deployed or have been placed in multiple machines.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkMultiplePlacement(final Algorithm algorithm, final int[][] modulePlacementMap) {
		double violations = 0;
		
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++) {
				if(modulePlacementMap[i][j] == 1)
					sum++;
			}
			
			if(sum == 1) continue;
			violations += Constants.REFERENCE_COST;
		}
		
		return violations;
	}
	
	/**
	 * Check whether all dependencies are accomplished.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkDependencies(final Algorithm algorithm, final int[][] modulePlacementMap, final int[][] tupleRoutingMap) {
		double violations = 0;
		int tmp = 0;
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] == 0) continue;
				
				int startNodeIndex = Solution.findModulePlacement(modulePlacementMap, i);
				int destNodeIndex = Solution.findModulePlacement(modulePlacementMap, j);
				
				if(tupleRoutingMap[tmp][0] != startNodeIndex) {
					violations += Constants.REFERENCE_COST;
				}
				
				if(tupleRoutingMap[tmp][algorithm.getNumberOfNodes()-1] != destNodeIndex) {
					violations += Constants.REFERENCE_COST;
				}
				
				for (int k = 0; k < algorithm.getNumberOfNodes()-1; k++) {
					if(algorithm.getfLatencyMap()[tupleRoutingMap[tmp][k]][tupleRoutingMap[tmp][k+1]] < Constants.INF) continue;
					violations += Constants.REFERENCE_COST;
				}
				
				tmp++;
			}
		}
		
		return violations;
	}
	
	/**
	 * Check whether bandwidth usage is exceeded.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkBandwidth(final Algorithm algorithm, final int[][] tupleRoutingMap) {
		double violations = 0;
		double bwUsage[][] = new double[algorithm.getNumberOfNodes()][algorithm.getNumberOfNodes()];
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[algorithm.getStartModDependency(i)][algorithm.getFinalModDependency(i)];
			
			for(int j = 0; j < algorithm.getNumberOfNodes() - 1; j++) {
				if(tupleRoutingMap[i][j] != tupleRoutingMap[i][j+1]) {
					bwUsage[tupleRoutingMap[i][j]][tupleRoutingMap[i][j+1]] += bwNeeded;
				}
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
				if(bwUsage[i][j] <= algorithm.getfBandwidthMap()[i][j] * Config.BW_PERCENTAGE_UTIL) continue;
				violations += Constants.REFERENCE_COST;
			}
		}
		
		return violations;
	}
	
	/**
	 * Check whether all migrations are accomplished.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param migrationRoutingMap matrix which contains the routing for each module migration
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	private static double checkMigration(final Algorithm algorithm, final int[][] modulePlacementMap,
			final int[][] migrationRoutingMap) {
		double violations = 0;
		int[][] currentPlacement = algorithm.getCurrentPositionInt();
		boolean firstOpt = algorithm.isFirstOptimization();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			int startNodeIndex = Solution.findModulePlacement(firstOpt == false ? currentPlacement : modulePlacementMap, i);
			int destNodeIndex = Solution.findModulePlacement(modulePlacementMap, i);
			
			if(migrationRoutingMap[i][0] != startNodeIndex) {
				violations += Constants.REFERENCE_COST;
			}
			
			if(migrationRoutingMap[i][algorithm.getNumberOfNodes()-1] != destNodeIndex) {
				violations += Constants.REFERENCE_COST;
			}
			
			for (int k = 0; k < algorithm.getNumberOfNodes()-1; k++) {
				if(algorithm.getfLatencyMap()[migrationRoutingMap[i][k]][migrationRoutingMap[i][k+1]] < Constants.INF) continue;
				violations += Constants.REFERENCE_COST;
			}
		}
		
		return violations;
	}
	
	/**
	 * Computes the module migration latency for the worst case scenario.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param migrationRoutingMap  matrix which contains the routing for each module migration
	 * @param moduleIndex index of the module to be analyzed
	 * @return the module migration latency for the worst case scenario
	 */
	private static double checkMigrationDeadlines(final Algorithm algorithm , Solution solution, final int[][] modulePlacementMap,
			final int[][] migrationRoutingMap) {
		double violations = 0;
		
		for (int i = 0; i < algorithm.getNumberOfModules(); i++) {
			double latency = 0;
			double size = algorithm.getmStrg()[i] + algorithm.getmRam()[i];
			
			for (int j = 0; j < algorithm.getNumberOfNodes() - 1; j++) { // Node index
				int start = migrationRoutingMap[i][j];
				int end = migrationRoutingMap[i][j+1];
				
				if(start == end) continue;
				
				double lat = algorithm.getfLatencyMap()[start][end];
				double bw = algorithm.getfBandwidthMap()[start][end]*(1-Config.BW_PERCENTAGE_UTIL) + Constants.EPSILON;
				
				latency += lat + size/bw;
			}
			
			if(latency != 0) latency += Config.SETUP_VM_TIME;
			
			solution.setMigrationDeadline(i, latency);
			
			if(latency <= algorithm.getmMigD()[i]) continue;
			violations += Constants.REFERENCE_COST;
		}
		
		return violations;
	}
	
}
