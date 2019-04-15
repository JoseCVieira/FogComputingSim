package org.fog.gui.core;

public class Link {
	private Node dest = null;
	private double latency = 0.0;
	private double bandwidth = 0;
	
	public Link(Node dest, double latency) {
		this.dest = dest;
		this.latency = latency;
	}
	
	public Link(Node dest, double latency, double bandwidth) {
		this.dest = dest;
		this.latency = latency;
		this.bandwidth = bandwidth;
	}

	public Node getNode() {
		return dest;
	}

	public double getBandwidth() {
		return bandwidth;
	}
	
	public double getLatency() {
		return latency;
	}
	
}