package org.fog.core;

public class Config {	
	// Fog Computing Simulator core
	public static final boolean DEBUG_MODE = false;
	
	// Optimization Algorithm
	public static final boolean PRINT_DETAILS = true;
	
	/**
	 * Configuration parameters for the optimization problem
	 */
	
	public static final boolean NORMALIZE = true;
	public static final double WILLING_TO_WAST_ENERGY_FOG_NODE = 0.01;
	public static final double WILLING_TO_WAST_ENERGY_CLIENT = 1;	
	
	// Operational weight
	public static final double OP_W = 0.01;
	
	// Power weight
	public static final double PW_W = 1000;
	
	// Processing weight
	public static final double PR_W = 0.01;
	
	// Latency weight
	public static final double LT_W = 0.01;
	
	// Bandwidth weight
	public static final double BW_W = 0.01;
	
	public static final int POPULATION_SIZE = 12;
	public static final int MAX_ITER_PLACEMENT = 100000;
	public static final int MAX_ITER_ROUTING = 500;
	public static final int MAX_ITER_PLACEMENT_CONVERGENCE = 250;
	public static final int MAX_ITER_ROUTING_CONVERGENCE = 5;
	
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