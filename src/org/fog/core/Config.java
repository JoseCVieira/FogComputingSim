package org.fog.core;

public class Config {
	// Global
	public static final double INF = Double.POSITIVE_INFINITY;
	public static final double EPSILON = 1E-9;
	
	// Fog Computing Simulator core
	public static final boolean DEBUG_MODE = false;
	public static final boolean COMPARE_WITH_LP = false;
	
	// Optimization Algorithm
	public static final String OPTIMIZATION_ALGORITHM = "GA";
	public static final boolean PRINT_DETAILS = true;
	
	public static final int OP_W = 1; 	// Operational weight
	public static final int EN_W = 1; 	// Energetic weight
	public static final int PR_W = 1; 	// Processing weight
	public static final int TX_W = 1;	// Transmission weight
	public static final int TR_C = 1;	// Transition cost
	
	public static final int POPULATION_SIZE = 100;
	public static final double AGREED_BOUNDARY = 0.0;
	public static final int MAX_ITER = 3000;
}
