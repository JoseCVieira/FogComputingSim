package org.fog.placement.algorithms.placement.GA;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.AlgorithmConstants;
import org.fog.placement.algorithms.placement.AlgorithmUtils;
import org.fog.placement.algorithms.placement.Job;

public class GA extends Algorithm {
	public GA(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		
		// current generation
		int generation = 1;
		boolean found = false;
		Individual[] population = new Individual[AlgorithmConstants.POPULATION_SIZE];
		 
	    // create initial population
	    for (int i = 0; i < AlgorithmConstants.POPULATION_SIZE; i++)
	    	population[i] = new Individual(this, Job.generateRandomJob(this, NR_NODES, NR_MODULES));
	    
	    while (!found && generation <= AlgorithmConstants.MAX_ITER) {
	    	// sort the population in increasing order of fitness score    	
    		Arrays.sort(population);
    		
	        // we have reached to the target and break the loop
	        if(population[0].getFitness() <= AlgorithmConstants.AGREED_BOUNDARY) {
	            found = true;
	            continue;
	        }
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[AlgorithmConstants.POPULATION_SIZE];
	  
	        // perform Elitism, that mean 10% of fittest population goes to the next generation
	        int start = (int)((10*AlgorithmConstants.POPULATION_SIZE)/100);
	        int mid = (int)((50*AlgorithmConstants.POPULATION_SIZE)/100);
	        int stop = (int)((90*AlgorithmConstants.POPULATION_SIZE)/100);
	        
	        for(int i = 0; i < start; i++)
	        	newGeneration[i] = population[i];
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = start; i < (start+stop); i++) {
	        	int r1 = new Random().nextInt(mid);
	        	int r2 = new Random().nextInt(mid);    			
	        	newGeneration[i] = population[r1].mate(population[r2]);
	        }
	        
	        for(int i = 0; i < AlgorithmConstants.POPULATION_SIZE; i++) 
	        	population[i] = newGeneration[i];
	        
	        //System.out.println(population[0]);
	        		
	        generation++;
	    }
	    
	    if(population[0].getFitness() == Double.MAX_VALUE)
	    	return null;
	    
	    int[][] modulePlacementMap = population[0].getChromosome().getModulePlacementMap();
    	int[][] routingMap =  population[0].getChromosome().getRoutingMap();
    	
	    Job solution = new Job(this, modulePlacementMap, routingMap);
	    
	    if(PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, solution);
		
		return solution;
	}
	
}