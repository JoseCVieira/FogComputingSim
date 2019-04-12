package org.fog.placement.algorithms.placement;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.routing.Vertex;

public abstract class Algorithm {
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
	
	protected double[][] latencyMap;
	protected double[][] dependencyMap;
	protected double[][] nwSizeMap;
	protected double[] mCpuSize;
	protected double[][] mandatoryMap;
	protected double[][][] bandwidthMap;
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
		dependencyMap = new double[NR_MODULES][NR_MODULES];
		nwSizeMap = new double[NR_MODULES][NR_MODULES];
		mandatoryMap = new double[NR_NODES][NR_MODULES];
		bandwidthMap = new double[NR_NODES-1][NR_NODES][NR_NODES];
	}
	
	protected int getModuleIdByModuleName(String name) {
		for(int i = 0; i < mName.length; i++)
			if(mName[i].equals(name))
				return i;
		return -1;
	}
	
	protected int getNodeIdByNodeName(String name) {
		for(int i = 0; i < fName.length; i++)
			if(fName[i].equals(name))
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
	
	public double[][] getNwSizeMap(){
		return nwSizeMap;
	}
	
}
