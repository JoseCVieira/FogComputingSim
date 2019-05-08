package org.fog.placement.algorithms.overall.GA;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

public class GA extends Algorithm {
	private int iteration = 0;
	private static final int FITTEST = (int)((10*Config.POPULATION_SIZE)/100);  // 10% of fittest population
	
	public GA(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		Individual[] population = new Individual[Config.POPULATION_SIZE];
		
		double bestValue = Constants.MIN_SOLUTION;
		int convergenceIter = 0;
		int generation = 1;
		
		long start = System.currentTimeMillis();
		
	    for (int i = 0; i < Config.POPULATION_SIZE; i++)
	    	population[i] = new Individual(this, new Job(Job.generateRandomPlacement(this, NR_NODES, NR_MODULES)));
	    
	    while (generation <= Config.MAX_ITER_PLACEMENT) {
	    	population = GARouting(population);
	    	
	    	Arrays.sort(population);
	    	
	    	double iterBest = population[0].getFitness();
    		if(bestValue >= iterBest) {
    			if(bestValue - iterBest <= Constants.EPSILON) {
    				if(++convergenceIter == Config.MAX_ITER_PLACEMENT_CONVERGENCE)
    					break;
    			}else
        			convergenceIter = 0;
    			
    			if(bestValue > iterBest) {
	    			bestValue = iterBest;
	    			valueIterMap.put(iteration, bestValue);
	    			System.out.println("iteration: " + iteration + " bestValue: " + bestValue);
    			}
    		}
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE];
	        
	        for(int i = 0; i < FITTEST; i++)
	        	newGeneration[i] = population[i];
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = FITTEST; i < Config.POPULATION_SIZE; i++) {
	        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE*0.5));
	        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE*0.5));
	        	newGeneration[i] = new Individual(this, new Job(population[r1].matePlacement(population[r2])));
	        }
	        
	        for(int i = 0; i < Config.POPULATION_SIZE; i++) 
	        	population[i] = newGeneration[i];
	        
	        generation++;
	    }
	    
	    long finish = System.currentTimeMillis();
	    elapsedTime = finish - start;
	    
	    int[][] bestModulePlacement = population[0].getChromosome().getModulePlacementMap();
		int[][] bestRoutingMap = population[0].getChromosome().getRoutingMap();
		Job solution = new Job(this, bestModulePlacement, bestRoutingMap);
	    
	    if(Config.PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, solution);
		
		return solution;
	}
	
	public Individual[] GARouting(Individual[] population) {
		int generation = 1;
		double bestValue = Constants.MIN_SOLUTION;
		int convergenceIter = 0;
		
		for (int i = 0; i < Config.POPULATION_SIZE; i++) {
			int[][] modulePlacementMap = population[i].getChromosome().getModulePlacementMap();
			Individual[] populationR = new Individual[Config.POPULATION_SIZE];
			
			int start = 1;
			if(population[i].getChromosome().getRoutingMap() == null) {
				start = 0;
			}else {
				populationR[0] = population[i];
			}
			
			for (int j = start; j < Config.POPULATION_SIZE; j++) {
				int[][] routingMap = Job.generateRandomRouting(this, modulePlacementMap, NR_NODES);
				populationR[j] = new Individual(this, new Job(this, modulePlacementMap, routingMap));
			}
			
			while (generation <= Config.MAX_ITER_ROUTING) {
	    		Arrays.sort(populationR);
	    		
	    		double iterBest = populationR[0].getFitness();
	    		if(bestValue >= iterBest) {
	    			if(bestValue - iterBest <= Constants.EPSILON) {
	    				if(++convergenceIter == Config.MAX_ITER_ROUTING_CONVERGENCE)
	    					break;
	    			}else
		    			convergenceIter = 0;
	    				
	    			if(bestValue > iterBest)
	    				bestValue = iterBest;
	    		}
	    		
		        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE];
		        
		        for(int z = 0; z < FITTEST; z++)
		        	newGeneration[z] = populationR[z];
		        
		        for(int z = FITTEST; z < Config.POPULATION_SIZE; z++) {
		        	int r1 = new Random().nextInt((int) (Config.POPULATION_SIZE*0.5));
		        	int r2 = new Random().nextInt((int) (Config.POPULATION_SIZE*0.5));
		        	newGeneration[z] = populationR[r1].mateRouting(populationR[r2]);
		        }
		        
		        for(int z = 0; z < Config.POPULATION_SIZE; z++) 
		        	populationR[z] = newGeneration[z];
		        
		        //System.out.println(populationR[0]);
		        
		        iteration++;
		        generation++;
			}
			population[i] = populationR[0];
		}
		
		return population;
	}
	
}