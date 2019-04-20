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

	// Node
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
	
	// Module
	protected String mName[];
	protected double mMips[];
	protected double mRam[];
	protected double mMem[];
	protected double mBw[];
	
	// Node to Node
	protected double[][] fLatencyMap;
	private double[][] fBandwidthMap;
	
	// Module to Module
	protected double[][] mDependencyMap;
	protected double[][] mBandwidthMap;
	
	// Node to Module
	protected double[][] mandatoryMap;
	
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
				
		fLatencyMap = new double[NR_NODES][NR_NODES];
		fBandwidthMap = new double[NR_NODES][NR_NODES];
		
		for (int i = 0; i < NR_NODES; i++)
			for (int j = 0; j < NR_NODES; j++)
				fLatencyMap[i][j] = Double.MAX_VALUE;
		
		mandatoryMap = new double[NR_NODES][NR_MODULES];
		
		mDependencyMap = new double[NR_MODULES][NR_MODULES];
		mBandwidthMap = new double[NR_MODULES][NR_MODULES];
		
		routingPaths = new HashMap<Map<Integer,Integer>, LinkedList<Vertex>>();
		
		extractDevicesCharacteristics(fogDevices, sensors, actuators);
		extractAppCharacteristics(applications, sensors, actuators);
		computeApplicationCharacteristics(applications, sensors);
		computeLatencyMap(fogDevices, sensors, actuators);
		computeDependencyMap(applications);
		
		if(PRINT_DETAILS)
			AlgorithmUtils.printDetails(this, fogDevices, applications, sensors, actuators);
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
			}
		}
	}
	
	private void computeApplicationCharacteristics(final List<Application> applications, final List<Sensor> sensors) {
		final int INTERVAL = 0;
		final int PROBABILITY = 1;
		
		Sensor sensor = null;		
		for(Application application : applications) {
			for(Sensor s : sensors)
				if(s.getAppId().equals(application.getAppId()))
					sensor = s;
		
			TreeMap<String, List<Double>> producers = new TreeMap<String, List<Double>>();
			List<Double> values;
			
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
								
								if(!isSensorTuple(sensors, appEdge.getTupleType())) {									
									appModule.setBw((long) (appModule.getBw() + probability*appEdge.getTupleNwLength()/interval));
									mBandwidthMap[edgeSourceIndex][edgeDestIndex] += probability*appEdge.getTupleNwLength()/interval;
									mBw[edgeSourceIndex] += probability*appEdge.getTupleNwLength()/interval;
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
	
	private void computeLatencyMap(final List<FogDevice> fogDevices,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		for(FogDevice fogDevice : fogDevices) {
			int dId = fogDevice.getId();
			
			for(int neighborId : fogDevice.getNeighborsIds()) {
				int lat = fogDevice.getLatencyMap().get(neighborId).intValue();
				double bw = fogDevice.getBandwidthMap().get(neighborId);
				
				getfLatencyMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(neighborId)] = lat;
				getfBandwidthMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(neighborId)] = bw;
			}
			
			getfLatencyMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(dId)] = 0;
			getfBandwidthMap()[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(dId)] = Double.MAX_VALUE;
		}
		
		
		for(Sensor sensor : sensors) {
			int id1 = sensor.getId();
			int id2 = sensor.getGatewayDeviceId();
			double lat = sensor.getLatency();
			
			getfBandwidthMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = Double.MAX_VALUE; //sensor and act only have latency
			getfBandwidthMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			getfBandwidthMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			
			getfLatencyMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = lat;
			getfLatencyMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = lat;
			getfLatencyMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = 0;
		}
		
		for(Actuator actuator : actuators) {
			int id1 = actuator.getId();
			int id2 = actuator.getGatewayDeviceId();
			double lat = actuator.getLatency();
			
			getfBandwidthMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = Double.MAX_VALUE;
			getfBandwidthMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			getfBandwidthMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = Double.MAX_VALUE;
			
			getfLatencyMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id2)] = lat;
			getfLatencyMap()[getNodeIndexByNodeId(id2)][getNodeIndexByNodeId(id1)] = lat;
			getfLatencyMap()[getNodeIndexByNodeId(id1)][getNodeIndexByNodeId(id1)] = 0;
		}
	}
	
	private void computeDependencyMap(final List<Application> applications) {
		/*for(Application application : applications) {
			for(AppEdge appEdge : application.getEdges()) {
				int col = getModuleIndexByModuleName(appEdge.getSource());
				int row = getModuleIndexByModuleName(appEdge.getDestination());
				mDependencyMap[col][row] += 1;
			}
		}*/
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
					if(mDependencyMap[i][destinationModuleIndex] > 0) {
						
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

	public double[] getfBusyPw() {
		return fBusyPw;
	}
	
	public double[] getfIdlePw() {
		return fIdlePw;
	}
	
	public double[][] getfLatencyMap() {
		return fLatencyMap;
	}
	
	public double[][] getmDependencyMap() {
		return mDependencyMap;
	}
	
	public double[][] getMandatoryMap() {
		return mandatoryMap;
	}
	
	public double[][] getmBandwidthMap(){
		return mBandwidthMap;
	}

	public double[][] getfBandwidthMap() {
		return fBandwidthMap;
	}
	
}
