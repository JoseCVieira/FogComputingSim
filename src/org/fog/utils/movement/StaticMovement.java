package org.fog.utils.movement;

/**
 * Class which defines the static movement of a node.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class StaticMovement extends Movement {
	
	public StaticMovement(Location location) {
		setLocation(location);
	}
	
	@Override
	public void updateLocation() {
		// Do nothing
	}

}
