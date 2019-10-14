package org.fog.placement.algorithm.rand;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;

/**
 * Class in which defines and executes the random algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class RandomAlgorithm extends Algorithm {
	private Solution bestSolution;
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
	public Solution execute() {
		iteration = 0;
		bestSolution = null;
		getValueIterMap().clear();
		
		// Time at the beginning of the execution of the algorithm
		long start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
		int convergenceIter = 0;
		while (iteration <= Config.MAX_ITER_RANDOM) {
			Solution solution = Solution.generateRandomSolution(this);
			
			// Check the convergence error
			if(Solution.checkConvergence(solution, bestSolution))
				convergenceIter++;
			else
    			convergenceIter = 0;
			
    		// Check whether the new individual is the new best solution
    		bestSolution = Solution.checkBestSolution(this, solution, bestSolution, iteration);
			
    		// If it found the same (or similar) solution a given number of times in a row break the loop
    		if(++convergenceIter == Config.MAX_ITER_CONVERGENCE_RANDOM) break;
			
			iteration++;
		}
		
		// Time at the end of the execution of the algorithm
		long finish = System.currentTimeMillis();
		
		setElapsedTime(finish - start);
		
		return bestSolution;
	}

}
