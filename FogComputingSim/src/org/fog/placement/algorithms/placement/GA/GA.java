package org.fog.placement.algorithms.placement.GA;

import java.util.Arrays;
import java.util.Random;

public class GA {
	
	public static final String GENES = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ 1234567890, .-;:_!#%&/()=?@${[]}";
	public static final String TARGET = "Teste Genetic Algorithm";
	private static final int POPULATION_SIZE = 100;
	
	public static void main(String[] args) {
	    // current generation
		int generation = 1;
		boolean found = false;
		Individual[] population = new Individual[POPULATION_SIZE];
	  
	    // create initial population
	    for (int i = 0; i < POPULATION_SIZE; i++) {
	    	String chromosome = Individual.create_chromosome();
	    	Individual individual = new Individual(chromosome);
	    	population[i] = individual;
	    }
	    	  
	    while (!found) {	    	
	    	// sort the population in increasing order of fitness score    	
    		Arrays.sort(population);
	  
	        // we have reached to the target and break the loop
	        if(population[0].fitness <= 0) {
	            found = true;
	            continue;
	        }
	  
	        // otherwise generate new offsprings for new generation
	        Individual[] new_generation = new Individual[POPULATION_SIZE];
	  
	        // perform Elitism, that mean 10% of fittest population goes to the next generation
	        int start = (int)((10*POPULATION_SIZE)/100);
	        int mid = (int)((50*POPULATION_SIZE)/100);
	        int stop = (int)((90*POPULATION_SIZE)/100);
	        
	        for(int i = 0; i < start; i++)
	        	new_generation[i] = population[i];
	        
	        // from 50% of fittest population, Individuals will mate to produce offspring
	        for(int i = start; i < (start+stop); i++) {
	        	int r1 = new Random().nextInt(mid);
	        	int r2 = new Random().nextInt(mid);    			
	            new_generation[i] = population[r1].mate(population[r2]);
	        }
	        
	        for(int i = 0; i < POPULATION_SIZE; i++) 
	        	population[i] = new_generation[i];
	        
	        System.out.println("Generation: " + generation + "\tString: " + population[0].chromosome + "\tFitness: " + population[0].fitness);
	        		
	        generation++;
	    }
	  
	    System.out.println("\n---- ---- END ---- ----");
	    System.out.println("Generation: " + generation + "\tString: " + population[0].chromosome + "\tFitness: " + population[0].fitness);	
	}
}