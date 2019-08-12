package org.fog.gui.core;

/**
 * The model that represents an edge with two vertexes, for physical link and virtual edge.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Link {
	/** Destination node */
	private Node dest;
	
	/** Link latency */
	private double latency;
	
	/** Link bandwidth */
	private double bandwidth;
	
	/**
	 * Creates a new link in the topology.
	 * 
	 * @param dest the destination node
	 * @param latency the link latency
	 * @param bandwidth the link bandwidth
	 */
	public Link(Node dest, double latency, double bandwidth) {
		this.dest = dest;
		this.latency = latency;
		this.bandwidth = bandwidth;
	}
	
	/**
	 * Gets the destination node.
	 * 
	 * @return the destination node
	 */
	public Node getNode() {
		return dest;
	}
	
	/**
	 * Gets the link bandwidth value.
	 * 
	 * @return the link bandwidth value
	 */
	public double getBandwidth() {
		return bandwidth;
	}
	
	/**
	 * Gets the link latency value.
	 * 
	 * @return the link latency value
	 */
	public double getLatency() {
		return latency;
	}
	
}