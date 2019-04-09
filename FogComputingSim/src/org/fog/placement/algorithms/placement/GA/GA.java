package org.fog.placement.algorithms.placement.GA;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.algorithms.placement.Algorithm;

public class GA extends Algorithm {
	private static final int POPULATION_SIZE = 100;
	private static final double AGREED_BOUNDARY = 0.0;
	private static final int MAX_ITER = 5000;
	
	public GA(List<FogDevice> fogDevices, List<Application> applications) {
		super(fogDevices, applications);
	}
	
	@Override
	public Map<String, List<String>> execute() {
		final int NR_FOG_NODES = getfMips().length;
		final int NR_MODULES = getmMips().length;
		
		// current generation
		int generation = 1;
		boolean found = false;
		Individual[] population = new Individual[POPULATION_SIZE];
	  
	    // create initial population
	    for (int i = 0; i < POPULATION_SIZE; i++) {
	    	int[][] chromosome = Individual.createChromosome(NR_FOG_NODES, NR_MODULES);
	    	Individual individual = new Individual(this, chromosome);
	    	population[i] = individual;
	    }
	    	  
	    while (!found && generation <= MAX_ITER) {
	    	// sort the population in increasing order of fitness score    	
    		Arrays.sort(population);
    		
	        // we have reached to the target and break the loop
	        if(population[0].getFitness() <= AGREED_BOUNDARY) {
	            found = true;
	            continue;
	        }
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] newGeneration = new Individual[POPULATION_SIZE];
	  
	        // perform Elitism, that mean 10% of fittest population goes to the next generation
	        int start = (int)((10*POPULATION_SIZE)/100);
	        int mid = (int)((50*POPULATION_SIZE)/100);
	        int stop = (int)((90*POPULATION_SIZE)/100);
	        
	        for(int i = 0; i < start; i++)
	        	newGeneration[i] = population[i];
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = start; i < (start+stop); i++) {
	        	int r1 = new Random().nextInt(mid);
	        	int r2 = new Random().nextInt(mid);    			
	        	newGeneration[i] = population[r1].mate(population[r2]);
	        }
	        
	        for(int i = 0; i < POPULATION_SIZE; i++) 
	        	population[i] = newGeneration[i];
	        
	        System.out.println("Generation: " + generation + "\n" + population[0]);
	        		
	        generation++;
	    }
	  
	    System.out.println("\n---- ---- END ---- ----");
	    System.out.println("Generation: " + generation + "\n" + population[0]);
	    
	    System.exit(0);
	    return null;
	}
	
}