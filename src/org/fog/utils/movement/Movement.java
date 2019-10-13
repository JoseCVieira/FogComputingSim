package org.fog.utils.movement;

import org.fog.core.Config;
import org.fog.core.FogComputingSim;

/**
 * Class which defines the movement of a node, being composed by its location, direction and velocity.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public abstract class Movement {
	protected static final int EAST = 0;
	protected static final int NORTHEAST = 1;
	protected static final int NORTH = 2;
	protected static final int NORTHWEST = 3;
	protected static final int WEST = 4;
	protected static final int SOUTHWEST = 5;
	protected static final int SOUTH = 6;
	protected static final int SOUTHEAST = 7;
	
	protected static final int SQUARE_SIDE = 10000;
	
	protected static final double MAX_VELOCITY = 33.3333;	// 33.3333 m/s = 120 km/h 	-> high speed car
	protected static final double MED_VELOCITY = 13.8889;	// 13.8889 m/s = 50 km/h  	-> slow speed car
	protected static final double MIN_VELOCITY = 1.34000;	// 1.34 m/s					-> average walking speed
	protected static final double NUL_VELOCITY = 0;			// 0 m/s					-> stopped
	
	/** The velocity of the node */
	private double velocity;
	
	/** The direction of the node */
	private int direction;
	
	/** The location of the node */
	private Location location;
	
	/**
	 * Computes the next location of the node based on its movement.
	 * 
	 * @return the next location of the node
	 */
	protected Location computeNextLocation() {
		double nextX = location.getX();
		double nextY = location.getY();
		
		switch (direction) {
		case EAST:
			nextX = location.getX() + velocity*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case WEST:
			nextX = location.getX() - velocity*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case SOUTH:
			nextY = location.getY() - velocity*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case NORTH:
			nextY = location.getY() + velocity*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case SOUTHEAST:
			nextX = location.getX() + velocity*Math.cos(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			nextY = location.getY() - velocity*Math.sin(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case NORTHWEST:
			nextX = location.getX() - velocity*Math.cos(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			nextY = location.getY() + velocity*Math.sin(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case SOUTHWEST:
			nextX = location.getX() - velocity*Math.cos(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			nextY = location.getY() - velocity*Math.sin(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		case NORTHEAST:
			nextX = location.getX() + velocity*Math.cos(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			nextY = location.getY() + velocity*Math.sin(45)*Config.PERIODIC_MOVEMENT_UPDATE;
			break;
		default:
			break;
		}
		
		return new Location(nextX, nextY);
	}
	
	/**
	 * Updates the location of the node based on its movement.
	 */
	public abstract void updateLocation();
	
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
	protected void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	/**
	 * Gets the direction of the node.
	 * 
	 * @return the direction of the node
	 */
	protected int getDirection() {
		return direction;
	}
	
	/**
	 * Sets the direction of the node.
	 * 
	 * @param direction the direction of the node
	 */
	protected void setDirection(int direction) {
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
	protected void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "Movement [velocity=" + velocity + ", direction=" + direction + ", location=" + location + "]";
	}
	
}
