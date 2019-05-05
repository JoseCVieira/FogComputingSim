package org.fog.placement.algorithms.overall.GA;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.overall.Algorithm;
import org.fog.placement.algorithms.overall.Job;
import org.fog.placement.algorithms.overall.util.AlgorithmUtils;

public class GA extends Algorithm {
	private int iteration = 0;
	
	public GA(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		int generation = 1;
		Individual[] population = new Individual[Config.POPULATION_SIZE];
		double bestValue = Config.INF;
		int convergenceIter = 0;
		
		long start = System.currentTimeMillis();
		
	    for (int i = 0; i < Config.POPULATION_SIZE; i++)
	    	population[i] = new Individual(this, new Job(Job.generateRandomPlacement(this, NR_NODES, NR_MODULES)));
	    
	    while (generation <= Config.MAX_ITER) {
	    	population = GARouting(population);
	    	
	    	Arrays.sort(population);
	    	
	    	//System.out.println(population[0]);
	    	
	    	double iterBest = population[0].getFitness();
    		if((bestValue < Config.INF && bestValue >= iterBest) || (bestValue == Config.INF && bestValue > iterBest)) {
    			if(bestValue - iterBest <= Config.ERROR_STEP_CONVERGENCE && ++convergenceIter == 3)
					break;
    			
    			bestValue = iterBest;
    			valueIterMap.put(iteration, bestValue);
    			System.out.println("bestValue: " + bestValue);
    		}
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE];
	  
	        // perform Elitism, that mean 10% of fittest population goes to the next generation
	        int fittest = (int)((10*Config.POPULATION_SIZE)/100);
	        
	        for(int i = 0; i < fittest; i++)
	        	newGeneration[i] = population[i];
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = fittest; i < Config.POPULATION_SIZE; i++) {
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
	    
	    if(population[0].getFitness() == Config.INF)
	    	return null;
	    
	    int[][] modulePlacementMap = population[0].getChromosome().getModulePlacementMap();
    	int[][] routingMap = population[0].getChromosome().getRoutingMap();
    	
	    Job solution = new Job(this, modulePlacementMap, routingMap);
	    
	    if(Config.PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, solution);
		
		return solution;
	}
	
	public Individual[] GARouting(Individual[] population) {
		int generation = 1;
		double bestValue = Config.INF;
		int convergenceIter = 0;
		
		for (int i = 0; i < Config.POPULATION_SIZE; i++) {
			int[][] modulePlacementMap = population[i].getChromosome().getModulePlacementMap();
			Individual[] populationR = new Individual[Config.POPULATION_SIZE];
			
			for (int j = 0; j < Config.POPULATION_SIZE; j++) {
				int[][] routingMap = Job.generateRandomRouting(this, modulePlacementMap, NR_NODES, NR_MODULES);
				populationR[j] = new Individual(this, new Job(this, modulePlacementMap, routingMap));
			}
			
			while (generation <= Config.MAX_ITER_ROUTING) {
				// sort the population in increasing order of fitness score
	    		Arrays.sort(populationR);
	    		
	    		double iterBest = populationR[0].getFitness();
	    		if((bestValue < Config.INF && bestValue >= iterBest) || (bestValue == Config.INF && bestValue > iterBest)) {
	    			if(bestValue - iterBest <= Config.ERROR_STEP_CONVERGENCE && ++convergenceIter == 3)
	    					break;
	    			bestValue = iterBest;
	    		}
	    		
	    		// otherwise generate new offsprings for new generation
		        Individual[] newGeneration = new Individual[Config.POPULATION_SIZE];
				
		        // perform Elitism, that mean 10% of fittest population goes to the next generation
		        int fittest = (int)((10*Config.POPULATION_SIZE)/100);
		        
		        for(int z = 0; z < fittest; z++)
		        	newGeneration[z] = populationR[z];
		        
		        // from 50% of fittest population, Individuals will mate to produce offspring
		        for(int z = fittest; z < Config.POPULATION_SIZE; z++) {
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