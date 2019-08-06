package org.fog.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

/**
 * Class representing applications in the Distributed Dataflow Model.
 * 
 * @author Harshit Gupta
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Application {
	/** List of application modules in the application */
	private List<AppModule> modules;
	
	/** List of application edges in the application */
	private List<AppEdge> edges;
	
	/** List of application loops to monitor for delay */
	private List<AppLoop> loops;
	
	/** Id of the application */
	private String appId;
	
	/**
	 * Creates a plain vanilla application with no modules, edges nor loops.
	 * 
	 * @param appId the application id
	 */
	public Application(String appId) {
		setAppId(appId);
		setModules(new ArrayList<AppModule>());
		setEdges(new ArrayList<AppEdge>());
		setLoops(new ArrayList<AppLoop>());
	}
	
	/**
	 * Creates a new application based on the application created in the GUI.
	 * 
	 * @param applicationGui the GUI application
	 */
	public Application(ApplicationGui applicationGui) {
		setAppId(applicationGui.getAppId());
		setModules(applicationGui.getModules());
		setEdges(applicationGui.getEdges());
		
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
	 * 
	 * @param moduleName the module name
	 * @param ram the ram size (kB) needed to run this module
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
	
	/**
	 * Adds an application module to the application.
	 * 
	 * @param m the application module
	 * @param nodeId the node id which needs to deploy the module
	 */
	public void addAppModule(AppModule m, int nodeId) {
		AppModule module = null;
		
		if(m.isClientModule() && m.isGlobalModule())
			FogComputingSim.err("Modules cannot be simultaneously client module and global module");
		
		if(m.isGlobalModule()) {
			module = new AppModule(FogUtils.generateEntityId(), m.getName(), appId, nodeId, 
					m.getMips(), m.getRam(), m.getBw(), m.getSize(), m.getVmm(), new CloudletSchedulerTimeShared(),
					new HashMap<Pair<String, String>, SelectivityModel>(), m.isClientModule(), m.isGlobalModule());
		}else {
			module = new AppModule(FogUtils.generateEntityId(), m.getName() + "_" + nodeId, appId, nodeId, 
				m.getMips(), m.getRam(), m.getBw(), m.getSize(), m.getVmm(), new CloudletSchedulerTimeShared(),
				new HashMap<Pair<String, String>, SelectivityModel>(), m.isClientModule(), m.isGlobalModule());
		}
		
		getModules().add(module);
		Logger.debug(getAppId(), "Added module: " + module.getName());
	}

	/**
	 * Adds a non-periodic edge to the application model.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in kilobytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 */
	public void addAppEdge(String source, String destination, double tupleCpuLength, double tupleNwLength, String tupleType, int edgeType) {
		AppEdge edge = new AppEdge(source, destination, tupleCpuLength, tupleNwLength, tupleType, edgeType);
		getEdges().add(edge);
	}
	
	/**
	 * Adds an edge to the application model.
	 * 
	 * @param e the application edge
	 * @param globalModules the list containing all global modules
	 * @param nodeId the node id which needs to deploy the module
	 */
	public void addAppEdge(AppEdge e, List<AppModule> globalModules, int nodeId) {
		String source = e.getSource() + "_" + nodeId;
		String destination = e.getDestination() + "_" + nodeId;
		
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
			addAppEdge(source, destination, e.getTupleCpuLength(), e.getTupleNwLength(), e.getTupleType() + "_" + nodeId, e.getEdgeType());
		}else {
			addAppEdge(source, destination, e.getPeriodicity(), e.getTupleCpuLength(), e.getTupleNwLength(), e.getTupleType() + "_" + nodeId, e.getEdgeType());
		}
	}

	/**
	 * Adds a periodic edge to the application model.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param periodicity the periodicity of the application edge
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in kilobytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 * 
	 */
	public void addAppEdge(String source, String destination, double periodicity, double tupleCpuLength, double tupleNwLength, String tupleType, int edgeType){
		AppEdge edge = new AppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength, tupleType, edgeType);
		getEdges().add(edge);
	}
	
	/**
	 * Defines the input-output relationship of an application module for a given input tuple type.
	 * 
	 * @param moduleName the name of the module
	 * @param inputTupleType the type of tuples carried by the incoming edge
	 * @param outputTupleType the type of tuples carried by the output edge
	 * @param selectivityModel the selectivity model governing the relation between the incoming and outgoing edge
	 */
	public void addTupleMapping(String moduleName, String inputTupleType, String outputTupleType, SelectivityModel selectivityModel) {
		AppModule module = getModuleByName(moduleName);
		module.getSelectivityMap().put(new Pair<String, String>(inputTupleType, outputTupleType), selectivityModel);
	}
	
	/**
	 * Defines the input-output relationship of an application module for a given input tuple type.
	 * 
	 * @param moduleName the name of the module
	 * @param pair the pair which contains both the input and output tuple type
	 * @param value the selectivity value governing the relation between the incoming and outgoing edge
	 * @param nodeId the node id which needs to deploy the module
	 */
	public void addTupleMapping(String moduleName, Pair<String, String> pair, double value, int nodeId) {
		AppModule module = getModuleByName(moduleName);
		
		// If it is not a global module
		if(module == null) {
			module = getModuleByName(moduleName + "_" + nodeId);
		}
		
		Pair<String, String> newPair = new Pair<String, String>(pair.getFirst() + "_" + nodeId, pair.getSecond() + "_" + nodeId);
		module.getSelectivityMap().put(newPair, new FractionalSelectivity(value));
		
		Logger.debug(getAppId(), "Added tuple mapping on module: " + module.getName() + " from: " + pair.getFirst() + "_" + nodeId +
				" to: " + pair.getSecond() + "_" + nodeId);
	}
	
	/**
	 * Gets the list of all periodic edges in the application with a given source module.
	 * 
	 * @param srcModule the source module
	 * @return the list of all periodic edges in the application
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
	 * Searches and returns the application module by its module name.
	 * 
	 * @param name the module name to be returned
	 * @return the application module
	 */
	public AppModule getModuleByName(String name) {
		for(AppModule module : modules) {
			if(module.getName().equals(name))
				return module;
		}
		return null;
	}
	
	/**
	 * Gets the tuples generated upon execution of incoming tuple by module named.
	 * 
	 * @param moduleName name of the module performing execution of incoming tuple and emitting resultant tuples
	 * @param inputTuple incoming tuple, whose execution creates resultant tuples
	 * @return the tuples generated upon execution of incoming tuple by module named
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
					
					if(edge.getEdgeType() == AppEdge.ACTUATOR)
						tuple.setDirection(Tuple.ACTUATOR);
					
					tuples.add(tuple);
				}
			}
		}
		return tuples;
	}
	
	/**
	 * Creates a tuple for a given application edge.
	 * 
	 * @param edge the application edge
	 * @param nodeId the ID of the owner of the destiny application module
	 * @return the tuple for a given application edge
	 */
	public Tuple createTuple(AppEdge edge, int nodeId) {
		Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), (long) (edge.getTupleCpuLength()), 1, (long) (edge.getTupleNwLength()),
				100, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
		
		tuple.setUserId(nodeId);
		tuple.setAppId(getAppId());
		tuple.setDestModuleName(edge.getDestination());
		tuple.setSrcModuleName(edge.getSource());
		tuple.setTupleType(edge.getTupleType());
		
		if(edge.getEdgeType() == AppEdge.ACTUATOR)
			tuple.setDirection(Tuple.ACTUATOR);
		
		return tuple;
	}
	
	/**
	 * Gets the application id.
	 * 
	 * @return the application id
	 */
	public String getAppId() {
		return appId;
	}
	
	/**
	 * Sets the application id.
	 * 
	 * @param appId the application id
	 */
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	/**
	 * Gets the list of application modules in the application.
	 * 
	 * @return the list of application modules in the application
	 */
	public List<AppModule> getModules() {
		return modules;
	}
	
	/**
	 * Sets the list of application modules in the application.
	 * 
	 * @param modules the list of application modules in the application
	 */
	public void setModules(List<AppModule> modules) {
		this.modules = modules;
	}
	
	/**
	 * Gets the list of application edges in the application.
	 * 
	 * @return the list of application edges in the application
	 */
	public List<AppEdge> getEdges() {
		return edges;
	}
	
	/**
	 * Sets the list of application edges in the application.
	 * 
	 * @param edges the list of application edges in the application
	 */
	public void setEdges(List<AppEdge> edges) {
		this.edges = edges;
	}

	/**
	 * Gets the list of application loops to monitor for delay.
	 * 
	 * @return the list of application loops to monitor for delay
	 */
	public List<AppLoop> getLoops() {
		return loops;
	}

	/**
	 * Sets the list of application loops to monitor for delay.
	 * 
	 * @param loops the list of application loops to monitor for delay
	 */
	public void setLoops(List<AppLoop> loops) {
		this.loops = loops;
	}
	
	@Override
	public String toString() {
		return "Application [appId=" + appId + "]";
	}

}