package org.fog.placement.algorithms.placement.GA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.placement.algorithms.placement.AlgorithmUtils;

public class Individual implements Comparable<Individual> {	
	private GA ga;
	private Chromosome chromosome;
	private double fitness;
	
	Individual(GA ga, Chromosome chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = calculateFitness();
	}
	
	// perform mating and produce new offspring
	public Individual mate(Individual par) {
		double[][] modulePlacementMap = chromosome.getModulePlacementMap();
		double[][] parModulePlacementMap = par.getChromosome().getModulePlacementMap();
		double[][] childModulePlacementMap = new double[modulePlacementMap.length][modulePlacementMap[0].length];
		
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
		
		double[][] routingMap = chromosome.getRoutingMap();
		double[][] parRoutingMap = par.getChromosome().getRoutingMap();		
		double[][] childRoutingMap = new double[routingMap.length][routingMap[0].length];
		
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
		
        Chromosome childChromosome = new Chromosome(childModulePlacementMap, childRoutingMap);
        return new Individual(ga, childChromosome);
	}
	
	private double calculateFitness() {
		double[][] modulePlacementMap = chromosome.getModulePlacementMap();
		
		if(isPossibleCombination(modulePlacementMap) == false) return Double.MAX_VALUE - 1;
			
		double fitness = /*isPossibleCombination(modulePlacementMap) == false ? Short.MAX_VALUE :*/ 0;

		fitness += calculateOperationalCost(modulePlacementMap);
		fitness += calculateEnergyConsumption(modulePlacementMap);
		//System.out.println("fitness1: " + fitness);
		
		//fitness += calculateProcessingLatency();
		fitness += calculateTransmittingCost(modulePlacementMap);
		//System.out.println("fitness2: " + fitness + "\n");
		
		//System.exit(0);
		
		return fitness;
	}
	
	private boolean isPossibleCombination(double[][] modulePlacementMap) {		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			double totalBw = 0;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				totalMips += modulePlacementMap[i][j] * ga.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * ga.getmRam()[j];
				totalMem += modulePlacementMap[i][j] * ga.getmMem()[j];
				totalBw += modulePlacementMap[i][j] * ga.getmBw()[j];
			}
			
			if(totalMips > ga.getfMips()[i] || totalRam > ga.getfRam()[i] ||
					totalMem > ga.getfMem()[i] || totalBw > ga.getfBw()[i])
				return false;
		}
		return true;
	}
	
	private double calculateOperationalCost(double[][] modulePlacementMap) {
		double cost = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				
				cost += modulePlacementMap[i][j]*(
						ga.getfMipsPrice()[i] * ga.getmMips()[j] +
						ga.getfRamPrice()[i] * ga.getmRam()[j] +
						ga.getfMemPrice()[i] * ga.getmMem()[j]);
			}
		}
		
		return cost;
	}
	
	private double calculateEnergyConsumption(double[][] modulePlacementMap) {
		double energy = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			double totalMips = 0;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++)
				totalMips += modulePlacementMap[i][j] * ga.getmMips()[j];
			
			energy += (ga.getfBusyPw()[i]-ga.getfIdlePw()[i])*(totalMips/ga.getfMips()[i]);
		}
		
		return energy;
	}
	
	private double calculateProcessingLatency(double[][] modulePlacementMap) {
		double latency = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			int nrModules = 0;
			double totalMips = 0;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				nrModules += modulePlacementMap[i][j];
				totalMips += modulePlacementMap[i][j] * ga.getmMips()[j];
			}
			
			double unnusedMips = ga.getfMips()[i] - totalMips;
			double mipsPie = 1; // TODO irrelevant value
			if(nrModules != 0 && unnusedMips != 0)
				mipsPie = unnusedMips/nrModules;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++)
				latency += modulePlacementMap[i][j] * ga.getmCpuSize()[j] / (ga.getmMips()[j] + mipsPie);
		}
		
		return latency;
	}
	
	private double calculateTransmittingCost(double[][] modulePlacementMap) {
		double[][] routingMap = chromosome.getRoutingMap();
		double[][] bwMap = new double[ga.getfName().length][ga.getfName().length];
		
		double transmittingCost = 0;
		
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		for(int i = 0; i < ga.getmDependencyMap().length; i++) {
			for (int j = 0; j < ga.getmDependencyMap()[0].length; j++) {
				if(ga.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		for(int i = 0; i < routingMap.length; i++) {
			for (int j = 1; j < routingMap[0].length; j++) {
				int from = (int) routingMap[i][j-1];
				int to = (int) routingMap[i][j];
				
				double dependencies = ga.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = ga.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				bwMap[from][to] += bwNeeded;
				transmittingCost += ga.getfLatencyMap()[from][to]*dependencies;
				transmittingCost += ga.getfBwPrice()[from]*bwNeeded;
				
				if(ga.getfBandwidthMap()[from][to] == 0)
					transmittingCost += Short.MAX_VALUE;
				else
					transmittingCost += bwNeeded/ga.getfBandwidthMap()[from][to];
			}
		}
		
		for(int i = 0; i < bwMap.length; i++)
			for (int j = 0; j < bwMap[0].length; j++)
				if(bwMap[i][j] > ga.getfBandwidthMap()[i][j])
					transmittingCost += Short.MAX_VALUE;
		
		return transmittingCost;
	}
	
	
	private int findModulePlacement(double[][] chromosome, int colomn) {
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
	
	Chromosome getChromosome() {
		return chromosome;
	}
	
	@Override
	public String toString() {
		double[][] modulePlacementMap = chromosome.getModulePlacementMap();
		double[][] routingMap = chromosome.getRoutingMap();
		
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