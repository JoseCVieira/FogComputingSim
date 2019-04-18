package org.fog.placement.algorithms.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Pe;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.routing.DijkstraAlgorithm;
import org.fog.placement.algorithms.routing.Edge;
import org.fog.placement.algorithms.routing.Graph;
import org.fog.placement.algorithms.routing.Vertex;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

public abstract class Algorithm {
	protected static final boolean PRINT_DETAILS = true;
	
	protected final int NR_NODES;
	protected final int NR_MODULES;
	
	protected double fMipsPrice[];
	protected double fRamPrice[];
	protected double fMemPrice[];
	protected double fBwPrice[];
	
	protected int fId[];
	protected String fName[];
	protected double fMips[];
	protected double fRam[];
	protected double fMem[];
	protected double fBw[];
	protected double fBusyPw[];
	protected double fIdlePw[];
	
	protected String mName[];
	protected double mMips[];
	protected double mRam[];
	protected double mMem[];
	protected double mBw[];
	protected double mCpuSize[];
	
	// Node to Node
	protected double[][] latencyMap;
	private double[][] bwCapacityMap;
	protected double[][][] bandwidthMap;
	
	// Node to Module
	protected double[][] mandatoryMap;
	
	// Module to Module
	protected double[][] dependencyMap;
	protected double[][] bwMap;
	
	protected Map<Map<Integer, Integer>, LinkedList<Vertex>> routingPaths;
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) throws IllegalArgumentException {
		
		if(applications == null || sensors == null || actuators == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		NR_NODES = fogDevices.size() + sensors.size() + actuators.size();
		fId = new int[NR_NODES];
		fName = new String[NR_NODES];
		fMips = new double[NR_NODES];
		fRam = new double[NR_NODES];
		fMem = new double[NR_NODES];
		fBw = new double[NR_NODES];
		fBusyPw = new double[NR_NODES];
		fIdlePw = new double[NR_NODES];
		fMipsPrice = new double[NR_NODES];
		fRamPrice = new double[NR_NODES];
		fMemPrice = new double[NR_NODES];
		fBwPrice = new double[NR_NODES];
		
		LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
		for(Application application : applications) {
			for(AppModule module : application.getModules())
				hashSet.add(module.getName());
			
			for(AppEdge appEdge : application.getEdges()) {
				hashSet.add(appEdge.getSource());
				hashSet.add(appEdge.getDestination());
			}
		}
		
		NR_MODULES = hashSet.size();
		mName = new String[NR_MODULES];
		mMips = new double[NR_MODULES];
		mRam = new double[NR_MODULES];
		mMem = new double[NR_MODULES];
		mBw = new double[NR_MODULES];
		mCpuSize = new double[NR_MODULES];
				
		latencyMap = new double[NR_NODES][NR_NODES];
		bwCapacityMap = new double[NR_NODES][NR_NODES];
		bandwidthMap = new double[NR_NODES-1][NR_NODES][NR_NODES];
		
		mandatoryMap = new double[NR_NODES][NR_MODULES];
		
		dependencyMap = new double[NR_MODULES][NR_MODULES];
		bwMap = new double[NR_MODULES][NR_MODULES];
		
		for (int i = 0; i < NR_NODES - 1; i++)
			for (int j = 0; j < NR_NODES; j++)
				for (int z = 0; z < NR_NODES; z++)
					bandwidthMap[i][j][z] = Double.MAX_VALUE;
		
		routingPaths = new HashMap<Map<Integer,Integer>, LinkedList<Vertex>>();
		
		extractDevicesCharacteristics(fogDevices, sensors, actuators);
		extractAppCharacteristics(applications, sensors, actuators);
		computeApplicationCharacteristics(applications, sensors);
		computeLatencyMap(fogDevices, sensors, actuators);
		computeDependencyMap(applications);
		
		if(PRINT_DETAILS)
			AlgorithmUtils.printDetails(this, fogDevices, applications, sensors, actuators);
		
		System.exit(0);
	}
	
	private void extractDevicesCharacteristics (final List<FogDevice> fogDevices,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		int i = 0;
		for(FogDevice fogDevice : fogDevices) {
			FogDeviceCharacteristics characteristics =
					(FogDeviceCharacteristics) fogDevice.getCharacteristics();
			
			double totalMips = 0;
			for(Pe pe : fogDevice.getHost().getPeList())
				totalMips += pe.getMips();
			
			fId[i] = fogDevice.getId();
			fName[i] = fogDevice.getName();
			fMips[i] = totalMips;
			fRam[i] = fogDevice.getHost().getRam();
			fMem[i] = fogDevice.getHost().getStorage();
			fBw[i] = fogDevice.getHost().getBw();
			fBusyPw[i] = ((FogLinearPowerModel) fogDevice.getHost().getPowerModel()).getBusyPower();
			fIdlePw[i] = ((FogLinearPowerModel) fogDevice.getHost().getPowerModel()).getStaticPower();
			fMipsPrice[i] = characteristics.getCostPerMips();
			fRamPrice[i] = characteristics.getCostPerMem();
			fMemPrice[i] = characteristics.getCostPerStorage();
			fBwPrice[i++] = characteristics.getCostPerBw();
		}
		
		// sensors and actuators are added to compute tuples latency
		for(Sensor sensor : sensors) {
			fId[i] = sensor.getId();
			fName[i] = sensor.getName();
			fMips[i++] = 1.0; // its value is irrelevant but needs to be different from 0
		}
		
		for(Actuator actuator : actuators) {
			fId[i] = actuator.getId();
			fName[i] = actuator.getName();
			fMips[i++] = 1.0; // its value is irrelevant but needs to be different from 0
		}
	}
	
	private void extractAppCharacteristics(final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		int i = 0;
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				mName[i] = module.getName();
				mMips[i] = module.getMips();
				mRam[i] = module.getRam();
				mMem[i] = module.getSize();
				mBw[i++] = module.getBw();
			}
			
			// sensors and actuators are added to compute tuples latency
			for(AppEdge appEdge : application.getEdges()) {
				if(getModuleIndexByModuleName(appEdge.getSource()) == -1) {
					for(Sensor sensor : sensors)
						if(sensor.getAppId().equals(application.getAppId()))
							mandatoryMap[getNodeIndexByNodeName(sensor.getName())][i] = 1;
					
					mName[i++] = appEdge.getSource();
				}
				
				if(getModuleIndexByModuleName(appEdge.getDestination()) == -1) {
					for(Actuator actuator : actuators)
						if(actuator.getAppId().equals(application.getAppId()))
							mandatoryMap[getNodeIndexByNodeName(actuator.getName())][i] = 1;
					
					mName[i++] = appEdge.getDestination();
				}
				
				int index = getModuleIndexByModuleName(appEdge.getDestination());
				mCpuSize[index] += appEdge.getTupleCpuLength();
			}
		}
	}
	
	private void computeApplicationCharacteristics(final List<Application> applications, final List<Sensor> sensors) {
		Sensor sensor = null;		
		for(Application application : applications) {
			for(Sensor s : sensors)
				if(s.getAppId().equals(application.getAppId()))
					sensor = s;
		
			TreeMap<String, Double> producers = new TreeMap<String, Double>();
	
			Distribution distribution = sensor.getTransmitDistribution();
			double avg = 0.0;
			if(distribution.getDistributionType() == Distribution.DETERMINISTIC)
				avg = ((DeterministicDistribution)distribution).getValue();
			else if(distribution.getDistributionType() == Distribution.NORMAL)
				avg = ((NormalDistribution)distribution).getMean() - 3*((NormalDistribution)distribution).getStdDev();
			else
				avg = ((UniformDistribution)distribution).getMin();
			
			producers.put(sensor.getTupleType(), avg);
			
			for(AppEdge appEdge : application.getEdges())
				if(appEdge.isPeriodic())
					producers.put(appEdge.getTupleType(), appEdge.getPeriodicity());
			
			int nrEdges = application.getEdges().size();
			int processed = 0;
			double interval = 0;
			AppModule module = null;
			String toProcess = "";
			boolean found = false;
			
			while(processed < nrEdges) {
				if(toProcess.isEmpty() || !found) {
					Entry<String, Double> entry = producers.pollFirstEntry();
					toProcess = entry.getKey();
					interval = entry.getValue();
				}else
					found = false;
				
				for(AppEdge appEdge : application.getEdges()) {
					if(appEdge.getTupleType().equals(toProcess)) {
						// BW and MIPS to sensors and actuators are not used
						for(AppModule appModule : application.getModules()) {
							if(appModule.getName().equals(appEdge.getDestination())) {
								int edgeSourceIndex = getModuleIndexByModuleName(appEdge.getSource());
								int edgeDestIndex = getModuleIndexByModuleName(appEdge.getDestination());
								
								appModule.setMips(appModule.getMips() + appEdge.getTupleCpuLength()/interval);
								mMips[edgeDestIndex] += appEdge.getTupleCpuLength()/interval;
								
								if(!isSensorTuple(sensors, appEdge.getTupleType())) {
									appModule.setBw((long) (appModule.getBw() + appEdge.getTupleNwLength()/interval));
									bwMap[edgeSourceIndex][edgeDestIndex] += appEdge.getTupleNwLength()/interval;
									mBw[edgeSourceIndex] += appEdge.getTupleNwLength()/interval;
								}
								
								module = appModule;
								break;
							}
						}
						
						processed++;
						break;
					}
				}
				
				for(Pair<String, String> pair : module.getSelectivityMap().keySet()) {
					if(pair.getFirst().equals(toProcess)) {
						FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)module.getSelectivityMap().get(pair));
						found = true;
						toProcess = pair.getSecond();
						interval *= fractionalSelectivity.getSelectivity();
						break;
					}
				}
			}
		}
	}
	
	private void computeLatencyMap(final List<FogDevice> fogDevices,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		Map<Integer, Vertex> mapNodes = new HashMap<Integer, Vertex>();
		Map<Map<Integer, Integer>, Double> bwMap = new HashMap<Map<Integer,Integer>, Double>();
		List<Edge> edges = new ArrayList<Edge>();
		
		for(int i = 0; i < NR_NODES; i++)
			mapNodes.put(fId[i], new Vertex(Integer.toString(fId[i])));
		
		for(FogDevice fogDevice : fogDevices) {
			int dId = fogDevice.getId();
			
			for(int neighborId : fogDevice.getNeighborsIds()) {
				int lat = fogDevice.getLatencyMap().get(neighborId).intValue();
				double bw = fogDevice.getBandwidthMap().get(neighborId);
				
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(neighborId), lat));
				
				Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
				connection.put(dId, neighborId);
				bwMap.put(connection, bw);
				
				getBwCapacityMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(neighborId)] = bw;
			}
			
			getBwCapacityMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(dId)] = Double.MAX_VALUE;
		}
		
		
		for(Sensor sensor : sensors) {
			int id1 = sensor.getId();
			int id2 = sensor.getGatewayDeviceId();
			double lat = sensor.getLatency();
			
			edges.add(new Edge(mapNodes.get(id1), mapNodes.get(id2), lat));
			edges.add(new Edge(mapNodes.get(id2), mapNodes.get(id1), lat));
			
			getBwCapacityMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = Double.MAX_VALUE; //sensor and act only have latency
			getBwCapacityMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			getBwCapacityMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
		}
		
		for(Actuator actuator : actuators) {
			int id1 = actuator.getId();
			int id2 = actuator.getGatewayDeviceId();
			double lat = actuator.getLatency();
			
			edges.add(new Edge(mapNodes.get(id1), mapNodes.get(id2), lat));
			edges.add(new Edge(mapNodes.get(id2), mapNodes.get(id1), lat));
			
			getBwCapacityMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = Double.MAX_VALUE;
			getBwCapacityMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			getBwCapacityMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
		}

		Graph graph = new Graph(new ArrayList<Vertex>(mapNodes.values()), edges);
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
		
		for(Vertex v1 : dijkstra.getNodes()) {
			int row = getNodeIndexByNodeId(Integer.parseInt(v1.getName()));
			
			for(Vertex v2 : dijkstra.getNodes()) {
				int col = getNodeIndexByNodeId(Integer.parseInt(v2.getName()));
				
				dijkstra.execute(v1);
				LinkedList<Vertex> path = dijkstra.getPath(v2);
				
				Map<Integer, Integer> connection = new HashMap<Integer, Integer>();
				connection.put(row, col);
				routingPaths.put(connection, path);
				
				double latency = 0;
				if(path != null) {
					for(int iter = 0; iter < path.size() - 1; iter++) {
						double bandwidth = Double.MAX_VALUE;
						
						for(Edge edge : edges) {
							if(edge.getSource().getName().equals(path.get(iter).getName()) &&
								edge.getDestination().getName().equals(path.get(iter+1).getName())) {
								
								int sourceId = Integer.parseInt(edge.getSource().getName());
								int destinationId = Integer.parseInt(edge.getDestination().getName());
								
								connection = new HashMap<Integer, Integer>();
								connection.put(sourceId, destinationId);
								
								try {
									bandwidth = bwMap.get(connection);
								} catch (Exception e) {
									bandwidth = Double.MAX_VALUE; // connection so a sensor or actuator only has latency
								}
								
								latency += edge.getWeight();
								break;
							}
						}
					
						bandwidthMap[iter][row][col] = bandwidth;
					}
				}					
				
				int c = getNodeIndexByNodeId(Integer.parseInt(v1.getName()));
				int r = getNodeIndexByNodeId(Integer.parseInt(v2.getName()));
				latencyMap[c][r] = latency;
			}
		}
	}
	
	private void computeDependencyMap(final List<Application> applications) {
		for(Application application : applications) {
			for(AppEdge appEdge : application.getEdges()) {
				int col = getModuleIndexByModuleName(appEdge.getSource());
				int row = getModuleIndexByModuleName(appEdge.getDestination());
				dependencyMap[col][row] += 1;
			}
		}
	}
	
	public abstract Map<String, List<String>> execute();
	
	public Map<Map<String, String>, Integer> extractRoutingMap(final Map<String, List<String>> moduleToNodeMap,
			final List<FogDevice> fogDevices, final List<Sensor> sensors, final List<Actuator> actuators) {
		Map<Map<String, String>, Integer> parsedPaths = new HashMap<Map<String,String>, Integer>();
		int destinationModuleIndex = -1, sourceNodeIndex = -1, destinationNodeIndex = -1;

		for(String node : moduleToNodeMap.keySet()) {
			for(String module : moduleToNodeMap.get(node)) {
				destinationModuleIndex = getModuleIndexByModuleName(module);
				destinationNodeIndex = getNodeIndexByNodeName(node);
				
				for(int i = 0; i < NR_MODULES; i++) {
					if(dependencyMap[i][destinationModuleIndex] > 0) {
						
						for(String nodeName : moduleToNodeMap.keySet())
							if(moduleToNodeMap.get(nodeName).contains(mName[i]))
								sourceNodeIndex = getNodeIndexByNodeName(nodeName);
						
						Map<Integer, Integer> connection1 = new HashMap<Integer, Integer>();
						connection1.put(sourceNodeIndex, destinationNodeIndex);
						
						List<Vertex> path = routingPaths.get(connection1);
						if(path == null) // both modules are within the same node
							continue;
						
						int iter = 0;
						for(Vertex v : path) {
							if(iter >= path.size()-1)
								break;
							
							Map<String, String> connection2 = new HashMap<String, String>();
							String nodeName = "";
							
							for(FogDevice fogDevice : fogDevices) {
								if(Integer.parseInt(v.getName()) == fogDevice.getId()) {
									nodeName = fogDevice.getName();
									break;
								}
							}
							
							if(nodeName == "") {
								for(Sensor sensor : sensors) {
									if(Integer.parseInt(v.getName()) == sensor.getId()) {
										nodeName = sensor.getName();
										break;
									}
								}
							}
							
							if(nodeName == "") {
								for(Actuator actuator : actuators) {
									if(Integer.parseInt(v.getName()) == actuator.getId()) {
										nodeName = actuator.getName();
										break;
									}
								}
							}
							
							connection2.put(nodeName, module);
							parsedPaths.put(connection2, Integer.parseInt(routingPaths.get(connection1).get(iter+1).getName()));
							iter++;
						}						
					}
				}
			}
		}
		
		return parsedPaths;
	}
	
	private int getModuleIndexByModuleName(String name) {
		for(int i = 0; i < NR_MODULES; i++)
			if(mName[i] != null && mName[i].equals(name))
				return i;
		return -1;
	}
	
	private int getNodeIndexByNodeName(String name) {
		for(int i = 0; i < NR_NODES; i++)
			if(fName[i] != null && fName[i].equals(name))
				return i;
		return -1;
	}
	
	private int getNodeIndexByNodeId(int id) {
		for(int i = 0; i < NR_NODES; i++)
			if(fId[i] == id)
				return i;
		return -1;
	}
	
	private boolean isSensorTuple(List<Sensor> sensors, String tupleType) {
		for(Sensor sensor : sensors)
			if(sensor.getTupleType().equals(tupleType))
				return true;
		return false;
	}
	
	public int moduleHasMandatoryPositioning(int col) {
		for(int i = 0; i < NR_NODES; i++)
			if(mandatoryMap[i][col] == 1)
				return i;
		return -1;
	}

	public double[] getfMipsPrice() {
		return fMipsPrice;
	}

	public double[] getfRamPrice() {
		return fRamPrice;
	}

	public double[] getfMemPrice() {
		return fMemPrice;
	}

	public double[] getfBwPrice() {
		return fBwPrice;
	}
	
	public int[] getfId() {
		return fId;
	}

	public String[] getfName() {
		return fName;
	}

	public double[] getfMips() {
		return fMips;
	}

	public double[] getfRam() {
		return fRam;
	}

	public double[] getfMem() {
		return fMem;
	}

	public double[] getfBw() {
		return fBw;
	}

	public String[] getmName() {
		return mName;
	}

	public double[] getmMips() {
		return mMips;
	}

	public double[] getmRam() {
		return mRam;
	}

	public double[] getmMem() {
		return mMem;
	}

	public double[] getmBw() {
		return mBw;
	}
	
	public double[] getmCpuSize(){
		return mCpuSize;
	}

	public double[] getfBusyPw() {
		return fBusyPw;
	}
	
	public double[] getfIdlePw() {
		return fIdlePw;
	}
	
	public double[][] getLatencyMap() {
		return latencyMap;
	}
	
	public double[][] getDependencyMap() {
		return dependencyMap;
	}
	
	public double[][] getMandatoryMap() {
		return mandatoryMap;
	}
	
	public double[][] getBandwidthMap(int iter){
		return bandwidthMap[iter];
	}
	
	public double[][] getBwMap(){
		return bwMap;
	}

	public double[][] getBwCapacityMap() {
		return bwCapacityMap;
	}
	
}
