package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.utils.Util;

/**
 * Class representing the solution of the optimization algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Job {
	/** Matrix representing the application module placement table (binary) */
	protected int[][] modulePlacementMap;
	
	/** Matrix representing the tuple routing table (each row is a dependency between different pair of nodes) */
	protected int[][] tupleRoutingMap;
	
	/** Matrix representing the virtual machine migration routing table */
	protected int[][] migrationRoutingMap;
	
	/** Result of the cost function */
	protected double cost;
	
	/** Defines whether the solution is valid (respects all constraints) */
	protected boolean valid;
	
	/**
	 * Creates a new job for subclasses.
	 */
	public Job() {
		// Do nothing
	}
	
	/**
	 * Creates a copy of a solution.
	 * 
	 * @param anotherJob the solution to be copied
	 */
	public Job(Job anotherJob) {
		this.modulePlacementMap = Util.copy(anotherJob.getModulePlacementMap());
		this.tupleRoutingMap = Util.copy(anotherJob.getTupleRoutingMap());
		this.migrationRoutingMap = Util.copy(anotherJob.getMigrationRoutingMap());
		this.cost = anotherJob.getCost();
		this.valid = anotherJob.isValid();
	}
	
	/**
	 * Creates a new solution only with the module placement defined (by default it's an invalid solution).
	 * 
	 * @param modulePlacementMap
	 */
	public Job(int[][] modulePlacementMap) {
		this.modulePlacementMap = modulePlacementMap;
	}
	
	/**
	 * Creates a solution based on the application module placement, tuple routing, and virtual machine migration
	 * routing tables.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param cf the object which contains the methods to analyze the cost and the constrains of the solution
	 * @param modulePlacementMap the module placement matrix (binary)
	 * @param tupleRoutingMap the tuple routing matrix
	 * @param migrationRoutingMap the migration routing matrix
	 */
	public Job(Algorithm algorithm, CostFunction cf, int[][] modulePlacementMap, int[][] tupleRoutingMap, int[][] migrationRoutingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		cf.analyzeSolution(algorithm, this);
	}

	/**
	 * Generates a random algorithm solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param cf the object which contains the methods to analyze the cost and the constrains of the solution; can be null
	 * @return the random algorithm solution 
	 */
	public static Job generateRandomJob(Algorithm algorithm, CostFunction cf) {
		int nrFogNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int nrDependencies = algorithm.getNumberOfDependencies();
		
		int[][] modulePlacementMap = generateRandomPlacement(algorithm, nrFogNodes, nrModules);
		int[][] tupleRoutingMap = generateRandomTupleRouting(algorithm, modulePlacementMap, nrFogNodes, nrDependencies);
		int[][] migrationRoutingMap = generateRandomMigrationRouting(algorithm, modulePlacementMap, nrFogNodes, nrModules);
		return new Job(algorithm, cf, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
	}
	
	/**
	 * Generates a random application module placement based on the possibles one.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param nrFogNodes the number of fog nodes in the topology
	 * @param nrModules the number of modules in the topology
	 * @return the random application module placement
	 */
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
	
	/**
	 * Generates a random tuple routing table based on a given module placement and the distances computed by the Dijkstra algorithm.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param modulePlacementMap the module placement
	 * @param nrFogNodes the number of fog nodes in the topology
	 * @param nrDependencies the number of dependencies between pairs of nodes
	 * @return the random tuple routing table
	 */
	public static int[][] generateRandomTupleRouting(Algorithm algorithm, int[][] modulePlacementMap, int nrFogNodes, int nrDependencies) {
		int[][] routingMap = new int[nrDependencies][nrFogNodes];
		
		for(int i  = 0; i < nrDependencies; i++) {			
			routingMap[i][0] = Job.findModulePlacement(modulePlacementMap, algorithm.getStartModDependency(i));
			routingMap[i][nrFogNodes-1] = Job.findModulePlacement(modulePlacementMap, algorithm.getFinalModDependency(i));
	        
			for(int j = 1; j < nrFogNodes - 1; j++) {
				// If its already the final node, then just fill the remain ones
				if(routingMap[i][j-1] == routingMap[i][nrFogNodes-1]) {
					for(; j < nrFogNodes - 1; j++) {
						routingMap[i][j] = routingMap[i][nrFogNodes-1];
					}
					break;
				}
				
				List<Integer> validValues = new ArrayList<Integer>();
				
				for(int z = 0; z < nrFogNodes; z++) {					
					if(algorithm.getfLatencyMap()[routingMap[i][j-1]][z] == Constants.INF) continue;
					if(!algorithm.isValidHop(z, routingMap[i][nrFogNodes-1], nrFogNodes - j)) continue;
					validValues.add(z);
				}
						
				routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
			}
		}
		
		return routingMap;
	}
	
	
	/**
	 * Generates a random virtual machine migration routing table based on a given module placement and current position and the distances
	 * computed by the Dijkstra algorithm.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param modulePlacementMap the module placement
	 * @param nrFogNodes the number of fog nodes in the topology
	 * @param nrModules the number of modules in the topology
	 * @return the random virtual machine migration routing table
	 */
	public static int[][] generateRandomMigrationRouting(Algorithm algorithm, int[][] modulePlacementMap, int nrFogNodes, int nrModules) {
		int[][] currentPosition = algorithm.getCurrentPositionInt();
		int[][] routingMap = new int[nrModules][nrFogNodes];
		
		for(int i = 0; i < nrModules; i++) { // Module index
			routingMap[i][0] = Job.findModulePlacement(algorithm.isFirstOptimization() ? modulePlacementMap : currentPosition, i);
			routingMap[i][nrFogNodes-1] = Job.findModulePlacement(modulePlacementMap, i);
			
			for(int j = 1; j < nrFogNodes - 1; j++) { // Routing hop index
				// If its already the final node, then just fill the remain ones
				if(routingMap[i][j-1] == routingMap[i][nrFogNodes-1]) {
					for(; j < nrFogNodes - 1; j++) {
						routingMap[i][j] = routingMap[i][nrFogNodes-1];
					}
					break;
				}
				
				List<Integer> validValues = new ArrayList<Integer>();
				
				for(int z = 0; z < nrFogNodes; z++) { // Node index
					if(algorithm.getfLatencyMap()[routingMap[i][j-1]][z] == Constants.INF) continue;
					if(!algorithm.isValidHop(z, routingMap[i][nrFogNodes-1], nrFogNodes - j)) continue;
					validValues.add(z);
				}
				
				routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
			}
		}
		
		return routingMap;
	}
	
	/**
	 * Gets the matrix representing the application module placement table (binary).
	 * 
	 * @return the matrix representing the application module placement table
	 */
	public int[][] getModulePlacementMap() {
		return modulePlacementMap;
	}
	
	/**
	 * Gets the matrix representing the tuple routing table (each row is a dependency between different pair of nodes).
	 * 
	 * @return the matrix representing the tuple routing table
	 */
	public int[][] getTupleRoutingMap() {
		return tupleRoutingMap;
	}
	
	/**
	 * Gets the matrix representing the virtual machine migration routing table.
	 * 
	 * @return the matrix representing the virtual machine migration routing table
	 */
	public int[][] getMigrationRoutingMap() {
		return migrationRoutingMap;
	}
	
	/**
	 * Gets the result of the cost function.
	 * 
	 * @return the result of the cost function
	 */
	public double getCost() {
		return cost;
	}
	
	/**
	 * Sets the result of the cost function.
	 * 
	 * @param cost the result of the cost function
	 */
	public void setCost(double cost) {
		this.cost = cost;
	}
	
	/**
	 * Verifies whether the solution is valid (respects all constraints).
	 * 
	 * @return true if the solution is valid, otherwise false
	 */
	public boolean isValid() {
		return valid;
	}
	
	/**
	 * Defines whether the solution is valid (respects all constraints).
	 * 
	 * @param valid if the solution is valid
	 */
	public void setValid(boolean valid) {
		this.valid = valid;
	}
	
	/**
	 * Finds the fog device index where a given module was placed.
	 * 
	 * @param binary the binary matrix representing the module placement
	 * @param colomn the module to find the fog device index
	 * @return the fog device index; -1 if it was not found
	 */
	public static int findModulePlacement(int[][] binary, int colomn) {
		for(int i = 0; i < binary.length; i++)
			if(binary[i][colomn] == 1)
				return i;
		return -1;
	}
	
}
