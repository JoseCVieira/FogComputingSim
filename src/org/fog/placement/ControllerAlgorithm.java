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
import org.fog.placement.algorithm.overall.random.RandomAlgorithm;

public class ControllerAlgorithm {
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
		Config.SINGLE_OBJECTIVE = true;
		
		switch (algorithmOp) {
			case FogComputingSim.MOLP:
				Config.SINGLE_OBJECTIVE = false;
				algorithmName = "Multiobjective Linear Programming";
				algorithm = new MultiObjectiveLinearProgramming(fogDevices, appList, sensors, actuators);
				break;
			case FogComputingSim.LP:
				algorithmName = "Linear Programming";
				algorithm = new LinearProgramming(fogDevices, appList, sensors, actuators);
				break;
			case FogComputingSim.GA:
				algorithmName = "Genetic Algorithm";
				algorithm = new GeneticAlgorithm(fogDevices, appList, sensors, actuators);
				break;
			case FogComputingSim.RAND:
				algorithmName = "Random Algorithm";
				algorithm = new RandomAlgorithm(fogDevices, appList, sensors, actuators);
				break;
			case FogComputingSim.BF:
				algorithmName = " Brute Force Algorithm";
				algorithm = new BruteForce(fogDevices, appList, sensors, actuators);
				break;
			default:
				FogComputingSim.err("Unknown algorithm");
		}
		
		System.out.println("Running the optimization algorithm: " + algorithmName + ".");
		solution = algorithm.execute(null);
		
		if(Config.PLOT_RESULTS)
			OutputControllerResults.plotResult(algorithm, algorithmName);
		
		if(solution == null || solution.getModulePlacementMap() == null || solution.getTupleRoutingMap() == null || !solution.isValid()) {
			FogComputingSim.err("There is no possible combination to deploy all applications");
		}
	}
	
	public void recomputeAlgorithm() {
		solution = algorithm.execute(solution.getModulePlacementMap());
		
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
