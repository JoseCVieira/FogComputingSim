package org.fog.placement.algorithm.ga;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;

/**
 * Class in which defines and executes the multiple objective genetic algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class GeneticAlgorithm extends Algorithm {
	/** 10% of the population */
	private static final int FITTEST_PLACEMENT = (int)(Config.POPULATION_SIZE_GA_PLACEMENT*0.1);
	private static final int FITTEST_ROUTING = (int)(Config.POPULATION_SIZE_GA_ROUTING*0.1);
	
	/** Best solution found by the algorithm */
	private Solution bestSolution;
	
	/** Current iteration of the algorithm */
	private int iteration;
	
	/** Time at the beginning of the execution of the algorithm */
	private long start;
	
	/** Time at the end of the execution of the algorithm */
	private long finish;
	
	public GeneticAlgorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Solution execute() {
		iteration = 0;
		bestSolution = null;
		getValueIterMap().clear();
		
		// Time at the beginning of the execution of the algorithm
		start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
		// Solve the problem
		solveModulePlacement();
	    
	    // Time at the end of the execution of the algorithm
 		finish = System.currentTimeMillis();
 		
 		setElapsedTime(finish - start);
 		
 		return bestSolution;
	}
			
	/**
	 * Solves the module placement map.
	 */
	private void solveModulePlacement() {
		int convergenceIter = 0;
		int generation = 0;
		Individual[] population = new Individual[Config.POPULATION_SIZE_GA_PLACEMENT];
		
		// Generate an initial population with random module placements
	    for(int i = 0; i < Config.POPULATION_SIZE_GA_PLACEMENT; i++){
	    	population[i] = new Individual(this, new Solution(this, Solution.generateRandomPlacement(this, getNumberOfNodes(), getNumberOfModules())));
	    }
	    
	    while (generation <= Config.MAX_ITER_PLACEMENT_GA) {
	    	// Solve both tuple and virtual machine migration routing tables
	    	population = GARouting(population);
	    	
	    	// Sort the array based on its value (ascending order)
	    	Arrays.sort(population);
	    	
	    	Solution prevBestSolution = null;
			if(bestSolution != null) prevBestSolution = new Solution(this, bestSolution);
			
			// Check whether the new individual is the new best solution
    		bestSolution = Solution.checkBestSolution(this, population[0].getChromosome(), bestSolution, iteration);
			
	    	// Check the convergence error
    		if(Solution.checkConvergence(prevBestSolution, bestSolution)) {
    			convergenceIter++;
    			
    			// If it found the same (or similar) solution a given number of times in a row break the loop
				if(convergenceIter == Config.MAX_ITER_PLACEMENT_CONVERGENCE_GA) break;
			}else
    			convergenceIter = 0;
	  
	        // Otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA_PLACEMENT];
	        
	        // Copy 10% of the fittest individuals to the next generation
	        for(int i = 0; i < FITTEST_PLACEMENT; i++) {
	        	newGeneration[i] = new Individual(this, new Solution(this, population[i].getChromosome().getModulePlacementMap()));
	        }
	        
	        // From 50% of fittest population, individuals will mate to produce offspring
	        for(int i = FITTEST_PLACEMENT; i < Config.POPULATION_SIZE_GA_PLACEMENT; i++) {
	        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA_PLACEMENT*0.5));
	        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA_PLACEMENT*0.5));
	        	
	        	// Create the new individual
	        	newGeneration[i] = new Individual(this, new Solution(this, population[r1].matePlacement(population[r2])));
	        }
	        
	        // Set the current generation's population
	        for(int i = 0; i < Config.POPULATION_SIZE_GA_PLACEMENT; i++) {
	        	population[i] = newGeneration[i];
	        }
	        
	        // Increments the generation for the module placement map
	        generation++;
	    }
	}
	
	/**
	 * Solves both the tuple and virtual machine migration routing maps.
	 * 
	 * @param population the population containing only the module placement map
	 * @return the population containing the whole solution (module placement map, and tuple and virtual machine migration routing maps)
	 */
	public Individual[] GARouting(Individual[] population) {
		// For each individual with a given module placement map
		for (int i = 0; i < Config.POPULATION_SIZE_GA_PLACEMENT; i++) {
			
			// Get it's module placement map
			int[][] modulePlacementMap = population[i].getChromosome().getModulePlacementMap();
			
			if(checkResourcesExceeded(modulePlacementMap)) {
				int[][]tupleRouting = new int[getNumberOfDependencies()][getNumberOfNodes()];
				int[][]migrationRouting = new int[getNumberOfModules()][getNumberOfNodes()];
						
				population[i] = new Individual(this, new Solution(this, modulePlacementMap, tupleRouting, migrationRouting));
				continue;
			}
			
			// Create a new population
			Individual[] populationR = new Individual[Config.POPULATION_SIZE_GA_ROUTING];
			
			// Generate the new population with that module placement map and random tuple and virtual machine routing maps
			for (int j = 0; j < Config.POPULATION_SIZE_GA_ROUTING; j++) {
				int[][] tupleRoutingMap = Solution.generateRandomTupleRouting(this, modulePlacementMap, getNumberOfNodes(), getNumberOfDependencies());
				int[][] migrationRoutingMap = Solution.generateRandomMigrationRouting(this, modulePlacementMap, getNumberOfNodes(), getNumberOfModules());
				populationR[j] = new Individual(this, new Solution(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap));
			}
			
			int convergenceIter = 0;
			int generation = 0;
			Solution bestSolutionR = null;
		
			while (generation <= Config.MAX_ITER_ROUTING_GA) {
				// Sort the array based on its value (ascending order)
	    		Arrays.sort(populationR);
	    		
	    		Solution prevBestSolution = null;
				if(bestSolutionR != null) prevBestSolution = new Solution(this, bestSolutionR);
				
				// Save the best value for that module placement
	    		bestSolutionR = Solution.checkBestSolution(this, populationR[0].getChromosome(), bestSolutionR, -1);
	    		
	    		// Check the convergence error
	    		if(Solution.checkConvergence(prevBestSolution, bestSolutionR)) {
	    			convergenceIter++;
	    			
	    			// If it found the same (or similar) solution a given number of times in a row break the loop
					if(convergenceIter == Config.MAX_ITER_ROUTING_CONVERGENCE_GA) break;
				}else
	    			convergenceIter = 0;
	    		
	    		// If the generation counter is above the defined maximum break the loop
	    		if(generation > Config.MAX_ITER_PLACEMENT_GA) break;
	    		
	    		// Otherwise generate new offsprings for new generation
		        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA_ROUTING];
		        
		        // Copy 10% of the fittest individuals to the next generation
		        for(int z = 0; z < FITTEST_ROUTING; z++)
		        	newGeneration[z] = populationR[z];
		        
		        // From 50% of fittest population, individuals will mate to produce offspring
		        for(int z = FITTEST_ROUTING; z < Config.POPULATION_SIZE_GA_ROUTING; z++) {
		        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA_ROUTING*0.5));
		        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA_ROUTING*0.5));
		        	
		        	int[][] childTupleRoutingMap = populationR[r1].mateTupleRouting(populationR[r2]);
		        	int[][] childMigrationRoutingMap = populationR[r1].mateMigrationRouting(populationR[r2]);
		        	
		        	// Create the new individual
		        	newGeneration[z] = new Individual(this, new Solution(this, modulePlacementMap, childTupleRoutingMap, childMigrationRoutingMap));
		        }
		        
		        // Set the current generation's population
		        for(int z = 0; z < Config.POPULATION_SIZE_GA_ROUTING; z++)
		        	populationR[z] = newGeneration[z];
		        
		        // Increments the number of iterations for the whole genetic algorithm
		        iteration++;
		        
		        // Increments the generation for the tuple and virtual machine routing maps
		        generation++;
			}
			
			// After running the genetic algorithm for the tuple and virtual machine routing maps (that module placement),
			// copy the best one to the final population
			population[i] = populationR[0];
		}
		
		return population;
	}
	
	/**
	 * Verifies whether resources are exceeded in a given node for a given placement matrix.
	 * 
	 * @param modulePlacementMap the current binary module placement map
	 * @param node the node to verify
	 * @return true if its resources are being exceeded. 0, otherwise
	 */
	private boolean checkResourcesExceeded(final int[][] modulePlacementMap) {
		for(int i = 0; i < getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalStrg = 0;
			
			for(int j = 0; j < getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * getmMips()[j];
				totalRam += modulePlacementMap[i][j] * getmRam()[j];
				totalStrg += modulePlacementMap[i][j] * getmStrg()[j];
			}
			
			if(totalMips > getfMips()[i] * Config.MIPS_PERCENTAGE_UTIL) return true;
			if(totalRam > getfRam()[i] * Config.MEM_PERCENTAGE_UTIL) return true;
			if(totalStrg > getfStrg()[i] * Config.STRG_PERCENTAGE_UTIL) return true;
		}
		
		return false;
	}
	
}
