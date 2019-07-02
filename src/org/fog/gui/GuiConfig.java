package org.fog.gui;

public class GuiConfig {
	public static final String ADD = "ADD";
	public static final String EDIT = "EDIT";
	
	public static final String FOG_NAME = "FogNode";
	public static final String ACTUATOR_NAME = "Actuator";
	public static final String SENSOR_NAME = "Sensor";
	
	// Prices from https://www.cloudsigma.com/pricing/
	public static final double BW = 10240;
	public static final double MIPS = 22733.5;
	public static final int RAM = 131072;
	public static final int STRG = 1048576;
	public static final double RATE_MIPS = 1.5855E-04;
	public static final double RATE_RAM = 1.14E-07;
	public static final double RATE_STRG = 5E-11;
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
}
