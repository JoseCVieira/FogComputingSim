package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;
import org.fog.placement.algorithm.routing.DijkstraAlgorithm;
import org.fog.placement.algorithm.routing.Edge;
import org.fog.placement.algorithm.routing.Graph;
import org.fog.placement.algorithm.routing.Vertex;
import org.fog.utils.Util;

public class Job implements Comparable<Job> {
	private int[][] modulePlacementMap;
	private int[][] tupleRoutingMap;		// Tuple routing map
	private int[][] migrationRoutingMap;	// Migration routing map
	private double cost;
	private boolean isValid;
	
	public Job(Job anotherJob) {
		this.modulePlacementMap = Util.copy(anotherJob.getModulePlacementMap());
		this.tupleRoutingMap = Util.copy(anotherJob.getTupleRoutingMap());
		this.setMigrationRoutingMap(Util.copy(anotherJob.getMigrationRoutingMap()));
		this.cost = anotherJob.getCost();
		this.setValid(anotherJob.isValid());
	}
	
	public Job(int[][] modulePlacementMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.cost = -1;
	}
	
	public Job(Algorithm algorithm, int[][] modulePlacementMap, int[][] tupleRoutingMap, int[][] migrationRoutingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		CostFunction.computeCost(this, algorithm);
	}
	
	public Job(Algorithm algorithm, int[][] modulePlacementMap, int[][][] tupleRoutingVectorMap,
			int[][][] migrationRoutingVectorMap,int[][] oldPlacement) {
		int nrDependencies = tupleRoutingVectorMap.length;
		int nrNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int[][] tupleRoutingMap = new int[nrDependencies][nrNodes];
		int[][] migrationRoutingMap = new int[nrModules][nrNodes];
		
		// Tuple routing map
		int iter = 0;
		for(int i = 0; i < nrModules; i++) {
			for (int j = 0; j < nrModules; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					for(int z = 0; z < nrNodes; z++)
						if(modulePlacementMap[z][i] == 1)
							tupleRoutingMap[iter++][0] = z;
				}
			}
		}
		
		for(int i = 0; i < nrDependencies; i++) {
			iter = 1;
			int from = tupleRoutingMap[i][0];
			
			boolean found = true;
			while(found) {
				found = false;
				
				for(int j = 0; j < nrNodes; j++) {
					if(tupleRoutingVectorMap[i][from][j] == 1) {
						tupleRoutingMap[i][iter++] = j;
						from = j;
						j = nrNodes;
						found = true;
					}
				}
			}
			
			for(int j = iter; j < nrNodes; j++) {
				tupleRoutingMap[i][j] = from;
			}
		}
		
		// Migration routing map
		if(oldPlacement != null) {			
			for(int i = 0; i < nrModules; i++) {
				iter = 1;
				
				for(int j = 0; j < nrNodes; j++) {
					if(oldPlacement[j][i] == 1) {
						migrationRoutingMap[i][0] = j;
					}
				}
				
				int from = migrationRoutingMap[i][0];
				
				int tmp = 0;
				for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
					if(algorithm.getPossibleDeployment()[j][i] != 0) {
						tmp++;
					}
				}
				
				// If they have fixed positions its not necessary to verify its routing VM map, once it will not be migrated
				if(tmp != 1) {
					boolean found = true;
					while(found) {
						found = false;
						
						for(int j = 0; j < nrNodes; j++) {
							if(migrationRoutingVectorMap[i][from][j] == 1) {
								migrationRoutingMap[i][iter++] = j;
								from = j;
								j = nrNodes;
								found = true;
							}
						}
					}
				}
				
				for(int j = iter; j < nrNodes; j++) {
					migrationRoutingMap[i][j] = from;
				}
			}
		}
		
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		CostFunction.computeCost(this, algorithm);
	}
	
	public static Job generateRandomJob(Algorithm algorithm, int nrFogNodes, int nrModules) {
		int[][] modulePlacementMap = generateRandomPlacement(algorithm, nrFogNodes, nrModules);
		int[][] tupleRoutingMap = generateRandomRouting(algorithm, modulePlacementMap, nrFogNodes);
		return new Job(algorithm, modulePlacementMap, tupleRoutingMap);
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

	public int[][] getTupleRoutingMap() {
		return tupleRoutingMap;
	}
	
	public double getCost() {
		return cost;
	}
	
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}
	
	public int[][] getMigrationRoutingMap() {
		return migrationRoutingMap;
	}

	public void setMigrationRoutingMap(int[][] migrationRoutingMap) {
		this.migrationRoutingMap = migrationRoutingMap;
	}
	
	public static int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
	@Override
	public int compareTo(Job job) {
		return Double.compare(this.cost, job.getCost());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.deepHashCode(modulePlacementMap);
		result = prime * result + Arrays.deepHashCode(tupleRoutingMap);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Job other = (Job) obj;
		if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
			return false;
		if (!Arrays.deepEquals(modulePlacementMap, other.modulePlacementMap))
			return false;
		if (!Arrays.deepEquals(tupleRoutingMap, other.tupleRoutingMap))
			return false;
		return true;
	}
	
}
