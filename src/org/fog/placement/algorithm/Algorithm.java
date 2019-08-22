package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.util.AlgorithmUtils;
import org.fog.placement.algorithm.util.routing.DijkstraAlgorithm;
import org.fog.placement.algorithm.util.routing.Edge;
import org.fog.placement.algorithm.util.routing.Graph;
import org.fog.placement.algorithm.util.routing.Vertex;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.Location;
import org.fog.utils.MobileBandwidthModel;
import org.fog.utils.MobilePathLossModel;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

/**
 * Class which is responsible to parse and hold all the information needed to run any optimization algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public abstract class Algorithm {
	private static final int START = 0;
	private static final int FINAL = 1;
	
	/** Number of fog devices within the fog network */
	private final int NR_NODES;
	
	/** Number of application modules within the fog network */
	private final int NR_MODULES;
	
	// Node Prices --------------------------------------------
	
	/** Vector holding the price of using processing resources in each fog device */
	private double fMipsPrice[];
	
	/** Vector holding the price of using memory resources in each fog device */
	private double fRamPrice[];
	
	/** Vector holding the price of using storage resources in each fog device */
	private double fStrgPrice[];
	
	/** Vector holding the price of using network resources in each fog device */
	private double fBwPrice[];
	
	/** Vector holding the price of energy consumption in each fog device */
	private double fEnPrice[];
	
	// Node characteristics -----------------------------------
	
	/** Vector holding the id of each fog device */
	private int fId[];
	
	/** Vector holding the name of each fog device */
	private String fName[];
	
	/** Vector holding the quantity of processing resources in each fog device */
	private double fMips[];
	
	/** Vector holding the quantity of memory resources in each fog device */
	private double fRam[];
	
	/** Vector holding the quantity of storage resources in each fog device */
	private double fStrg[];
	
	/** Vector holding the power consumption while using the full processing capacity in each fog device */
	private double fBusyPw[];
	
	/** Vector holding the power consumption while using no processing resources in each fog device */
	private double fIdlePw[];
	
	/** Vector holding the power consumption while sending data to another device in each fog device */
	private double fTxPw[];
	
	// Module -------------------------------------------------
	
	/** Vector holding the name of each application module */
	private String mName[];
	
	/** Vector holding the quantity of processing resources needed in each application module */
	private double mMips[];
	
	/** Vector holding the quantity of memory resources needed in each application module */
	private double mRam[];
	
	/** Vector holding the quantity of storage resources needed in each application module */
	private double mStrg[];
	
	// Node to Node -------------------------------------------
	
	/** Matrix holding the link latency between each two fog nodes */
	private double[][] fLatencyMap;
	
	/** Matrix holding the link bandwidth available between each two fog nodes */
	private double[][] fBandwidthMap;
	
	// Module to Module ---------------------------------------
	/** Matrix holding the dependencies index (source and destination) between each pair of modules */
	private int[][] dependenciesIndex;
	
	/** Matrix holding the average dependencies (dependency = probability / periodicity) between each two modules */
	private double[][] mDependencyMap;
	
	/** Matrix holding the average bandwidth (bandwidth  = probability * network size / periodicity) needed between each two modules */
	private double[][] mBandwidthMap;
	
	/** Matrix holding the tuple network sizes between each two modules */
	private double[][] mNWMap;
	
	/** Matrix holding the tuple CPU sizes between each two modules */
	private double[][] mCPUMap;
	
	// Node to Module -----------------------------------------
	
	/** Matrix holding the possible position of each module (e.g., if is a GUI, it makes sense to only run inside the client node) */
	private double[][] possibleDeployment;
	
	/** Matrix holding the current module placement map (this is used to reconfigure/migrate modules through the network) */
	private double[][] currentPlacement;
	
	// Loops --------------------------------------------------
	
	/**
	 *  Matrix holding the loops. Each loop measures the total latency between one node to another, including:
	 *  - Processing latency of the loop
	 *  - Tuple/dependency transmission between modules within the loop
	 *  - Migration of modules within the loop
	 */
	private int[][] loops;
	
	/** Vector holding the deadline of each loop */
	private double[] loopsDeadline;
	
	/** List with the nodes for the execution of the Dijkstra Algorithm */
	private List<Vertex> dijkstraNodes;
	
	/** Object responsible for running the Dijkstra Algorithm */
	private DijkstraAlgorithm dijkstra;
	
	// Algorithm results --------------------------------------
	
	/** Map containing the correspondence between iteration and value in the execution of the optimization algorithm */
	private Map<Integer, Double> valueIterMap;
	
	/** Elapsed time during the execution of the optimization algorithm */
	private long elapsedTime;
	
	/**
	 * Creates a new object, and parses all the information needed to run the optimization algorithm.
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 * @param applications the list containing all applications to be deployed into the fog network
	 * @param sensors the list containing all sensors
	 * @param actuators the list containing all sensors
	 * @throws IllegalArgumentException if some of the arguments are null
	 */
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) throws IllegalArgumentException {
		
		if(fogDevices == null || applications == null || sensors == null || actuators == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				hashSet.add(module.getName());
			}
			
			for(AppEdge appEdge : application.getEdges()) {
				hashSet.add(appEdge.getSource());
				hashSet.add(appEdge.getDestination());
			}
		}
		
		
		NR_NODES = fogDevices.size();
		NR_MODULES = hashSet.size();
		
		init();
		extractDevicesCharacteristics(fogDevices);
		extractModulesCharacteristics(applications, hashSet);
		computeApplicationCharacteristics(applications, sensors);
		computeConnectionMap(fogDevices);
		extractDependenciesIndex();
		extractApplicationLoops(applications);
		
		if(Config.PRINT_DETAILS)
			AlgorithmUtils.printAlgorithmDetails(this, fogDevices, applications, sensors, actuators);
	}
	
	/**
	 * Initializes all variables with the correct lengths.
	 */
	private void init() {
		valueIterMap = new HashMap<Integer, Double>();
		
		fId = new int[NR_NODES];
		fName = new String[NR_NODES];
		fMips = new double[NR_NODES];
		fRam = new double[NR_NODES];
		fStrg = new double[NR_NODES];
		fBusyPw = new double[NR_NODES];
		fIdlePw = new double[NR_NODES];
		fTxPw = new double[NR_NODES];
		
		fMipsPrice = new double[NR_NODES];
		fRamPrice = new double[NR_NODES];
		fStrgPrice = new double[NR_NODES];
		fBwPrice = new double[NR_NODES];
		fEnPrice = new double[NR_NODES];
		
		mName = new String[NR_MODULES];
		mMips = new double[NR_MODULES];
		mRam = new double[NR_MODULES];
		mStrg = new double[NR_MODULES];
		
		fLatencyMap = new double[NR_NODES][NR_NODES];
		fBandwidthMap = new double[NR_NODES][NR_NODES];
		
		possibleDeployment = new double[NR_NODES][NR_MODULES];
		currentPlacement = new double[NR_NODES][NR_MODULES];
		
		mDependencyMap = new double[NR_MODULES][NR_MODULES];
		mBandwidthMap = new double[NR_MODULES][NR_MODULES];
		mNWMap = new double[NR_MODULES][NR_MODULES];
		mCPUMap = new double[NR_MODULES][NR_MODULES];
	}
	
	// ------------------------------------------------------ Parse functions start --------------------------------------------------------
	
	/**
	 * Extracts the information from the fog devices list.
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 */
	private void extractDevicesCharacteristics(final List<FogDevice> fogDevices) {
		int i = 0;
		for(FogDevice fogDevice : fogDevices) {
			FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) fogDevice.getCharacteristics();
			
			double totalMips = 0;
			for(Pe pe : fogDevice.getHost().getPeList()) {
				totalMips += pe.getMips();
			}
			
			fId[i] = fogDevice.getId();
			fName[i] = fogDevice.getName();
			fMips[i] = totalMips;
			fRam[i] = fogDevice.getHost().getRam();
			fStrg[i] = fogDevice.getHost().getStorage();
			fBusyPw[i] = ((FogLinearPowerModel) fogDevice.getHost().getPowerModel()).getBusyPower();
			fIdlePw[i] = ((FogLinearPowerModel) fogDevice.getHost().getPowerModel()).getIdlePower();
			
			fMipsPrice[i] = characteristics.getCostPerMips();
			fRamPrice[i] = characteristics.getCostPerMem();
			fStrgPrice[i] = characteristics.getCostPerStorage();
			fBwPrice[i] = characteristics.getCostPerBw();
			fEnPrice[i++] = characteristics.getCostPerEnergy();
		}
	}
	
	/**
	 * Extracts the information from the application modules list.
	 * 
	 * @param applications the list containing all applications to be deployed into the fog network
	 * @param hashSet the list containing the names of all application modules
	 */
	private void extractModulesCharacteristics(final List<Application> applications, final LinkedHashSet<String> hashSet) {
		for(int i  = 0; i < NR_NODES; i++) {
			Arrays.fill(possibleDeployment[i], 1);
		}
		
		int i = 0;
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				
				// MIPS and BW will be computed later
				if(getModuleIndexByModuleName(module.getName()) == -1) {
					mName[i] = module.getName();
					mRam[i] = module.getRam();
					mStrg[i] = module.getSize();
					
					if(module.isClientModule()) {
						String[] parts = module.getName().split("_");
						int nodeId = Integer.parseInt(parts[parts.length-1]);
						int nodeIndex = getNodeIndexByNodeId(nodeId);
						
						for(int j  = 0; j < NR_NODES; j++) {
							if(j == nodeIndex) continue;
							possibleDeployment[j][i] = 0;
						}
					}
					i++;
				}
			}
		}
		
		// Add the sensors and actuators modules
		for(String str : hashSet) {
			if(getModuleIndexByModuleName(str) == -1) {
				mName[i] = str;
				
				String[] parts = str.split("_");
				int nodeId = Integer.parseInt(parts[parts.length-1]);
				int nodeIndex = getNodeIndexByNodeId(nodeId);
				
				for(int j  = 0; j < NR_NODES; j++) {
					if(j != nodeIndex)
						possibleDeployment[j][i] = 0;
					else
						currentPlacement[j][i] = 1;
				}
				
				i++;
			}
		}
	}
	
	/**
	 * Computes the dependencies between modules (i.e., CPU length, network length, bandwidth and dependency)
	 * 
	 * @param applications the list containing all applications to be deployed into the fog network
	 * @param sensors the list containing all sensors
	 */
	private void computeApplicationCharacteristics(final List<Application> applications, final List<Sensor> sensors) {
		final int INTERVAL = 0;
		final int PROBABILITY = 1;
		
		Sensor sensor = null;
		for(Application application : applications) {
			
			TreeMap<String, List<Double>> producers = new TreeMap<String, List<Double>>();
			List<Double> values;
			
			for(Sensor s : sensors) {
				if(s.getAppId().equals(application.getAppId())) {
					sensor = s;
					
					Distribution distribution = sensor.getTransmitDistribution();
					double avg = 0.0;
					if(distribution.getDistributionType() == Distribution.DETERMINISTIC)
						avg = ((DeterministicDistribution)distribution).getValue();
					else if(distribution.getDistributionType() == Distribution.NORMAL)
						avg = ((NormalDistribution)distribution).getMean() - 3*((NormalDistribution)distribution).getStdDev();
					else
						avg = ((UniformDistribution)distribution).getMin();
					
					values = new ArrayList<Double>();
					values.add(avg);
					values.add(1.0);
					producers.put(sensor.getTupleType(), values);
					
					for(AppEdge appEdge : application.getEdges()) {
						if(appEdge.isPeriodic()) {
							values = new ArrayList<Double>();
							values.add(appEdge.getPeriodicity());
							values.add(1.0);
							producers.put(appEdge.getTupleType(), values);
						}
					}
				}
			}
			
			int nrEdges = application.getEdges().size();
			int processed = 0;
			double interval = 0;
			double probability = 0;
			AppModule module = null;
			String toProcess = "";
			
			while(processed < nrEdges) {
				Entry<String, List<Double>> entry = producers.pollFirstEntry();
				
				toProcess = entry.getKey();
				interval = entry.getValue().get(INTERVAL);
				probability = entry.getValue().get(PROBABILITY);
				
				for(AppEdge appEdge : application.getEdges()) {
					if(appEdge.getTupleType().equals(toProcess)) {
						int col = getModuleIndexByModuleName(appEdge.getSource());
						int row = getModuleIndexByModuleName(appEdge.getDestination());
						
						mDependencyMap[col][row] += probability/interval;
						
						for(AppModule appModule : application.getModules()) { // BW and MIPS to sensors and actuators are not used
							if(appModule.getName().equals(appEdge.getDestination())) {								
								int edgeSourceIndex = getModuleIndexByModuleName(appEdge.getSource());
								int edgeDestIndex = getModuleIndexByModuleName(appEdge.getDestination());
								
								appModule.setMips(appModule.getMips() + probability*appEdge.getTupleCpuLength()/interval);
								mMips[edgeDestIndex] += probability*appEdge.getTupleCpuLength()/interval;
								mCPUMap[edgeSourceIndex][edgeDestIndex] += appEdge.getTupleCpuLength();
								
								if(!isSensorTuple(sensors, appEdge.getTupleType())) {
									appModule.setBw((long) (appModule.getBw() + probability*appEdge.getTupleNwLength()/interval));
									mBandwidthMap[edgeSourceIndex][edgeDestIndex] += probability*appEdge.getTupleNwLength()/interval;
									mNWMap[edgeSourceIndex][edgeDestIndex] += appEdge.getTupleNwLength();
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
						values = new ArrayList<Double>();
						values.add(interval);
						values.add(probability*fractionalSelectivity.getSelectivity());
						producers.put(pair.getSecond(), values);
					}
				}
			}
		}
	}
	
	/**
	 * Computes the latency and bandwidth for each link. 
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 */
	private void computeConnectionMap(final List<FogDevice> fogDevices) {
		for (int i = 0; i < NR_NODES; i++) {
			for (int j = 0; j < NR_NODES; j++) {
				fLatencyMap[i][j] = Constants.INF;
				fBandwidthMap[i][j] = 0;
			}
		}
		
		for(FogDevice fogDevice : fogDevices) {
			int dId = fogDevice.getId();
			int dIndex = getNodeIndexByNodeId(dId);
			
			if(!fogDevice.isStaticNode()) {
				fTxPw[dIndex] = MobilePathLossModel.TX_POWER;
			}
			
			for(int neighborId : fogDevice.getLatencyMap().keySet()) {
				double lat = fogDevice.getLatencyMap().get(neighborId);
				double bw = fogDevice.getBandwidthMap().get(neighborId);
				int neighborIndex = getNodeIndexByNodeId(neighborId);
								
				fLatencyMap[dIndex][neighborIndex] = lat;
				fBandwidthMap[dIndex][neighborIndex] = bw;
			}
			
			fLatencyMap[dIndex][dIndex] = 0;
			fBandwidthMap[dIndex][dIndex] = Constants.INF;
		}
	}
	
	/**
	 * Extracts the dependency indexes between each pair of application modules.
	 */
	private void extractDependenciesIndex() {
		int tmp = 0;
		for(int i = 0; i < NR_MODULES; i++) {
			for (int j = 0; j < NR_MODULES; j++) {
				if(mDependencyMap[i][j] != 0) {
					tmp++;
				}
			}
		}
		
		dependenciesIndex = new int[2][tmp];
		
		tmp = 0;
		for(int i = 0; i < NR_MODULES; i++) {
			for (int j = 0; j < NR_MODULES; j++) {
				if(mDependencyMap[i][j] != 0) {
					dependenciesIndex[START][tmp] = i;
					dependenciesIndex[FINAL][tmp++] = j;
				}
			}
		}
	}
	
	/**
	 * Extracts the application modules within each loop and its deadline.
	 * 
	 * @param applications the list containing all applications to be deployed into the fog network
	 */
	private void extractApplicationLoops(final List<Application> applications) {
		int tmp = 0;
		for(Application application : applications) {
			tmp += application.getLoops().size();
		}
		
		loops = new int[tmp][NR_MODULES];
		loopsDeadline = new double[tmp];
		
		for(int i = 0; i < tmp; i++) {
			Arrays.fill(loops[i],-1);
		}
		
		tmp = 0;
		for(Application application : applications) {
			for(AppLoop loop : application.getLoops()) {
				int tmp2 = 0;
				for(String moduleName : loop.getModules()) {
					int modIndex = getModuleIndexByModuleName(moduleName);
					loops[tmp][tmp2++] = modIndex;
				}
				loopsDeadline[tmp++] = loop.getDeadline();
			}
		}
	}
	
	// ------------------------------------------------------- Parse functions end --------------------------------------------------------
	
	/**
	 * Changes the connections of a given mobile node (handover).
	 * 
	 * @param mobile the mobile node which has changes its connection
	 * @param from the fixed node from which the mobile node has disconnected
	 * @param to the fixed node where the mobile node will be connected
	 */
	public void changeConnectionMap(FogDevice mobile, FogDevice from, FogDevice to) {
		int mobileIndex = getNodeIndexByNodeId(mobile.getId());
		int fromIndex = getNodeIndexByNodeId(from.getId());
		int toIndex = getNodeIndexByNodeId(to.getId());
		
		fLatencyMap[mobileIndex][fromIndex] = Constants.INF;
		fBandwidthMap[mobileIndex][fromIndex] = 0;
		
		fLatencyMap[fromIndex][mobileIndex] = Constants.INF;
		fBandwidthMap[fromIndex][mobileIndex] = 0;
		
		double distance = Location.computeDistance(mobile, to);
		double rxPower = MobilePathLossModel.computeReceivedPower(distance);
		Map<String, Double> map = MobileBandwidthModel.computeCommunicationBandwidth(1, rxPower);
		double bandwidth = map.entrySet().iterator().next().getValue();
		
		fLatencyMap[mobileIndex][toIndex] = MobilePathLossModel.LATENCY;
		fBandwidthMap[mobileIndex][toIndex] = bandwidth;
		
		fLatencyMap[toIndex][mobileIndex] = MobilePathLossModel.LATENCY;
		fBandwidthMap[toIndex][mobileIndex] = bandwidth;
	}
	
	/**
	 * Updates both the connections latency and velocity.
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 */
	public void updateConnectionCharacteristcs(final List<FogDevice> fogDevices) {
		for(int i = 0; i < NR_NODES; i++) {
			for(int j = 0; j < NR_NODES; j++) {
				if(i != j) {
					fBandwidthMap[i][j] = 0;
					fLatencyMap[i][j] = Constants.INF;
				}else {
					fBandwidthMap[i][j] = Constants.INF;
					fLatencyMap[i][j] = 0;
				}
			}
		}
		
		for(FogDevice fogDevice : fogDevices) {
			int fogIndex = getNodeIndexByNodeId(fogDevice.getId());
			
			for(int neighborId : fogDevice.getBandwidthMap().keySet()) {
				int neighborIndex = getNodeIndexByNodeId(neighborId);
				double bandwidth = fogDevice.getBandwidthMap().get(neighborId);
				double latency = fogDevice.getLatencyMap().get(neighborId);
				
				fBandwidthMap[fogIndex][neighborIndex] = bandwidth;
				fLatencyMap[fogIndex][neighborIndex] = latency;
			}
		}
	}
	
	/**
	 * Executes a given algorithm in order to find the best solution (the solution with the lower cost which respects all constraints).
	 * 
	 * @return the best solution; can be null
	 */
	public abstract Job execute();
	
	/**
	 * Checks whether the node is valid. It is valid if, and only if, from the current node index towards the final node index
	 * there is a path which has lower or equal number of hops as the provided maximum distance.
	 * 
	 * @param nodeIndex the current node index
	 * @param finalNodeIndex the final node index
	 * @param maxDistance the maximum allowed distance
	 * @return true is it's a valid node. False, otherwise.
	 */
	public boolean isValidHop(final int nodeIndex, final int finalNodeIndex, final int maxDistance) {
			getDijkstra().execute(getDijkstraNodes().get(nodeIndex));
			LinkedList<Vertex> path = getDijkstra().getPath(getDijkstraNodes().get(finalNodeIndex));
			
			// If path is null, means that both start and finish refer to the same node, thus it can be added
	        if((path != null && path.size() <= maxDistance) || path == null)
	        	return true;
	        
	        return false;
	}
	
	/**
	 * Extracts the placement map based on a given binary matrix.
	 * 
	 * @param placementMap the binary matrix which maps the application modules deployment into the fog nodes
	 * @return the parsed list
	 */
	public Map<String, List<String>> extractPlacementMap(final int[][] placementMap) {
		Map<String, List<String>> result = new HashMap<>();
		
		for(int i = 0; i < NR_NODES; i++) {
			List<String> modules = new ArrayList<String>();
			
			for(int j = 0; j < NR_MODULES; j++)
				if(placementMap[i][j] == 1)
					modules.add(mName[j]);
			
			result.put(fName[i], modules);
		}
		
		return result;
	}
	
	/**
	 *  Extracts the tuple routing map based on a given matrix.
	 * 
	 * @param routingMap the matrix which maps the path followed by each dependency between pair of application modules
	 * @return parsed list
	 */
	public Map<Map<Integer, Map<String, String>>, Integer> extractRoutingMap(final int[][] routingMap) {
		Map<Map<Integer, Map<String, String>>, Integer> result = new HashMap<Map<Integer,Map<String,String>>, Integer>();
		
		int iter = 0;
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_MODULES; j++) {
				if(mDependencyMap[i][j] != 0) {					
					Map<String, String> tupleTransmission = new HashMap<String, String>();
					tupleTransmission.put(mName[i], mName[j]);
					
					for(int z = 0; z < routingMap[0].length-1; z++) {
						if(routingMap[iter][z] != routingMap[iter][z+1]) {
							Map<Integer, Map<String, String>> hop = new HashMap<Integer, Map<String,String>>();
							hop.put(routingMap[iter][z], tupleTransmission);
							
							result.put(hop, routingMap[iter][z+1]);
						}
					}
					
					iter++;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Generates the Dijkstra graph based on the current topology.
	 */
	protected void generateDijkstraGraph() {
		// Generate a new Dijkstra graph
		dijkstraNodes = new ArrayList<Vertex>();
		List<Edge> edges = new ArrayList<Edge>();
		
		for(int i  = 0; i < NR_NODES; i++) {
			dijkstraNodes.add(new Vertex("Node=" + i));
		}
		
		for(int i  = 0; i < NR_NODES; i++) {
			for(int j  = 0; j < NR_NODES; j++) {
				if(fLatencyMap[i][j] < Constants.INF) {
					 edges.add(new Edge(dijkstraNodes.get(i), dijkstraNodes.get(j), 1.0));
				}
			}
        }
		
		Graph graph = new Graph(dijkstraNodes, edges);
		dijkstra = new DijkstraAlgorithm(graph);
	}
	
	/**
	 * Gets the index of a application module by its name.
	 * 
	 * @param name the name of the module
	 * @return the id of the module; -1 if it was not found
	 */
	public int getModuleIndexByModuleName(String name) {
		for(int i = 0; i < NR_MODULES; i++)
			if(mName[i] != null && mName[i].equals(name))
				return i;
		return -1;
	}
	
	/**
	 *  Gets the index of a fog node by its id.
	 * 
	 * @param id the id of the fog node
	 * @return the index of the module; -1 if it was not found
	 */
	public int getNodeIndexByNodeId(int id) {
		for(int i = 0; i < NR_NODES; i++)
			if(fId[i] == id)
				return i;
		return -1;
	}
	
	/**
	 * Verifies whether a given tuple is generated by a sensor.
	 * 
	 * @param sensors the list of all sensores
	 * @param tupleType the tuple type to be verified
	 * @return true if it is, otherwise false
	 */
	private boolean isSensorTuple(List<Sensor> sensors, String tupleType) {
		for(Sensor sensor : sensors)
			if(sensor.getTupleType().equals(tupleType))
				return true;
		return false;
	}
	
	/**
	 * Gets the number of fog devices within the fog network.
	 * 
	 * @return the number of fog devices within the fog network
	 */
	public int getNumberOfNodes() {
		return NR_NODES;
	}
	
	/**
	 * Gets the number of application modules within the fog network.
	 * 
	 * @return the number of application modules within the fog network
	 */
	public int getNumberOfModules() {
		return NR_MODULES;
	}
	
	/**
	 * Gets the number of pair of nodes with dependencies.
	 * 
	 * @return the number of pair of nodes with dependencies
	 */
	public int getNumberOfDependencies() {
		int nrDependencies = 0;
		
		for(int i = 0; i < NR_MODULES; i++)
			for(int j = 0; j < NR_MODULES; j++)
				if(mDependencyMap[i][j] != 0)
					nrDependencies++;
		
		return nrDependencies;
	}
	
	/**
	 * Checks whether the current optimization is the first one.
	 * 
	 * @return true it it is, otherwise false
	 */
	public boolean isFirstOptimization() {
		int cnt = 0;
		for(int i  = 0; i < NR_NODES; i++) {
			for(int j  = 0; j < NR_MODULES; j++) {
				if(currentPlacement[i][j] == 1) {
					cnt++;
				}
			}
		}
		
		return !(cnt == NR_MODULES);
	}
	
	/**
	 * Gets the vector holding the price of using processing resources in each fog device.
	 * 
	 * @return the vector holding the price of using processing resources in each fog device
	 */
	public double[] getfMipsPrice() {
		return fMipsPrice;
	}
	
	/**
	 * Gets the vector holding the price of using memory resources in each fog device.
	 * 
	 * @return the vector holding the price of using memory resources in each fog device
	 */
	public double[] getfRamPrice() {
		return fRamPrice;
	}
	
	/**
	 * Gets the vector holding the price of using storage resources in each fog device.
	 * 
	 * @return the vector holding the price of using storage resources in each fog device
	 */
	public double[] getfStrgPrice() {
		return fStrgPrice;
	}
	
	/**
	 * Gets the vector holding the price of using network resources in each fog device.
	 * 
	 * @return the vector holding the price of using network resources in each fog device
	 */
	public double[] getfBwPrice() {
		return fBwPrice;
	}
	
	/**
	 * Gets the vector holding the price of energy consumption in each fog device.
	 * 
	 * @return the vector holding the price of energy consumption in each fog device
	 */
	public double[] getfEnPrice() {
		return fEnPrice;
	}
	
	/**
	 * Gets the vector holding the id of each fog device.
	 * 
	 * @return the vector holding the id of each fog device
	 */
	public int[] getfId() {
		return fId;
	}
	
	/**
	 * Gets the vector holding the name of each fog device.
	 * 
	 * @return the vector holding the name of each fog device
	 */
	public String[] getfName() {
		return fName;
	}
	
	/**
	 * Gets the vector holding the quantity of processing resources in each fog device.
	 * 
	 * @return the vector holding the quantity of processing resources in each fog device
	 */
	public double[] getfMips() {
		return fMips;
	}
	
	/**
	 * Gets the vector holding the quantity of memory resources in each fog device.
	 * 
	 * @return the vector holding the quantity of memory resources in each fog device
	 */
	public double[] getfRam() {
		return fRam;
	}
	
	/**
	 * Gets the vector holding the quantity of storage resources in each fog device.
	 * 
	 * @return the vector holding the quantity of storage resources in each fog device
	 */
	public double[] getfStrg() {
		return fStrg;
	}
	
	/**
	 * Gets the vector holding the power consumption while using the full processing capacity in each fog device.
	 * 
	 * @return the vector holding the power consumption while using the full processing capacity in each fog device
	 */
	public double[] getfBusyPw() {
		return fBusyPw;
	}
	
	/**
	 * Gets the vector holding the power consumption while using no processing resources in each fog device.
	 * 
	 * @return the vector holding the power consumption while using no processing resources in each fog device
	 */
	public double[] getfIdlePw() {
		return fIdlePw;
	}
	
	/**
	 * Gets the vector holding the power consumption while sending data to another device in each fog device.
	 * 
	 * @return the vector holding the power consumption while sending data to another device in each fog device
	 */
	public double[] getfTxPw() {
		return fTxPw;
	}	
	
	/**
	 * Gets the vector holding the name of each application module.
	 * 
	 * @return the vector holding the name of each application module
	 */
	public String[] getmName() {
		return mName;
	}
	
	/**
	 * Gets the vector holding the quantity of processing resources needed in each application module.
	 * 
	 * @return the vector holding the quantity of processing resources needed in each application module
	 */
	public double[] getmMips() {
		return mMips;
	}
	
	/**
	 * Gets the vector holding the quantity of memory resources needed in each application module.
	 * 
	 * @return the vector holding the quantity of memory resources needed in each application module
	 */
	public double[] getmRam() {
		return mRam;
	}
	
	/**
	 * Gets the vector holding the quantity of storage resources needed in each application module.
	 * 
	 * @return the vector holding the quantity of storage resources needed in each application module
	 */
	public double[] getmStrg() {
		return mStrg;
	}
	
	/**
	 * Gets the matrix holding the link latency between each two fog nodes.
	 * 
	 * @return matrix holding the link latency between each two fog nodes
	 */
	public double[][] getfLatencyMap() {
		return fLatencyMap;
	}
	
	/**
	 * Gets the matrix holding the link bandwidth available between each two fog nodes.
	 * 
	 * @return matrix holding the link bandwidth available between each two fog nodes
	 */
	public double[][] getfBandwidthMap() {
		return fBandwidthMap;
	}
	
	/**
	 * Gets the matrix holding the dependencies index (source and destination) between each pair of modules.
	 * 
	 * @return the matrix holding the dependencies index (source and destination) between each pair of modules
	 */
	public double[][] getmDependencyMap() {
		return mDependencyMap;
	}
	
	/**
	 * Gets the matrix holding the average bandwidth needed between each two modules.
	 * 
	 * @return the matrix holding the average bandwidth needed between each two modules
	 */
	public double[][] getmBandwidthMap(){
		return mBandwidthMap;
	}
	
	/**
	 * Gets the matrix holding the tuple network sizes between each two modules.
	 * 
	 * @return the matrix holding the tuple network sizes between each two modules
	 */
	public double[][] getmNWMap(){
		return mNWMap;
	}
	
	/**
	 * Gets the matrix holding the tuple CPU sizes between each two modules.
	 * 
	 * @return the matrix holding the tuple CPU sizes between each two modules
	 */
	public double[][] getmCPUMap(){
		return mCPUMap;
	}
	
	/**
	 * Gets the matrix holding the possible position of each module.
	 * 
	 * @return the matrix holding the possible position of each module
	 */
	public double[][] getPossibleDeployment() {
		return possibleDeployment;
	}
	
	/**
	 * Sets the matrix holding the possible position of each module.
	 * 
	 * @param possibleDeployment the matrix holding the possible position of each module
	 */
	public void setPossibleDeployment(double[][] possibleDeployment) {
		this.possibleDeployment = possibleDeployment;
	}
	
	/**
	 * Gets the matrix holding the current module placement map.
	 * 
	 * @return the matrix holding the current module placement map
	 */
	public double[][] getCurrentPlacement() {
		return currentPlacement;
	}
	
	/**
	 * Gets the integer matrix holding the current module placement map.
	 * 
	 * @return the integer matrix holding the current module placement map
	 */
	public int[][] getCurrentPositionInt() {
		int[][] currentPositionInt = new int[NR_NODES][NR_MODULES];
	    
		for(int j = 0; j < NR_NODES; j++) {
			for (int z = 0; z < NR_MODULES; z++) {
				currentPositionInt[j][z] = (int) currentPlacement[j][z];
			}
		}
		
		return currentPositionInt;
	}
	
	/**
	 * Changes the matrix holding the current module placement map.
	 * 
	 * @param module the module index
	 * @param node the current position (node index) of that module
	 */
	public void setCurrentPlacement(int module, int node) {
		for(int i  = 0; i < NR_NODES; i++) {
			if(i != node)
				currentPlacement[i][module] = 0;
			else
				currentPlacement[i][module] = 1;
		}
	}
	
	/**
	 * Gets the source module index of a given dependency index.
	 * 
	 * @param index the index of the dependency
	 * @return the index of the source module
	 */
	public int getStartModDependency(int index) {
		return dependenciesIndex[START][index];
	}
	
	/**
	 * Gets the destination module index of a given dependency index.
	 * 
	 * @param index the index of the dependency
	 * @return the index of the destination module
	 */
	public int getFinalModDependency(int index) {
		return dependenciesIndex[FINAL][index];
	}	
	
	/**
	 * Gets the matrix holding the loops. Each row corresponds to a given loop. Inside each loop, each entry corresponds to
	 * a given application module index. Each module index has a dependency to the module index at its right. The vector is
	 * then filled it -1 entries, meaning that there are no more modules within the loop.
	 * 
	 * @return the the matrix holding the loops
	 */
	public int[][] getLoops() {
		return loops;
	}
	
	/**
	 * Gets the vector holding the deadline of each loop.
	 * 
	 * @return the vector holding the deadline of each loop
	 */
	public double[] getLoopsDeadline() {
		return loopsDeadline;
	}
	
	/**
	 * Gets the list with the nodes for the execution of the Dijkstra Algorithm.
	 * 
	 * @return the list with the nodes for the execution of the Dijkstra Algorithm
	 */
	public List<Vertex> getDijkstraNodes() {
		return dijkstraNodes;
	}
	
	/**
	 * Gets the object responsible for running the Dijkstra Algorithm.
	 * 
	 * @return the object responsible for running the Dijkstra Algorithm
	 */
	public DijkstraAlgorithm getDijkstra() {
		return dijkstra;
	}
	
	/**
	 * Gets the map containing the correspondence between iteration and value in the execution of the optimization algorithm.
	 * 
	 * @return the value/iter map
	 */
	public Map<Integer, Double> getValueIterMap() {
		return valueIterMap;
	}
	
	/**
	 * Gets the elapsed time during the execution of the optimization algorithm.
	 * 
	 * @return the elapsed time during the execution of the optimization algorithm
	 */
	public long getElapsedTime() {
		return elapsedTime;
	}
	
	/**
	 * Sets the elapsed time during the execution of the optimization algorithm.
	 * 
	 * @param elapsedTime the elapsed time during the execution of the optimization algorithm
	 */
	public void setElapsedTime(long elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	
}