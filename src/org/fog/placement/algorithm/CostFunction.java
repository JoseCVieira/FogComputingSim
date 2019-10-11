package org.fog.placement.algorithm;

import org.fog.core.Config;
import org.fog.core.Constants;

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
