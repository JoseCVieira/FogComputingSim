package org.fog.placement.algorithms.placement.GA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Chromosome {
	private double[][] modulePlacementMap;
	private double[][] routingMap;
	
	public Chromosome(double[][] modulePlacementMap, double[][] routingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.routingMap = routingMap;
	}
	
	public Chromosome(GA ga, int nrFogNodes, int nrModules) {
		this.modulePlacementMap = createModulePlacementMap(ga, nrFogNodes, nrModules);
		this.routingMap = createRoutingMap(ga, nrFogNodes-1);
	}
	
	double[][] createModulePlacementMap(GA ga, int nrFogNodes, int nrModules) {
		double[][] modulePlacementMap = new double[nrFogNodes][nrModules];
		double[][] possibleDeployment = ga.getPossibleDeployment();
		
		for(int i = 0; i < nrModules; i++) {
			List<Integer> validValues = new ArrayList<Integer>();
			
			for(int j = 0; j < nrFogNodes; j++)
				if(possibleDeployment[j][i] == 1)
					validValues.add(j);
			
			modulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
		}
		
        return modulePlacementMap;
	}
	
	double[][] createRoutingMap(GA ga, int nrConnections) {
		List<Integer> initialNodes = new ArrayList<Integer>();
		List<Integer> finalNodes = new ArrayList<Integer>();
		
		for(int i = 0; i < ga.getmDependencyMap().length; i++) {
			for(int j = 0; j < ga.getmDependencyMap()[0].length; j++) {
				if(ga.getmDependencyMap()[i][j] != 0) {
					initialNodes.add(findModulePlacement(modulePlacementMap, i));
					finalNodes.add(findModulePlacement(modulePlacementMap, j));
				}
			}
		}
		
		double[][] routingMap = new double[initialNodes.size()][nrConnections];
		
		for(int i  = 0; i < initialNodes.size(); i++) {
			for(int j = 0; j < nrConnections; j++) {
				if(j == 0)
					routingMap[i][j] = initialNodes.get(i);
				else if(j == nrConnections -1)
					routingMap[i][j] = finalNodes.get(i);
				else {
					List<Integer> validValues = new ArrayList<Integer>();
					
					for(int z = 0; z < nrConnections + 1; z++)
						if(ga.getfLatencyMap()[(int) routingMap[i][j-1]][z] < Double.MAX_VALUE)
							validValues.add(z);
							
					routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				}
			}
		}
		
		return routingMap;
	}
	
	private int findModulePlacement(double[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
	public double[][] getModulePlacementMap() {
		return modulePlacementMap;
	}
	
	public double[][] getRoutingMap() {
		return routingMap;
	}
	
}
