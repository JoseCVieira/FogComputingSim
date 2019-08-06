package org.fog.application;

import java.util.List;

import org.fog.utils.TimeKeeper;

/**
 * Class representing application loops to monitor for delay.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AppLoop {
	/** Id of the loop */
	private int loopId;
	
	/** List of modules composing the loop to analyze */
	private List<String> modules;
	
	/** Deadline of the loop (given in seconds) */
	private double deadline;
	
	/**
	 * Creates an application loop to monitor for delay with no deadline.
	 * 
	 * @param modules the list of modules composing the loop to analyze
	 */
	public AppLoop(List<String> modules){
		setLoopId(TimeKeeper.getInstance().getUniqueId());
		setModules(modules);
		setDeadline(Double.MAX_VALUE);
	}
	
	/**
	 * Creates an application loop to monitor for delay with deadline.
	 * 
	 * @param modules the list of modules composing the loop to analyze
	 */
	public AppLoop(List<String> modules, double deadline){
		setLoopId(TimeKeeper.getInstance().getUniqueId());
		setModules(modules);
		setDeadline(deadline);
	}
	
	/**
	 * Checks whether the edge has a given source and destination application module name.
	 * 
	 * @param src the source application module name
	 * @param dest the destination application module name
	 * @return true if the loop has a source and destination application module name equals to the provided ones, otherwise false
	 */
	public boolean hasEdge(String src, String dest){
		for(int i = 0; i < modules.size()-1; i++){
			if(modules.get(i).equals(src) && modules.get(i+1).equals(dest))
				return true;
		}
		return false;
	}
	
	/**
	 * Gets the first module within the loop.
	 * 
	 * @return the first module within the loop
	 */
	public String getStartModule(){
		return modules.get(0);
	}
	
	/**
	 * Gets the last module within the loop.
	 * 
	 * @return the last module within the loop
	 */
	public String getEndModule(){
		return modules.get(modules.size()-1);
	}
	
	/**
	 * Checks whether the provided application module name is equal to the first module within the loop.
	 * 
	 * @param module the application module name
	 * @return true if the provided application module name is equal to the first module within the loop, otherwise false
	 */
	public boolean isStartModule(String module){
		if(getStartModule().equals(module))
			return true;
		return false;
	}
	
	/**
	 * Checks whether the provided application module name is equal to the last module within the loop.
	 * 
	 * @param module the application module name
	 * @return true if the provided application module name is equal to the last module within the loop, otherwise false
	 */
	public boolean isEndModule(String module){
		if(getEndModule().equals(module))
			return true;
		return false;
	}
	
	/**
	 * Gets the list of modules composing the loop to analyze.
	 * 
	 * @return the list of modules composing the loop to analyze
	 */
	public List<String> getModules() {
		return modules;
	}
	
	/**
	 * Sets the list of modules composing the loop to analyze.
	 * 
	 * @param modules the list of modules composing the loop to analyze
	 */
	public void setModules(List<String> modules) {
		this.modules = modules;
	}

	/**
	 * Gets the id of the loop.
	 * 
	 * @return the id of the loop
	 */
	public int getLoopId() {
		return loopId;
	}

	/**
	 * Sets the id of the loop.
	 * 
	 * @param loopId the id of the loop
	 */
	public void setLoopId(int loopId) {
		this.loopId = loopId;
	}
	
	/**
	 * Gets the deadline of the loop (given in seconds).
	 * 
	 * @return the deadline of the loop
	 */
	public double getDeadline() {
		return deadline;
	}

	/**
	 * Sets the deadline of the loop (given in seconds).
	 * 
	 * @param deadline the deadline of the loop
	 */
	public void setDeadline(double deadline) {
		this.deadline = deadline;
	}

	
}
