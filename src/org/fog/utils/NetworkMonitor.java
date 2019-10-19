package org.fog.utils;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.core.FogComputingSim;
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
	/** The number of tuples dropped */
	private static int packetDrop;
	
	/** The number of tuples successfully delivered */
	private static int packetSuccess;
	
	/** Map holding the size of data transmitted between entities */
	private static Map<Map<Integer, Integer>, Long> networkUsageMap = new HashMap<Map<Integer,Integer>, Long>();
	
	/** Map holding the last creation time of each link and time total time each link was available */
	private static Map<Map<Integer, Integer>, Map<Double, Double>> connectionsMap = new HashMap<Map<Integer,Integer>, Map<Double,Double>>();
	
	/** Map holding the connections velocity */
	private static Map<Map<Integer, Integer>, Double> connectionsVelocityMap = new HashMap<Map<Integer,Integer>, Double>();
	
	/**
	 * Updates the network usage map.
	 * 
	 * @param tuple the tuple which will be sent
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 */
	public static void sendingTuple(final Tuple tuple, final int from, final int to) {
		long size = tuple.getCloudletFileSize();
		
		Map<Integer, Integer> map = new HashMap<Integer, Integer>();
		map.put(from, to);
		if(networkUsageMap.containsKey(map))
			networkUsageMap.put(map, networkUsageMap.get(map) + size);
		else
			networkUsageMap.put(map, size);
	}
	
	/**
	 * Adds a new connection to the connections map and update its start time.
	 * 
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 * @param velocity the connection velocity
	 */
	public static void addConnection(final int from, final int to, final double velocity) {
		Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
		Map<Double, Double> time = new HashMap<Double, Double>();
		
		connection.put(from, to);
		
		if(connectionsMap.containsKey(connection)) {
			double totalTime = connectionsMap.get(connection).entrySet().iterator().next().getValue();
			time.put(CloudSim.clock(), totalTime);
		}else {
			time.put(CloudSim.clock(), 0.0);
			connectionsVelocityMap.put(connection, velocity);
			networkUsageMap.put(connection, (long) 0);
		}
		
		connectionsMap.put(connection, time);
	}
	
	/**
	 * Updates the connection time in the connections map.
	 * 
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 */
	public static void removeConnection(final int from, final int to) {
		Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
		Map<Double, Double> time = new HashMap<Double, Double>();
		
		connection.put(from, to);
		
		double startTime = connectionsMap.get(connection).entrySet().iterator().next().getKey();
		double totalTime = connectionsMap.get(connection).entrySet().iterator().next().getValue();
		
		if(startTime == -1)
			FogComputingSim.err("NetworkMonitor Err: Should not happen");
		
		time.put(-1.0, CloudSim.clock() - startTime + totalTime);
		
		connectionsMap.put(connection, time);
	}
	
	/**
	 * Gets the total connection time between two nodes.
	 * 
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 * @return the total connection time between two nodes; -1 if the connection had never existed
	 */
	public static double getTotalConnectionTime(final int from, final int to) {
		Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
		connection.put(from, to);
		
		if(connectionsMap.containsKey(connection)) {
			double startTime = connectionsMap.get(connection).entrySet().iterator().next().getKey();
			double totalTime = connectionsMap.get(connection).entrySet().iterator().next().getValue();
			
			if(startTime == -1)
				return totalTime;
			
			return CloudSim.clock() - startTime + totalTime;
		}
		
		return -1;
	}
	
	/**
	 * Gets the connection velocity between two nodes.
	 *
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 * @return the connection velocity between two nodes; -1 if the connection had never existed
	 */
	public static double getConnectionVelocity(final int from, final int to) {
		Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
		connection.put(from, to);
		
		if(connectionsVelocityMap.containsKey(connection))
			return connectionsVelocityMap.get(connection);
		
		return -1;
	}
	
	/**
	 * Gets the map holding the size of data transmitted between entities.
	 * 
	 * @return the network usage map
	 */
	
	/**
	 * Gets the total size transmitted within a given link.
	 *
	 * @param from the id of the source node
	 * @param to the id of the destination node
	 * @return the total size transmitted within a given link; -1 if the connection had never existed
	 */
	public static long getNetworkUsageMap(final int from, final int to) {
		Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
		connection.put(from, to);
		
		if(networkUsageMap.containsKey(connection))
			return networkUsageMap.get(connection);
		
		return -1;
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
