package org.fog.placement.algorithms.placement.GA;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.AlgorithmUtils;
import org.fog.placement.algorithms.placement.Job;

public class GA extends Algorithm {
	private static final int POPULATION_SIZE = 100;
	private static final double AGREED_BOUNDARY = 0.0;
	private static final int MAX_ITER = 3000;
	
	public GA(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		
		// current generation
		int generation = 1;
		boolean found = false;
		Individual[] population = new Individual[POPULATION_SIZE];
		 
	    // create initial population
	    for (int i = 0; i < POPULATION_SIZE; i++)
	    	population[i] = new Individual(this, generateChromosome(NR_NODES, NR_MODULES));
	    
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
	    
	    int[][] modulePlacementMap = population[0].getChromosome().getModulePlacementMap();
    	int[][] routingMap =  population[0].getChromosome().getRoutingMap();
    	
	    Job solution = new Job(this, modulePlacementMap, routingMap);
	    
	    if(PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, solution);
		
		return solution;
	}
	
	private Job generateChromosome(int nrFogNodes, int nrModules){
		int[][] modulePlacementMap = new int[nrFogNodes][nrModules];
		double[][] possibleDeployment = getPossibleDeployment();
		
		for(int i = 0; i < nrModules; i++) {
			List<Integer> validValues = new ArrayList<Integer>();
			
			for(int j = 0; j < nrFogNodes; j++)
				if(possibleDeployment[j][i] == 1)
					validValues.add(j);
			
			modulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
		}
		
		int nrConnections = nrFogNodes-1;
		
		List<Integer> initialNodes = new ArrayList<Integer>();
		List<Integer> finalNodes = new ArrayList<Integer>();
		
		for(int i = 0; i < getmDependencyMap().length; i++) {
			for(int j = 0; j < getmDependencyMap()[0].length; j++) {
				if(getmDependencyMap()[i][j] != 0) {
					initialNodes.add(findModulePlacement(modulePlacementMap, i));
					finalNodes.add(findModulePlacement(modulePlacementMap, j));
				}
			}
		}
		
		int[][] routingMap = new int[initialNodes.size()][nrConnections];
		
		for(int i  = 0; i < initialNodes.size(); i++) {
			for(int j = 0; j < nrConnections; j++) {
				if(j == 0)
					routingMap[i][j] = initialNodes.get(i);
				else if(j == nrConnections -1)
					routingMap[i][j] = finalNodes.get(i);
				else {
					List<Integer> validValues = new ArrayList<Integer>();
					
					for(int z = 0; z < nrConnections + 1; z++)
						if(getfLatencyMap()[(int) routingMap[i][j-1]][z] < Double.MAX_VALUE)
							validValues.add(z);
							
					routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return new Job(this, modulePlacementMap, routingMap);
	}
	
	private int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
}