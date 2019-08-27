package org.fog.core;

/**
 * Class representing the configuration parameters used within the FogComputingSim
 * (e.g., used by the optimization algorithms and the simulation).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Config {
	// --------------------------------------- Configuration parameters for the optimization problem ---------------------------------------
	
	// General ------------------------------------------------
	
	/** Defines whether the best value is printed between iterations are printed */
	public static boolean PRINT_ALGORITHM_BEST_ITER = true;
	
	/** Defines whether the details of the constraints of each iteration are printed */
	public static boolean PRINT_ALGORITHM_CONSTRAINTS = false;
	
	/** Defines whether the final result of the algorithm (i.e., the best solution) is printed */
	public static boolean PRINT_ALGORITHM_RESULTS = true;
	
	/** Defines whether the iteration-value map is plotted */
	public static boolean PLOT_ALGORITHM_RESULTS = false;
	
	/** Defines whether the output values are written to the excel file */
	public static boolean EXPORT_RESULTS_EXCEL = true;
	
	/** Defines the percentage of the bandwidth available in the links which is used for transmitting tuples between modules */
	public static final double BW_PERCENTAGE_TUPLES = 0.8;
	
	/** Defines the number of defined objectives inside the multiple objective problem */
	public static final int NR_OBJECTIVES = 6;
	
	/** The index of each objective */
	public static final int OPERATIONAL_COST = 0;
	public static final int POWER_COST = 1;
	public static final int PROCESSING_COST = 2;
	public static final int LATENCY_COST = 3;
	public static final int BANDWIDTH_COST = 4;
	public static final int MIGRATION_COST = 5;
	
	/** 
	 * Order of importance for multiple objective optimization
	 * Note: The CPLEX multiobjective optimization algorithm sorts the objectives by decreasing priority value.
	 * 		 If several objectives have the same priority, they are blended in a single objective using the
	 * 	 	 weight attributes provided.
	 * 
	 * Note that for the current problem it makes no sense to sum different costs, thus their priorities must be all different.
	 */
	public static final int[] priorities = new int[] {
			5,		// Operational cost
			4,		// Power cost
			3,		// Processing cost
			2,		// Latency cost
			6,		// Bandwidth cost
			6		// Migration cost
	};
	
	/** The names of the objectives (used to export the results to the excel file) */
	public static final String[] objectiveNames = new String[] {
			"Oper.",
			"Pwr.", 
			"Proc.",
			"Lat.", 
			"Bw.",
			"Mig."
	};
	
	/** Absolute error value in which is considered that two solutions are equal */
	public static final double CONVERGENCE_ERROR = 0.01;
	
	// Genetic algorithm --------------------------------------
	
	/** Number of individuals running in the Genetic Algorithm */
	public static final int POPULATION_SIZE_GA = 30;
	
	/** Maximum number of iterations to solve the module placement through genetic algorithm */
	public static final int MAX_ITER_PLACEMENT_GA = 500;
	
	/** Maximum number of iterations to solve the tuple routing through genetic algorithm */
	public static final int MAX_ITER_ROUTING_GA = 10;
	
	/** Maximum number of equal cost solutions of module placement through genetic algorithm to stop it */
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE_GA = 10;
	
	/** Maximum number of equal cost solutions of tuple routing through genetic algorithm to stop it */
	public static final int MAX_ITER_ROUTING_CONVERGENCE_GA = 3;
	
	// Random algorithm ---------------------------------------
	
	public static final int MAX_ITER_RANDOM = 10000;
	
	// ------------------------------------------------------------ Simulation -------------------------------------------------------------
	
	// Simulation
	/** Defines whether the simulation runs in debug mode (i.e., prints the debug logs defined in the original version of iFogSim) */
	public static boolean DEBUG_MODE = false;
	
	/** Defines whether the simulation should print logs about the simulation (e.g., tuple transmission, migrations, processing, etc.) */
	public static boolean PRINT_DETAILS = true;
	
	/** Defines whether the simulation should print logs about the costs and resource usage */
	public static boolean PRINT_COST_DETAILS = false;
	
	/** Defines whether the simulation is dynamic (i.e., mobile nodes actualy move around) */
	public static boolean DYNAMIC_SIMULATION = false;
	
	/** Defines whether the simulation is allowed to perform migrations of VMs */
	public static boolean ALLOW_MIGRATION = true;
	
	/** Defines the threshold used to define if its necessary to perform an handover */
	public static final int HANDOVER_THRESHOLD = 75;
	
	/** Defines the bandwidth available in fixed link */
	public static final double FIXED_COMMUNICATION_BW = 50*1024*1024;	// 50 MBytes
	
	/** Defines the time needed to perform the setup of the VM after the migration is completed */
	public static final double SETUP_VM_TIME = 20;
	
	/** Defines the maximum time of simulation which is performed */
	public static final int MAX_SIMULATION_TIME = 10000;
	
	/** Defines the periodicity that the controller will check whether it's necessary to run the optimization algorithm again */
	public static final int RECONFIG_PERIOD = 1;
	
	// ------------------------------------------- Movement of the mobile nodes for test purposes ------------------------------------------
	public static final int SQUARE_SIDE = 10000;
	public static final double PROB_CHANGE_DIRECTION = 0.25;
	public static final double PROB_CHANGE_VELOCITY = 0.35;
	public static final double PROB_MAX_VELOCITY = 0.1667;
	public static final double PROB_MED_VELOCITY = 0.6667;
	public static final double PROB_MIN_VELOCITY = 0.1667;
	public static final double MAX_VELOCITY = 33.3333;				// 33.3333 m/s = 120 km/h 	-> high speed car
	public static final double MED_VELOCITY = 13.8889;				// 13.8889 m/s = 50 km/h  	-> slow speed car
	public static final double MIN_VELOCITY = 1.34000;				// 1.34 m/s					-> average walking speed
	
}
