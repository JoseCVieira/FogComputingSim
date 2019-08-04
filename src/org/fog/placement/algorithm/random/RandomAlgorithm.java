package org.fog.placement.algorithm.overall.random;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;

public class RandomAlgorithm extends Algorithm {
	private Job bestSolution;
	private double bestCost;
	private int iteration;

	public RandomAlgorithm(List<FogDevice> fogDevices, List<Application> applications,
			List<Sensor> sensors, List<Actuator> actuators) throws IllegalArgumentException {
		super(fogDevices, applications, sensors, actuators);
	}

	@Override
	public Job execute() {
		iteration = 0;
		bestCost = Constants.REFERENCE_COST;
		bestSolution = null;
		
		long start = System.currentTimeMillis();		
		while (iteration <= Config.MAX_ITER_RANDOM) {
			Job job = Job.generateRandomJob(this, currentPlacement);
			
			if(bestCost > job.getCost()) {
				bestCost = job.getCost();
    			valueIterMap.put(iteration, bestCost);
    			bestSolution = new Job(job);
    			
    			if(Config.PRINT_BEST_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestCost);
			}
			
			iteration++;
		}
		
		long finish = System.currentTimeMillis();
		elapsedTime = finish - start;
			
		if(Config.PRINT_DETAILS) {
	    	AlgorithmUtils.printResults(this, bestSolution);
		}
		
		return bestSolution;
	}

}
