package org.fog.utils;

import org.fog.entities.Tuple;

/**
 * Class which computes the network usage (given in seconds) in transmit both tuples and virtual machines
 * between nodes. It is also responsible for counting both the number of packages dropped and successfully
 * delivered during the whole simulation.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class NetworkMonitor {
	/** The total efficient time in which the network was busy sending tuples [s] */
	private static double networkUsage = 0.0;
	
	/** The number of tuples dropped */
	private static int packetDrop;
	
	/** The number of tuples successfully delivered */
	private static int packetSuccess;
	
	/**
	 * Updates the network usage.
	 * 
	 * @param bandwidth the bandwidth available in the link
	 * @param tupleNwSize the size of the tuple to be transmitted
	 */
	public static void sendingTuple(final double bandwidth, final Tuple tuple) {
		networkUsage += tuple.getCloudletFileSize()/bandwidth;
	}
	
	/**
	 * Gets the total network usage.
	 * 
	 * @return the total network usage
	 */
	public static double getNetworkUsage(){
		return networkUsage;
	}
	
	/**
	 * Increment the number of tuples dropped.
	 */
	public static void incrementPacketDrop() {
		packetDrop++;
	}
	
	/**
	 * Get the number of tuples dropped.
	 * 
	 * @return the number of tuples dropped
	 */
	public static int getPacketDrop() {
		return packetDrop;
	}
	
	/**
	 * Increment the number of tuples successfully delivered.
	 */
	public static void incrementPacketSuccess() {
		packetSuccess++;
	}
	
	/**
	 * Get the number of tuples successfully delivered.
	 * 
	 * @return the number of tuples successfully delivered
	 */
	public static int getPacketSuccess() {
		return packetSuccess;
	}
}
