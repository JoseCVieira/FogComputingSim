package org.fog.placement.algorithm.ga;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.CostFunction;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.SingleObjectiveCostFunction;

/**
 * Class in which defines and executes the genetic algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class GeneticAlgorithm extends Algorithm {
	/** 10% of the population */
	private static final int FITTEST = (int)(Config.POPULATION_SIZE_GA*0.1);
	
	/** Best solution found by the algorithm */
	private Job bestSolution;
	
	/** Best cost found by the algorithm */
	private double bestCost;
	
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
	public Job execute() {
		iteration = 0;
		bestCost = Constants.REFERENCE_COST;
		bestSolution = null;
		
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
		Individual[] population = new Individual[Config.POPULATION_SIZE_GA];
		
		// Generate an initial population with random module placements
	    for (int i = 0; i < Config.POPULATION_SIZE_GA; i++)
	    	population[i] = new Individual(this, new Job(Job.generateRandomPlacement(this, getNumberOfNodes(), getNumberOfModules())));
	    
	    while (generation <= Config.MAX_ITER_PLACEMENT_GA) {
	    	// Solve both tuple and virtual machine migration routing tables
	    	population = GARouting(population);
	    	
	    	// Sort the array based on its value (ascending order)
	    	Arrays.sort(population);
	    	
	    	// Get the best value found in the current generation
	    	double iterGen = population[0].getFitness();
			
	    	// Check the convergence error
    		if(Math.abs(bestCost - iterGen) <= Config.CONVERGENCE_ERROR) {
    			// If it found the same (or similar) solution a given number of times in a row break the loop
				if(++convergenceIter == Config.MAX_ITER_PLACEMENT_CONVERGENCE_GA)
					generation = Config.MAX_ITER_PLACEMENT_GA + 1;
			}else
    			convergenceIter = 0;
    		
    		// If it's a better solution, save and add it to the value/iteration map
    		if(bestCost > iterGen) {
    			bestSolution = new Job(population[0].getChromosome());
    			bestCost = iterGen;
				
				getValueIterMap().put(iteration, bestCost);
    			
				if(Config.PRINT_ALGORITHM_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestCost);
    		}
    		
    		// If the generation counter is above the defined maximum break the loop
    		if(generation > Config.MAX_ITER_PLACEMENT_GA) break;
	  
	        // Otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA];
	        
	        // Copy 10% of the fittest individuals to the next generation
	        for(int i = 0; i < FITTEST; i++) {
	        	newGeneration[i] = new Individual(this, new Job(population[i].getChromosome().getModulePlacementMap()));
	        }
	        
	        // From 50% of fittest population, individuals will mate to produce offspring
	        for(int i = FITTEST; i < Config.POPULATION_SIZE_GA; i++) {
	        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
	        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
	        	
	        	// Create the new individual
	        	newGeneration[i] = new Individual(this, new Job(population[r1].matePlacement(population[r2])));
	        }
	        
	        // Set the current generation's population
	        for(int i = 0; i < Config.POPULATION_SIZE_GA; i++) {
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
		CostFunction cf = new SingleObjectiveCostFunction();
		
		// For each individual with a given module placement map
		for (int i = 0; i < Config.POPULATION_SIZE_GA; i++) {
			
			// Get it's module placement map
			int[][] modulePlacementMap = population[i].getChromosome().getModulePlacementMap();
			
			// Create a new population
			Individual[] populationR = new Individual[Config.POPULATION_SIZE_GA];
			
			// Generate the new population with that module placement map and random tuple and virtual machine routing maps
			for (int j = 0; j < Config.POPULATION_SIZE_GA; j++) {
				int[][] tupleRoutingMap = Job.generateRandomTupleRouting(this, modulePlacementMap, getNumberOfNodes(), getNumberOfDependencies());
				int[][] migrationRoutingMap = Job.generateRandomMigrationRouting(this, modulePlacementMap, getNumberOfNodes(), getNumberOfModules());
				populationR[j] = new Individual(this, new Job(this, cf, modulePlacementMap, tupleRoutingMap, migrationRoutingMap));
			}
			
			double bestValue = Constants.REFERENCE_COST;
			int convergenceIter = 0;
			int generation = 0;
			
			while (generation <= Config.MAX_ITER_ROUTING_GA) {
				// Sort the array based on its value (ascending order)
	    		Arrays.sort(populationR);
	    		
	    		// Get the best value found in the current generation
		    	double iterGen = populationR[0].getFitness();
    			
		    	// Check the convergence error
	    		if(Math.abs(bestValue - iterGen) <= Constants.EPSILON) {
	    			// If it found the same (or similar) solution a given number of times in a row break the loop
    				if(++convergenceIter == Config.MAX_ITER_ROUTING_CONVERGENCE_GA)
    					generation = Config.MAX_ITER_PLACEMENT_GA + 1;
    			}else
	    			convergenceIter = 0;
	    		
	    		// Save the best value for that module placement
	    		if(bestValue > iterGen) {
    				bestValue = iterGen;
	    		}
	    		
	    		// If the generation counter is above the defined maximum break the loop
	    		if(generation > Config.MAX_ITER_PLACEMENT_GA) break;
	    		
	    		// Otherwise generate new offsprings for new generation
		        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA];
		        
		        // Copy 10% of the fittest individuals to the next generation
		        for(int z = 0; z < FITTEST; z++)
		        	newGeneration[z] = populationR[z];
		        
		        // From 50% of fittest population, individuals will mate to produce offspring
		        for(int z = FITTEST; z < Config.POPULATION_SIZE_GA; z++) {
		        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
		        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
		        	
		        	int[][] childTupleRoutingMap = populationR[r1].mateTupleRouting(populationR[r2]);
		        	int[][] childMigrationRoutingMap = populationR[r1].mateMigrationRouting(populationR[r2]);
		        	
		        	// Create the new individual
		        	newGeneration[z] = new Individual(this, new Job(this, cf, modulePlacementMap, childTupleRoutingMap, childMigrationRoutingMap));
		        }
		        
		        // Set the current generation's population
		        for(int z = 0; z < Config.POPULATION_SIZE_GA; z++)
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
	
}
