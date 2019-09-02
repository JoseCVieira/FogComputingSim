package org.fog.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.entities.Tuple;

/**
 * Class which is responsible for the simulation time analysis (e.g., CPU execution time, tuple transmission latency, etc).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
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
	private Map<Map<Integer, String>, Double> tupleNw;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwLatAverage;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwBwAverage;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwAverage;
	
	/**
	 * Creates a new time keeper.
	 */
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
		setLoopIdToCurrentNwAverage(new HashMap<String, Map<Double,Integer>>());
		setTupleNwLat(new HashMap<Map<Integer, String>, Double>());
		setTupleNwBw(new HashMap<Map<Integer, String>, Double>());
		setTupleNw(new HashMap<Map<Integer, String>, Double>());
	}
	
	/**
	 * Gets the current instance.
	 * 
	 * @return the time keeper instance
	 */
	public static TimeKeeper getInstance(){
		if(instance == null)
			instance = new TimeKeeper();
		return instance;
	}
	
	/**
	 * Gets a unique id.
	 * 
	 * @return the unique id
	 */
	public int getUniqueId(){
		return count++;
	}
	
	/**
	 * Stores the initial time in which the tuple has started to be executed inside the CPU.
	 * 
	 * @param tuple the tuple which began to be executed
	 */
	public void tupleStartedExecution(Tuple tuple){
		tupleIdToCpuStartTime.put(tuple.getCloudletId(), CloudSim.clock());
	}
	
	/**
	 * Updates the average execution time of a given tuple.
	 * 
	 * @param tuple the processed tuple
	 */
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
	
	/**
	 * Stores the initial time in which the tuple been added to the transmission queue.
	 * 
	 * @param tuple the tuple which has been added to the transmission queue
	 */
	public void tryingTransmissionOfTuple(Tuple tuple) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		
		if(!tupleNw.containsKey(map)) {
			tupleNw.put(map, CloudSim.clock());
		}
	}
	
	/**
	 * Stores the initial time in which the tuple has started to be sent to another fog device.
	 * 
	 * @param tuple the tuple which has started to be sent to another fog device
	 * @param lat the link latency
	 * @param bw the link bandwidth
	 */
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
	
	/**
	 * Stores the final time in which the tuple has been sent to another fog device.
	 * 
	 * @param tuple the tuple which has been sent to another fog device
	 */
	public void receivedTuple(Tuple tuple) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		
		if(!tupleNwLat.containsKey(map)) return;
		
		double prevLat = tupleNwLat.get(map);
		double prevBw = tupleNwBw.get(map);
		double prevTime = CloudSim.clock() - tupleNw.get(map);
		String tupleType = tuple.getTupleType();
		
		tupleNwLat.remove(map);
		tupleNwBw.remove(map);
		tupleNw.remove(map);
		
		if(!loopIdToCurrentNwLatAverage.containsKey(tuple.getTupleType())) {
			Map<Double, Integer> newMap = new HashMap<Double, Integer>();
			newMap.put(prevLat, 1);
			loopIdToCurrentNwLatAverage.put(tupleType, newMap);
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(prevBw, 1);
			loopIdToCurrentNwBwAverage.put(tupleType, newMap);
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(prevTime, 1);
			loopIdToCurrentNwAverage.put(tupleType, newMap);			
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
			
			newMap = loopIdToCurrentNwAverage.get(tupleType);
			double totalTime = newMap.entrySet().iterator().next().getKey();
			newMap = new HashMap<Double, Integer>();
			newMap.put(totalTime + prevTime, counter + 1);
			loopIdToCurrentNwAverage.put(tupleType, newMap);
		}
		
	}
	
	/**
	 * Removes the entry from each list when a tuple has been dropped.
	 * 
	 * @param tuple the tuple which has been lost
	 */
	public void lostTuple(Tuple tuple) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		
		if(!tupleNwLat.containsKey(map)) return;
		
		tupleNwLat.remove(map);
		tupleNwBw.remove(map);
		tupleNw.remove(map);
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
	
	public Map<Map<Integer, String>, Double> getTupleNw() {
		return tupleNw;
	}

	public void setTupleNw(Map<Map<Integer, String>, Double> tupleNw) {
		this.tupleNw = tupleNw;
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
	
	public Map<String, Map<Double, Integer>> getLoopIdToCurrentNwAverage() {
		return loopIdToCurrentNwAverage;
	}

	public void setLoopIdToCurrentNwAverage(Map<String, Map<Double, Integer>> loopIdToCurrentNwAverage) {
		this.loopIdToCurrentNwAverage = loopIdToCurrentNwAverage;
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
