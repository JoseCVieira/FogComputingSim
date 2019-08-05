package org.fog.utils;

/**
 * Class which defines the events used in the whole simulation.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class FogEvents {
	private static final int BASE = 50;
	
	// Basic events
	public static final int TUPLE_ARRIVAL = BASE + 1;
	public static final int LAUNCH_MODULE = BASE + 2;
	public static final int APP_SUBMIT = BASE + 3;
	public static final int UPDATE_TUPLE_QUEUE = BASE + 4;
	public static final int ACTUATOR_JOINED = BASE + 5;
	public static final int STOP_SIMULATION = BASE + 6;
	public static final int SEND_PERIODIC_TUPLE = BASE + 7;
	public static final int RESOURCE_MGMT = BASE + 8;
	public static final int EMIT_TUPLE = BASE + 9;
	
	// Mobile events
	public static final int UPDATE_PERIODIC_MOVEMENT = BASE + 10;
	public static final int CONNECTION_LOST = BASE + 11;
	public static final int UPDATE_TOPOLOGY = BASE + 12;
	public static final int MIGRATION = BASE + 13;
	public static final int FINISH_MIGRATION = BASE + 14;
	public static final int FINISH_SETUP_MIGRATION = BASE + 15;
	public static final int UPDATE_VM_POSITION = BASE + 16;
	
}
