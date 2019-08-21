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
	public static double MAX_CONN_LAT = 5;
	
	/** Maximum bandwidth generated between two nodes */
	public static double MAX_CONN_BW = 50*1024*1024;
	
	/** Probability of creating a connection between two nodes */
	public static double CONNECTION_PROB = 0.7;
	
	/** Probability of being a client (i.e., to have an application to be deployed) */
	public static double CLIENT_PROBABILITY = 0.4;
	
	/** Deviation of the resources normal distribution */
	public static double RESOURCES_DEV = 100;
	
	/** Deviation of the energy normal distribution */
	public static double ENERGY_DEV = 5;
	
	/** Deviation of the cost normal distribution */
	public static double COST_DEV = 1E-5;
	
	public static String FOG_NAME = "FogNode";
	
	// Prices from https://www.cloudsigma.com/pricing/
	/** Mean of the processing capacity normal distribution */
	public static double MIPS = 22733.5;
	
	/** Mean of the memory normal distribution */
	public static int RAM = 131072;
	
	/** Mean of the storage normal distribution */
	public static int STRG = 1048576;
	
	/** Mean of the rate per processing capacity usage normal distribution */
	public static double RATE_MIPS = 1.5855E-04;
	
	/** Mean of the rate per memory usage normal distribution */
	public static double RATE_RAM = 1.14E-07;
	
	/** Mean of the rate per storage usage normal distribution */
	public static double RATE_STRG = 5E-11;
	
	/** Mean of the rate per network usage normal distribution */
	public static double RATE_BW = 3.9E-05;
	
	/** Mean of the rate per energy consumed normal distribution */
	public static double RATE_EN = 5E-8;
	
	/** Mean of the idle power normal distribution */
	public static double IDLE_POWER = 83.4333;
	
	/** Mean of the busy power normal distribution */
	public static double BUSY_POWER = 127.339;
	
	/** Mean of the memory of the modules normal distribution */
	public static int MODULE_RAM = 8192;
	
	/** Default CPU length value for the tuples */
	public static double EDGE_CPU_LENGTH = 3000;
	
	/** Default network length value for the tuples */
	public static double EDGE_NW_LENGTH = 500;
	
	/** Default periodicity value for the tuples */
	public static double EDGE_PERIODICITY = 10;
	
	/** Mean value for sensor tuple generation */
	public static double SENSOR_DESTRIBUTION = 5.1;
}
