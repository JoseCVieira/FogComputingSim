package org.fog.placement.algorithm.overall.ga;

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
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;

public class GeneticAlgorithm extends Algorithm {
	private static final int FITTEST = (int)((10*Config.POPULATION_SIZE_GA)/100);  // 10% of fittest population
	
	private Job bestSolution = null;
	private double bestCost = Constants.REFERENCE_COST;
	private int iteration = 0;
	
	public GeneticAlgorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		Individual[] population = new Individual[Config.POPULATION_SIZE_GA];
		
		int convergenceIter = 0;
		int generation = 1;
		
		long start = System.currentTimeMillis();
		
	    for (int i = 0; i < Config.POPULATION_SIZE_GA; i++)
	    	population[i] = new Individual(this, new Job(Job.generateRandomPlacement(this, NR_NODES, NR_MODULES)));
	    
	    while (generation <= Config.MAX_ITER_PLACEMENT_GA) {
	    	population = GARouting(population);
	    	
	    	Arrays.sort(population);
	    	
	    	double iterBest = population[0].getFitness();
			
    		if(Math.abs(bestCost - iterBest) <= Config.CONVERGENCE_ERROR) {
				if(++convergenceIter == Config.MAX_ITER_PLACEMENT_CONVERGENCE_GA)
					generation = Config.MAX_ITER_PLACEMENT_GA + 1;
			}else
    			convergenceIter = 0;
    		
    		if(bestCost > iterBest) {
    			bestSolution = new Job(population[0].getChromosome());
    			bestCost = iterBest;
				
				valueIterMap.put(iteration, bestCost);
    			
				if(Config.PRINT_BEST_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestCost);
    		}
    		
    		if(generation > Config.MAX_ITER_PLACEMENT_GA) break;
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA];
	        
	        for(int i = 0; i < FITTEST; i++) {
	        	newGeneration[i] = new Individual(this, new Job(population[i].getChromosome().getModulePlacementMap()));
	        }
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = FITTEST; i < Config.POPULATION_SIZE_GA; i++) {
	        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
	        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
	        	newGeneration[i] = new Individual(this, new Job(population[r1].matePlacement(population[r2])));
	        }
	        
	        for(int i = 0; i < Config.POPULATION_SIZE_GA; i++) 
	        	population[i] = newGeneration[i];
	        
	        generation++;
	    }
	    
	    long finish = System.currentTimeMillis();
	    elapsedTime = finish - start;
	    
	    if(Config.PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, bestSolution);
		
		return bestSolution;
	}
	
	public Individual[] GARouting(Individual[] population) {
		
		for (int i = 0; i < Config.POPULATION_SIZE_GA; i++) {
			int[][] modulePlacementMap = population[i].getChromosome().getModulePlacementMap();
			Individual[] populationR = new Individual[Config.POPULATION_SIZE_GA];
			
			for (int j = 0; j < Config.POPULATION_SIZE_GA; j++) {
				int[][] routingMap = Job.generateRandomTupleRouting(this, modulePlacementMap, NR_NODES);
				populationR[j] = new Individual(this, new Job(this, modulePlacementMap, routingMap));
			}
			
			int generation = 1;
			double bestValue = Constants.REFERENCE_COST;
			int convergenceIter = 0;
			
			while (generation <= Config.MAX_ITER_ROUTING_GA) {
	    		Arrays.sort(populationR);
	    		
	    		double iterBest = populationR[0].getFitness();
    			
	    		if(Math.abs(bestValue - iterBest) <= Constants.EPSILON) {
    				if(++convergenceIter == Config.MAX_ITER_ROUTING_CONVERGENCE_GA) {
    					generation = Config.MAX_ITER_PLACEMENT_GA + 1;
    				}
    			}else
	    			convergenceIter = 0;
	    		
	    		if(bestValue > iterBest) {
    				bestValue = iterBest;
	    		}
	    		
	    		if(generation > Config.MAX_ITER_PLACEMENT_GA) break;
	    		
		        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE_GA];
		        
		        for(int z = 0; z < FITTEST; z++)
		        	newGeneration[z] = populationR[z];
		        
		        for(int z = FITTEST; z < Config.POPULATION_SIZE_GA; z++) {
		        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
		        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE_GA*0.5));
		        	newGeneration[z] = populationR[r1].mateRouting(populationR[r2]);
		        }
		        
		        for(int z = 0; z < Config.POPULATION_SIZE_GA; z++) 
		        	populationR[z] = newGeneration[z];
		        
		        iteration++;
		        generation++;
			}
			
			population[i] = populationR[0];
		}
		
		return population;
	}
	
}
