package org.fog.core;

public class Config {	
	// Fog Computing Simulator core
	public static final boolean DEBUG_MODE = false;
	
	// Optimization Algorithm
	public static final boolean PRINT_DETAILS = true;
	
	// Simulation
	public static final boolean PRINT_COMMUNICATION_DETAILS = false;
	public static final boolean PRINT_COST_DETAILS = false;
	
	/**
	 * Configuration parameters for the optimization problem
	 */
	
	public static final boolean NORMALIZE = false;
	public static final double WILLING_TO_WAST_ENERGY_FOG_NODE = 1;
	public static final double WILLING_TO_WAST_ENERGY_CLIENT = 0.1;	
	
	// Operational weight
	public static final double OP_W = 1;
	
	// Power weight
	public static final double PW_W = 1;
	
	// Processing weight
	public static final double PR_W = 1;
	
	// Latency weight
	public static final double LT_W = 1;
	
	// Bandwidth weight
	public static final double BW_W = 1;
	
	// GA
	public static final boolean PRINT_BEST_ITER = true;
	public static final int POPULATION_SIZE = 15;
	public static final int MAX_ITER = 5000;
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE = 10;
	public static final int MAX_ITER_ROUTING_CONVERGENCE = 20;
	public static final double CONVERGENCE_ERROR = 0.001;
	
	// RANDOM ALGORITHM
	public static final int MAX_ITER_RANDOM = 50000;
	
	// PSO
	public static final int POPULATION_SIZE_PSO = 10;
	public static final int MAX_ITER_PSO = 200000;
	public static final double DEFAULT_INERTIA = 0.729844;
    public static final double DEFAULT_COGNITIVE = 2.07;//1.496180;
    public static final double DEFAULT_SOCIAL = 2.07;//1.496180;
	
	// Random topology
	public static final String CLOUD_NAME = "Cloud";
	public static final int NR_FOG_DEVICES = 5;
	
	public static final int MAX_CONN_LAT = 100;
	public static final int MAX_CONN_BW = 10000;
	
	public static final double CONNECTION_PROB = 0.4;
	public static final double DEPLOY_APP_PROB = 0.35;
	
	public static final double RESOURCES_DEV = 100;
	public static final double ENERGY_DEV = 5;
	public static final double COST_DEV = 1E-5;
}