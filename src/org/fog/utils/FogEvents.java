package org.fog.utils;

public class FogEvents {
	private static final int BASE = 50;
	
	public static final int TUPLE_ARRIVAL = BASE + 1;
	public static final int LAUNCH_MODULE = BASE + 2;
	public static final int RELEASE_OPERATOR = BASE + 3;
	public static final int APP_SUBMIT = BASE + 4;
	public static final int CALCULATE_INPUT_RATE = BASE + 5;
	public static final int CALCULATE_UTIL = BASE + 6;
	public static final int UPDATE_RESOURCE_USAGE = BASE + 7;
	public static final int ADAPTIVE_OPERATOR_REPLACEMENT = BASE + 8;
	public static final int GET_RESOURCE_USAGE = BASE + 9;
	public static final int RESOURCE_USAGE = BASE + 10;
	public static final int CONTROL_MSG_ARRIVAL = BASE + 11;
	public static final int UPDATE_TUPLE_QUEUE = BASE + 12;
	public static final int ACTUATOR_JOINED = BASE + 13;
	public static final int STOP_SIMULATION = BASE + 14;
	public static final int SEND_PERIODIC_TUPLE = BASE + 15;
	public static final int RESOURCE_MGMT = BASE + 16;
	public static final int INITIALIZE_SENSOR = BASE + 17;
	public static final int EMIT_TUPLE = BASE + 18;
	
	// Mobile events
	public static final int UPDATE_PERIODIC_MOVEMENT = BASE + 19;
	public static final int CONNECTION_LOST = BASE + 20;
	public static final int UPDATE_TOPOLOGY = BASE + 21;
	public static final int MIGRATION = BASE + 22;
	public static final int FINISH_MIGRATION = BASE + 23;
	public static final int DEALLOCATE_MEMORY = BASE + 24;
}
