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
	
	// GUI
	public static final String ADD = "ADD";
	public static final String EDIT = "EDIT";
	
	public static final String FOG_NAME = "FogNode";
	public static final String ACTUATOR_NAME = "Actuator";
	public static final String SENSOR_NAME = "Sensor";
	
	public static final String FOG_TYPE = "FOG_DEVICE";
	public static final String SENSOR_TYPE = "SENSOR";
	public static final String ACTUATOR_TYPE = "ACTUATOR";
	public static final String SENSOR_MODULE_TYPE = "SENSOR_MODULE";
	public static final String ACTUATOR_MODULE_TYPE = "ACTUATOR_MODULE";
	public static final String APP_MODULE_TYPE = "APP_MODULE";
	
	// Prices from https://www.cloudsigma.com/pricing/
	public static final double BW = 10240;
	public static final double MIPS = 22733.5;
	public static final int RAM = 131072;
	public static final int MEM = 1048576;
	public static final double RATE_MIPS = 1.5855E-04;
	public static final double RATE_RAM = 1.14E-07;
	public static final double RATE_MEM = 5E-11;
	public static final double RATE_BW = 3.9E-05;
	public static final double IDLE_POWER = 83.4333;
	public static final double BUSY_POWER = 127.339;
	public static final double COST_PER_SEC = 3.0;
	
	public static final int MODULE_RAM = 8192;
	public static final long MODULE_SIZE = 10000;
	
	public static final double EDGE_CPU_LENGTH = 3000;
	public static final double EDGE_NW_LENGTH = 500;
	public static final double EDGE_PERIODICITY = 1000;
	
	public static final double SENSOR_LATENCY = 6;
	public static final double ACTUATOR_LATENCY = 1;
	
	public static final double SENSOR_DESTRIBUTION = 5.1;
	
	public static final double RESOURCE_MGMT_INTERVAL = 100;
	public static final int MAX_SIMULATION_TIME = 10000;
	public static final int RESOURCE_MANAGE_INTERVAL = 100;
	public static final String FOG_DEVICE_ARCH = "x86";
	public static final String FOG_DEVICE_OS = "Linux";
	public static final String FOG_DEVICE_VMM = "Xen";
	public static final double FOG_DEVICE_TIMEZONE = 0.0;
	public static final double SCHEDULING_INTERVAL = 10;
}
