package org.fog.core;

public class Config {
	// --------------------------------------- Configuration parameters for the optimization problem ---------------------------------------
	
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
	 * 	 	 weight attributes provided.
	 */
	public static final int[] priorities = new int[] {
			1,		// Operational cost
			1,		// Power cost
			1,		// Processing cost
			2,		// Latency cost
			1,		// Bandwidth cost
			1		// Migration cost
	};
	
	public static final double[] weights = new double[] {
			1.0,	// Operational cost
			1.0,	// Power cost
			1.0,	// Processing cost
			1.0,	// Latency cost
			1.0,	// Bandwidth cost
			1.0		// Migration cost
	};
	
	/**
	 * Allow a small degradation in the first objective. AbsTols represents a list of absolute tolerances.
	 */
	public static final double[] absTols = new double[] {
			0.0,	// Operational cost
			0.0,	// Power cost
			0.0,	// Processing cost
			0.0,	// Latency cost
			0.0,	// Bandwidth cost
			0.0		// Migration cost
	};
	
	/**
	 * Allow a small degradation in the first objective. RelTols represents a list of relative tolerances.
	 */
	public static final double[] relTols = new double[] {
			0.0,	// Operational cost
			0.0,	// Power cost
			0.0,	// Processing cost
			0.0,	// Latency cost
			0.0,	// Bandwidth cost
			0.0		// Migration cost
	};
	
	public static boolean NORMALIZE_VALUES = false;
	
	public static final double CONVERGENCE_ERROR = 0.01;
	
	// Genetic algorithm
	public static final int POPULATION_SIZE_GA = 30;
	public static final int MAX_ITER_PLACEMENT_GA = 500;
	public static final int MAX_ITER_ROUTING_GA = 10;
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE_GA = 10;
	public static final int MAX_ITER_ROUTING_CONVERGENCE_GA = 3;
	
	// Random algorithm
	public static final int MAX_ITER_RANDOM = 10000;
	
	// -------------------------------------------------------------- General --------------------------------------------------------------
	
	// Algorithms
	public static final boolean PRINT_BEST_ITER = false;
	public static final boolean PLOT_ALGORITHM_RESULTS = false;
	
	// Simulation
	public static final boolean DEBUG_MODE = false;
	public static final boolean PRINT_DETAILS = true;
	public static final boolean PRINT_COST_DETAILS = false;
	
	public static boolean DYNAMIC_SIMULATION = true;
	public static final boolean ALLOW_MIGRATION = false;
	
	public static final int HANDOVER_THRESHOLD = 25;
	public static final double FIXED_COMMUNICATION_BW = 50*1024;		// 50MB
	public static final double SETUP_VM_TIME = 20;
	public static final int MAX_SIMULATION_TIME = 10000;
	public static final int RECONFIG_PERIOD = 1;
	
	// For test purposes -------------------------------------------------------------------
	public static final int SQUARE_SIDE = 10000;
	public static final double PROB_CHANGE_DIRECTION = 0.25;
	public static final double PROB_CHANGE_VELOCITY = 0.35;
	public static final double PROB_MAX_VELOCITY = 0.1667;
	public static final double PROB_MED_VELOCITY = 0.6667;
	public static final double PROB_MIN_VELOCITY = 0.1667;
	public static final double MAX_VELOCITY = 33.3333;			// 33.3333 m/s = 120 km/h 	-> high speed car
	public static final double MED_VELOCITY = 13.8889;			// 13.8889 m/s = 50 km/h  	-> slow speed car
	public static final double MIN_VELOCITY = 1.34000;			// 1.34 m/s					-> average walking speed
	
}
