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
	private Map<Integer, Double> loopIdToCurrentAverage;
	private Map<Integer, Double> loopIdToCurrentNetworkAverage;
	private Map<Integer, Integer> loopIdToCurrentNum;
	
	private Map<Map<Integer, String>, Double> tupleNwLat;
	private Map<Map<Integer, String>, Double> tupleNwBw;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwLatAverage;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwBwAverage;
	
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
		setTupleNwLat(new HashMap<Map<Integer, String>, Double>());
		setTupleNwBw(new HashMap<Map<Integer, String>, Double>());
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
	
	public void startedTransmissionOfTuple(Tuple tuple, double lat, double bw) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		double size = tuple.getCloudletFileSize();
		
		if(!tupleNwLat.containsKey(map)) {
			tupleNwLat.put(map, lat);
			tupleNwBw.put(map, size/bw);
		}else {
			double prevLat = tupleNwLat.get(map);
			tupleNwLat.put(map, prevLat + lat);
			
			double prevBw = tupleNwBw.get(map);
			tupleNwBw.put(map, prevBw + (size/bw));
		}
	}
	
	public void receivedTuple(Tuple tuple) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		
		if(!tupleNwLat.containsKey(map)) return;
		
		double prevLat = tupleNwLat.get(map);
		double prevBw = tupleNwBw.get(map);
		String tupleType = tuple.getTupleType();
		
		tupleNwLat.remove(map);
		tupleNwBw.remove(map);
		
		if(!loopIdToCurrentNwLatAverage.containsKey(tuple.getTupleType())) {
			Map<Double, Integer> newMap = new HashMap<Double, Integer>();
			newMap.put(prevLat, 1);
			loopIdToCurrentNwLatAverage.put(tupleType, newMap);
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(prevBw, 1);
			loopIdToCurrentNwBwAverage.put(tupleType, newMap);
		}else {
			Map<Double, Integer> newMap = loopIdToCurrentNwLatAverage.get(tupleType);
			double totalLat = newMap.entrySet().iterator().next().getKey();
			int counter = newMap.entrySet().iterator().next().getValue();
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(totalLat + prevLat, counter + 1);
			loopIdToCurrentNwLatAverage.put(tupleType, newMap);
			
			newMap = loopIdToCurrentNwBwAverage.get(tupleType);
			double totalBw = newMap.entrySet().iterator().next().getKey();
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(totalBw + prevBw, counter + 1);
			loopIdToCurrentNwBwAverage.put(tupleType, newMap);
		}
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
	
	public Map<Map<Integer, String>, Double> getTupleNwLat() {
		return tupleNwLat;
	}

	public void setTupleNwLat(Map<Map<Integer, String>, Double> tupleNwLat) {
		this.tupleNwLat = tupleNwLat;
	}
	
	public Map<Map<Integer, String>, Double> getTupleNwBw() {
		return tupleNwBw;
	}

	public void setTupleNwBw(Map<Map<Integer, String>, Double> tupleNwBw) {
		this.tupleNwBw = tupleNwBw;
	}
	
	public Map<String, Map<Double, Integer>> getLoopIdToCurrentNwLatAverage() {
		return loopIdToCurrentNwLatAverage;
	}

	public void setLoopIdToCurrentNwLatAverage(Map<String, Map<Double, Integer>> loopIdToCurrentNwLatAverage) {
		this.loopIdToCurrentNwLatAverage = loopIdToCurrentNwLatAverage;
	}
	
	public Map<String, Map<Double, Integer>> getLoopIdToCurrentNwBwAverage() {
		return loopIdToCurrentNwBwAverage;
	}

	public void setLoopIdToCurrentNwBwAverage(Map<String, Map<Double, Integer>> loopIdToCurrentNwBwAverage) {
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
