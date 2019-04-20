package org.fog.placement.algorithms.placement.GA;

import java.util.Random;

import org.fog.placement.algorithms.placement.AlgorithmUtils;

public class Individual implements Comparable<Individual> {
	
	private GA ga;
	private double[][] chromosome;
	private double fitness;
	
	Individual(GA ga, double[][] chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = calculateFitness();
	}
	
	static double[][] createChromosome(GA ga, int nrFogNodes, int nrModules) {
		double[][] chromosome = new double[nrFogNodes][nrModules];
		
		for(int i = 0; i < nrModules; i++) {
			int result;
			if((result = ga.moduleHasMandatoryPositioning(i)) != -1) {
				chromosome[result][i] = 1;
				continue;
			}else
				chromosome[new Random().nextInt(nrFogNodes)][i] = 1;
		}
		
        return chromosome;
	}
	
	// perform mating and produce new offspring
	public Individual mate(Individual par) {
		int nrFogNodes = chromosome.length;
		int nrModules = chromosome[0].length;
		
		double[][] childChromosome = new double[nrFogNodes][nrModules];
		
		for(int i = 0; i < nrModules; i++) {
			int result;
			if((result = ga.moduleHasMandatoryPositioning(i)) != -1) {
				childChromosome[result][i] = 1;
				continue;
			}
			
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
		double fitness;
		
		if(!isPossibleCombination())
			return Double.MAX_VALUE;

		fitness = calculateOperationalCost();
		fitness += calculateEnergyConsumption();
		fitness += calculateTransmittingCost();
		
		/*fitness += calculateProcessingLatency();
		fitness += calculateTransmittingLatency();*/
		
		return fitness;
	}
	
	private boolean isPossibleCombination() {
		
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
			}
			
			if(totalMips > ga.getfMips()[i] || totalRam > ga.getfRam()[i] ||
					totalMem > ga.getfMem()[i] || totalBw > ga.getfBw()[i])
				return false;
		}
		return true;
	}
	
	private double calculateOperationalCost() {		
		double cost = 0;
		for(int i = 0; i < chromosome.length; i++) {
			for(int j = 0; j < chromosome[i].length; j++) {
				
				cost += chromosome[i][j]*(
						ga.getfMipsPrice()[i] * ga.getmMips()[j] +
						ga.getfRamPrice()[i] * ga.getmRam()[j] +
						ga.getfMemPrice()[i] * ga.getmMem()[j] +
						ga.getfBwPrice()[i] * ga.getmBw()[j]);
			}
		}
		return cost;
	}
	
	private double calculateEnergyConsumption() {
		double energy = 0;
		
		for(int i = 0; i < chromosome.length; i++) {
			double totalMips = 0;
			
			for(int j = 0; j < chromosome[i].length; j++)
				totalMips += chromosome[i][j] * ga.getmMips()[j];
			
			energy += (ga.getfBusyPw()[i]-ga.getfIdlePw()[i])*(totalMips/ga.getfMips()[i]);
		}
		
		return energy;
	}
	
	private double calculateProcessingLatency() {
		double latency = 0;
		
		for(int i = 0; i < chromosome.length; i++) {
			int nrModules = 0;
			double totalMips = 0;
			
			for(int j = 0; j < chromosome[i].length; j++) {
				nrModules += chromosome[i][j];
				totalMips += chromosome[i][j] * ga.getmMips()[j];
			}
			
			double unnusedMips = ga.getfMips()[i] - totalMips;
			double mipsPie = 1; // irrelevant value
			if(nrModules != 0 && unnusedMips != 0)
				mipsPie = unnusedMips/nrModules;
			
			for(int j = 0; j < chromosome[i].length; j++)
				latency += chromosome[i][j] * ga.getmCpuSize()[j] / (ga.getmMips()[j] + mipsPie);
		}
		
		return latency;
	}
	
	private double calculateTransmittingCost() {
		double latency = 0;
		
		if(fitness != Double.MAX_VALUE) {
			double[][] aux1 = null;
			double[][] aux2 = null;
			
			//link latency
			try {
				aux1 = AlgorithmUtils.multiplyMatrices(chromosome, ga.getDependencyMap());				
				aux1 = AlgorithmUtils.multiplyMatrices(aux1, AlgorithmUtils.transposeMatrix(chromosome));
				aux1 = AlgorithmUtils.dotProductMatrices(aux1, ga.getLatencyMap());				
				latency = AlgorithmUtils.sumAllElementsMatrix(aux1);				
			} catch (Exception e) {
				System.err.println(e);
				System.err.println("FogComputingSim will terminate abruptally.\n");
				System.exit(0);
			}
			
			//network latency (tuple size / bandwidth available)
			try {
				aux1 = AlgorithmUtils.multiplyMatrices(chromosome, ga.getNwSizeMap());
				aux1 = AlgorithmUtils.multiplyMatrices(aux1, AlgorithmUtils.transposeMatrix(chromosome));
				
				for(int i = 0; i < ga.getfName().length-1; i++) {
					aux2 = AlgorithmUtils.dotDivisionMatrices(aux1, ga.getBandwidthMap(i));
					latency += AlgorithmUtils.sumAllElementsMatrix(aux2);
				}
			} catch (Exception e) {
				System.err.println(e);
				System.err.println("FogComputingSim will terminate abruptally.\n");
				System.exit(0);
			}
		}
		
		return latency;
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
	
	double[][] getChromosome() {
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