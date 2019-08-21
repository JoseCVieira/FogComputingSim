package org.fog.gui.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fog.application.Application;

/**
 * A graph model. Normally a model should not have any logic, but in this case we implement logic to manipulate the
 * devicesList like reorganizing, adding nodes, removing nodes.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Graph implements Serializable {
	private static final long serialVersionUID = 745864022429447529L;
	
	/** List holding both the fog nodes and all its links */
	private Map<Node, List<Link>> devicesList;
	
	/** List which contains all the defined applications */
	private List<Application> appList;
	
	/**
	 * Creates a new empty graph.
	 */
	public Graph() {
		devicesList = new HashMap<Node, List<Link>>();
		appList = new ArrayList<Application>();
	}
	
	/**
	 * Adds a given device to the devices list. If the base node is not yet part of the devicesList a new entry is added.
	 * 
	 * @param key the node
	 * @param value the link
	 */
	public void addEdge(Node key, Link value) {
		if (devicesList.containsKey(key)) {
			if (devicesList.get(key) == null)
				devicesList.put(key, new ArrayList<Link>());
			
			if (value != null)
				devicesList.get(key).add(value);
		} else {
			List<Link> edges = new ArrayList<Link>();
			if (value != null)
				edges.add(value);

			devicesList.put(key, edges);
		}
	}
	
	/**
	 * Simply adds a new node, without setting any edges.
	 * 
	 * @param node the node
	 */
	public void addNode(Node node) {
		addEdge(node, null);
	}
	
	/**
	 * Removes an edge from the devices list.
	 * 
	 * @param key the node
	 * @param value the link
	 */
	public void removeEdge(Node key, Link value) {
		if (!devicesList.containsKey(key))
			throw new IllegalArgumentException("The devices list does not contain a node for the given key: " + key);
		
		List<Link> edges = devicesList.get(key);
		
		if (!edges.contains(value)) 
			throw new IllegalArgumentException("The list of edges does not contain the given edge to remove: " + value);

		edges.remove(value);
		
		List<Link> reverseEdges = devicesList.get(value.getNode());
		List<Link> toRemove = new ArrayList<Link>();
		for (Link edge : reverseEdges) {
			if (edge.getNode().getName().equals(key.getName())) {
				toRemove.add(edge);
			}
		}
		
		reverseEdges.removeAll(toRemove);
	}
	
	/**
	 * Deletes a node.
	 * 
	 * @param key the node to be removed
	 */
	public void removeNode(Node key) {
		if (!devicesList.containsKey(key))
			throw new IllegalArgumentException("The devices list does not contain a node for the given key: " + key);

		devicesList.remove(key);

		// clean up all edges
		for (Entry<Node, List<Link>> entry : devicesList.entrySet()) {
			List<Link> toRemove = new ArrayList<Link>();

			for (Link edge : entry.getValue())
				if (edge.getNode().getName().equals(key.getName()))
					toRemove.add(edge);
			
			entry.getValue().removeAll(toRemove);
		}
	}
	
	/**
	 * Verifies whether there are some other nodes with the same name as the provided one.
	 * 
	 * @param name the name to be checked
	 * @return true it's a repeated name, otherwise false
	 */
	public boolean isRepeatedName(String name) {
		for(Node node : devicesList.keySet())
			if(node.getName().equals(name))
				return true;
		return false;
	}
	
	/**
	 * Adds a new application to the application list.
	 * 
	 * @param application the application to be added
	 */
	public void addApp(Application application) {
		getAppList().add(application);
	}
	
	/**
	 * Removes an application from the application list.
	 * 
	 * @param application the application to be removed
	 */
	public void removeApp(Application application) {
		getAppList().remove(application);
	}
	
	/**
	 * Gets the application list.
	 * 
	 * @return the application list
	 */
	public List<Application> getAppList() {
		return appList;
	}
	
	/**
	 * Sets the application list.
	 * 
	 * @param appList the application list
	 */
	public void setAppList(List<Application> appList) {
		this.appList = appList;
	}
	
	/**
	 * Converts the current topology into a JSON string.
	 * 
	 * @return the JSON string
	 */
	public String toJsonString(){
		return Bridge.graphToJson(this);
	}
	
	/**
	 * Sets the devices list.
	 * 
	 * @param devicesList the devices list
	 */
	public void setDevicesList(Map<Node, List<Link>> devicesList) {
		this.devicesList = devicesList;
	}
	
	/**
	 * Gets the devices list.
	 * 
	 * @return the devices list
	 */
	public Map<Node, List<Link>> getDevicesList() {
		return devicesList;
	}
	
	/**
	 * Gets the maximum level of the graph (levels are only used within the GUI).
	 * 
	 * @return the maximum level of the graph
	 */
	public int getMaxLevel() {
		int maxLevel = -1;
		
		for (Entry<Node, List<Link>> entry : devicesList.entrySet()) {
			Node node = entry.getKey();
			if(maxLevel < node.getLevel())
				maxLevel = node.getLevel();
		}
		
		return maxLevel;
	}
	
	public void clean() {
		devicesList = new HashMap<Node, List<Link>>();
		appList = new ArrayList<Application>();
	}
	
	@Override
	public String toString() {
		return "Graph [devicesList=" + devicesList + "]";
	}
}