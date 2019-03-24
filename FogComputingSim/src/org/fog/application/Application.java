package org.fog.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Tuple;
import org.fog.scheduler.TupleScheduler;
import org.fog.utils.FogUtils;
import org.fog.utils.dijkstra.DijkstraAlgorithm;

// Class represents an application in the Distributed Dataflow Model.
public class Application {
	
	private DijkstraAlgorithm dijkstraAlgorithm;
	private Map<String, AppEdge> edgeMap;
	private List<AppModule> modules; //List of application modules in the application
	private List<AppEdge> edges; //List of application edges in the application
	private List<AppLoop> loops; // List of application loops to monitor for delay
	private String appId;
	private int userId;
	
	public Application(String appId, int userId) {
		setAppId(appId + "_" + userId);
		setUserId(userId);
		setModules(new ArrayList<AppModule>());
		setEdges(new ArrayList<AppEdge>());
		setLoops(new ArrayList<AppLoop>());
		setEdgeMap(new HashMap<String, AppEdge>());
	}
	
	/**
	 * Adds an application module to the application.
	 * @param moduleName
	 * @param ram
	 */
	public void addAppModule(String moduleName, int ram){
		int mips = 1000;
		long size = 10000;
		long bw = 1000;
		String vmm = "Xen";
		
		AppModule module = new AppModule(FogUtils.generateEntityId(), moduleName, appId, userId, 
			mips, ram, bw, size, vmm, new TupleScheduler(mips, 1),
			new HashMap<Pair<String, String>, SelectivityModel>());
		
		getModules().add(module);
	}
	
	public void addAppModule(AppModule m){ //ADDED
		AppModule module = new AppModule(FogUtils.generateEntityId(), m.getName() + "_" + userId, appId, userId, 
			m.getMips(), m.getRam(), m.getBw(), m.getSize(), m.getVmm(), new TupleScheduler(m.getMips(), 1),
			new HashMap<Pair<String, String>, SelectivityModel>());
		getModules().add(module);
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
	public void addAppEdge(String source, String destination, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int direction, int edgeType){
		AppEdge edge = new AppEdge(source, destination, tupleCpuLength, tupleNwLength,
				tupleType, direction, edgeType);
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
	}
	
	public void addAppEdge(AppEdge e){ //ADDED
		AppEdge edge = null;
		
		if(!e.isPeriodic())
			edge = new AppEdge(e.getSource() + "_" + userId, e.getDestination() + "_" + userId, e.getTupleCpuLength(),
				e.getTupleNwLength(), e.getTupleType() + "_" + userId, e.getDirection(), e.getEdgeType());
		else
			edge = new AppEdge(e.getSource() + "_" + userId, e.getDestination() + "_" + userId, e.getPeriodicity(),
					e.getTupleCpuLength(), e.getTupleNwLength(), e.getTupleType() + "_" + userId,
					e.getDirection(), e.getEdgeType());
		
		getEdges().add(edge);
		getEdgeMap().put(edge.getTupleType(), edge);
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
			double tupleNwLength, String tupleType, int direction, int edgeType){
		AppEdge edge = new AppEdge(source, destination, periodicity, tupleCpuLength, tupleNwLength,
			tupleType, direction, edgeType);
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
	public void addTupleMapping(String moduleName, String inputTupleType,
			String outputTupleType, SelectivityModel selectivityModel){
		AppModule module = getModuleByName(moduleName);
		module.getSelectivityMap().put(new Pair<String, String>(inputTupleType, outputTupleType),
				selectivityModel);
	}
	
	public void addTupleMapping(String moduleName, Pair<String, String> pair, double value){  //ADDED
		AppModule module = getModuleByName(moduleName + "_" + userId);
		Pair<String, String> newPair = new Pair<String, String>(pair.getFirst() + "_" + userId, pair.getSecond() + "_" + userId);
		module.getSelectivityMap().put(newPair, new FractionalSelectivity(value));
	}
	
	/**
	 * Get a list of all periodic edges in the application.
	 * @param srcModule
	 * @return
	 */
	public List<AppEdge> getPeriodicEdges(String srcModule){
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
	public AppModule getModuleByName(String name){
		for(AppModule module : modules){
			if(module.getName().equals(name))
				return module;
		}
		return null;
	}
	
	/**
	 * Get the tuples generated upon execution of incoming tuple <i>inputTuple</i> by module named <i>moduleName</i>
	 * @param moduleName name of the module performing execution of incoming tuple and emitting resultant tuples
	 * @param inputTuple incoming tuple, whose execution creates resultant tuples
	 * @param sourceDeviceId
	 * @return
	 */
	public List<Tuple> getResultantTuples(String moduleName, Tuple inputTuple, int sourceDeviceId, int sourceModuleId){
		List<Tuple> tuples = new ArrayList<Tuple>();
		AppModule module = getModuleByName(moduleName);
		
		for(AppEdge edge : getEdges()){
			if(edge.getSource().equals(moduleName)){
				Pair<String, String> pair = new Pair<String, String>(inputTuple.getTupleType(), edge.getTupleType());
				
				if(module.getSelectivityMap().get(pair)==null) continue;
				
				SelectivityModel selectivityModel = module.getSelectivityMap().get(pair);
				if(selectivityModel.canSelect()){
					Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),
							(long) (edge.getTupleCpuLength()),
							inputTuple.getNumberOfPes(),
							(long) (edge.getTupleNwLength()),
							inputTuple.getCloudletOutputSize(),
							inputTuple.getUtilizationModelCpu(),
							inputTuple.getUtilizationModelRam(),
							inputTuple.getUtilizationModelBw()
							);
					tuple.setActualTupleId(inputTuple.getActualTupleId());
					tuple.setUserId(inputTuple.getUserId());
					tuple.setAppId(inputTuple.getAppId());
					tuple.setDestModuleName(edge.getDestination());
					tuple.setSrcModuleName(edge.getSource());
					tuple.setTupleType(edge.getTupleType());
					tuple.setSourceModuleId(sourceModuleId);
					
					if(edge.getEdgeType() == AppEdge.ACTUATOR)
						tuple.setDirection(Tuple.ACTUATOR);
					else
						tuple.setDirection(edge.getDirection());
					
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
	public Tuple createTuple(AppEdge edge, int sourceModuleId){
		Tuple tuple = new Tuple(appId, FogUtils.generateTupleId(), edge.getDirection(),
					(long) (edge.getTupleCpuLength()),
					1,
					(long) (edge.getTupleNwLength()),
					100,
					new UtilizationModelFull(), 
					new UtilizationModelFull(), 
					new UtilizationModelFull()
					);
			
			tuple.setUserId(getUserId());
			tuple.setAppId(getAppId());
			tuple.setDestModuleName(edge.getDestination());
			tuple.setSrcModuleName(edge.getSource());
			tuple.setTupleType(edge.getTupleType());
			tuple.setSourceModuleId(sourceModuleId);
		
		if(edge.getEdgeType() == AppEdge.ACTUATOR)
			tuple.setDirection(Tuple.ACTUATOR);
		else
			tuple.setDirection(edge.getDirection());
		
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

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public Map<String, AppEdge> getEdgeMap() {
		return edgeMap;
	}

	public void setEdgeMap(Map<String, AppEdge> edgeMap) {
		this.edgeMap = edgeMap;
	}
	
	public DijkstraAlgorithm getDijkstraAlgorithm() {
		return dijkstraAlgorithm;
	}

	public void setDijkstraAlgorithm(DijkstraAlgorithm dijkstraAlgorithm) {
		this.dijkstraAlgorithm = dijkstraAlgorithm;
	}
	
	@Override
	public String toString() {
		return "Application [appId=" + appId + ", userId=" + userId + "]";
	}
}
