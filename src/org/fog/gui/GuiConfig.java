package org.fog.gui;

/**
 * Class which holds all configuration values used within the GUI.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GuiConfig {
	
	// General ------------------------------------------------
	
	/** Number of fog devices generated in this topology */
	public static int NR_FOG_DEVICES = 5;
	
	/** Probability of creating a connection between two nodes */
	public static double CONNECTION_PROB = 0.65;
	
	/** Probability of being a client (i.e., to have an application to be deployed) */
	public static double CLIENT_PROBABILITY = 0.4;
	
	// Network ------------------------------------------------
	
	/** Mean of the latency normal distribution */
	public static double LAT_MEAN = 5;
	
	/** Deviation of the latency normal distribution */
	public static double LAT_DEV = 1;
	
	/** Mean of the bandwidth normal distribution */
	public static double BW_MEAN = 50*1024*1024;
	
	/** Deviation of the bandwidth normal distribution */
	public static double BW_DEV = 1*1024*1024;
	
	// Device capacity ----------------------------------------
	
	/** Mean of the processing capacity normal distribution */
	public static double MIPS_MEAN = 300000;
	
	/** Deviation of the processing capacity normal distribution */
	public static double MIPS_DEV = 15000;
	
	/** Mean of the memory normal distribution */
	public static int RAM_MEAN = 128*1024*1024;
	
	/** Deviation of the memory normal distribution */
	public static int RAM_DEV = 4*1024*1024;
	
	/** Mean of the storage normal distribution */
	public static int STRG_MEAN = 1024*1024*1024;
	
	/** Deviation of the storage normal distribution */
	public static int STRG_DEV = 128*1024*1024;
	
	/** Level decadency factor (mean and deviation resource values are multiplied by 1/(level*LEVEL_DECADENCY)) */
	public static int LEVEL_DECADENCY = 5;
	
	// Device price -------------------------------------------
	
	/** Mean of the rate per processing capacity usage normal distribution */
	public static double RATE_MIPS_MEAN = 1.5855E-5;
	
	/** Deviation of the rate per processing capacity usage normal distribution */
	public static double RATE_MIPS_DEV = 1E-6;
	
	/** Mean of the rate per memory usage normal distribution */
	public static double RATE_RAM_MEAN = 1.14E-5;
	
	/** Deviation of the rate per memory usage normal distribution */
	public static double RATE_RAM_DEV = 1.1E-6;
	
	/** Mean of the rate per storage usage normal distribution */
	public static double RATE_STRG_MEAN = 5E-5;
	
	/** Deviation of the rate per storage usage normal distribution */
	public static double RATE_STRG_DEV = 1.2E-6;
	
	/** Mean of the rate per network usage normal distribution */
	public static double RATE_BW_MEAN = 3.9E-5;
	
	/** Deviation of the rate per network usage normal distribution */
	public static double RATE_BW_DEV = 1.3E-6;
	
	/** Mean of the rate per energy consumed normal distribution */
	public static double RATE_EN_MEAN = 5E-5;
	
	/** Deviation of the rate per energy consumed normal distribution */
	public static double RATE_EN_DEV = 1.4E-6;
	
	// Device power -------------------------------------------
	
	/** Mean of the busy power normal distribution */
	public static double BUSY_POWER = 127.339*16;
	
	/** Mean of the idle power normal distribution */
	public static double IDLE_POWER = 83.4333*16;
	
	/** Deviation of the energy normal distribution */
	public static double ENERGY_DEV = 5;
	
	// Module -------------------------------------------------
	
	/** Mean of the memory of the modules normal distribution */
	public static int MODULE_RAM = 2*1024*1024;
	
	// Application edge ---------------------------------------
	
	/** Default CPU length value for the tuples */
	public static double EDGE_CPU_LENGTH = 3000;
	
	/** Default network length value for the tuples */
	public static double EDGE_NW_LENGTH = 500;
	
	/** Default periodicity value for the tuples */
	public static double EDGE_PERIODICITY = 10;
	
	// Sensor -------------------------------------------------
	
	/** Mean value for sensor tuple generation */
	public static double SENSOR_DESTRIBUTION = 5.1;
}
