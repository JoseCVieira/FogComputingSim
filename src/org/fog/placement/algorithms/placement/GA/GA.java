package org.fog.placement.algorithms.placement.GA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
	private static final int POPULATION_SIZE = 100;
	private static final double AGREED_BOUNDARY = 0.0;
	private static final int MAX_ITER = 1000;
	
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
	    	Chromosome chromosome = new Chromosome(this, NR_FOG_NODES, NR_MODULES);
	    	population[i] = new Individual(this, chromosome);
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
	        
	        //System.out.println(population[0]);
	        		
	        generation++;
	    }
	    
	    if(population[0].getFitness() == Double.MAX_VALUE)
	    	return null;
	    
	    System.out.println(population[0]);
	  
	    if(PRINT_DETAILS) {
	    	System.out.println("\n*******************************************************");
			System.out.println("\t\tALGORITHM OUTPUT (generation = " + generation + " Cost = " +
					population[0].getFitness() + "):");
			System.out.println("*******************************************************\n");
			
			System.out.format(AlgorithmUtils.centerString(15, " "));
			for (int i = 0; i < getmName().length; i++)
				System.out.format(AlgorithmUtils.centerString(15, getmName()[i]));
			System.out.println();
			
			for (int i = 0; i < getfName().length; i++) {
				System.out.format(AlgorithmUtils.centerString(15, getfName()[i]));
				for (int j = 0; j < getmName().length; j++) {
					if(population[0].getChromosome().getModulePlacementMap()[i][j] != 0)
						System.out.format(AlgorithmUtils.centerString(15, Integer.toString((int)population[0].getChromosome().getModulePlacementMap()[i][j])));
					else
						System.out.format(AlgorithmUtils.centerString(15, "-"));
				}
				System.out.println();
			}
	    }
	    
	    Map<String, List<String>> resMap = new HashMap<>();
		
		for(int i = 0; i < NR_FOG_NODES; i++) {
			List<String> modules = new ArrayList<String>();
			
			for(int j = 0; j < NR_MODULES; j++)
				if(population[0].getChromosome().getModulePlacementMap()[i][j] == 1)
					modules.add(getmName()[j]);
			
			resMap.put(getfName()[i], modules);
		}
		
		return resMap;
	}
	
}