package org.fog.utils;

import org.fog.core.FogComputingSim;

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
	
	public Movement(double velocity, int direction, Location location) {
		setVelocity(velocity);
		setDirection(direction);
		setLocation(location);
	}
	
	public void updateLocation() {
		switch (direction) {
		case EAST:
			location.setX(location.getX() + velocity);
			break;
		case WEST:
			location.setX(location.getX() - velocity);
			break;
		case SOUTH:
			location.setY(location.getY() + velocity);
			break;
		case NORTH:
			location.setY(location.getY() - velocity);
			break;
		case SOUTHEAST:
			location.setX(location.getX() + velocity/2);
			location.setY(location.getY() + velocity/2);
			break;
		case NORTHWEST:
			location.setX(location.getX() - velocity/2);
			location.setY(location.getY() - velocity/2);
			break;
		case SOUTHWEST:
			location.setX(location.getX() - velocity/2);
			location.setY(location.getY() + velocity/2);
			break;
		case NORTHEAST:
			location.setX(location.getX() + velocity/2);
			location.setY(location.getY() - velocity/2);
			break;
		default:
			break;
		}
	}
	
	public double getVelocity() {
		return velocity;
	}
	
	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	public int getDirection() {
		return direction;
	}
	
	public void setDirection(int direction) {
		if(direction < EAST || direction > SOUTHEAST)
			FogComputingSim.err("Unknown direction.\nFogComputingSim will terminate abruptally");
		
		this.direction = direction;
	}

	public Location getLocation() {
		return location;
	}
	
	private void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "Movement [velocity=" + velocity + ", direction=" + direction + ", location=" + location + "]";
	}
	
}
