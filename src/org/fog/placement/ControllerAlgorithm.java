package org.fog.placement;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.bf.BruteForce;
import org.fog.placement.algorithm.overall.ga.GeneticAlgorithm;
import org.fog.placement.algorithm.overall.lp.LinearProgramming;
import org.fog.placement.algorithm.overall.lp.MultiObjectiveLinearProgramming;
import org.fog.placement.algorithm.overall.nsga2.MultiObjectiveGeneticAlgorithm;
import org.fog.placement.algorithm.overall.random.RandomAlgorithm;

public class ControllerAlgorithm {
	public static final int MOLP = 1;
	public static final int MOGA = 2;
	public static final int LP = 3;
	public static final int GA = 4;
	public static final int RAND = 5;
	public static final int BF = 6;
	public static final int NR_ALGORITHMS = 6;
	
	private Algorithm algorithm;
	private Job solution;
	
	private List<FogDevice> fogDevices;
	private List<Application> appList;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	private int algorithmOp;
	
	private String algorithmName = "";
	
	public ControllerAlgorithm(List<FogDevice> fogDevices, List<Application> appList, List<Sensor> sensors, List<Actuator> actuators, int algorithmOp) {
		this.fogDevices = fogDevices;
		this.appList = appList;
		this.sensors = sensors;
		this.actuators = actuators;
		this.algorithmOp = algorithmOp;
	}
	
	public void computeAlgorithm() {
		switch (algorithmOp) {
			case MOLP:
				algorithmName = "Multiobjective Linear Programming";
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, appList, sensors, actuators);
				break;
			case MOGA:
				algorithmName = "Multiobjective Genetic Algorithm";
				algorithm = new MultiObjectiveGeneticAlgorithm(fogDevices, appList, sensors, actuators);
				break;
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
		
		System.out.println("Running the optimization algorithm: " + algorithmName + ".");
		solution = algorithm.execute();
		
		if(Config.PLOT_ALGORITHM_RESULTS)
			OutputControllerResults.plotResult(algorithm, algorithmName);
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getTupleRoutingMap() == null || !solution.isValid()) {
			FogComputingSim.err("There is no possible combination to deploy all applications");
		}
	}
	
	public void recomputeAlgorithm() {
		solution = algorithm.execute();
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getTupleRoutingMap() == null || !solution.isValid()) {
			FogComputingSim.err("There is no possible combination to deploy all applications");
		}
	}
	
	public Algorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(Algorithm algorithm) {
		this.algorithm = algorithm;
	}

	public Job getSolution() {
		return solution;
	}

	public void setSolution(Job solution) {
		this.solution = solution;
	}
	
}
