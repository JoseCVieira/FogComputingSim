package org.fog.placement.algorithms.overall.random;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.overall.Algorithm;
import org.fog.placement.algorithms.overall.Job;
import org.fog.placement.algorithms.overall.util.AlgorithmUtils;

public class Random extends Algorithm {
	private Job bestSolution = null;
	private double bestCost = Constants.MIN_SOLUTION;
	private int iteration = 0;

	public Random(List<FogBroker> fogBrokers, List<FogDevice> fogDevices, List<Application> applications,
			List<Sensor> sensors, List<Actuator> actuators) throws IllegalArgumentException {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}

	@Override
	public Job execute() {		
		long start = System.currentTimeMillis();
		
		while (iteration <= Config.MAX_ITER) {
			Job job = Job.generateRandomJob(this, getNumberOfNodes(), getNumberOfModules());
			
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
