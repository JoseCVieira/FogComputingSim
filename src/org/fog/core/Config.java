package org.fog.core;

public class Config {	
	// Fog Computing Simulator core
	public static final boolean DEBUG_MODE = false;
	
	// Optimization Algorithm
	public static final boolean PRINT_DETAILS = true;
	
	public static final double WILLING_TO_WAST_ENERGY_CLIENT = 0.01;
	public static final double WILLING_TO_WAST_ENERGY_FOG_NODE = 1;
	
	public static final double OP_W = 1;	// Operational weight
	public static final double EN_W = 1; 	// Energetic weight
	public static final double PR_W = 1; 	// Processing weight
	public static final double LT_W = 1; 	// Latency weight
	public static final double BW_W = 1; 	// Bandwidth weight
	
	public static final int POPULATION_SIZE = 10;
	public static final int MAX_ITER = 1000;
	public static final int MAX_ITER_ROUTING = 1000;
	public static final int MAX_ITER_CONVERGENCE = 3;
	public static final double ERROR_STEP_CONVERGENCE = 0.1;
	
	// Random topology
	public static final String CLOUD_NAME = "Cloud";
	public static final int NR_FOG_DEVICES = 2;
	
	public static final int MAX_CONN_LAT = 100;
	public static final int MAX_CONN_BW = 10000;
	
	public static final double CONNECTION_PROB = 0.4;
	public static final double DEPLOY_APP_PROB = 0.1;//0.35;
	
	public static final double RESOURCES_DEV = 100;
	public static final double ENERGY_DEV = 5;
	public static final double COST_DEV = 1E-5;
}
