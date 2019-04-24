package org.fog.placement.algorithms.placement.GA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.placement.algorithms.placement.AlgorithmUtils;
import org.fog.placement.algorithms.placement.Job;

public class Individual implements Comparable<Individual> {	
	private GA ga;
	private Job chromosome;
	private double fitness;
	
	Individual(GA ga, Job chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = chromosome.getCost();
	}
	
	// perform mating and produce new offspring
	public Individual mate(Individual par) {
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] parModulePlacementMap = par.getChromosome().getModulePlacementMap();
		int[][] childModulePlacementMap = new int[modulePlacementMap.length][modulePlacementMap[0].length];
		
		for(int i = 0; i < modulePlacementMap[0].length; i++) {
        	float prob = new Random().nextFloat();
        	
        	// if prob is less than 0.45, insert gene from parent 1
            if (prob < 0.45)
            	childModulePlacementMap[findModulePlacement(modulePlacementMap, i)][i] = 1;
            // if prob is between 0.45 and 0.90, insert gene from parent 2
            else if (prob < 0.90)
            	childModulePlacementMap[findModulePlacement(parModulePlacementMap, i)][i] = 1;
            // otherwise insert random gene(mutate), for maintaining diversity
            else {
            	double[][] possibleDeployment = ga.getPossibleDeployment();
        		
    			List<Integer> validValues = new ArrayList<Integer>();
    			
    			for(int j = 0; j < possibleDeployment.length; j++)
    				if(possibleDeployment[j][i] == 1)
    					validValues.add(j);
    			
    			childModulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
            }
            	
		}
		
		int[][] routingMap = chromosome.getRoutingMap();
		int[][] parRoutingMap = par.getChromosome().getRoutingMap();		
		int[][] childRoutingMap = new int[routingMap.length][routingMap[0].length];
		
		List<Integer> initialNodes = new ArrayList<Integer>();
		List<Integer> finalNodes = new ArrayList<Integer>();
		
		for(int i = 0; i < ga.getmDependencyMap().length; i++) {
			for(int j = 0; j < ga.getmDependencyMap()[0].length; j++) {
				if(ga.getmDependencyMap()[i][j] != 0) {
					initialNodes.add(findModulePlacement(childModulePlacementMap, i));
					finalNodes.add(findModulePlacement(childModulePlacementMap, j));
				}
			}
		}
		
		for (int i = 0; i < routingMap.length; i++) {
			for (int j = 0; j < routingMap[0].length; j++) {
				if(j == 0)
            		childRoutingMap[i][j] = initialNodes.get(i);
				else if(j == routingMap[i].length - 1)
					childRoutingMap[i][j] = finalNodes.get(i);
				else {
					float prob = new Random().nextFloat();
		            if (prob < 0.45)
		            	childRoutingMap[i][j] = routingMap[i][j];
		            else if (prob < 0.90)
		            	childRoutingMap[i][j] = parRoutingMap[i][j];
		            else {
						List<Integer> validValues = new ArrayList<Integer>();
						
						for(int z = 0; z < routingMap[0].length + 1; z++)
							if(ga.getfLatencyMap()[(int) routingMap[i][j-1]][z] < Double.MAX_VALUE)
								validValues.add(z);
						
						childRoutingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
		            }
				}
			}
		}
		
		Job childChromosome = new Job(ga, childModulePlacementMap, childRoutingMap);
        return new Individual(ga, childChromosome);
	}
	
	private int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
	@Override
	public int compareTo(Individual individual) {
		return Double.compare(this.getFitness(), individual.getFitness());
	}

	double getFitness() {
		return fitness;
	}
	
	Job getChromosome() {
		return chromosome;
	}
	
	@Override
	public String toString() {
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] routingMap = chromosome.getRoutingMap();
		
		String toReturn = "\nChromosome: \n";
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			for(int j = 0; j < modulePlacementMap[i].length; j++)
				toReturn += modulePlacementMap[i][j] + " ";
			toReturn += "\n";
		}
		toReturn += "\n";
		
		int iter = 0;
		for(int i = 0; i < ga.getmDependencyMap().length; i++) {
			for(int j = 0; j < ga.getmDependencyMap()[i].length; j++) {
				if(ga.getmDependencyMap()[i][j] != 0) {
					toReturn += AlgorithmUtils.centerString(40, "[" + ga.getmName()[i] + " -> " + ga.getmName()[j] + "]:");
					
					for(int z = 0; z < routingMap[iter].length; z++) {
						if(z < routingMap[iter].length - 1)
							toReturn += AlgorithmUtils.centerString(10, ga.getfName()[(int) routingMap[iter][z]]) + " -> ";
						else
							toReturn += AlgorithmUtils.centerString(10, ga.getfName()[(int) routingMap[iter][z]]) + "\n";
					}
					iter++;
				}
			}
		}
		
		toReturn += "\nAnd it has fitness= " + getFitness() + "";
		
		return toReturn;
	}
	
}