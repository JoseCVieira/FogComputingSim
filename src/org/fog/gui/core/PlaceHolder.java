package org.fog.gui.core;

import org.fog.utils.movement.Location;

/**
 * The model that represents the node and its GUI coordinates.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class PlaceHolder {
	/** GUI coordinates */
	protected Location coordinates;
	
	/** Fog device */
	protected Node node;
	
	/**
	 * Creates a new place holder for the GUI.
	 * 
	 * @param x the x coordinate of the node
	 * @param y the y coordinate of the node
	 */
	public PlaceHolder(int x, int y){
		setCoordinates(new Location(x, y));
	}
	
	/**
	 * Gets the node.
	 * 
	 * @return the node
	 */
	public Node getNode() {
		return node;
	}
	
	/**
	 * Sets the node.
	 * 
	 * @param node the node
	 */
	public void setNode(Node node) {
		this.node = node;
	}
	
	/**
	 * Gets the fog device coordinates.
	 * 
	 * @return the fog device coordinates
	 */
	public Location getCoordinates() {
		return coordinates;
	}
	
	/**
	 * Sets the fog device coordinates.
	 * 
	 * @param coordinates the fog device coordinates
	 */
	public void setCoordinates(Location coordinates) {
		this.coordinates = coordinates;
	}
	
}
