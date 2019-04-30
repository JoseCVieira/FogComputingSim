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
		this.cost = CostFunction.computeCost(this, algorithm);
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
		this.cost = CostFunction.computeCost(this, algorithm);
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
		
		int nrConnections = nrFogNodes;
		
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
					
					for(int z = 0; z < nrFogNodes; z++)
						if(algorithm.getfLatencyMap()[(int) routingMap[i][j-1]][z] < Double.MAX_VALUE)
							validValues.add(z);
							
					routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return new Job(algorithm, modulePlacementMap, routingMap);
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
