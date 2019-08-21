package org.fog.gui;

/**
 * Class which holds all configuration values used within the GUI.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GuiConfig {
	/** Number of fog devices generated in this topology */
	public static int NR_FOG_DEVICES = 5;
	
	/** Maximum latency generated between two nodes */
	public static double MAX_CONN_LAT = 100;
	
	/** Maximum bandwidth generated between two nodes */
	public static double MAX_CONN_BW = 10000;
	
	/** Probability of creating a connection between two nodes */
	public static double CONNECTION_PROB = 0.4;
	
	/** Probability of being a client (i.e., to have an application to be deployed) */
	public static double CLIENT_PROBABILITY = 0.4;
	
	/** Deviation of the normal distribution defined to the resources */
	public static double RESOURCES_DEV = 100;
	
	/** Deviation of the normal distribution defined to the energy */
	public static double ENERGY_DEV = 5;
	
	/** Deviation of the normal distribution defined to the cost */
	public static double COST_DEV = 1E-5;
	
	public static String FOG_NAME = "FogNode";
	
	// Prices from https://www.cloudsigma.com/pricing/
	public static double BW = 10240;
	public static double MIPS = 22733.5;
	public static int RAM = 131072;
	public static int STRG = 1048576;
	public static double RATE_MIPS = 1.5855E-04;
	public static double RATE_RAM = 1.14E-07;
	public static double RATE_STRG = 5E-11;
	public static double RATE_BW = 3.9E-05;
	public static double RATE_EN = 5E-8;
	public static double IDLE_POWER = 83.4333;
	public static double BUSY_POWER = 127.339;
	
	public static int MODULE_RAM = 8192;
	public static long MODULE_SIZE = 10000;
	
	public static double EDGE_CPU_LENGTH = 3000;
	public static double EDGE_NW_LENGTH = 500;
	public static double EDGE_PERIODICITY = 1000;
	
	public static double SENSOR_DESTRIBUTION = 5.1;
}
