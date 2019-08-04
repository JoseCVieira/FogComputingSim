package org.fog.utils;

/**
 * Class which defines the model used to compute the network usage (given in seconds) to use transmit both by tuples
 * and virtual machines between nodes.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @see     tecnico.ulisboa.pt
 * @since   July, 2019
 */
public class NetworkUsageMonitor {
	private static double networkUsage = 0.0;
	
	/**
	 * Updates the network usage.
	 * 
	 * @param latency the latency in the link
	 * @param bandwidth the bandwidth available in the link
	 * @param tupleNwSize the size of the tuple to be transmitted
	 */
	public static void sendingTuple(double latency, double bandwidth, double tupleNwSize){
		networkUsage += latency + (tupleNwSize/bandwidth);
	}
	
	/**
	 * Gets the total network usage.
	 * 
	 * @return the total network usage
	 */
	public static double getNetworkUsage(){
		return networkUsage;
	}
}
