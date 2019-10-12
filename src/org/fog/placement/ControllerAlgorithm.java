package org.fog.placement;

import java.io.IOException;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.util.AlgorithmUtils;
import org.fog.utils.output.ExcelUtils;
import org.fog.utils.output.MatlabChartUtils;
import org.fog.placement.algorithm.Solution;
import org.fog.placement.algorithm.bf.BruteForce;
import org.fog.placement.algorithm.ga.GeneticAlgorithm;
import org.fog.placement.algorithm.lp.LinearProgramming2;
import org.fog.placement.algorithm.lp.LinearProgramming;
import org.fog.placement.algorithm.rand.RandomAlgorithm;

/**
 * Class which is responsible for choosing and running the optimization algorithm in order to
 * optimize the module disposition within the fog network.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ControllerAlgorithm {
	public static final int NR_ALGORITHMS = 5;
	
	private static final int LP1 = 1;
	private static final int LP2 = 2;
	private static final int GA = 3;
	private static final int RAND = 4;
	private static final int BF = 5;
	
	
	/** Object which holds all the information needed to run the optimization algorithm */
	private Algorithm algorithm;
	
	/** Object which holds the results of the optimization algorithm */
	private Solution solution;
	
	/** Id of the optimization algorithm chosen to be executed */
	private int algorithmOp;
	
	/** Name of the optimization algorithm chosen to be executed */
	private String algorithmName = "";
	
	/**
	 * Creates a new instance and receives all the information needed for the optimization algorithm.
	 * 
	 * @param algorithmOp the id of the optimization algorithm chosen to be executed
	 */
	public ControllerAlgorithm(int algorithmOp) {
		this.algorithmOp = algorithmOp;
	}
	
	/**
	 * Parses the information and executes the chosen optimization algorithm.
	 * 
	 * @param fogDevices the list containing all fog devices within the fog network
	 * @param appList the list containing all applications to be deployed into the fog network
	 * @param sensors the list containing all sensors
	 * @param actuators the list containing all actuators
	 */
	public void computeAlgorithm(List<FogDevice> fogDevices, List<Application> appList, List<Sensor> sensors, List<Actuator> actuators) {
		// It will be the first execution of the optimization algorithm
		if(algorithm == null) {
			switch (algorithmOp) {
				case LP1:
					algorithmName = "Linear Programming (NxN)";
					algorithm = new LinearProgramming(fogDevices, appList, sensors, actuators);
					break;
				case LP2:
					algorithmName = "Linear Programming (Edges)";
					algorithm = new LinearProgramming2(fogDevices, appList, sensors, actuators);
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
		
		if(Config.PRINT_DETAILS)
			System.out.println("\n\nRunning the optimization algorithm: " + algorithmName + ".");
		
		solution = algorithm.execute();
		
		if(solution == null || !solution.isValid())
			FogComputingSim.err("There is no possible combination to deploy all applications");
		
		if(Config.PRINT_ALGORITHM_RESULTS)
	    	AlgorithmUtils.printAlgorithmResults(algorithm, solution);
		
		if(Config.PLOT_ALGORITHM_RESULTS && !algorithmName.equals("Multi-objective Linear Programming"))
			new MatlabChartUtils(algorithm, algorithmName);
		
		if(Config.EXPORT_RESULTS_EXCEL) {
			try {
				ExcelUtils.writeExcel(solution, algorithmName, algorithm.getElapsedTime());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
	public Solution getSolution() {
		return solution;
	}
	
	/**
	 * Sets the results of the optimization algorithm.
	 * 
	 * @param solution the results of the optimization algorithm
	 */
	public void setSolution(Solution solution) {
		this.solution = solution;
	}
	
}
