package org.fog.utils;

import java.util.HashMap;
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
	
	private Map<Integer, Double> tupleIdToCpuStartTime;
	private Map<String, Double> tupleTypeToAverageCpuTime;
	private Map<String, Integer> tupleTypeToExecutedTupleCount;
	
	private Map<Map<Integer, String>, Double> tupleNw;
	private Map<String, Map<Double, Integer>> loopIdToCurrentNwAverage;
	
	/**
	 * Creates a new time keeper.
	 */
	private TimeKeeper(){
		count = 1;
		setTupleTypeToAverageCpuTime(new HashMap<String, Double>());
		tupleTypeToExecutedTupleCount = new HashMap<String, Integer>();
		setTupleIdToCpuStartTime(new HashMap<Integer, Double>());
		setLoopIdToCurrentNwAverage(new HashMap<String, Map<Double,Integer>>());
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
	public void tupleStartedTransmission(Tuple tuple) {
		Map<Integer, String> map = new HashMap<Integer, String>();
		map.put(tuple.getActualTupleId(), tuple.getTupleType());
		
		if(!tupleNw.containsKey(map)) {
			tupleNw.put(map, CloudSim.clock());
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
		
		if(!tupleNw.containsKey(map)) return;
		
		double prevTime = CloudSim.clock() - tupleNw.get(map);
		String tupleType = tuple.getTupleType();
		
		tupleNw.remove(map);
		
		if(!loopIdToCurrentNwAverage.containsKey(tuple.getTupleType())) {
			Map<Double, Integer> newMap = new HashMap<Double, Integer>();
			newMap.put(prevTime, 1);
			loopIdToCurrentNwAverage.put(tupleType, newMap);			
		}else {
			Map<Double, Integer> newMap = loopIdToCurrentNwAverage.get(tupleType);
			int counter = newMap.entrySet().iterator().next().getValue();
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
		
		if(!tupleNw.containsKey(map)) return;
		tupleNw.remove(map);
	}

	public Map<String, Double> getTupleTypeToAverageCpuTime() {
		return tupleTypeToAverageCpuTime;
	}

	public void setTupleTypeToAverageCpuTime(
			Map<String, Double> tupleTypeToAverageCpuTime) {
		this.tupleTypeToAverageCpuTime = tupleTypeToAverageCpuTime;
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
	
	public Map<Map<Integer, String>, Double> getTupleNw() {
		return tupleNw;
	}

	public void setTupleNw(Map<Map<Integer, String>, Double> tupleNw) {
		this.tupleNw = tupleNw;
	}
	
	public Map<String, Map<Double, Integer>> getLoopIdToCurrentNwAverage() {
		return loopIdToCurrentNwAverage;
	}

	public void setLoopIdToCurrentNwAverage(Map<String, Map<Double, Integer>> loopIdToCurrentNwAverage) {
		this.loopIdToCurrentNwAverage = loopIdToCurrentNwAverage;
	}
	
}
