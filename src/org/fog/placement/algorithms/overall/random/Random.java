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

	public Random(List<FogBroker> fogBrokers, List<FogDevice> fogDevices, List<Application> applications,
			List<Sensor> sensors, List<Actuator> actuators) throws IllegalArgumentException {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}

	@Override
	public Job execute() {
		double bestValue = Constants.MIN_SOLUTION;
		Job bestJob = null;
		int iteration = 1;
		
		long start = System.currentTimeMillis();
		
		while (iteration <= Config.MAX_ITER) {
			Job job = Job.generateRandomJob(this, getNumberOfNodes(), getNumberOfModules());
			
			if(bestValue > job.getCost()) {
    			bestValue = job.getCost();
    			valueIterMap.put(iteration, bestValue);
    			bestJob = job;
    			
    			if(Config.PRINT_BEST_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestValue);
			}
			
			iteration++;
		}
		
		long finish = System.currentTimeMillis();
		elapsedTime = finish - start;
		
		if(bestValue < Constants.MIN_SOLUTION) {
			
			if(Config.PRINT_DETAILS) {
		    	AlgorithmUtils.printResults(this, bestJob);
			}
			
			return bestJob;
		}
		
		return null;
	}

}
