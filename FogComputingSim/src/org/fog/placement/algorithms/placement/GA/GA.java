package org.fog.placement.algorithms.placement.GA;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.AlgorithmUtils;

public class GA extends Algorithm {
	private static final int POPULATION_SIZE = 2;
	private static final double AGREED_BOUNDARY = 0.0;
	private static final int MAX_ITER = 1;
	
	public GA(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
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
	    	double[][] chromosome = Individual.createChromosome(this, NR_FOG_NODES, NR_MODULES);
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
	        
	        //System.out.println("Generation: " + generation + "\n" + population[0]);
	        		
	        generation++;
	    }
	  
	    if(PRINT_DETAILS) {
	    	System.out.println("\n*******************************************************");
			System.out.println("\t\tALGORITHM OUTPUT (generation = " + generation + " Cost = " +
					population[0].getFitness() + "):");
			System.out.println("*******************************************************\n");
			
			final String[][] table = new String[getfName().length+1][getmName().length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < getmName().length; i++)
				table[0][i+1] = getmName()[i];
			
			for(int i = 0; i < getfName().length; i++) {
				table[i+1][0] = getfName()[i];
				
				for(int j = 0; j < getmName().length; j++)
					table[i+1][j+1] = Double.toString(population[0].getChromosome()[i][j]);
			}
			
			String repeated = AlgorithmUtils.repeate(getmName().length, "%17s");
			
			for (final Object[] row : table)
			    System.out.format("%23s" + repeated + "\n", row);
	    }
	    
	    System.exit(0);
	    return null;
	}
	
}