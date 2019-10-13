package org.fog.utils.movement;

/**
 * Class which defines the rectangular movement of a node.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class RectangleMovement extends Movement {
	
	/** The x maximum of the rectangle */
	private double xUpper;
	
	/** The x minimum of the rectangle */
	private double xLower;
	
	/** The y maximum of the rectangle */
	private double yUpper;
	
	/** The y minimum of the rectangle */
	private double yLower;
	
	/** The x length */
	private double xLength;
	
	/** The y length */
	private double yLength;
	
	/**
	 * Defines a new random movement.
	 * 
	 * @param location the initial location
	 * @param xLength the total x length
	 * @param yLength the total y length
	 */
	public RectangleMovement(Location location, double velocity, double xLength, double yLength) {
		setLocation(location);
		setVelocity(velocity);
		setDirection(EAST);
		this.xUpper = location.getX() + xLength/2;
		this.xLower = location.getX() - xLength/2;
		this.yUpper = location.getY() + yLength/2;
		this.yLower = location.getY() - yLength/2;
		this.xLength = xLength;
		this.yLength = yLength;
	}
	
	/**
	 * Updates the location of the node based on its movement.
	 */
	@Override
	public void updateLocation() {
		Location location = computeNextLocation();
		double x = location.getX();
		double y = location.getY();
		
		switch (getDirection()) {
		case EAST:
			if(x > SQUARE_SIDE) {
				x = SQUARE_SIDE;
				setDirection(NORTH);
			}
			
			if(x > xUpper) {
				x = xUpper;
				setDirection(NORTH);
			}
			break;
		case NORTH:
			if(y > SQUARE_SIDE) {
				y = SQUARE_SIDE;
				setDirection(WEST);
			}
			
			if(y > yUpper) {
				y = yUpper;
				setDirection(WEST);
			}
			break;
		case WEST:
			if(x < -SQUARE_SIDE) {
				x = -SQUARE_SIDE;
				setDirection(SOUTH);
			}
			
			if(x < xLower) {
				x = xLower;
				setDirection(SOUTH);
			}
			break;
		case SOUTH:
			if(y < -SQUARE_SIDE) {
				y = -SQUARE_SIDE;
				setDirection(EAST);
			}
			
			if(y < yLower) {
				y = yLower;
				setDirection(EAST);
			}
			break;
		default:
			break;
		}
		
		setLocation(new Location(x, y));
	}
	
	/**
	 * Gets the x length.
	 * 
	 * @return the x length
	 */
	public double getxLength() {
		return xLength;
	}
	
	/**
	 * Gets the y length.
	 * 
	 * @return the y length
	 */
	public double getyLength() {
		return yLength;
	}
	
}
