package org.fog.placement.algorithms.placement;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.utils.Util;

public class Job {
	private int[][] modulePlacementMap;
	private int[][] routingMap;
	private double cost;
	
	public Job(Job anotherJob) {
		this.modulePlacementMap = Util.copy(anotherJob.getModulePlacementMap());
		this.routingMap =  Util.copy(anotherJob.getRoutingMap());
		this.cost = anotherJob.getCost();
	}
	
	public Job(Algorithm algorithm, int[][] modulePlacementMap, int[][] routingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.routingMap = routingMap;
		this.cost = computeCost(algorithm);
	}
	
	public Job(Algorithm algorithm, int[][] modulePlacementMap, int[][][] routingVectorMap) {
		int nrDependencies = routingVectorMap.length;
		int nrNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int[][] routingMap = new int[nrDependencies][nrNodes];
		
		int iter = 0;
		for(int i = 0; i < nrModules; i++) {
			for (int j = 0; j < nrModules; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					for(int z = 0; z < nrNodes; z++)
						if(modulePlacementMap[z][i] == 1)
							routingMap[iter++][0] = z;
				}
			}
		}
		
		for(int i = 0; i < nrDependencies; i++) {
			iter = 1;
			int from = routingMap[i][0];
			
			boolean found = true;
			while(found) {
				found = false;
				
				for(int j = 0; j < nrNodes; j++) {
					if(routingVectorMap[i][from][j] == 1) {
						routingMap[i][iter++] = j;
						from = j;
						j = nrNodes;
						found = true;
					}
				}
			}
			
			for(int j = iter; j < nrNodes; j++) {
				routingMap[i][j] = from;
			}
		}
		
		this.modulePlacementMap = modulePlacementMap;
		this.routingMap = routingMap;
		this.cost = computeCost(algorithm);
	}
	
	public static Job generateRandomJob(Algorithm algorithm, int nrFogNodes, int nrModules) {
		int[][] modulePlacementMap = new int[nrFogNodes][nrModules];
		double[][] possibleDeployment = algorithm.getPossibleDeployment();
		
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
		
		for(int i = 0; i < algorithm.getmDependencyMap().length; i++) {
			for(int j = 0; j < algorithm.getmDependencyMap()[0].length; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
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
						if(algorithm.getfLatencyMap()[(int) routingMap[i][j-1]][z] < Double.MAX_VALUE)
							validValues.add(z);
							
					routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return new Job(algorithm, modulePlacementMap, routingMap);
	}
	
	public double computeCost(Algorithm algorithm) {
		if(isPossibleCombination(algorithm) == false)
			return Double.MAX_VALUE - 1;
		
		double cost = calculateOperationalCost(algorithm);
		cost += calculateEnergyConsumption(algorithm);
		cost += calculateProcessingCost(algorithm);
		cost += calculateTransmittingCost(algorithm);
		
		return cost;
	}
	
	private boolean isPossibleCombination(Algorithm algorithm) {
		for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
			int sum = 0;
			for(int i  = 0; i < algorithm.getNumberOfNodes(); i++)
				if(modulePlacementMap[i][j] == 1)
					sum++;
			
			if(sum != 1)
				return false;
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			double totalBw = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
				totalRam += modulePlacementMap[i][j] * algorithm.getmRam()[j];
				totalMem += modulePlacementMap[i][j] * algorithm.getmMem()[j];
				totalBw += modulePlacementMap[i][j] * algorithm.getmBw()[j];
			}
			
			if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] ||
					totalMem > algorithm.getfMem()[i] || totalBw > algorithm.getfBw()[i])
				return false;
		}
		return true;
	}
	
	private double calculateOperationalCost(Algorithm algorithm) {
		double cost = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				
				cost += modulePlacementMap[i][j]*(
						algorithm.getfMipsPrice()[i] * algorithm.getmMips()[j] +
						algorithm.getfRamPrice()[i] * algorithm.getmRam()[j] +
						algorithm.getfMemPrice()[i] * algorithm.getmMem()[j]);
			}
		}
		
		return cost;
	}
	
	private double calculateEnergyConsumption(Algorithm algorithm) {
		double energy = 0;
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			
			for(int j = 0; j < algorithm.getNumberOfModules(); j++)
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
			
			energy += (algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i])*(totalMips/algorithm.getfMips()[i]);
		}
		
		return energy;
	}
	
	private double calculateProcessingCost(Algorithm algorithm) {
		double cost = 0;
		
		for(int i = 0; i < modulePlacementMap.length; i++) {
			int nrModules = 0;
			double totalMips = 0;
			
			for(int j = 0; j < modulePlacementMap[i].length; j++) {
				nrModules += modulePlacementMap[i][j];
				totalMips += modulePlacementMap[i][j] * algorithm.getmMips()[j];
			}
			
			double unnusedMips = algorithm.getfMips()[i] - totalMips;
			cost += nrModules / (1E-9 + unnusedMips);
		}
		
		return cost;
	}
	
	private double calculateTransmittingCost(Algorithm algorithm) {
		double[][] bwMap = new double[algorithm.getNumberOfNodes()][algorithm.getNumberOfNodes()];
		
		double transmittingCost = 0;
		
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
			for (int j = 0; j < algorithm.getNumberOfModules(); j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		for(int i = 0; i < routingMap.length; i++) {
			for (int j = 1; j < routingMap[0].length; j++) {
				int from = (int) routingMap[i][j-1];
				int to = (int) routingMap[i][j];
				
				double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				bwMap[from][to] += bwNeeded;
				transmittingCost += algorithm.getfLatencyMap()[from][to]*dependencies;
				transmittingCost += algorithm.getfBwPrice()[from]*bwNeeded;
				
				if(algorithm.getfBandwidthMap()[from][to] == 0)
					transmittingCost += Short.MAX_VALUE;
				else
					transmittingCost += bwNeeded/algorithm.getfBandwidthMap()[from][to];
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++)
			for (int j = 0; j < algorithm.getNumberOfNodes(); j++)
				if(bwMap[i][j] > algorithm.getfBandwidthMap()[i][j])
					transmittingCost += Short.MAX_VALUE;
		
		return transmittingCost;
	}
	
	public int[][] getModulePlacementMap() {
		return modulePlacementMap;
	}

	public int[][] getRoutingMap() {
		return routingMap;
	}
	
	public double getCost() {
		return cost;
	}
	
	private static int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
}
