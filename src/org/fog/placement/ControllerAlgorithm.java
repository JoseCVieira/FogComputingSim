package org.fog.placement;

/**
 * Class which is responsible for choosing and running the optimization algorithm in order to
 * optimize the module disposition within the fog network.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.bf.BruteForce;
import org.fog.placement.algorithm.lp.LinearProgramming;
import org.fog.placement.algorithm.lp.MultiObjectiveLinearProgramming;
import org.fog.placement.algorithm.ga.GeneticAlgorithm;
import org.fog.placement.algorithm.random.RandomAlgorithm;
import org.fog.placement.algorithm.util.AlgorithmUtils;

public class ControllerAlgorithm {
	public static final int MOLP = 1;
	public static final int MOGA = 2;
	public static final int LP = 3;
	public static final int GA = 4;
	public static final int RAND = 5;
	public static final int BF = 6;
	public static final int NR_ALGORITHMS = 6;
	
	/** Object which holds all the information needed to run the optimization algorithm */
	private Algorithm algorithm;
	
	/** Object which holds the results of the optimization algorithm */
	private Job solution;
	
	/** List containing all fog devices within the fog network */
	private List<FogDevice> fogDevices;
	
	/** List containing all applications to be deployed into the fog network */
	private List<Application> appList;
	
	/** List containing all sensors (there is one sensor per user's application) */
	private List<Sensor> sensors;
	
	/** List containing all actuators (there is one actuator per user's application) */
	private List<Actuator> actuators;
	
	/** Id of the optimization algorithm chosen to be executed */
	private int algorithmOp;
	
	/** Name of the optimization algorithm chosen to be executed */
	private String algorithmName = "";
	
	/**
	 * Creates a new instance and receives all the information needed for the optimization algorithm.
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 * @param appList the list containing all applications to be deployed into the fog network
	 * @param sensors the list containing all sensors
	 * @param actuators the list containing all actuators
	 * @param algorithmOp the id of the optimization algorithm chosen to be executed
	 */
	public ControllerAlgorithm(List<FogDevice> fogDevices, List<Application> appList, List<Sensor> sensors,
			List<Actuator> actuators, int algorithmOp) {
		this.fogDevices = fogDevices;
		this.appList = appList;
		this.sensors = sensors;
		this.actuators = actuators;
		this.algorithmOp = algorithmOp;
	}
	
	/**
	 * Parses the information and executes the chosen optimization algorithm.
	 */
	public void computeAlgorithm() {
		// It will be the first execution of the optimization algorithm
		if(algorithm == null) {
			switch (algorithmOp) {
				case MOLP:
					algorithmName = "Multiobjective Linear Programming";
					algorithm = new MultiObjectiveLinearProgramming(fogDevices, appList, sensors, actuators);
					break;
				/*case MOGA:
					algorithmName = "Multiobjective Genetic Algorithm";
					algorithm = new MultiObjectiveGeneticAlgorithm(fogDevices, appList, sensors, actuators);
					break;*/
				case LP:
					algorithmName = "Linear Programming";
					algorithm = new LinearProgramming(fogDevices, appList, sensors, actuators);
					break;
				case GA:
					algorithmName = "Genetic Algorithm";
					algorithm = new GeneticAlgorithm(fogDevices, appList, sensors, actuators);
					break;
				case RAND:
					algorithmName = "Random Algorithm";
					algorithm = new RandomAlgorithm(fogDevices, appList, sensors, actuators);
					break;
				case BF:
					algorithmName = "Brute Force Algorithm";
					algorithm = new BruteForce(fogDevices, appList, sensors, actuators);
					break;
				default:
					FogComputingSim.err("Unknown algorithm");
			}
		}
		
		System.out.println("Running the optimization algorithm: " + algorithmName + ".");
		solution = algorithm.execute();
		
		if(solution == null || !solution.isValid())
			FogComputingSim.err("There is no possible combination to deploy all applications");
		
		if(Config.PRINT_ALGORITHM_RESULTS)
	    	AlgorithmUtils.printAlgorithmResults(algorithm, solution);
		
		if(Config.PLOT_ALGORITHM_RESULTS)
			OutputControllerResults.plotResult(algorithm, algorithmName);
	}
	
	/**
	 * Gets the object which holds all the information needed to run the optimization algorithm.
	 * 
	 * @return the object which holds all the information needed to run the optimization algorithm
	 */
	public Algorithm getAlgorithm() {
		return algorithm;
	}
	
	/**
	 * Sets the object which holds all the information needed to run the optimization algorithm. 
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 */
	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}
	
	/**
	 * Gets the results of the optimization algorithm.
	 * 
	 * @return the results of the optimization algorithm
	 */
	public Job getSolution() {
		return solution;
	}
	
	/**
	 * Sets the results of the optimization algorithm.
	 * 
	 * @param solution the results of the optimization algorithm
	 */
	public void setSolution(Job solution) {
		this.solution = solution;
	}
	
}
