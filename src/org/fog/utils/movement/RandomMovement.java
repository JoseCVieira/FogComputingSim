package org.fog.utils.movement;

import java.util.Random;

import org.fog.core.FogComputingSim;
import org.fog.utils.Util;

/**
 * Class which defines the random movement of a node.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class RandomMovement extends Movement {
	public static final double PROB_CHANGE_DIRECTION = 0.25;
	public static final double PROB_CHANGE_VELOCITY = 0.35;
	public static final double PROB_MAX_VELOCITY = 0.1111;
	public static final double PROB_MED_VELOCITY = 0.6667;
	public static final double PROB_MIN_VELOCITY = 0.1111;
	public static final double PROB_NUL_VELOCITY = 0.1111;
	
	/**
	 * Defines a new random movement.
	 * 
	 * @param location the initial location
	 */
	public RandomMovement(Location location) {
		setLocation(location);
	}
	
	/**
	 * Updates the location of the node based on its movement.
	 */
	@Override
	public void updateLocation() {
		int counter = 0;
		Random random = new Random();
		Location location;
		
		double changeDirProb = PROB_CHANGE_DIRECTION;
		double changeVelProb = PROB_CHANGE_VELOCITY;
		
		while(true) {
			if(random.nextDouble() < changeDirProb) {
				setDirection(random.nextInt(Movement.SOUTHEAST + 1));
			}
			
			if(random.nextDouble() < changeVelProb) {
				double value = random.nextDouble();
				
				double pNul = PROB_NUL_VELOCITY;
				double pMin = PROB_MIN_VELOCITY + PROB_NUL_VELOCITY;
				double pMed = PROB_MIN_VELOCITY + PROB_NUL_VELOCITY + PROB_MED_VELOCITY;
				
				if(value < pNul)
					setVelocity(NUL_VELOCITY);
				else if(value >= pNul && value < pMin)
					setVelocity(Util.normalRand(MIN_VELOCITY, 1));
				else if(value >= pMin && value < pMed)
					setVelocity(Util.normalRand(MED_VELOCITY, 1));
				else
					setVelocity(Util.normalRand(MAX_VELOCITY, 1));
			}
			
			// Change the direction or velocity just to ensure devices are within the defined square for test purposes (it can be removed)
			// The movement model which is defined in the current method can also be modified
			// Compute next position (but does not update; just to check if it will end up within the defined square)
			location = computeNextLocation();
			double x = location.getX();
			double y = location.getY();
			
			if(x >= -SQUARE_SIDE && x <= SQUARE_SIDE && y >= -SQUARE_SIDE && y <= SQUARE_SIDE)
				break;
			else
				changeDirProb = 1;
			
			if(++counter > 100)
				FogComputingSim.err("It looks like the random update location method is running in an infinite loop");
		}
		
		setLocation(location);
	}
}
