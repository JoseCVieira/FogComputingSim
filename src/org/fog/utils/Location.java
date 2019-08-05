package org.fog.utils;

import org.fog.entities.FogDevice;

/**
 * Class which defines the location of both mobile and static machines.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since   July, 2019
 */
public class Location {
	/** The X coordinate of the node */
	private double x;
	
	/** The Y coordinate of the node */
	private double y;
	
	/**
	 * Creates a new location.
	 * 
	 * @param x the X coordinate of the node
	 * @param y the Y coordinate of the node
	 */
	public Location(final double x, double y){
		this.setX(x);
		this.setY(y);
	}
	
	/**
	 * Computes the distance between two nodes.
	 * 
	 * @param f1 the first node
	 * @param f2 the second node
	 * @return the distance between two nodes
	 */
	public static double computeDistance(final FogDevice f1, final FogDevice f2) {
		Location l1 = f1.getMovement().getLocation();
		Location l2 = f2.getMovement().getLocation();
		
		double first = Math.pow(l1.getX() - l2.getX(), 2);
		double second = Math.pow(l1.getY() - l2.getY(), 2);
		return Math.sqrt(first + second);
	}

	/**
	 * Gets the X coordinate of the node.
	 * 
	 * @return the X coordinate of the node
	 */
	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	/**
	 * Gets the Y coordinate of the node.
	 * 
	 * @return the Y coordinate of the node
	 */
	public double getY() {
		return y;
	}

	/**
	 * Sets the Y coordinate of the node.
	 * 
	 * @param the Y coordinate of the node
	 */
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return "Location [x=" + x + ", y=" + y + "]";
	}
	
}
