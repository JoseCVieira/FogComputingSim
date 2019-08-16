package org.fog.placement.algorithm.ga;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.placement.algorithm.Job;

/**
 * Class representing individuals used within the genetic algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Individual implements Comparable<Individual> {
	/** Object which holds all the information needed to run the optimization algorithm */
	private GeneticAlgorithm ga;
	
	/** Object which contains the problem solution for the individual */
	private Job chromosome;
	
	/** Result of the cost function */
	private double fitness;
	
	/**
	 * Creates a new individual with a given chromosome.
	 * 
	 * @param ga the object which holds all the information needed to run the optimization algorithm
	 * @param chromosome the problem solution
	 */
	Individual(GeneticAlgorithm ga, Job chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = chromosome.getCost();
	}
	
	/**
	 * Performs mating and produce new offspring.
	 * 
	 * @param par the individual which the mate will occur
	 * @return the module placement offspring
	 */
	public int[][] matePlacement(Individual par) {
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] parModulePlacementMap = par.getChromosome().getModulePlacementMap();
		int[][] childModulePlacementMap = new int[modulePlacementMap.length][modulePlacementMap[0].length];
		
		for(int i = 0; i < modulePlacementMap[0].length; i++) {
        	float prob = new Random().nextFloat();
        	
        	// If probability is less than 0.45, insert gene from one of the parents
            if (prob < 0.45)
            	childModulePlacementMap[Job.findModulePlacement(modulePlacementMap, i)][i] = 1;
            // If probability is between 0.45 and 0.90, insert gene from the other parent
            else if (prob < 0.90)
            	childModulePlacementMap[Job.findModulePlacement(parModulePlacementMap, i)][i] = 1;
            // Otherwise insert random gene(mutate), for maintaining diversity
            else {
            	double[][] possibleDeployment = ga.getPossibleDeployment();
        		
    			List<Integer> validValues = new ArrayList<Integer>();
    			
    			for(int j = 0; j < possibleDeployment.length; j++) {
    				if(possibleDeployment[j][i] == 1) {
    					validValues.add(j);
    				}
    			}
    			
    			childModulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
            }
		}
		
		return childModulePlacementMap;
	}
	
	/**
	 * Performs mating of tuple routing map and produce new offspring.
	 * 
	 * @param par the individual which the mate will occur
	 * @return the tuple routing map offspring
	 */
	public int[][] mateTupleRouting(Individual par) {
		int nrFogNodes = ga.getNumberOfNodes();
		int nrDependencies = ga.getNumberOfDependencies();
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] tupleRoutingMap = chromosome.getTupleRoutingMap();
		int[][] parTupleRoutingMap = par.getChromosome().getTupleRoutingMap();
		int[][] childTupleRoutingMap = new int[nrDependencies][nrFogNodes];
		
		for(int i = 0; i < nrDependencies; i++) {
			float prob = new Random().nextFloat();
			
			// If probability is less than 0.45, insert gene from one of the parents
			if (prob < 0.45)
				childTupleRoutingMap[i] = Arrays.copyOf(tupleRoutingMap[i], nrFogNodes);
			// If probability is between 0.45 and 0.90, insert gene from the other parent
			else if (prob < 0.9)
				childTupleRoutingMap[i] = Arrays.copyOf(parTupleRoutingMap[i], nrFogNodes);
			// Otherwise insert random gene(mutate), for maintaining diversity
			else {
				childTupleRoutingMap[i][0] = Job.findModulePlacement(modulePlacementMap, ga.getStartModDependency(i));
				childTupleRoutingMap[i][nrFogNodes-1] = Job.findModulePlacement(modulePlacementMap, ga.getFinalModDependency(i));
		        
				for(int j = 1; j < nrFogNodes - 1; j++) {
					// If its already the final node, then just fill the remain ones
					if(childTupleRoutingMap[i][j-1] == childTupleRoutingMap[i][nrFogNodes-1]) {
						for(; j < nrFogNodes - 1; j++) {
							childTupleRoutingMap[i][j] = childTupleRoutingMap[i][nrFogNodes-1];
						}
						break;
					}
					
					List<Integer> validValues = new ArrayList<Integer>();
					
					for(int z = 0; z < nrFogNodes; z++) {					
						if(ga.getfLatencyMap()[childTupleRoutingMap[i][j-1]][z] == Constants.INF) continue;
						if(!ga.isValidHop(z, childTupleRoutingMap[i][nrFogNodes-1], nrFogNodes - j)) continue;
						validValues.add(z);
					}
							
					childTupleRoutingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return childTupleRoutingMap;
	}
	
	/**
	 * Performs mating of virtual machine migration routing map and produce new offspring.
	 * 
	 * @param par the individual which the mate will occur
	 * @return the virtual machine migration routing map offspring
	 */
	public int[][] mateMigrationRouting(Individual par) {
		int nrFogNodes = ga.getNumberOfNodes();
		int nrModules = ga.getNumberOfModules();
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] currentPositionInt = ga.getCurrentPositionInt();
		int[][] migrationRoutingMap = chromosome.getMigrationRoutingMap();
		int[][] parMigrationRoutingMap = par.getChromosome().getMigrationRoutingMap();
		int[][] childMigrationRoutingMap = new int[nrModules][nrFogNodes];
		
		for (int i = 0; i < nrModules; i++) {
			float prob = new Random().nextFloat();
			
			// If probability is less than 0.45, insert gene from one of the parents
			if (prob < 0.45)
				childMigrationRoutingMap[i] = Arrays.copyOf(migrationRoutingMap[i], nrFogNodes);
			// If probability is between 0.45 and 0.90, insert gene from the other parent
			else if (prob < 0.9)
				childMigrationRoutingMap[i] = Arrays.copyOf(parMigrationRoutingMap[i], nrFogNodes);
			// Otherwise insert random gene(mutate), for maintaining diversity
			else {
				childMigrationRoutingMap[i][0] = Job.findModulePlacement(ga.isFirstOptimization() ? modulePlacementMap : currentPositionInt, i);
				childMigrationRoutingMap[i][nrFogNodes-1] = Job.findModulePlacement(modulePlacementMap, i);
					
				for(int j = 1; j < nrFogNodes - 1; j++) { // Routing hop index
					// If its already the final node, then just fill the remain ones
					if(childMigrationRoutingMap[i][j-1] == childMigrationRoutingMap[i][nrFogNodes-1]) {
						for(; j < nrFogNodes - 1; j++) {
							childMigrationRoutingMap[i][j] = childMigrationRoutingMap[i][nrFogNodes-1];
						}
						break;
					}
						
					List<Integer> validValues = new ArrayList<Integer>();
						
					for(int z = 0; z < nrFogNodes; z++) { // Node index
						if(ga.getfLatencyMap()[childMigrationRoutingMap[i][j-1]][z] == Constants.INF) continue;
						if(!ga.isValidHop(z, childMigrationRoutingMap[i][nrFogNodes-1], nrFogNodes - j)) continue;
						validValues.add(z);
					}
						
					childMigrationRoutingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return childMigrationRoutingMap;
	}
	
	/**
	 * Compares two individuals based on it's fitness value (used to sort arrays of individuals).
	 */
	@Override
	public int compareTo(Individual individual) {
		return Double.compare(this.getFitness(), individual.getFitness());
	}
	
	/**
	 * Gets the object which contains the problem solution for the individual.
	 * 
	 * @return the object which contains the problem solution for the individual
	 */
	Job getChromosome() {
		return chromosome;
	}
	
	/**
	 * Gets the result of the cost function.
	 * 
	 * @return the result of the cost function
	 */
	double getFitness() {
		return fitness;
	}
	
}