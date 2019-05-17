package org.fog.placement.algorithms.overall;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.placement.algorithms.routing.DijkstraAlgorithm;
import org.fog.placement.algorithms.routing.Edge;
import org.fog.placement.algorithms.routing.Graph;
import org.fog.placement.algorithms.routing.Vertex;
import org.fog.utils.Util;

public class Job {
	private int[][] modulePlacementMap;
	private int[][] routingMap;
	private double cost;
	
	public Job(Job anotherJob) {
		this.modulePlacementMap = Util.copy(anotherJob.getModulePlacementMap());
		this.routingMap = Util.copy(anotherJob.getRoutingMap());
		this.cost = anotherJob.getCost();
	}
	
	public Job(int[][] modulePlacementMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.cost = -1;
	}
	
	public Job(int[][] modulePlacementMap, int[][] routingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.routingMap = routingMap;
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
		int[][] modulePlacementMap = generateRandomPlacement(algorithm, nrFogNodes, nrModules);
		int[][] routingMap = generateRandomRouting(algorithm, modulePlacementMap, nrFogNodes);
		return new Job(algorithm, modulePlacementMap, routingMap);
	}
	
	public static int[][] generateRandomPlacement(Algorithm algorithm, int nrFogNodes, int nrModules) {
		int[][] modulePlacementMap = new int[nrFogNodes][nrModules];
		double[][] possibleDeployment = algorithm.getPossibleDeployment();
		
		for(int i = 0; i < nrModules; i++) {
			List<Integer> validValues = new ArrayList<Integer>();
			
			for(int j = 0; j < nrFogNodes; j++)
				if(possibleDeployment[j][i] == 1)
					validValues.add(j);
			
			modulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
		}
		
		return modulePlacementMap;
	}
	
	public static int[][] generateRandomRouting(Algorithm algorithm, int[][] modulePlacementMap, int nrFogNodes) {
		List<Integer> initialNodes = new ArrayList<Integer>();
		List<Integer> finalNodes = new ArrayList<Integer>();
		List<Vertex> nodes = new ArrayList<Vertex>();
		List<Edge> edges = new ArrayList<Edge>();
		
		for(int i = 0; i < algorithm.getmDependencyMap().length; i++) {
			for(int j = 0; j < algorithm.getmDependencyMap()[0].length; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialNodes.add(findModulePlacement(modulePlacementMap, i));
					finalNodes.add(findModulePlacement(modulePlacementMap, j));
				}
			}
		}
		
		for(int i  = 0; i < nrFogNodes; i++) {
			nodes.add(new Vertex("Node=" + i));
		}
		
		for(int i  = 0; i < nrFogNodes; i++) {
			for(int j  = 0; j < nrFogNodes; j++) {
				if(algorithm.getfLatencyMap()[i][j] < Constants.INF) {
					 edges.add(new Edge(nodes.get(i), nodes.get(j), 1.0));
				}
			}
        }
		
		int[][] routingMap = new int[initialNodes.size()][nrFogNodes];
		
		for(int i  = 0; i < initialNodes.size(); i++) {
			routingMap[i][0] = initialNodes.get(i);
			routingMap[i][nrFogNodes - 1] = finalNodes.get(i);
			
			Graph graph = new Graph(nodes, edges);
	        DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
	        
			for(int j = 1; j < nrFogNodes - 1; j++) {
				List<Integer> validValues = new ArrayList<Integer>();
				
				for(int z = 0; z < nrFogNodes; z++) {
					if(algorithm.getfLatencyMap()[routingMap[i][j-1]][z] < Constants.INF) {
						
						dijkstra.execute(nodes.get(z));
						LinkedList<Vertex> path = dijkstra.getPath(nodes.get(finalNodes.get(i)));
						
						
						// If path is null, means that both start and finish refer to the same node, thus it can be added
				        if((path != null && path.size() <= nrFogNodes - j) || path == null) {
				        	validValues.add(z);
				        }
					}
				}
						
				routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
			}
		}
		return routingMap;
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
	
	public static int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
}
