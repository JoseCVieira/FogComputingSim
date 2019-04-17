package org.fog.gui.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Tuple;
import org.fog.utils.FogUtils;

public class ApplicationGui {
	private Map<String, AppEdge> edgeMap;
	
	private List<AppModule> modules;
	private List<AppEdge> edges;
	private List<Tuple> tuples;
	
	private List<List<String>> loops = new ArrayList<List<String>>();
	
	private String appId;

	@SuppressWarnings("unchecked")
	public ApplicationGui(String appId, Object loops) {
		this.setAppId(appId);
		this.setLoops((List<List<String>>) loops);
		this.setEdges(new ArrayList<AppEdge>());
		this.setModules(new ArrayList<AppModule>());
		this.setTuples(new ArrayList<Tuple>());
		this.setEdgeMap(new HashMap<String, AppEdge>());
	}
	
	public void addAppModule(String moduleName, int ram, long mem){
		String vmm = "Xen";
		int userId = -1;
		
		AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, userId, 0, ram, 0, mem, vmm,
				new CloudletSchedulerTimeShared(), new HashMap<Pair<String, String>, SelectivityModel>());
		
		getModules().add(module);
	}
	
	public void addAppModule(String moduleName, double mips, int ram, long mem, long bw){
		String vmm = "Xen";
		int userId = -1;
		
		AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, userId, mips, ram, bw, mem, vmm,
				new CloudletSchedulerTimeShared(), new HashMap<Pair<String, String>, SelectivityModel>());
		
		getModules().add(module);
	}

	public void addAppEdge(String source, String destination, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int edgeType){
		AppEdge edge = new AppEdge(source, destination, tupleCpuLength, tupleNwLength, tupleType, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}

	public void addAppEdge(String source, String destination, double periodicity, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int edgeType){
		AppEdge edge = new AppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength,
			tupleType, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	public void addTupleMapping(String moduleName, String inputTupleType, String outputTupleType, SelectivityModel selectivityModel){
		AppModule module = getModuleByName(moduleName);
		module.getSelectivityMap().put(new Pair<String, String>(inputTupleType, outputTupleType), selectivityModel);
	}
	
	public AppModule getModuleByName(String name){
		for(AppModule module : modules){
			if(module.getName().equals(name))
				return module;
		}
		return null;
	}
	
	public void addAppLoop(List<String> loop){
		loops.add(loop);
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public List<AppModule> getModules() {
		return modules;
	}

	public void setModules(List<AppModule> modules) {
		this.modules = modules;
	}

	public List<AppEdge> getEdges() {
		return edges;
	}

	public void setEdges(List<AppEdge> edges) {
		this.edges = edges;
	}

	public List<Tuple> getTuples() {
		return tuples;
	}

	public void setTuples(List<Tuple> tuples) {
		this.tuples = tuples;
	}
	
	public Map<String, AppEdge> getEdgeMap() {
		return edgeMap;
	}

	public void setEdgeMap(Map<String, AppEdge> edgeMap) {
		this.edgeMap = edgeMap;
	}
	
	public List<List<String>> getLoops(){
		return loops;
	}
	
	public void setLoops(List<List<String>> loops){
		this.loops = loops;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((appId == null) ? 0 : appId.hashCode());
		result = prime * result + ((edgeMap == null) ? 0 : edgeMap.hashCode());
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result + ((loops == null) ? 0 : loops.hashCode());
		result = prime * result + ((modules == null) ? 0 : modules.hashCode());
		result = prime * result + ((tuples == null) ? 0 : tuples.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ApplicationGui other = (ApplicationGui) obj;
		if (appId == null) {
			if (other.appId != null)
				return false;
		} else if (!appId.equals(other.appId))
			return false;
		if (edgeMap == null) {
			if (other.edgeMap != null)
				return false;
		} else if (!edgeMap.equals(other.edgeMap))
			return false;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (loops == null) {
			if (other.loops != null)
				return false;
		} else if (!loops.equals(other.loops))
			return false;
		if (modules == null) {
			if (other.modules != null)
				return false;
		} else if (!modules.equals(other.modules))
			return false;
		if (tuples == null) {
			if (other.tuples != null)
				return false;
		} else if (!tuples.equals(other.tuples))
			return false;
		return true;
	}

}