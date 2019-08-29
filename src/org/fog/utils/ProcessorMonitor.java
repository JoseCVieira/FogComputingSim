package org.fog.utils;

import java.util.LinkedList;
import java.util.Queue;

import org.cloudbus.cloudsim.core.SimEvent;

/**
 * Class which holds the CPU tuple queue, the processor current state as well as the counters
 * for the ordered and processed MIs.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ProcessorMonitor {
	/** Queue (FIFO) which holds all tuples to be processed */
	private Queue<SimEvent> tupleCPUQueue;
	
	/** State of the processor */
	private boolean isCPUBusy;
	
	/** Number of million instructions ordered to be processed by the processor */
	private double orderedMI;
	
	/** Number of million instructions processed by the processor */
	private double processedMI;
	
	/**
	 * Creates a new processor monitor.
	 */
	public ProcessorMonitor() {
		tupleCPUQueue = new LinkedList<SimEvent>();
	}
	
	/**
	 * Adds a new tuple to the queue.
	 * 
	 * @param ev the event which contains the tuple
	 */
	public void addTupleToQueue(SimEvent ev) {
		tupleCPUQueue.add(ev);
	}
	
	/**
	 * Gets the next tuple in the list (in a FIFO manner)
	 * 
	 * @return the oldest tuple in the list; null if it is empty
	 */
	public SimEvent getPopTupleFromQueue() {
		if(!tupleCPUQueue.isEmpty())
			return tupleCPUQueue.poll();
		return null;
	}
	
	/**
	 * Verifies whether there still are tuples waiting inside the list.
	 * 
	 * @return true if the list is empty, otherwise false
	 */
	public boolean isEmptyTupleQueue() {
		return tupleCPUQueue.isEmpty();
	}
	
	/**
	 * Gets the state of the processor.
	 * 
	 * @return true if the processor is busy, otherwise false
	 */
	public boolean isCPUBusy() {
		return isCPUBusy;
	}
	
	/**
	 * Changes the processor state.
	 * 
	 * @param isCPUBusy the processor state
	 */
	public void setCPUBusy(boolean isCPUBusy) {
		this.isCPUBusy = isCPUBusy;
	}
	
	/**
	 * Gets the number of million instructions ordered to be processed by the processor.
	 * 
	 * @return the number of million instructions ordered to be processed by the processor
	 */
	public double getOrderedMI() {
		return orderedMI;
	}
	
	/**
	 * Adds a given number of million instructions ordered to be processed by the processor.
	 * 
	 * @param orderedMI the number of million instructions ordered to be processed by the processor
	 */
	public void addOrderedMI(double orderedMI) {
		this.orderedMI += orderedMI;
	}
	
	/**
	 * Gets the number of million instructions processed by the processor.
	 * 
	 * @return the number of million instructions processed by the processor
	 */
	public double getProcessedMI() {
		return processedMI;
	}
	
	/**
	 * Adds a given number of million instructions processed by the processor.
	 * 
	 * @param processedMI the number of million instructions processed by the processor
	 */
	public void addProcessedMI(double processedMI) {
		this.processedMI += processedMI;
	}
	
}
