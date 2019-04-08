package org.fog.placement.algorithms.placement.GA;

import java.util.Random;

public class Individual implements Comparable<Individual>{
	
	String chromosome;
	int fitness;
	
	// class representing individual in population 
	Individual(String chromosome) {
		this.chromosome = chromosome;
		this.fitness = cal_fitness();
	}

	private static char mutated_genes() {
		// create random genes for mutation
		int r = new Random().nextInt(GA.GENES.length());
		return GA.GENES.charAt(r);
	}
	
	static String create_chromosome() {
		// create chromosome or string of genes
        int gnome_len = GA.TARGET.length();
        
        char[] m_genes = new char[gnome_len];
        for (int i = 0; i < gnome_len; i++)
        	m_genes[i] = mutated_genes();
        return String.valueOf(m_genes);
	}
	
	// perform mating and produce new offspring
	public Individual mate(Individual par) {
        // chromosome for offspring
		int gnome_len = GA.TARGET.length();
		char[] child_chromosome = new char[gnome_len];
		
		for (int i = 0; i < gnome_len; i++) {
			// random probability
        	float prob = new Random().nextFloat();
        	
        	// if prob is less than 0.45, insert gene from parent 1
            if (prob < 0.45)
                child_chromosome[i] = this.chromosome.charAt(i);
            // if prob is between 0.45 and 0.90, insert gene from parent 2
            else if (prob < 0.90)
            	child_chromosome[i] = par.chromosome.charAt(i);
            // otherwise insert random gene(mutate), for maintaining diversity
            else
            	child_chromosome[i] = mutated_genes();
		}
  
        // create new Individual(offspring) using generated chromosome for offspring
        return new Individual(String.valueOf(child_chromosome));
	}
	
	int cal_fitness() {
		// calculate fittness score, it is the number of characters in string which differ from target string
		int fitness = 0;
		
		for (int i = 0; i < GA.TARGET.length(); i++)
			if (this.chromosome.charAt(i) != GA.TARGET.charAt(i))
				fitness++;
		
		return fitness;
	}

	@Override
	public int compareTo(Individual individual) {
		return Integer.compare(this.fitness, individual.fitness);
	}

	@Override
	public String toString() {
		return "Individual [chromosome=" + chromosome + ", fitness=" + fitness + "]";
	}
	
}