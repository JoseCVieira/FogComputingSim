package org.fog.utils;

import org.fog.core.FogComputingSim;

/**
 * Class which defines the movement of a node, being composed by its location, direction and velocity.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @see     tecnico.ulisboa.pt
 * @since   July, 2019
 */
public class Movement {
	public static final int EAST = 0;
	public static final int NORTHEAST = 1;
	public static final int NORTH = 2;
	public static final int NORTHWEST = 3;
	public static final int WEST = 4;
	public static final int SOUTHWEST = 5;
	public static final int SOUTH = 6;
	public static final int SOUTHEAST = 7;
	
	private double velocity;
	private int direction;
	private Location location;
	
	/**
	 * Defines the movement of the node.
	 * 
	 * @param velocity the velocity of the node
	 * @param direction the direction of the node
	 * @param location the location of the node
	 */
	public Movement(double velocity, int direction, Location location) {
		setVelocity(velocity);
		setDirection(direction);
		setLocation(location);
	}
	
	/**
	 * Computes the next location of the node based on its movement.
	 * 
	 * @return the next location of the node
	 */
	public Location computeNextLocation() {
		double nextX = location.getX();
		double nextY = location.getY();
		
		switch (direction) {
		case EAST:
			nextX = location.getX() + velocity;
			break;
		case WEST:
			nextX = location.getX() - velocity;
			break;
		case SOUTH:
			nextY = location.getY() + velocity;
			break;
		case NORTH:
			nextY = location.getY() - velocity;
			break;
		case SOUTHEAST:
			nextX = location.getX() + velocity/2;
			nextY = location.getY() + velocity/2;
			break;
		case NORTHWEST:
			nextX = location.getX() - velocity/2;
			nextY = location.getY() - velocity/2;
			break;
		case SOUTHWEST:
			nextX = location.getX() - velocity/2;
			nextY = location.getY() + velocity/2;
			break;
		case NORTHEAST:
			nextX = location.getX() + velocity/2;
			nextY = location.getY() - velocity/2;
			break;
		default:
			break;
		}
		
		return new Location(nextX, nextY);
	}
	
	/**
	 * Updates the location of the node based on its movement.
	 */
	public void updateLocation() {
		Location newLocation = computeNextLocation();
		location.setX(newLocation.getX());
		location.setY(newLocation.getY());
	}
	
	/**
	 * Gets the velocity of the node.
	 * 
	 * @return the velocity of the node
	 */
	public double getVelocity() {
		return velocity;
	}
	
	/**
	 * Sets the velocity of the node.
	 * 
	 * @param velocity the velocity of the node
	 */
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	/**
	 * Gets the direction of the node.
	 * 
	 * @return the direction of the node
	 */
	public int getDirection() {
		return direction;
	}
	
	/**
	 * Sets the direction of the node.
	 * 
	 * @param direction the direction of the node
	 */
	public void setDirection(int direction) {
		if(direction < EAST || direction > SOUTHEAST)
			FogComputingSim.err("Unknown direction");
		
		this.direction = direction;
	}

	/**
	 * Gets the location of the node.
	 * 
	 * @return the location of the node
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Sets the location of the node.
	 * 
	 * @param location the location of the node
	 */
	private void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "Movement [velocity=" + velocity + ", direction=" + direction + ", location=" + location + "]";
	}
	
}
