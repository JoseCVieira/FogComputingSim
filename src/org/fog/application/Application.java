package org.fog.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.core.FogComputingSim;
import org.fog.entities.Tuple;
import org.fog.gui.core.ApplicationGui;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;

// Class represents an application in the Distributed Dataflow Model.
public class Application {
	private Map<String, AppEdge> edgeMap;
	private List<AppModule> modules; 	//List of application modules in the application
	private List<AppEdge> edges; 		//List of application edges in the application
	private List<AppLoop> loops; 		// List of application loops to monitor for delay
	private String appId;
	
	public Application(String appId) {
		setAppId(appId);
		setModules(new ArrayList<AppModule>());
		setEdges(new ArrayList<AppEdge>());
		setLoops(new ArrayList<AppLoop>());
		setEdgeMap(new HashMap<String, AppEdge>());
	}
	
	public Application(ApplicationGui applicationGui) {
		setAppId(applicationGui.getAppId());
		setModules(applicationGui.getModules());
		setEdges(applicationGui.getEdges());
		setEdgeMap(applicationGui.getEdgeMap());
		
		List<AppLoop> loops = new ArrayList<AppLoop>();
		for(List<String> loop : applicationGui.getLoops()) {
			ArrayList<String> l = new ArrayList<String>();
			for(String name : loop)
				l.add(name);
			loops.add(new AppLoop(l));
		}
		
		setLoops(loops);
	}
	
	/**
	 * Adds an application module to the application.
	 * @param moduleName
	 * @param ram
	 */
	public void addAppModule(String moduleName, int ram, boolean clientModule, boolean glogbalModule) {
		int mips = 0;
		long size = 10000;
		long bw = 0;
		String vmm = "Xen";
		
		if(clientModule && glogbalModule)
			FogComputingSim.err("Modules cannot be simultaneously client module and global module");
			
		AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, -1, 
			mips, ram, bw, size, vmm, new CloudletSchedulerTimeShared(), new HashMap<Pair<String, String>, SelectivityModel>(),
			clientModule, glogbalModule);
		
		getModules().add(module);
	}
	
	public void addAppModule(AppModule m, int fogId) {
		AppModule module = null;
		
		if(m.isClientModule() && m.isGlobalModule())
			FogComputingSim.err("Modules cannot be simultaneously client module and global module");
		
		if(m.isGlobalModule()) {
			module = new AppModule(FogUtils.generateEntityId(), m.getName(), appId, fogId, 
					m.getMips(), m.getRam(), m.getBw(), m.getSize(), m.getVmm(), new CloudletSchedulerTimeShared(),
					new HashMap<Pair<String, String>, SelectivityModel>(), m.isClientModule(), m.isGlobalModule());
		}else {
			module = new AppModule(FogUtils.generateEntityId(), m.getName() + "_" + fogId, appId, fogId, 
				m.getMips(), m.getRam(), m.getBw(), m.getSize(), m.getVmm(), new CloudletSchedulerTimeShared(),
				new HashMap<Pair<String, String>, SelectivityModel>(), m.isClientModule(), m.isGlobalModule());
		}
		
		getModules().add(module);
		Logger.debug(getAppId(), "Added module: " + module.getName());
	}

	/**
	 * Adds a non-periodic edge to the application model.
	 * @param source
	 * @param destination
	 * @param tupleCpuLength
	 * @param tupleNwLength
	 * @param tupleType
	 * @param direction
	 * @param edgeType
	 */
	public void addAppEdge(String source, String destination, double tupleCpuLength, double tupleNwLength, String tupleType, int edgeType) {
		AppEdge edge = new AppEdge(source, destination, tupleCpuLength, tupleNwLength,
				tupleType, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	public void addAppEdge(AppEdge e, List<AppModule> globalModules, int fogId) {
		String source = e.getSource() + "_" + fogId;
		String destination = e.getDestination() + "_" + fogId;
		
		if(globalModules != null) {
			for (AppModule gmod : globalModules) {
				if(gmod.getName().equals(e.getSource())) {
					source = e.getSource();
				}
				
				if(gmod.getName().equals(e.getDestination())) {
					destination = e.getDestination();
				}
			}
		}
		
		if(!e.isPeriodic()) {
			addAppEdge(
					source,
					destination,
					e.getTupleCpuLength(),
					e.getTupleNwLength(),
					e.getTupleType() + "_" + fogId,
					e.getEdgeType());
		}else {
			addAppEdge(
					source,
					destination,
					e.getPeriodicity(),
					e.getTupleCpuLength(),
					e.getTupleNwLength(),
					e.getTupleType() + "_" + fogId,
					e.getEdgeType());
		}
		
		Logger.debug(getAppId(), "Added edge from: " + source + " to: " + destination);
	}
	
	/**
	 * Adds a periodic edge to the application model.
	 * @param source
	 * @param destination
	 * @param tupleCpuLength
	 * @param tupleNwLength
	 * @param tupleType
	 * @param direction
	 * @param edgeType
	 */
	public void addAppEdge(String source, String destination, double periodicity, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int edgeType){
		AppEdge edge = new AppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength,
			tupleType, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	/**
	 * Define the input-output relationship of an application module for a given input tuple type.
	 * @param moduleName Name of the module
	 * @param inputTupleType Type of tuples carried by the incoming edge
	 * @param outputTupleType Type of tuples carried by the output edge
	 * @param selectivityModel Selectivity model governing the relation between the incoming and outgoing edge
	 */
	public void addTupleMapping(String moduleName, String inputTupleType, String outputTupleType, SelectivityModel selectivityModel) {
		AppModule module = getModuleByName(moduleName);
		module.getSelectivityMap().put(new Pair<String, String>(inputTupleType, outputTupleType),
				selectivityModel);
	}
	
	public void addTupleMapping(String moduleName, Pair<String, String> pair, double value, int fogId) {
		AppModule module = getModuleByName(moduleName);
		
		// If it is not a global module
		if(module == null) {
			module = getModuleByName(moduleName + "_" + fogId);
		}
		
		Pair<String, String> newPair = new Pair<String, String>(pair.getFirst() + "_" + fogId, pair.getSecond() + "_" + fogId);
		module.getSelectivityMap().put(newPair, new FractionalSelectivity(value));
		
		Logger.debug(getAppId(), "Added tuple mapping on module: " + module.getName() + " from: " + pair.getFirst() + "_" + fogId +
				" to: " + pair.getSecond() + "_" + fogId);
	}
	
	/**
	 * Get a list of all periodic edges in the application.
	 * @param srcModule
	 * @return
	 */
	public List<AppEdge> getPeriodicEdges(String srcModule) {
		List<AppEdge> result = new ArrayList<AppEdge>();
		for(AppEdge edge : edges){
			if(edge.isPeriodic() && edge.getSource().equals(srcModule))
				result.add(edge);
		}
		return result;
	}

	/**
	 * Search and return an application module by its module name
	 * @param name the module name to be returned
	 * @return
	 */
	public AppModule getModuleByName(String name) {
		for(AppModule module : modules) {
			if(module.getName().equals(name))
				return module;
		}
		return null;
	}
	
	/**
	 * Get the tuples generated upon execution of incoming tuple <i>inputTuple</i> by module named <i>moduleName</i>
	 * @param moduleName name of the module performing execution of incoming tuple and emitting resultant tuples
	 * @param inputTuple incoming tuple, whose execution creates resultant tuples
	 * @return
	 */
	public List<Tuple> getResultantTuples(String moduleName, Tuple inputTuple, int sourceModuleId) {
		List<Tuple> tuples = new ArrayList<Tuple>();
		AppModule module = getModuleByName(moduleName);
		
		for(AppEdge edge : getEdges()){
			if(edge.getSource().equals(moduleName)){
				Pair<String, String> pair = new Pair<String, String>(inputTuple.getTupleType(), edge.getTupleType());
				
				if(module.getSelectivityMap().get(pair)==null) continue;
				
				SelectivityModel selectivityModel = module.getSelectivityMap().get(pair);
				if(selectivityModel.canSelect()){
					Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), (long) (edge.getTupleCpuLength()),
							inputTuple.getNumberOfPes(), (long) (edge.getTupleNwLength()), inputTuple.getCloudletOutputSize(),
							inputTuple.getUtilizationModelCpu(), inputTuple.getUtilizationModelRam(), inputTuple.getUtilizationModelBw());
					
					tuple.setActualTupleId(inputTuple.getActualTupleId());
					tuple.setUserId(inputTuple.getUserId());
					tuple.setAppId(inputTuple.getAppId());
					tuple.setDestModuleName(edge.getDestination());
					tuple.setSrcModuleName(edge.getSource());
					tuple.setTupleType(edge.getTupleType());
					tuple.setSourceModuleId(sourceModuleId);
					
					if(edge.getEdgeType() == AppEdge.ACTUATOR)
						tuple.setDirection(Tuple.ACTUATOR);
					
					tuples.add(tuple);
				}
			}
		}
		return tuples;
	}
	
	/**
	 * Create a tuple for a given application edge
	 * @param edge
	 * @param sourceDeviceId
	 * @return
	 */
	public Tuple createTuple(AppEdge edge, int sourceModuleId, int fogId) {
		Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), (long) (edge.getTupleCpuLength()),
				1, (long) (edge.getTupleNwLength()), 100, new UtilizationModelFull(), new UtilizationModelFull(), 
				new UtilizationModelFull());
		
		tuple.setUserId(fogId);
		tuple.setAppId(getAppId());
		tuple.setDestModuleName(edge.getDestination());
		tuple.setSrcModuleName(edge.getSource());
		tuple.setTupleType(edge.getTupleType());
		tuple.setSourceModuleId(sourceModuleId);
		
		if(edge.getEdgeType() == AppEdge.ACTUATOR)
			tuple.setDirection(Tuple.ACTUATOR);
		
		return tuple;
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

	public List<AppLoop> getLoops() {
		return loops;
	}

	public void setLoops(List<AppLoop> loops) {
		this.loops = loops;
	}

	public Map<String, AppEdge> getEdgeMap() {
		return edgeMap;
	}

	public void setEdgeMap(Map<String, AppEdge> edgeMap) {
		this.edgeMap = edgeMap;
	}
	
	@Override
	public String toString() {
		return "Application [appId=" + appId + "]";
	}

}