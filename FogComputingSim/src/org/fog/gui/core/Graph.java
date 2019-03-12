package org.fog.gui.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.fog.utils.Config;

/**
 * A graph model. Normally a model should not have any logic, but in this case we implement logic to manipulate the
 * devicesList like reorganizing, adding nodes, removing nodes
 */
public class Graph implements Serializable {
	private static final long serialVersionUID = 745864022429447529L;
	
	private Map<Node, List<Edge>> devicesList;
	private List<ApplicationGui> appList;

	public Graph() {
		devicesList = new HashMap<Node, List<Edge>>();
		appList = new ArrayList<ApplicationGui>();
	}

	public Graph(Map<Node, List<Edge>> devicesList) {
		this.devicesList = devicesList;
	}

	/** Adds a given device to the devicesList. If the base node is not yet part of the devicesList a new entry is added */
	public void addEdge(Node key, Edge value) {
		if (devicesList.containsKey(key)) {
			if (devicesList.get(key) == null)
				devicesList.put(key, new ArrayList<Edge>());
			
			if (value != null)
				devicesList.get(key).add(value);
		} else {
			List<Edge> edges = new ArrayList<Edge>();
			if (value != null)
				edges.add(value);

			devicesList.put(key, edges);
		}
	}

	/** Simply adds a new node, without setting any edges */
	public void addNode(Node node) {
		addEdge(node, null);
	}

	public void removeEdge(Node key, Edge value) {
		if (!devicesList.containsKey(key))
			throw new IllegalArgumentException("The devices list does not contain a node for the given key: " + key);
		List<Edge> edges = devicesList.get(key);

		if (!edges.contains(value)) 
			throw new IllegalArgumentException("The list of edges does not contain the given edge to remove: " + value);

		edges.remove(value);
		// remove bidirectional
		List<Edge> reverseEdges = devicesList.get(value.getNode());
		List<Edge> toRemove = new ArrayList<Edge>();
		for (Edge edge : reverseEdges) {
			if (edge.getNode().equals(key)) {
				toRemove.add(edge);
			}
		}
		//normally only one element
		reverseEdges.removeAll(toRemove);
	}

	/** Deletes a node */
	public void removeNode(Node key) {
		if (!devicesList.containsKey(key))
			throw new IllegalArgumentException("The devices list does not contain a node for the given key: " + key);

		devicesList.remove(key);

		// clean up all edges
		for (Entry<Node, List<Edge>> entry : devicesList.entrySet()) {
			List<Edge> toRemove = new ArrayList<Edge>();

			for (Edge edge : entry.getValue())
				if (edge.getNode().equals(key))
					toRemove.add(edge);
			
			entry.getValue().removeAll(toRemove);
		}
	}
	
	public boolean isRepeatedName(String name) {
		for(Node node : devicesList.keySet())
			if(node.getName().equals(name))
				return true;
		return false;
	}
	
	public void addApp(ApplicationGui applicationGui) {
		getAppList().add(applicationGui);
	}
	
	public void removeApp(ApplicationGui applicationGui) {
		getAppList().remove(applicationGui);
	}
	
	public List<ApplicationGui> getAppList() {
		return appList;
	}

	public void setAppList(List<ApplicationGui> appList) {
		this.appList = appList;
	}
	
	public void clearGraph(){
		devicesList.clear();
	}
	
	public String toJsonString(){
		return Bridge.graphToJson(this);
	}
	
	public void setDevicesList(Map<Node, List<Edge>> devicesList) {
		this.devicesList = devicesList;
	}

	public Map<Node, List<Edge>> getDevicesList() {
		return devicesList;
	}
	
	public int getMaxLevel() {
		int maxLevel = -1;
		
		for (Entry<Node, List<Edge>> entry : devicesList.entrySet()) {
			Node node = entry.getKey();
			
			if(node.getType() == Config.FOG_TYPE) {
				if(maxLevel < ((FogDeviceGui)node).getLevel())
					maxLevel = ((FogDeviceGui)node).getLevel();
			}
		}
		
		return maxLevel;
	}
	
	@Override
	public String toString() {
		return "Graph [devicesList=" + devicesList + "]";
	}
}