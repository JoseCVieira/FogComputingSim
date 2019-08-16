package org.fog.placement.algorithm.random;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.SingleObjectiveCostFunction;

/**
 * Class in which defines and executes the random algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class RandomAlgorithm extends Algorithm {
	private Job bestSolution;
	private double bestCost;
	private int iteration;

	public RandomAlgorithm(List<FogDevice> fogDevices, List<Application> applications,
			List<Sensor> sensors, List<Actuator> actuators) throws IllegalArgumentException {
		super(fogDevices, applications, sensors, actuators);
	}
	
	/**
	 * Executes the random algorithm in order to find the best solution (the solution with the lower cost which respects all constraints).
	 * 
	 * @return the best solution; can be null
	 */
	@Override
	public Job execute() {
		iteration = 0;
		bestCost = Constants.REFERENCE_COST;
		bestSolution = null;
		
		// Time at the beginning of the execution of the algorithm
		long start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
		while (iteration <= Config.MAX_ITER_RANDOM) {
			Job job = Job.generateRandomJob(this, new SingleObjectiveCostFunction());
			
			if(bestCost > job.getCost()) {
				bestCost = job.getCost();
    			getValueIterMap().put(iteration, bestCost);
    			bestSolution = new Job(job);
    			
    			if(Config.PRINT_ALGORITHM_ITER)
    				System.out.println("Iteration: " + iteration + " value: " + bestCost);
			}
			
			iteration++;
		}
		
		// Time at the end of the execution of the algorithm
		long finish = System.currentTimeMillis();
		
		setElapsedTime(finish - start);
		
		return bestSolution;
	}

}
