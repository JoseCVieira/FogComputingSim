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
	
	private Map<Map<Integer, String>, Double> tupleNw;
	private Map<String, Map<Double, Integer>> tupleTotalNw;
	
	private Map<Integer, Double> tupleCpu;
	private Map<String, Map<Double, Integer>> tupleTotalCpu;
	
	private Map<List<String>, Double> minLoop;
	private Map<List<String>, Map<Double, Integer>> totalLoop;
	private Map<List<String>, Double> maxLoop;
	private Map<List<String>, Integer> violatedLoop;
	
	/**
	 * Creates a new time keeper.
	 */
	private TimeKeeper() {
		count = 1;
		
		tupleCpu = new HashMap<Integer, Double>();
		tupleTotalCpu = new HashMap<String, Map<Double,Integer>>();
		
		tupleNw = new HashMap<Map<Integer, String>, Double>();
		tupleTotalNw = new HashMap<String, Map<Double,Integer>>();
		
		minLoop = new HashMap<List<String>, Double>();
		totalLoop = new HashMap<List<String>, Map<Double,Integer>>();
		maxLoop = new HashMap<List<String>, Double>();
		violatedLoop = new HashMap<List<String>, Integer>();
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
		tupleCpu.put(tuple.getCloudletId(), CloudSim.clock());
	}
	
	/**
	 * Updates the average execution time of a given tuple.
	 * 
	 * @param tuple the processed tuple
	 */
	public void tupleEndedExecution(Tuple tuple){
		if(!tupleCpu.containsKey(tuple.getCloudletId()))
			return;
		
		double executionTime = CloudSim.clock() - tupleCpu.get(tuple.getCloudletId());
		
		Map<Double, Integer> newMap;
		if(!tupleTotalCpu.containsKey(tuple.getTupleType())) {
			newMap = new HashMap<Double, Integer>();
			newMap.put(executionTime, 1);
		}else {
			newMap = tupleTotalCpu.get(tuple.getTupleType());
			int counter = newMap.entrySet().iterator().next().getValue();
			double totalTime = newMap.entrySet().iterator().next().getKey();
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(totalTime + executionTime, counter + 1);
		}
		
		tupleTotalCpu.put(tuple.getTupleType(), newMap);
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
		
		Map<Double, Integer> newMap;
		if(!tupleTotalNw.containsKey(tuple.getTupleType())) {
			newMap = new HashMap<Double, Integer>();
			newMap.put(prevTime, 1);		
		}else {
			newMap = tupleTotalNw.get(tupleType);
			int counter = newMap.entrySet().iterator().next().getValue();
			double totalTime = newMap.entrySet().iterator().next().getKey();
			
			newMap = new HashMap<Double, Integer>();
			newMap.put(totalTime + prevTime, counter + 1);
		}
		
		tupleTotalNw.put(tupleType, newMap);
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
	
	/**
	 * Updates the loop statistics.
	 * 
	 * @param path the loop path
	 * @param delay the measured delay
	 * @param deadline the loop deadline
	 */
	public void finishedLoop(final List<String> path, final double delay, final double deadline) {
		Map<Double, Integer> avgMap = new HashMap<Double, Integer>();
		
		if(!totalLoop.containsKey(path)) {
			minLoop.put(path, delay);
			maxLoop.put(path, delay);
			violatedLoop.put(path, delay <= deadline ? 0 : 1);
			
			avgMap.put(delay, 1);
			totalLoop.put(path, avgMap);
		}else {
			double min = minLoop.get(path);
			double max = maxLoop.get(path);
			double avg = totalLoop.get(path).entrySet().iterator().next().getKey();
			int cnt = totalLoop.get(path).entrySet().iterator().next().getValue();
			int nrViolated = violatedLoop.get(path);
			
			if(min > delay)
				minLoop.put(path, delay);
			
			if(max < delay)
				maxLoop.put(path, delay);
			
			violatedLoop.put(path, nrViolated + (delay <= deadline ? 0 : 1));
			
			avgMap.put(avg+delay, cnt + 1);
			totalLoop.put(path, avgMap);
		}		
	}
	
	public long getSimulationStartTime() {
		return simulationStartTime;
	}

	public void setSimulationStartTime(long simulationStartTime) {
		this.simulationStartTime = simulationStartTime;
	}

	public Map<String, Map<Double, Integer>> getTupleTotalCpu() {
		return tupleTotalCpu;
	}
	
	public Map<String, Map<Double, Integer>> getTupleTotalNw() {
		return tupleTotalNw;
	}
	
	public Map<List<String>, Double> getMinLoop() {
		return minLoop;
	}

	public Map<List<String>, Map<Double, Integer>> getTotalLoop() {
		return totalLoop;
	}

	public Map<List<String>, Double> getMaxLoop() {
		return maxLoop;
	}
	
	public Map<List<String>, Integer> getViolatedLoop() {
		return violatedLoop;
	}
	
}
