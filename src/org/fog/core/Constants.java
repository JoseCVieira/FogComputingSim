package org.fog.core;

/**
 * Class which holds all constants used within the FogComputingSim.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Constants {
	public static final double INF = Double.MAX_VALUE;
	public static final double EPSILON = 1E-9;
	public static final double REFERENCE_COST = Integer.MAX_VALUE;
	
	public static final double RESOURCE_MGMT_INTERVAL = 100;
	public static final String FOG_DEVICE_ARCH = "x86";
	public static final String FOG_DEVICE_OS = "Linux";
	public static final String FOG_DEVICE_VMM = "Xen";
	public static final double FOG_DEVICE_TIMEZONE = 0.0;
	public static final double SCHEDULING_INTERVAL = 10;
}
