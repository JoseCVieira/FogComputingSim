package org.fog.core;

public class Config {
	// -------------------------------------------------------------- General --------------------------------------------------------------
	
	// Algorithms
	public static final boolean PRINT_DETAILS = true;
	public static final boolean PRINT_BEST_ITER = true;
	
	// Simulation
	public static final boolean DEBUG_MODE = false;
	public static final boolean PRINT_COMMUNICATION_DETAILS = false;
	public static final boolean PRINT_COST_DETAILS = false;
	
	// --------------------------------------- Configuration parameters for the optimization problem ---------------------------------------
	
	// Type
	public static boolean SINGLE_OBJECTIVE = true;
	
	// weights for single objective
	public static final double OP_W = 1; 	// Operational cost weight
	public static final double PW_W = 1; 	// Power cost weight
	public static final double PR_W = 1;	// Processing cost weight
	public static final double LT_W = 1;	// Latency cost weight
	public static final double BW_W = 1;	// Bandwidth cost weight
	
	/** 
	 * Order of importance for multiple objective optimization
	 * Note: The CPLEX multiobjective optimization algorithm sorts the objectives by decreasing priority value.
	 * 		 If several objectives have the same priority, they are blended in a single objective using the
	 * 	 	 weight attributes provided (as we provide null, all costs have the same weight; thus, having the
	 * 		 same importance).
	 */
	public static final int[] priorities = new int[] {
			1,	// Operational cost
			1,	// Power cost
			2,	// Processing cost
			1,	// Latency cost
			1	// Bandwidth cost
	};
	
	// Genetic algorithm
	public static final int POPULATION_SIZE = 15;
	public static final int MAX_ITER = 5000;
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE = 10;
	public static final int MAX_ITER_ROUTING_CONVERGENCE = 20;
	public static final double CONVERGENCE_ERROR = 0.001;
	
	// Random algorithm
	public static final int MAX_ITER_RANDOM = 50000;
}