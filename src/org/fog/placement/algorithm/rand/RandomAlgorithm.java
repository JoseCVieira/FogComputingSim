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
		getValueIterMap().clear();
		
		// Time at the beginning of the execution of the algorithm
		long start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
		int convergenceIter = 0;
		boolean hasConverged = false;
		while (iteration <= Config.MAX_ITER_RANDOM) {
			Job job = Job.generateRandomJob(this, new SingleObjectiveCostFunction());
			
			// Check the convergence error
			if(job.getCost() < Constants.REFERENCE_COST) {
	    		if(Math.abs(bestCost - job.getCost()) <= Config.CONVERGENCE_ERROR) {
	    			// If it found the same (or similar) solution a given number of times in a row break the loop
					if(++convergenceIter == Config.MAX_ITER_CONVERGENCE_RANDOM)
						hasConverged = true;
				}else
	    			convergenceIter = 0;
			}
			
			if(bestCost > job.getCost()) {
				bestCost = job.getCost();
    			getValueIterMap().put(iteration, bestCost);
    			bestSolution = new Job(job);
    			
    			if(Config.PRINT_ALGORITHM_BEST_ITER)
    				System.out.println("Iteration: " + iteration + " value: " + bestCost);
			}
			
			if(hasConverged) break;
			
			iteration++;
		}
		
		// Time at the end of the execution of the algorithm
		long finish = System.currentTimeMillis();
		
		setElapsedTime(finish - start);
		
		return bestSolution;
	}

}
