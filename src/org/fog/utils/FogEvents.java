package org.fog.utils;

public class FogEvents {
	private static final int BASE = 50;
	public static final int TUPLE_ARRIVAL = BASE + 1;
	public static final int LAUNCH_MODULE = BASE + 2;
	public static final int RELEASE_OPERATOR = BASE + 3;
	//public static final int SENSOR_JOINED = BASE + 4;
	//public static final int TUPLE_ACK = BASE + 5;
	public static final int APP_SUBMIT = BASE + 6;
	public static final int CALCULATE_INPUT_RATE = BASE + 7;
	public static final int CALCULATE_UTIL = BASE + 8;
	public static final int UPDATE_RESOURCE_USAGE = BASE + 9;
	//public static final int TUPLE_FINISHED = BASE + 10;
	//public static final int ACTIVE_APP_UPDATE = BASE + 11;
	//public static final int CONTROLLER_RESOURCE_MANAGE = BASE + 12;
	public static final int ADAPTIVE_OPERATOR_REPLACEMENT = BASE + 13;
	public static final int GET_RESOURCE_USAGE = BASE + 14;
	public static final int RESOURCE_USAGE = BASE + 15;
	public static final int CONTROL_MSG_ARRIVAL = BASE + 16;
	public static final int UPDATE_TUPLE_QUEUE = BASE + 17;
	//public static final int UPDATE_NORTH_TUPLE_QUEUE = BASE + 17;
	//public static final int UPDATE_SOUTH_TUPLE_QUEUE = BASE + 18;
	public static final int ACTUATOR_JOINED = BASE + 19;
	public static final int STOP_SIMULATION = BASE + 20;
	public static final int SEND_PERIODIC_TUPLE = BASE + 21;
	//public static final int LAUNCH_MODULE_INSTANCE = BASE + 22;
	public static final int RESOURCE_MGMT = BASE + 23;
	public static final int INITIALIZE_SENSOR = BASE + 24;
	public static final int EMIT_TUPLE = BASE + 25;
	
	// Mobile events
	public static final int UPDATE_PERIODIC_MOVEMENT = BASE + 26;
	public static final int VERIFY_HANDOVER = BASE + 27;
	public static final int REMOVE_LINK = BASE + 28;
	public static final int ADD_NEW_LINK = BASE + 29;
	public static final int HANDOVER_COMPLETED = BASE + 30;
	
}
