package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

public abstract class Algorithm {
	protected Map<Integer, Double> valueIterMap = new HashMap<Integer, Double>();
	protected long elapsedTime;
	
	protected final int NR_NODES;
	protected final int NR_MODULES;

	// Node
	protected double fMipsPrice[];
	protected double fRamPrice[];
	protected double fStrgPrice[];
	protected double fBwPrice[];
	
	protected int fId[];
	protected String fName[];
	protected double fMips[];
	protected double fRam[];
	protected double fStrg[];
	protected double fBusyPw[];
	protected double fIdlePw[];
	
	// Module
	protected String mName[];
	protected double mMips[];
	protected double mRam[];
	protected double mStrg[];
	
	// Node to Node
	protected double[][] fLatencyMap;
	private double[][] fBandwidthMap;
	private double[][] fLatencyMapOriginalValues;
	private double[][] fBandwidthMapOriginalValues;
	
	// Module to Module
	protected double[][] mDependencyMap;
	protected double[][] mBandwidthMap;
	private double[][] mBandwidthMapOriginalValues;
	
	// Node to Module
	protected double[][] possibleDeployment;
	protected double[][] currentPlacement;
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) throws IllegalArgumentException {
		
		if(fogDevices == null || applications == null || sensors == null || actuators == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		NR_NODES = fogDevices.size();
		
		fId = new int[NR_NODES];
		fName = new String[NR_NODES];
		fMips = new double[NR_NODES];
		fRam = new double[NR_NODES];
		fStrg = new double[NR_NODES];
		fBusyPw = new double[NR_NODES];
		fIdlePw = new double[NR_NODES];
		fMipsPrice = new double[NR_NODES];
		fRamPrice = new double[NR_NODES];
		fStrgPrice = new double[NR_NODES];
		fBwPrice = new double[NR_NODES];
		
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
		
		NR_MODULES = hashSet.size();
		mName = new String[NR_MODULES];
		mMips = new double[NR_MODULES];
		mRam = new double[NR_MODULES];
		mStrg = new double[NR_MODULES];
		
		fLatencyMap = new double[NR_NODES][NR_NODES];
		fBandwidthMap = new double[NR_NODES][NR_NODES];
		fLatencyMapOriginalValues = new double[NR_NODES][NR_NODES];
		fBandwidthMapOriginalValues = new double[NR_NODES][NR_NODES];
		
		possibleDeployment = new double[NR_NODES][NR_MODULES];
		currentPlacement = new double[NR_NODES][NR_MODULES];
		
		mDependencyMap = new double[NR_MODULES][NR_MODULES];
		mBandwidthMap = new double[NR_MODULES][NR_MODULES];
		mBandwidthMapOriginalValues = new double[NR_MODULES][NR_MODULES];
		
		extractDevicesCharacteristics(fogDevices);
		extractAppCharacteristics(applications, hashSet);
		computeApplicationCharacteristics(applications, sensors);
		computeConnectionMap(fogDevices);
		normalizeValues();
		
		if(Config.PRINT_DETAILS)
			AlgorithmUtils.printDetails(this, fogDevices, applications, sensors, actuators);
	}
	
	private void extractDevicesCharacteristics (final List<FogDevice> fogDevices) {
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
			fIdlePw[i] = ((FogLinearPowerModel) fogDevice.getHost().getPowerModel()).getStaticPower();
			
			fMipsPrice[i] = characteristics.getCostPerMips();
			fRamPrice[i] = characteristics.getCostPerMem();
			fStrgPrice[i] = characteristics.getCostPerStorage();
			fBwPrice[i++] = characteristics.getCostPerBw();
		}
	}
	
	private void extractAppCharacteristics(final List<Application> applications, final LinkedHashSet<String> hashSet) {
		for(int i  = 0; i < NR_NODES; i++) {
			for(int j  = 0; j < NR_MODULES; j++) {
				possibleDeployment[i][j] = 1;
			}
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
							if(j != nodeIndex) {
								possibleDeployment[j][i] = 0;
							}
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
				
				for(int j = 0; j < NR_NODES; j++) {
					if(j != nodeIndex)
						possibleDeployment[j][i] = 0;
					else
						currentPlacement[j][i] = 1;
				}
				
				i++;
			}
		}
	}
	
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
								
								if(!isSensorTuple(sensors, appEdge.getTupleType())) {
									appModule.setBw((long) (appModule.getBw() + probability*appEdge.getTupleNwLength()/interval));
									mBandwidthMap[edgeSourceIndex][edgeDestIndex] += probability*appEdge.getTupleNwLength()/interval;
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
	
	private void computeConnectionMap(final List<FogDevice> fogDevices) {
		for (int i = 0; i < NR_NODES; i++) {
			for (int j = 0; j < NR_NODES; j++) {
				fLatencyMap[i][j] = Constants.INF;
				fBandwidthMap[i][j] = 0;
			}
		}
		
		for(FogDevice fogDevice : fogDevices) {
			int dId = fogDevice.getId();
			
			for(int neighborId : fogDevice.getLatencyMap().keySet()) {
				double lat = fogDevice.getLatencyMap().get(neighborId);
				double bw = fogDevice.getBandwidthMap().get(neighborId);
				
				fLatencyMap[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(neighborId)] = lat;
				fBandwidthMap[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(neighborId)] = bw;
			}
			
			fLatencyMap[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(dId)] = 0;
			fBandwidthMap[getNodeIndexByNodeId(dId)][getNodeIndexByNodeId(dId)] = Constants.INF;
		}
	}
	
	// Mobile communications are characterize by a fixed latency and bandwidth
	public void changeConnectionMap(FogDevice mobile, FogDevice from, FogDevice to) {		
		fLatencyMap[getNodeIndexByNodeId(mobile.getId())][getNodeIndexByNodeId(to.getId())] = Config.MOBILE_LATENCY;
		fBandwidthMap[getNodeIndexByNodeId(mobile.getId())][getNodeIndexByNodeId(to.getId())] = Config.MOBILE_COMMUNICATION_BW;
		
		fLatencyMap[getNodeIndexByNodeId(to.getId())][getNodeIndexByNodeId(mobile.getId())] = Config.MOBILE_LATENCY;
		fBandwidthMap[getNodeIndexByNodeId(to.getId())][getNodeIndexByNodeId(mobile.getId())] = Config.MOBILE_COMMUNICATION_BW;
		
		fLatencyMap[getNodeIndexByNodeId(mobile.getId())][getNodeIndexByNodeId(from.getId())] = Constants.INF;
		fBandwidthMap[getNodeIndexByNodeId(mobile.getId())][getNodeIndexByNodeId(from.getId())] = 0;
		
		fLatencyMap[getNodeIndexByNodeId(from.getId())][getNodeIndexByNodeId(mobile.getId())] = Constants.INF;
		fBandwidthMap[getNodeIndexByNodeId(from.getId())][getNodeIndexByNodeId(mobile.getId())] = 0;
	}
	
	private void normalizeValues() {
		AlgorithmUtils.print("\n\n\n\n\n\n\nfMipsPrice", fMipsPrice);
		AlgorithmUtils.print("fRamPrice", fRamPrice);
		AlgorithmUtils.print("fStrgPrice", fStrgPrice);
		AlgorithmUtils.print("fBwPrice", fBwPrice);
		
		AlgorithmUtils.print("fMips", fMips);
		AlgorithmUtils.print("mMips", mMips);
		
		AlgorithmUtils.print("fRam", fRam);
		AlgorithmUtils.print("mRam", mRam);
		
		AlgorithmUtils.print("fStrg", fStrg);
		AlgorithmUtils.print("mStrg", mStrg);
		
		AlgorithmUtils.print("fBusyPw", fBusyPw);
		AlgorithmUtils.print("fIdlePw", fIdlePw);
		
		AlgorithmUtils.print("fBandwidthMap", fBandwidthMap);
		AlgorithmUtils.print("mBandwidthMap", mBandwidthMap);
		
		AlgorithmUtils.print("fLatencyMap", fLatencyMap);
		
		AlgorithmUtils.print("mDependencyMap", mDependencyMap);
		
		saveOriginalValues();
		
		double max = computeMax(fMipsPrice, fRamPrice, fStrgPrice, fBwPrice);
		fMipsPrice = normalize(fMipsPrice, max);
		fRamPrice = normalize(fRamPrice, max);
		fStrgPrice = normalize(fStrgPrice, max);
		fBwPrice = normalize(fBwPrice, max);
		
		max = computeMax(fMips, mMips);
		fMips = normalize(fMips, max);
		mMips = normalize(mMips, max);
		
		max = computeMax(fRam, mRam);
		fRam = normalize(fRam, max);
		mRam = normalize(mRam, max);
		
		max = computeMax(fStrg, mStrg);
		fStrg = normalize(fStrg, max);
		mStrg = normalize(mStrg, max);
		
		max = computeMax(fBusyPw, fIdlePw);
		fBusyPw = normalize(fBusyPw, max);
		fIdlePw = normalize(fIdlePw, max);
		
		max = computeMax(fBandwidthMap, mBandwidthMap);
		fBandwidthMap = normalize(fBandwidthMap, max);
		mBandwidthMap = normalize(mBandwidthMap, max);
		
		max = computeMax(fLatencyMap);
		fLatencyMap = normalize(fLatencyMap, max);
		
		max = computeMax(mDependencyMap);
		mDependencyMap = normalize(mDependencyMap, max);
		
		AlgorithmUtils.print("fMipsPrice", fMipsPrice);
		AlgorithmUtils.print("fRamPrice", fRamPrice);
		AlgorithmUtils.print("fStrgPrice", fStrgPrice);
		AlgorithmUtils.print("fBwPrice", fBwPrice);
		
		AlgorithmUtils.print("fMips", fMips);
		AlgorithmUtils.print("mMips", mMips);
		
		AlgorithmUtils.print("fRam", fRam);
		AlgorithmUtils.print("mRam", mRam);
		
		AlgorithmUtils.print("fStrg", fStrg);
		AlgorithmUtils.print("mStrg", mStrg);
		
		AlgorithmUtils.print("fBusyPw", fBusyPw);
		AlgorithmUtils.print("fIdlePw", fIdlePw);
		
		AlgorithmUtils.print("fBandwidthMap", fBandwidthMap);
		AlgorithmUtils.print("mBandwidthMap", mBandwidthMap);
		
		AlgorithmUtils.print("fLatencyMap", fLatencyMap);
		
		AlgorithmUtils.print("mDependencyMap", mDependencyMap);
		
		System.out.println("\n\n\n\n\n\n");
	}
	
	public void recomputeNormalizationValues() {
		AlgorithmUtils.print("\n\n\n\n\nfBandwidthMap", fBandwidthMap);
		AlgorithmUtils.print("mBandwidthMap", mBandwidthMap);
		
		AlgorithmUtils.print("fLatencyMap", fLatencyMap);
		
		saveOriginalValues();
		
		double max = computeMax(fBandwidthMap, mBandwidthMap);
		fBandwidthMap = normalize(fBandwidthMap, max);
		mBandwidthMap = normalize(mBandwidthMap, max);
		
		max = computeMax(fLatencyMap);
		fLatencyMap = normalize(fLatencyMap, max);
		
		AlgorithmUtils.print("fBandwidthMap", fBandwidthMap);
		AlgorithmUtils.print("mBandwidthMap", mBandwidthMap);
		
		AlgorithmUtils.print("fLatencyMap", fLatencyMap);
		
		System.out.println("\n\n\n\n\n\n");
	}
	
	private double computeMax(double[]... vectors) {
		double max = 0;
		
		for (int i = 0; i < vectors.length; i++) {
			for (int j = 0; j < vectors[i].length; j++) {
				if(vectors[i][j] == Constants.INF) {
					continue;
				}
				
				if (max < vectors[i][j]) {
					max = vectors[i][j];
				}
			}
		}
		
		return max;
	}
	
	private double computeMax(double[][]... matrices) {
		double max = 0;
		
		for (int i = 0; i < matrices.length; i++) {
			for (int j = 0; j < matrices[i].length; j++) {
				for (int z = 0; z < matrices[i][0].length; z++) {
					if(matrices[i][j][z] == Constants.INF) {
						continue;
					}
					
					if (max < matrices[i][j][z]) {
						max = matrices[i][j][z];
					}
				}
			}
		}
		
		return max;
	}
	
	private double[] normalize(double[] vector, double max) {
		for (int i = 0; i < vector.length; i++) {
			if(vector[i] == Constants.INF) {
				continue;
			}
			
			vector[i] = vector[i]/max;
		}
		
		return vector;
	}
	
	private double[][] normalize(double[][] matix, double max) {
		for (int i = 0; i < matix.length; i++) {
			for (int j = 0; j < matix[0].length; j++) {
				if(matix[i][j] == Constants.INF) {
					continue;
				}
				
				matix[i][j] = matix[i][j]/max;
			}
		}
		
		return matix;
	}
	
	public abstract Job execute();
	
	public Map<String, List<String>> extractPlacementMap(final int[][] placementMap) {
		Map<String, List<String>> result = new HashMap<>();
		
		for(int i = 0; i < NR_NODES; i++) {
			List<String> modules = new ArrayList<String>();
			
			for(int j = 0; j < NR_MODULES; j++)
				if(placementMap[i][j] == 1)
					modules.add(getmName()[j]);
			
			result.put(getfName()[i], modules);
		}
		
		return result;
	}
	
	public Map<Map<Integer, Map<String, String>>, Integer> extractRoutingMap(final int[][] routingMap) {
		Map<Map<Integer, Map<String, String>>, Integer> result = new HashMap<Map<Integer,Map<String,String>>, Integer>();
		
		int iter = 0;
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_MODULES; j++) {
				if(getmDependencyMap()[i][j] != 0) {					
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
	
	public int getModuleIndexByModuleName(String name) {
		for(int i = 0; i < NR_MODULES; i++)
			if(mName[i] != null && mName[i].equals(name))
				return i;
		return -1;
	}
	
	public int getNodeIndexByNodeId(int id) {
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

	public double[] getfMipsPrice() {
		return fMipsPrice;
	}

	public double[] getfRamPrice() {
		return fRamPrice;
	}

	public double[] getfStrgPrice() {
		return fStrgPrice;
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

	public double[] getfStrg() {
		return fStrg;
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

	public double[] getmStrg() {
		return mStrg;
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
	
	public double[][] getPossibleDeployment() {
		return possibleDeployment;
	}
	
	public void setPossibleDeployment(double[][] possibleDeployment) {
		this.possibleDeployment = possibleDeployment;
	}
	
	public double[][] getmBandwidthMap(){
		return mBandwidthMap;
	}

	public double[][] getfBandwidthMap() {
		return fBandwidthMap;
	}
	
	public int getNumberOfNodes() {
		return NR_NODES;
	}
	
	public int getNumberOfModules() {
		return NR_MODULES;
	}
	
	public int getNumberOfDependencies() {
		int nrDependencies = 0;
		
		for(int i = 0; i < NR_MODULES; i++)
			for(int j = 0; j < NR_MODULES; j++)
				if(getmDependencyMap()[i][j] != 0)
					nrDependencies++;
		
		return nrDependencies;
	}
	
	public Map<Integer, Double> getValueIterMap() {
		return valueIterMap;
	}
	
	public long getElapsedTime() {
		return elapsedTime;
	}
	
	public double[][] getCurrentPlacement() {
		return currentPlacement;
	}

	public void setCurrentPlacement(int module, int node) {
		for(int i  = 0; i < NR_NODES; i++) {
			if(i != node)
				currentPlacement[i][module] = 0;
			else
				currentPlacement[i][module] = 1;
		}
	}
	
	// If is the first optimization there is no migration of modules
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
	
	public int[][] getCurrentPositionInt() {
		int[][] currentPositionInt = new int[NR_NODES][NR_MODULES];
	    
		for(int j = 0; j < NR_NODES; j++) {
			for (int z = 0; z < NR_MODULES; z++) {
				currentPositionInt[j][z] = (int) currentPlacement[j][z];
			}
		}
		
		return currentPositionInt;
	}
	
	private void saveOriginalValues() {
		for(int i = 0; i < NR_NODES; i++) {
			for(int j = 0; j < NR_NODES; j++) {
				fLatencyMapOriginalValues[i][j] = fLatencyMap[i][j];
				fBandwidthMapOriginalValues[i][j] = fBandwidthMap[i][j];
			}
		}
		
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_MODULES; j++) {
				mBandwidthMapOriginalValues[i][j] = mBandwidthMap[i][j];
			}
		}
	}
	
	public void loadOriginalValues() {
		for(int i = 0; i < NR_NODES; i++) {
			for(int j = 0; j < NR_NODES; j++) {
				fLatencyMap[i][j] = fLatencyMapOriginalValues[i][j];
				fBandwidthMap[i][j] = fBandwidthMapOriginalValues[i][j];
			}
		}
		
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_MODULES; j++) {
				mBandwidthMap[i][j] = mBandwidthMapOriginalValues[i][j];
			}
		}
	}
	
}