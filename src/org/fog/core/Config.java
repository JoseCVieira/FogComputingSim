package org.fog.core;

public class Config {
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
			1,	// Processing cost
			2,	// Latency cost
			1,	// Bandwidth cost
			1	// Migration cost
	};
	
	public static final double CONVERGENCE_ERROR = 0.01;
	
	// Genetic algorithm
	public static final int POPULATION_SIZE_GA = 30;
	public static final int MAX_ITER_PLACEMENT_GA = 500;
	public static final int MAX_ITER_ROUTING_GA = 10;
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE_GA = 10;
	public static final int MAX_ITER_ROUTING_CONVERGENCE_GA = 3;
	
	// Random algorithm
	public static final int MAX_ITER_RANDOM = 5000;
	
	// -------------------------------------------------------------- General --------------------------------------------------------------
	
	// Algorithms
	public static final boolean PRINT_DETAILS = true;
	public static final boolean PRINT_BEST_ITER = true;
	public static final boolean PLOT_RESULTS = false;
	
	// Simulation
	public static final boolean DEBUG_MODE = false;
	public static final boolean PRINT_COMMUNICATION_DETAILS = false;
	public static final boolean PRINT_COST_DETAILS = false;
	public static final boolean PRINT_HANDOVER_DETAILS = true;

	
	public static boolean DYNAMIC_SIMULATION = true;
	public static final boolean ALLOW_MIGRATION = true;
	
	public static final int HANDOVER_THRESHOLD = 25;
	public static final double MOBILE_COMMUNICATION_BW = 10*1024;		// 10MB
	public static final double FIXED_COMMUNICATION_BW = 50*1024;		// 50MB
	public final static double FIXED_COMMUNICATION_LATENCY = 204E-3; 	// Latency/m
	public final static double MIN_LATENCY = 150E-4;
	public static final int MAX_SIMULATION_TIME = 10000;
	
	// For test purposes
	public static final int SQUARE_SIDE = 1000;
	
	public static final double PROB_CHANGE_DIRECTION = 0.25;
	public static final double PROB_CHANGE_VELOCITY = 0.35;
	public static final double PROB_MAX_VELOCITY = 0.1667;
	public static final double PROB_MED_VELOCITY = 0.6667;
	public static final double PROB_MIN_VELOCITY = 0.1667;
	public static final double MAX_VELOCITY = 100;
	public static final double MED_VELOCITY = 50;
	public static final double MIN_VELOCITY = 1;
	
}
