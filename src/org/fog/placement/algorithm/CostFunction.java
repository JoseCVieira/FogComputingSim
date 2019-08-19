package org.fog.placement.algorithm;

/**
 * Class in which defines the optimization problem (single or multiple-objective) and its constraints.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since   July, 2019
 */
public abstract class CostFunction {
	
	/**
	 * Analyzes the both the constraints and the cost function.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 */
	public void analyzeSolution(Algorithm algorithm, Job solution) {
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		int[][] migrationRoutingMap = solution.getMigrationRoutingMap();
		
		// Verify if all constraints are met
		double constraint = Constraints.checkConstraints(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		
		solution.setCost(constraint + computeCost(algorithm, solution));
		solution.setValid(constraint == 0 ? true : false);
	}
	
	/**
	 * Computes the cost of the best solution found by running the optimization algorithm.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the result of the cost function
	 */
	public abstract double computeCost(Algorithm algorithm, Job solution);
	
}
