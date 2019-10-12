package org.fog.placement.algorithm;

import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;

/**
 * Class in which defines the optimization problem multiple objective and its constraints.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since   July, 2019
 */
public class CostFunction {

	/**
	 * Analyzes the both the constraints and the cost function.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 */
	public static void analyzeSolution(Algorithm algorithm, Solution solution) {
		double constraint = Constraints.checkConstraints(algorithm, solution);
		solution.setDetailedCost(Config.QOS_COST, algorithm.getNumberOfLoops() - computeQoS(algorithm, solution));
		solution.setDetailedCost(Config.POWER_COST, computePowerCost(algorithm, solution));
		solution.setDetailedCost(Config.PROCESSING_COST, computeProcessingCost(algorithm, solution));
		solution.setDetailedCost(Config.BANDWIDTH_COST, computeBandwidthCost(algorithm, solution));
		solution.setDetailedCost(Config.MIGRATION_COST, computeMigrationCost(algorithm, solution));
		solution.setConstraint(constraint);
	}
	
	/**
	 * Computes the processing cost function for a given solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the computed cost
	 */
	private static double computeProcessingCost(Algorithm algorithm, Solution solution) {
		double cost = 0;
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			if(algorithm.getfIsFogDevice()[i] == 0) continue;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				cost += modulePlacementMap[i][j]*algorithm.getmMips()[j]/(algorithm.getfMips()[i]*Config.MIPS_PERCENTAGE_UTIL);
			}
		}
		
		return cost;
	}
	
	/**
	 * Computes the quality of service cost function for a given solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the computed cost
	 */
	private static double computeQoS(Algorithm algorithm, Solution solution) {
		int [][] loops = algorithm.getLoops();
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfLoops(); i++) { // Loop index
			double latency = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules() - 1; j++) { // Module index
				if(loops[i][j+1] == -1) break;
				
				latency += computeProcessingLatency(algorithm, modulePlacementMap, loops[i][j+1]);
				latency += computeDependencyLatency(algorithm, tupleRoutingMap, loops[i][j], loops[i][j+1]);
			}
			
			solution.setLoopDeadline(i, latency);
			if(latency <= algorithm.getLoopsDeadline()[i]) continue;
			cost++;
		}
		
		return cost;
	}
	
	/**
	 * Computes the processing latency for the worst case scenario.
	 * 
	 * @param algorithm object which contains all information about the topology and which algorithm was used
	 * @param modulePlacementMap matrix which represents the next module placement
	 * @param moduleIndex index of the module to be analyzed
	 * @return the processing latency for the worst case scenario
	 */
	private static double computeProcessingLatency(final Algorithm algorithm, final int[][] modulePlacementMap, final int moduleIndex) {
		if(algorithm.getmMips()[moduleIndex] == 0) return 0;

		int nodeIndex = Solution.findModulePlacement(modulePlacementMap, moduleIndex);
		double totalMis = 0;

		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			if(algorithm.getmMips()[i] == 0) continue; // Sensor and actuator modules does not count
			if(Solution.findModulePlacement(modulePlacementMap, i) != nodeIndex) continue; // Only matter the ones deployed in the same node

			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMis += algorithm.getmCPUMap()[j][i];
			}
		}
		
		return totalMis/(algorithm.getfMips()[nodeIndex]*Config.MIPS_PERCENTAGE_UTIL);
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
	private static double computeDependencyLatency(final Algorithm algorithm, final int[][] tupleRoutingMap,
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
		
		// For each Link, in the tuple routing map sum the total latency
		for (int i = 0; i < algorithm.getNumberOfNodes() - 1; i++) {
			int start = tupleRoutingMap[depIndex][i];
			int end = tupleRoutingMap[depIndex][i+1];
			
			if(start == end) continue;
			
			double bw = algorithm.getfBandwidthMap()[start][end] * Config.BW_PERCENTAGE_UTIL + Constants.EPSILON; // Link bandwidth
			double lat = algorithm.getfLatencyMap()[start][end]; // Link latency
			double totalSize = 0;

			// For each different dependency which uses the same link, sum the size of the dependency
			for (int j = 0; j < algorithm.getNumberOfDependencies(); j++) { // Dependency index
				for(int k = 0; k < algorithm.getNumberOfNodes() - 1; k++) {
					if(tupleRoutingMap[j][k] == start && tupleRoutingMap[j][k+1] == end) {
						totalSize += algorithm.getmNWMap()[algorithm.getStartModDependency(j)][algorithm.getFinalModDependency(j)];
					}
				}
			}
			
			// In the worst case, the dependency in study is the last one to be sent
			latency += lat + totalSize/bw;
		}
		
		return latency;
	}
	
	/**
	 * Computes the power cost function for a given solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the computed cost
	 */
	private static double computePowerCost(Algorithm algorithm, Solution solution) {
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			if(algorithm.getfIsFogDevice()[i] == 0) continue;
				
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				double pw = algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i];
				cost += modulePlacementMap[i][j]*algorithm.getmMips()[j]/(algorithm.getfMips()[i]*Config.MIPS_PERCENTAGE_UTIL)*pw;
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {			
			double bandwidth = algorithm.getmBandwidthMap()[algorithm.getStartModDependency(i)][algorithm.getFinalModDependency(i)];
			
			for(int j = 0; j < algorithm.getNumberOfNodes()-1; j++) {
				int start = tupleRoutingMap[i][j];
				int end = tupleRoutingMap[i][j+1];
				
				if(algorithm.getfIsFogDevice()[start] == 0) continue;
				if(start == end) continue;
						
				double bw = bandwidth/(algorithm.getfBandwidthMap()[start][end]*Config.BW_PERCENTAGE_UTIL + Constants.EPSILON);
				cost += bw*algorithm.getfTxPw()[start];
			}
		}
		
		return cost;
	}
	
	/**
	 * Computes the bandwidth cost function for a given solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the computed cost
	 */
	private static double computeBandwidthCost(Algorithm algorithm, Solution solution) {
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {			
			double bandwidth = algorithm.getmBandwidthMap()[algorithm.getStartModDependency(i)][algorithm.getFinalModDependency(i)];
			
			for(int j = 0; j < algorithm.getNumberOfNodes()-1; j++) {
				int start = tupleRoutingMap[i][j];
				int end = tupleRoutingMap[i][j+1];
				
				if(algorithm.getfIsFogDevice()[start] == 0) continue;
				if(start == end) continue;
						
				cost += bandwidth/(algorithm.getfBandwidthMap()[start][end]*Config.BW_PERCENTAGE_UTIL + Constants.EPSILON);
			}
		}
		
		return cost;
	}
	
	/**
	 * Computes the migration cost function for a given solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the computed cost
	 */
	private static double computeMigrationCost(Algorithm algorithm, Solution solution) {
		int[][] migrationRoutingMap = solution.getMigrationRoutingMap();
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			double size = algorithm.getmStrg()[i] + algorithm.getmRam()[i];
			
			for(int j = 0; j < algorithm.getNumberOfNodes()-1; j++) {
				int start = migrationRoutingMap[i][j];
				int end = migrationRoutingMap[i][j+1];
				
				if(algorithm.getfIsFogDevice()[start] == 0) continue;
				if(start == end) continue;
			
				double linkBw = algorithm.getfBandwidthMap()[start][end]*(1-Config.BW_PERCENTAGE_UTIL) + Constants.EPSILON;
				double totalDep = 0;
				for(int l = 0; l < algorithm.getNumberOfModules(); l++) {
					totalDep += algorithm.getmDependencyMap()[l][i];
				}
				
				cost += size/linkBw*totalDep;
			}
		}
		
		return cost;
	}
	
}
