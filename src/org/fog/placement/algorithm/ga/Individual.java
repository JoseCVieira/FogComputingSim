package org.fog.placement.algorithm.overall.ga;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;
import org.fog.placement.algorithm.routing.DijkstraAlgorithm;
import org.fog.placement.algorithm.routing.Edge;
import org.fog.placement.algorithm.routing.Graph;
import org.fog.placement.algorithm.routing.Vertex;

public class Individual implements Comparable<Individual> {	
	private GeneticAlgorithm ga;
	private Job chromosome;
	private double fitness;
	
	Individual(GeneticAlgorithm ga, Job chromosome) {
		this.ga = ga;
		this.chromosome = chromosome;
		this.fitness = chromosome.getCost();
	}
	
	// perform mating and produce new offspring
	public int[][] matePlacement(Individual par) {
		int[][] modulePlacementMap = chromosome.getModulePlacementMap();
		int[][] parModulePlacementMap = par.getChromosome().getModulePlacementMap();
		int[][] childModulePlacementMap = new int[modulePlacementMap.length][modulePlacementMap[0].length];
		
		for(int i = 0; i < modulePlacementMap[0].length; i++) {
        	float prob = new Random().nextFloat();
        	
        	// if prob is less than 0.45, insert gene from parent 1
            if (prob < 0.45)
            	childModulePlacementMap[Job.findModulePlacement(modulePlacementMap, i)][i] = 1;
            // if prob is between 0.45 and 0.90, insert gene from parent 2
            else if (prob < 0.90)
            	childModulePlacementMap[Job.findModulePlacement(parModulePlacementMap, i)][i] = 1;
            // otherwise insert random gene(mutate), for maintaining diversity
            else {
            	double[][] possibleDeployment = ga.getPossibleDeployment();
        		
    			List<Integer> validValues = new ArrayList<Integer>();
    			
    			for(int j = 0; j < possibleDeployment.length; j++) {
    				if(possibleDeployment[j][i] == 1) {
    					validValues.add(j);
    				}
    			}
    			
    			childModulePlacementMap[validValues.get(new Random().nextInt(validValues.size()))][i] = 1;
            }
		}
		
		return childModulePlacementMap;
	}
	
	public Individual mateRouting(Individual par) {
		int[][] modulePlacementMap = getChromosome().getModulePlacementMap();
		int[][] currentPositionInt = ga.getCurrentPositionInt();
		
		int[][] tupleRoutingMap = chromosome.getTupleRoutingMap();
		int[][] parTupleRoutingMap = par.getChromosome().getTupleRoutingMap();
		int[][] migrationRoutingMap = chromosome.getMigrationRoutingMap();
		int[][] parMigrationRoutingMap = par.getChromosome().getMigrationRoutingMap();
		
		int[][] childTupleRoutingMap = new int[tupleRoutingMap.length][tupleRoutingMap[0].length];
		int[][] childMigrationRoutingMap = new int[migrationRoutingMap.length][migrationRoutingMap[0].length];
		
		List<Integer> tupleInitialNodes = new ArrayList<Integer>();
		List<Integer> tupleFinalNodes = new ArrayList<Integer>();
		List<Integer> migrationInitialNodes = new ArrayList<Integer>();
		List<Integer> migrationFinalNodes = new ArrayList<Integer>();
		List<Vertex> nodes = new ArrayList<Vertex>();
		List<Edge> edges = new ArrayList<Edge>();
		
		int nrFogNodes = ga.getNumberOfNodes();
		int nrModule = ga.getNumberOfModules();
		
		for(int i = 0; i < ga.getmDependencyMap().length; i++) {
			for(int j = 0; j < ga.getmDependencyMap()[0].length; j++) {
				if(ga.getmDependencyMap()[i][j] != 0) {
					tupleInitialNodes.add(Job.findModulePlacement(modulePlacementMap, i));
					tupleFinalNodes.add(Job.findModulePlacement(modulePlacementMap, j));
				}
			}
		}
		
		for(int i = 0; i < nrModule; i++) {
			if(ga.isFirstOptimization())
				migrationInitialNodes.add(Job.findModulePlacement(modulePlacementMap, i));
			else
				migrationInitialNodes.add(Job.findModulePlacement(currentPositionInt, i));
			migrationFinalNodes.add(Job.findModulePlacement(modulePlacementMap, i));
		}
		
		for(int i  = 0; i < nrFogNodes; i++) {
			nodes.add(new Vertex("Node=" + i));
		}
		
		for(int i  = 0; i < nrFogNodes; i++) {
			for(int j  = 0; j < nrFogNodes; j++) {
				if(ga.getfLatencyMap()[i][j] < Constants.INF) {
					 edges.add(new Edge(nodes.get(i), nodes.get(j), 1.0));
				}
			}
        }
		
		for(int i = 0; i < tupleRoutingMap.length; i++) {
			float prob = new Random().nextFloat();
			
			 if (prob < 0.45) {
				 for (int j = 0; j < ga.getNumberOfNodes(); j++) {
					 childTupleRoutingMap[i][j] = tupleRoutingMap[i][j];
				 }
			 }else if (prob < 0.9) {
				 for (int j = 0; j < ga.getNumberOfNodes(); j++) {
					 childTupleRoutingMap[i][j] = parTupleRoutingMap[i][j];
				 }
			 }else {
				 childTupleRoutingMap[i][0] = tupleInitialNodes.get(i);
				 childTupleRoutingMap[i][ga.getNumberOfNodes()-1] = tupleFinalNodes.get(i);
				 
				 Graph graph = new Graph(nodes, edges);
				 DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
				 dijkstra.execute(nodes.get(tupleFinalNodes.get(i)));
				
				 for(int j = 1; j < nrFogNodes - 1; j++) {
					 List<Integer> validValues = new ArrayList<Integer>();
					
					 for(int z = 0; z < nrFogNodes; z++) {
						 
						 // If there is a connection with the previous vertex
						 if(ga.getfLatencyMap()[childTupleRoutingMap[i][j-1]][z] < Constants.INF) {
							 
							 LinkedList<Vertex> path = dijkstra.getPath(nodes.get(z));
							 
							 // If is possible to connect with the final node
							 if(path != null && path.size() <= nrFogNodes - j) {
								 validValues.add(z);
							 }
							 
							 validValues.add(childTupleRoutingMap[i][j-1]);
						 }
					 }
					 
					 childTupleRoutingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				 }
			 }
		}
		
		
		
		for (int i = 0; i < migrationRoutingMap.length; i++) {
			float prob = new Random().nextFloat();
			
			 if (prob < 0.45) {
				 for (int j = 0; j < ga.getNumberOfNodes(); j++) {
					 childMigrationRoutingMap[i][j] = migrationRoutingMap[i][j];
				 }
			 }else if (prob < 0.9) {
				 for (int j = 0; j < ga.getNumberOfNodes(); j++) {
					 childMigrationRoutingMap[i][j] = parMigrationRoutingMap[i][j];
				 }
			 }else {
				 childMigrationRoutingMap[i][0] = migrationInitialNodes.get(i);
				 childMigrationRoutingMap[i][ga.getNumberOfNodes()-1] = migrationFinalNodes.get(i);
				 
				 Graph graph = new Graph(nodes, edges);
				 DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
				 dijkstra.execute(nodes.get(migrationFinalNodes.get(i)));
				
				 for(int j = 1; j < nrFogNodes - 1; j++) {
					 List<Integer> validValues = new ArrayList<Integer>();
					
					 for(int z = 0; z < nrFogNodes; z++) {
						 
						 // If there is a connection with the previous vertex
						 if(ga.getfLatencyMap()[childMigrationRoutingMap[i][j-1]][z] < Constants.INF) {
							 
							 LinkedList<Vertex> path = dijkstra.getPath(nodes.get(z));
							 
							 // If is possible to connect with the final node
							 if(path != null && path.size() <= nrFogNodes - j) {
								 validValues.add(z);
							 }
							 
							 validValues.add(childMigrationRoutingMap[i][j-1]);
						 }
					 }
					 
					 childMigrationRoutingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
				 }
			 }
		}
		
		Job childChromosome = new Job(ga, modulePlacementMap, childTupleRoutingMap, childMigrationRoutingMap);
		
        return new Individual(ga, childChromosome);
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
		int[][] routingMap = chromosome.getTupleRoutingMap();
		
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