package org.fog.core;

public class Constants {
	public static final double INF = Double.MAX_VALUE;
	public static final double IINF = Integer.MAX_VALUE;
	public static final double SINF = Short.MAX_VALUE;
	public static final double EPSILON = 1E-9;
	
	public static final double MIN_SOLUTION = Short.MAX_VALUE;
	
	public static final String FOG_TYPE = "FOG_DEVICE";
	public static final String SENSOR_TYPE = "SENSOR";
	public static final String ACTUATOR_TYPE = "ACTUATOR";
	public static final String SENSOR_MODULE_TYPE = "SENSOR_MODULE";
	public static final String ACTUATOR_MODULE_TYPE = "ACTUATOR_MODULE";
	public static final String APP_MODULE_TYPE = "APP_MODULE";
	
	public static final double RESOURCE_MGMT_INTERVAL = 100;
	public static final int MAX_SIMULATION_TIME = 10000;
	public static final int RESOURCE_MANAGE_INTERVAL = 100;
	public static final String FOG_DEVICE_ARCH = "x86";
	public static final String FOG_DEVICE_OS = "Linux";
	public static final String FOG_DEVICE_VMM = "Xen";
	public static final double FOG_DEVICE_TIMEZONE = 0.0;
	public static final double SCHEDULING_INTERVAL = 10;
}
