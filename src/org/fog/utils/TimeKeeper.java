package org.fog.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

public class TimeKeeper {
	private static TimeKeeper instance;
	private long simulationStartTime;
	private int count;
	
	private Map<Integer, Double> emitTimes;
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<String, Double> tupleTypeToAverageCpuTime;
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	
	private Map<Integer, List<Integer>> loopIdToTupleIds;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwLatAverage;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwBwAverage;
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Double> loopIdToCurrentNetworkAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;
	
	private TimeKeeper(){
		count = 1;
		setEmitTimes(new HashMap<Integer, Double>());
		setLoopIdToTupleIds(new HashMap<Integer, List<Integer>>());
		setTupleTypeToAverageCpuTime(new HashMap<String, Double>());
		setTupleTypeToExecutedTupleCount(new HashMap<String, Integer>());
		setTupleIdToCpuStartTime(new HashMap<Integer, Double>());
		setLoopIdToCurrentAverage(new HashMap<Integer, Double>());
		setLoopIdToCurrentNum(new HashMap<Integer, Integer>());
		setLoopIdToCurrentNwLatAverage(new HashMap<String, Map<Double,Integer>>());
		setLoopIdToCurrentNwBwAverage(new HashMap<String, Map<Double,Integer>>());
	}
	
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}
	
	public int getUniqueId(){
		return count++;
	}
	
	public void tupleStartedExecution(Tuple tuple){
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());
	}
	
	public void tupleEndedExecution(Tuple tuple){
		if(!tupleIdToCpuStartTime.containsKey(tuple.getCloudletId()))
			return;
		double executionTime = CloudSim.clock() - tupleIdToCpuStartTime.get(tuple.getCloudletId());
		if(!tupleTypeToAverageCpuTime.containsKey(tuple.getTupleType())){
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), executionTime);
			tupleTypeToExecutedTupleCount.put(tuple.getTupleType(), 1);
		} else{
			double currentAverage = tupleTypeToAverageCpuTime.get(tuple.getTupleType());
			int currentCount = tupleTypeToExecutedTupleCount.get(tuple.getTupleType());
			tupleTypeToAverageCpuTime.put(tuple.getTupleType(), 
					(currentAverage*currentCount+executionTime)/(currentCount+1));
		}
	}
	
	public void startedTransmissionOfTuple(String tupleType, double lat, double bw, double size) {
		if(!loopIdToCurrentNwLatAverage.containsKey(tupleType)) {
			Map<Double, Integer> map = new HashMap<Double, Integer>();
			map.put(lat, 0);
			loopIdToCurrentNwLatAverage.put(tupleType, map);
			
			map = new HashMap<Double, Integer>();
			map.put(size/bw, 0);
			loopIdToCurrentNwBwAverage.put(tupleType, map);
		}else {
			Map<Double, Integer> map = loopIdToCurrentNwLatAverage.get(tupleType);
			double totalLat = map.entrySet().iterator().next().getKey();
			int counter = map.entrySet().iterator().next().getValue();
			
			map = new HashMap<Double, Integer>();
			map.put(totalLat + lat, counter);
			loopIdToCurrentNwLatAverage.put(tupleType, map);
			
			map = loopIdToCurrentNwBwAverage.get(tupleType);
			double totalBw = map.entrySet().iterator().next().getKey();
			
			map = new HashMap<Double, Integer>();
			map.put(totalBw + (size/bw), counter);
			loopIdToCurrentNwBwAverage.put(tupleType, map);
		}
	}
	
	public void receivedTuple(String tupleType) {
		if(!loopIdToCurrentNwLatAverage.containsKey(tupleType)) return;
		
		Map<Double, Integer> map = loopIdToCurrentNwLatAverage.get(tupleType);
		double totalLat = map.entrySet().iterator().next().getKey();
		int counter = map.entrySet().iterator().next().getValue();
		
		map = new HashMap<Double, Integer>();
		map.put(totalLat, counter+1);
		loopIdToCurrentNwLatAverage.put(tupleType, map);
		
		map = loopIdToCurrentNwBwAverage.get(tupleType);
		double totalBw = map.entrySet().iterator().next().getKey();
		map = new HashMap<Double, Integer>();
		map.put(totalBw, counter+1);
		loopIdToCurrentNwBwAverage.put(tupleType, map);
	}

	public Map<Integer, Double> getEmitTimes() {
		return emitTimes;
	}

	public void setEmitTimes(Map<Integer, Double> emitTimes) {
		this.emitTimes = emitTimes;
	}

	public Map<Integer, List<Integer>> getLoopIdToTupleIds() {
		return loopIdToTupleIds;
	}

	public void setLoopIdToTupleIds(Map<Integer, List<Integer>> loopIdToTupleIds) {
		this.loopIdToTupleIds = loopIdToTupleIds;
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
	}

	public Map<String, Integer> getTupleTypeToExecutedTupleCount() {
		return tupleTypeToExecutedTupleCount;
	}

	public void setTupleTypeToExecutedTupleCount(
			Map<String, Integer> tupleTypeToExecutedTupleCount) {
		this.tupleTypeToExecutedTupleCount = tupleTypeToExecutedTupleCount;
	}

	public Map<Integer, Double> getTupleIdToCpuStartTime() {
		return tupleIdToCpuStartTime;
	}

	public void setTupleIdToCpuStartTime(Map<Integer, Double> tupleIdToCpuStartTime) {
		this.tupleIdToCpuStartTime = tupleIdToCpuStartTime;
	}

	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<Integer, Double> getLoopIdToCurrentAverage() {
		return loopIdToCurrentAverage;
	}

	public void setLoopIdToCurrentAverage(Map<Integer, Double> loopIdToCurrentAverage) {
		this.loopIdToCurrentAverage = loopIdToCurrentAverage;
	}
	
	public  Map<String, Map<Double, Integer>> getLoopIdToCurrentNwLatAverage() {
		return loopIdToCurrentNwLatAverage;
	}

	public void setLoopIdToCurrentNwLatAverage( Map<String, Map<Double, Integer>> loopIdToCurrentNwLatAverage) {
		this.loopIdToCurrentNwLatAverage = loopIdToCurrentNwLatAverage;
	}
	
	public  Map<String, Map<Double, Integer>> getLoopIdToCurrentNwBwAverage() {
		return loopIdToCurrentNwBwAverage;
	}

	public void setLoopIdToCurrentNwBwAverage( Map<String, Map<Double, Integer>> loopIdToCurrentNwBwAverage) {
		this.loopIdToCurrentNwBwAverage = loopIdToCurrentNwBwAverage;
	}
	
	public Map<Integer, Double> getLoopIdToCurrentNetworkAverage() {
		return loopIdToCurrentNetworkAverage;
	}

	public void setLoopIdToCurrentNetworkAverage(Map<Integer, Double> loopIdToCurrentNetworkAverage) {
		this.loopIdToCurrentNetworkAverage = loopIdToCurrentNetworkAverage;
	}

	public Map<Integer, Integer> getLoopIdToCurrentNum() {
		return loopIdToCurrentNum;
	}

	public void setLoopIdToCurrentNum(Map<Integer, Integer> loopIdToCurrentNum) {
		this.loopIdToCurrentNum = loopIdToCurrentNum;
	}
	
}
