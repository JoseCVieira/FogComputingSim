package org.fog.placement.algorithms.placement.GA;

import java.util.Random;

public class Individual implements Comparable<Individual> {
	
	private GA ga;
	private int[][] chromosome;
	private double fitness;
	
	Individual(GA ga, int[][] chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = calculateFitness();
	}
	
	static int[][] createChromosome(int nrFogNodes, int nrModules) {
		int[][] chromosome = new int[nrFogNodes][nrModules];
		
		for(int i = 0; i < nrModules; i++)
			chromosome[new Random().nextInt(nrFogNodes)][i] = 1;
		
        return chromosome;
	}
	
	// perform mating and produce new offspring
	public Individual mate(Individual par) {
		int nrFogNodes = chromosome.length;
		int nrModules = chromosome[0].length;
		
		int[][] childChromosome = new int[nrFogNodes][nrModules];
		
		for(int i = 0; i < nrModules; i++) {
        	float prob = new Random().nextFloat();
        	
        	// if prob is less than 0.45, insert gene from parent 1
            if (prob < 0.45)
            	childChromosome[findModulePlacement(chromosome, i)][i] = 1;
            // if prob is between 0.45 and 0.90, insert gene from parent 2
            else if (prob < 0.90)
            	childChromosome[findModulePlacement(par.getChromosome(), i)][i] = 1;
            // otherwise insert random gene(mutate), for maintaining diversity
            else
            	childChromosome[new Random().nextInt(nrFogNodes)][i] = 1;
		}
  
        return new Individual(ga, childChromosome);
	}
	
	private double calculateFitness() {
		double fitness = 0, cost = 0, energy = 0, latTransmitting = 0, latProcessing = 0;
		
		for(int i = 0; i < chromosome.length; i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			double totalBw = 0;
			
			for(int j = 0; j < chromosome[i].length; j++) {
				totalMips += chromosome[i][j] * ga.getmMips()[j];
				totalRam += chromosome[i][j] * ga.getmRam()[j];
				totalMem += chromosome[i][j] * ga.getmMem()[j];
				totalBw += chromosome[i][j] * ga.getmBw()[j];
				
				cost += chromosome[i][j] * ga.getfMipsPrice()[i] * ga.getmMips()[j] +
						chromosome[i][j] * ga.getfRamPrice()[i] * ga.getmRam()[j] +
						chromosome[i][j] * ga.getfMemPrice()[i] * ga.getmMem()[j] +
						chromosome[i][j] * ga.getfBwPrice()[i] * ga.getmBw()[j];
				
				latTransmitting = 0;
				latProcessing = 0;
				
				fitness += cost + latTransmitting + latProcessing;
			}
			
			if(totalMips > ga.getfMips()[i] || totalRam > ga.getfRam()[i] ||
					totalMem > ga.getfMem()[i] || totalBw > ga.getfBw()[i]) {
				fitness = Double.MAX_VALUE;
				break;
			}
			
			energy = ga.getfPwModel()[i].getPower(totalMips/ga.getfMips()[i]);
			fitness += energy;
		}
		return fitness;
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
	
	int[][] getChromosome() {
		return chromosome;
	}
	
	@Override
	public String toString() {
		String toReturn = "Individual with chromosome: \n";
		
		for(int i = 0; i < chromosome.length; i++) {
			for(int j = 0; j < chromosome[i].length; j++)
				toReturn += chromosome[i][j] + " ";
			toReturn += "\n";
		}
		toReturn += "has fitness= " + getFitness() + ".";
		
		return toReturn;
	}
	
}