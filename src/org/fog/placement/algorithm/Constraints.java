package org.fog.placement.algorithm;

import java.util.ArrayList;

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
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @param migrationRoutingMap matrix which contains the routing for each module migration
	 * @return the number of violations times a constant
	 */
	public static double checkConstraints(final Algorithm algorithm, final int[][] modulePlacementMap,
			final int[][] tupleRoutingMap, final int[][] migrationRoutingMap) {
		Constraints.checkVariableSizeType(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		
		double constraint = checkResourcesExceeded(algorithm, modulePlacementMap);
		constraint += checkPossiblePlacement(algorithm, modulePlacementMap);
		constraint += checkMultiplePlacement(algorithm, modulePlacementMap);
		constraint += checkDependencies(algorithm, modulePlacementMap, tupleRoutingMap);
		constraint += checkBandwidth(algorithm, tupleRoutingMap);
		constraint += checkMigration(algorithm, modulePlacementMap, migrationRoutingMap);
		constraint += checkDeadlines(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		
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
	public static void checkVariableSizeType(final Algorithm algorithm, final int[][] modulePlacementMap,
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
	public static double checkResourcesExceeded(final Algorithm algorithm, final int[][] modulePlacementMap) {
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
			
			if(totalMips <= algorithm.getfMips()[i] && totalRam <= algorithm.getfRam()[i] && totalStrg <= algorithm.getfStrg()[i]) continue;
			violations += Constants.REFERENCE_COST;
		}
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution has at least one module placed which is exceeding the machine resources");
		
		return violations;
	}
	
	/**
	 * Check whether the solutions placement respects the possible deployment matrix.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	public static double checkPossiblePlacement(final Algorithm algorithm, final int[][] modulePlacementMap) {
		double violations = 0;
		
		for(int i  = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(modulePlacementMap[i][j] <= algorithm.getPossibleDeployment()[i][j]) continue;
				violations += Constants.REFERENCE_COST;
			}
		}
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution does not respect possible deployment");
		
		return violations;
	}
	
	/**
	 * Check whether the solutions placement contains modules which have not been deployed or have been placed in multiple machines.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	public static double checkMultiplePlacement(final Algorithm algorithm, final int[][] modulePlacementMap) {
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
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution has at least one module placed in multiple machines or has not been deployed");
		
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
	public static double checkDependencies(final Algorithm algorithm, final int[][] modulePlacementMap, final int[][] tupleRoutingMap) {
		double violations = 0;
		int tmp = 0;
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] == 0) continue;
				
				int startNodeIndex = Job.findModulePlacement(modulePlacementMap, i);
				int destNodeIndex = Job.findModulePlacement(modulePlacementMap, j);
				
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
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution has at least one dependency which is not accomplished by the tuple routing map");
		
		return violations;
	}
	
	/**
	 * Check whether bandwidth usage is exceeded.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	public static double checkBandwidth(final Algorithm algorithm, final int[][] tupleRoutingMap) {
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
				if(bwUsage[i][j] <= algorithm.getfBandwidthMap()[i][j] * Config.BW_PERCENTAGE_TUPLES) continue;
				violations += Constants.REFERENCE_COST;
			}
		}
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution has at least one links which is overloaded by the tuple routing map");
		
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
	public static double checkMigration(final Algorithm algorithm, final int[][] modulePlacementMap,
			final int[][] migrationRoutingMap) {
		double violations = 0;
		int[][] currentPlacement = algorithm.getCurrentPositionInt();
		boolean firstOpt = algorithm.isFirstOptimization();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			int startNodeIndex = Job.findModulePlacement(firstOpt == false ? currentPlacement : modulePlacementMap, i);
			int destNodeIndex = Job.findModulePlacement(modulePlacementMap, i);
			
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
		
		if(violations != 0 && Config.PRINT_ALGORITHM_CONSTRAINTS)
			System.out.println("Solution has at least one migration which is not accomplished by the migration routing map");
		
		return violations;
	}
	
	/**
	 * Check whether, in the worst case scenario, all deadlines are met
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @param migrationRoutingMap matrix which contains the routing for each module migration
	 * @return the number of violations times a constant (zero if this constraint has been respected)
	 */
	public static double checkDeadlines(final Algorithm algorithm, final int[][] modulePlacementMap,
			final int[][] tupleRoutingMap, final int[][] migrationRoutingMap) {
		double violations = 0;
		int [][] loops = algorithm.getLoops();
		ArrayList<Double> loopWorstLat = new ArrayList<Double>();
		
		for(int i = 0; i < loops.length; i++) { // Loop index
			double latency = 0;
			
			ArrayList<Integer> computedMigrations = new ArrayList<Integer>();
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) { // Module index
				
				if(!computedMigrations.contains(loops[i][j])) {
					latency += computeMigrationLatency(algorithm, migrationRoutingMap, loops[i][j]);
					computedMigrations.add(loops[i][j]);
				}
				
				if(j == algorithm.getNumberOfModules()-1 || loops[i][j+1] == -1) break;
				
				latency += computeProcessingLatency(algorithm, loops[i][j], loops[i][j+1]);
				latency += computeDependencyLatency(algorithm, tupleRoutingMap, loops[i][j], loops[i][j+1]);
				
				if(latency <= algorithm.getLoopsDeadline()[i]) continue;
				violations += Constants.REFERENCE_COST;
			}
			
			loopWorstLat.add(latency);
		}
		
		if(Config.PRINT_ALGORITHM_CONSTRAINTS) {
			if(violations != 0)
				System.out.println("Solution has at least one loop deadline which is not accomplished");
			
			int i = 0;
			for(double lat : loopWorstLat) {
				System.out.print("Loop " + i + ": [ " );
				
				for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
					if(j == algorithm.getNumberOfModules() - 1 || loops[i][j+1] == -1) {
						System.out.print(algorithm.getmName()[loops[i][j]] + " ]");
						break;
					}
					System.out.print(algorithm.getmName()[loops[i][j]] + " -> ");
				}
				
				System.out.print("\t Worst case latency: " + lat + " sec\t Deadline: " + algorithm.getLoopsDeadline()[i] + " sec\n");
				i++;
			}
			
			System.out.println("\n");
		}
		
		return violations;
	}
	
	/**
	 * Computes the processing latency for the worst case scenario.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param moduleIndex1 index of the starting module to be analyzed
	 * @param moduleIndex2 index of the destination module to be analyzed
	 * @return the processing latency for the worst case scenario
	 */
	public static double computeProcessingLatency(final Algorithm algorithm, final int moduleIndex1, final int moduleIndex2) {
		// Sensor and actuator modules does not count
		if(algorithm.getmMips()[moduleIndex2] == 0) return 0;
		
		return algorithm.getmCPUMap()[moduleIndex1][moduleIndex2]/algorithm.getmMips()[moduleIndex2];
	}
	
	/**
	 * Computes the transmission tuple latency for the worst case scenario between a pair of modules.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param tupleRoutingMap matrix which contains the routing for each module pair dependency
	 * @param moduleIndex1 index of the starting module to be analyzed
	 * @param moduleIndex2 index of the destination module to be analyzed
	 * @return the transmission tuple latency for the worst case scenario between a pair of modules
	 */
	public static double computeDependencyLatency(final Algorithm algorithm, final int[][] tupleRoutingMap,
			final int moduleIndex1, final int moduleIndex2) {
		double latency = 0;
		int depIndex = -1;
		
		// Find dependency index
		for (int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			if(algorithm.getStartModDependency(i) == moduleIndex1 && algorithm.getFinalModDependency(i) == moduleIndex2) {
				depIndex = i;
				break;
			}
		}
		
		if(depIndex == -1)
			FogComputingSim.err("Should not happen (Constraints)");
		
		double bw = algorithm.getmBandwidthMap()[moduleIndex1][moduleIndex2];
		double nwSize = algorithm.getmNWMap()[moduleIndex1][moduleIndex2];
		
		// For each Link, in the tuple routing map sum the total latency
		for (int i = 0; i < algorithm.getNumberOfNodes() - 1; i++) { // Node index
			int start = tupleRoutingMap[depIndex][i];
			int end = tupleRoutingMap[depIndex][i+1];
			
			if(start == end) continue;
			
			double linkLat = algorithm.getfLatencyMap()[start][end];
			
			latency += linkLat + nwSize/bw;
		}
		
		return latency;
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
	public static double computeMigrationLatency(final Algorithm algorithm, final int[][] migrationRoutingMap, final int moduleIndex) {
		double latency = 0;
		double size = algorithm.getmStrg()[moduleIndex] + algorithm.getmRam()[moduleIndex];
		
		for (int i = 0; i < algorithm.getNumberOfNodes()-1; i++) {
			int start = migrationRoutingMap[moduleIndex][i];
			int end = migrationRoutingMap[moduleIndex][i+1];
			
			if(start == end) continue;
			
			double lat = algorithm.getfLatencyMap()[start][end];
			double bw = algorithm.getfBandwidthMap()[start][end];
			
			latency += lat + size/bw;
		}
		
		if(latency != 0)
			latency += Config.SETUP_VM_TIME;
		
		return latency;
	}
	
}
