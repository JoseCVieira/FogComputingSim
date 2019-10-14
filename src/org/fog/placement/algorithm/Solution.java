package org.fog.placement.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.placement.algorithm.util.AlgorithmUtils;
import org.fog.utils.Util;

/**
 * Class representing the solution of the multiple objective optimization algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Solution implements Comparable<Solution> {
	
	/** Matrix representing the application module placement table (binary) */
	protected int[][] modulePlacementMap;
	
	/** Matrix representing the tuple routing table (each row is a dependency between different pair of nodes) */
	protected int[][] tupleRoutingMap;
	
	/** Matrix representing the virtual machine migration routing table */
	protected int[][] migrationRoutingMap;

	/** Vector contains the details of the cost function (i.e., the cost of each objective) */
	private double[] cost;
	
	/** Vector contains the details of the loop delays */
	private double[] loopDeadline;
	
	/** Vector contains the details of the migration delays */
	private double[] migrationDeadline;
	
	/** Defines whether the solution is valid (respects all constraints) */
	private double constraint;
	
	/**
	 * Creates a copy of a solution.
	 * 
	 * @param anotherSolution the solution to be copied
	 */
	public Solution(Algorithm algorithm, Solution anotherSolution) {
		this.modulePlacementMap = Util.copy(anotherSolution.getModulePlacementMap());
		this.tupleRoutingMap = Util.copy(anotherSolution.getTupleRoutingMap());
		this.migrationRoutingMap = Util.copy(anotherSolution.getMigrationRoutingMap());
		this.constraint = anotherSolution.getConstraint();
		
		this.cost = new double[Config.NR_OBJECTIVES];
		for(int i = 0; i < Config.NR_OBJECTIVES; i++)
			this.cost[i] = anotherSolution.getDetailedCost(i);
		
		this.loopDeadline = new double[algorithm.getNumberOfLoops()];
		for(int i = 0; i < algorithm.getNumberOfLoops(); i++)
			this.loopDeadline[i] = anotherSolution.getLoopDeadline(i);
		
		this.migrationDeadline = new double[algorithm.getNumberOfModules()];
		for(int i = 0; i < algorithm.getNumberOfModules(); i++)
			this.migrationDeadline[i] = anotherSolution.getMigrationDeadline(i);
	}
	
	/**
	 * Creates a new solution only with the module placement defined (by default it's an invalid solution).
	 * 
	 * @param modulePlacementMap
	 */
	public Solution(Algorithm algorithm, int[][] modulePlacementMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.cost = new double[Config.NR_OBJECTIVES];
		this.loopDeadline = new double[algorithm.getNumberOfLoops()];
		this.migrationDeadline = new double[algorithm.getNumberOfModules()];
	}
	
	/**
	 * Creates a solution based on the application module placement, tuple routing, and virtual machine migration
	 * routing tables.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param modulePlacementMap the module placement matrix (binary)
	 * @param tupleRoutingMap the tuple routing matrix
	 * @param migrationRoutingMap the migration routing matrix
	 */
	public Solution(Algorithm algorithm, int[][] modulePlacementMap, int[][] tupleRoutingMap, int[][] migrationRoutingMap) {
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		this.cost = new double[Config.NR_OBJECTIVES];
		this.loopDeadline = new double[algorithm.getNumberOfLoops()];
		this.migrationDeadline = new double[algorithm.getNumberOfModules()];
		CostFunction.analyzeSolution(algorithm, this);
	}
	
	/**
	 * Creates a solution based on the application module placement, tuple routing, and virtual machine migration
	 * routing tables (all of which being binary tables).
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param modulePlacementMap the module placement matrix (binary)
	 * @param tupleRoutingVectorMap the tuple routing matrix (binary)
	 * @param migrationRoutingVectorMap the migration routing matrix (binary)
	 */
	public Solution(Algorithm algorithm, int[][] modulePlacementMap, int[][][] tupleRoutingVectorMap,
			int[][][] migrationRoutingVectorMap) {		
		int nrDependencies = tupleRoutingVectorMap.length;
		int nrNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int[][] tupleRoutingMap = new int[nrDependencies][nrNodes];
		int[][] migrationRoutingMap = new int[nrModules][nrNodes];
		
		int iter;
		
		// Tuple routing map
		for(int i = 0; i < nrDependencies; i++) {
			int from = Solution.findModulePlacement(modulePlacementMap, algorithm.getStartModDependency(i));
			tupleRoutingMap[i][0] = from;
			iter = 1;
			
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
		for(int i = 0; i < nrModules; i++) {
			int from = Solution.findModulePlacement(algorithm.isFirstOptimization() ? modulePlacementMap : algorithm.getCurrentPositionInt(), i);
			migrationRoutingMap[i][0] = from;
			iter = 1;
			
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
			
			for(int j = iter; j < nrNodes; j++) {
				migrationRoutingMap[i][j] = from;
			}
		}
		
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		this.cost = new double[Config.NR_OBJECTIVES];
		this.loopDeadline = new double[algorithm.getNumberOfLoops()];
		this.migrationDeadline = new double[algorithm.getNumberOfModules()];
	}
	
	/**
	 * Generates a random algorithm solution.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param cf the object which contains the methods to analyze the cost and the constrains of the solution; can be null
	 * @return the random algorithm solution 
	 */
	public static Solution generateRandomSolution(Algorithm algorithm) {
		int nrFogNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int nrDependencies = algorithm.getNumberOfDependencies();
		
		int[][] modulePlacementMap = generateRandomPlacement(algorithm, nrFogNodes, nrModules);
		int[][] tupleRoutingMap = generateRandomTupleRouting(algorithm, modulePlacementMap, nrFogNodes, nrDependencies);
		int[][] migrationRoutingMap = generateRandomMigrationRouting(algorithm, modulePlacementMap, nrFogNodes, nrModules);
		return new Solution(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
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
			routingMap[i][0] = Solution.findModulePlacement(modulePlacementMap, algorithm.getStartModDependency(i));
			routingMap[i][nrFogNodes-1] = Solution.findModulePlacement(modulePlacementMap, algorithm.getFinalModDependency(i));
	        
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
					if(algorithm.getfLatencyMap()[routingMap[i][j-1]][z] == 0) continue;
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
			routingMap[i][0] = Solution.findModulePlacement(algorithm.isFirstOptimization() ? modulePlacementMap : currentPosition, i);
			routingMap[i][nrFogNodes-1] = Solution.findModulePlacement(modulePlacementMap, i);
			
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
					if(algorithm.getfLatencyMap()[routingMap[i][j-1]][z] == 0) continue;
					if(!algorithm.isValidHop(z, routingMap[i][nrFogNodes-1], nrFogNodes - j)) continue;
					validValues.add(z);
				}
				
				routingMap[i][j] = validValues.get(new Random().nextInt(validValues.size()));
			}
		}
		
		return routingMap;
	}
	
	/**
	 * Checks whether the new individual's solution is close enough to the best one.
	 * 
	 * @param newIndividual the new found solution
	 * @param bestSolution the best current found solution
	 * @return true if they are close enough, otherwise false
	 */
	public static boolean checkConvergence(Solution newSolution, Solution bestSolution) {
		if(!newSolution.isValid() || bestSolution == null || !bestSolution.isValid()) return false;
		
		for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
			double oldV = bestSolution.getDetailedCost(i);
			double newV = newSolution.getDetailedCost(i);
			
			if(Math.abs(oldV - newV) > Config.CONVERGENCE_ERROR) return false;
		}
		
		return true;
	}
	
	/**
	 * Checks whether the new solution is better than the previously found one.
	 * 
	 * @param newIndividual the new found solution
	 * @param bestSolution the best current found solution
	 * @param iteration a negative number if its not to be displayed. Otherwise, the iteration value
	 * @return the best solution
	 */
	public static Solution checkBestSolution(Algorithm algorithm, Solution newSolution, Solution bestSolution,
			int iteration) {		
		if(bestSolution == null && !newSolution.isValid()) return bestSolution;
		
		if(bestSolution == null && newSolution.isValid()) {
			bestSolution = new Solution(algorithm, newSolution);
			if(iteration >= 0) displayNewBestSolution(algorithm, newSolution, iteration);
			return bestSolution;
		}
		
		if(!newSolution.isValid()) return bestSolution;
		
		Solution[] tmpPop = new Solution[]{newSolution, bestSolution};
		Arrays.sort(tmpPop);
		
		for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
			if(bestSolution.getDetailedCost(i) != tmpPop[0].getDetailedCost(i)) {				
				bestSolution = new Solution(algorithm, tmpPop[0]);
				if(iteration >= 0) displayNewBestSolution(algorithm, tmpPop[0], iteration);
				return bestSolution;
			}
		}
		
		return bestSolution;
	}
	
	/**
	 * Displays the best solution and adds it to the iteration/value map.
	 * 
	 * @param bestSolution the best solution
	 * @param iteration a negative number if its not to be displayed. Otherwise, the iteration value
	 */
	public static void displayNewBestSolution(Algorithm algorithm, Solution bestSolution, int iteration) {
		for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
			Map<Integer, Integer> map = new HashMap<Integer, Integer>();
			map.put(i, iteration);
			
			algorithm.getValueIterMap().put(map, bestSolution.getDetailedCost(i));
		}
		
		if(!Config.PRINT_ALGORITHM_BEST_ITER) return;
		AlgorithmUtils.printSolution(algorithm, bestSolution, iteration);
	}
	
	/**
	 * Compares two individuals based on it's fitness value (used to sort arrays of individuals).
	 */
	@Override
	public int compareTo(Solution solution) {
		int value = compare(getConstraint(), solution.getConstraint(), 0);
		if(value != 0) return value;
		
		int nrAnalysed = 0;
		int lastPrio = Config.NR_OBJECTIVES + 1;
		while(nrAnalysed < Config.NR_OBJECTIVES) {
			int index = getNextHighestPriority(lastPrio);
			lastPrio = Config.priorities[index];
			
			value = compare(getDetailedCost(index), solution.getDetailedCost(index), Config.relTols[index]);
			if(++nrAnalysed == Config.NR_OBJECTIVES) return value;
			if(value != 0) return value;
		}
		
		FogComputingSim.err("Should not happen (Solution)");
		return -1;
	}
	
	/**
	 * Compares the passed values.
	 * 
	 * @param d1 the first value
	 * @param d2 the second value
	 * @param p error percentage acting as RelTol
	 * @return 0 if both values are equal. -1 if the first value is less than the second one. 1 otherwise
	 */
	private int compare(double d1, double d2, double p) {
		/*if(d1 < d2) return -1;
		else if(d1 > d2) return 1;
		return 0;*/
		if(d1*(1+p) < d2) return -1;
		else if(d1*(1-p) > d2) return 1;
		return 0;
		
	}
	
	/**
	 * Gets the next highest priority based on the last one.
	 * 
	 * @param lastPrio the last highest priority
	 * @return the highest priority
	 */
	private int getNextHighestPriority(int lastPrio) {
		int index = -1;
		int value = -1;
		
		for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
			if(Config.priorities[i] < lastPrio && Config.priorities[i] > value) {
				index = i;
				value = Config.priorities[i];
			}
		}
		
		return index;
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
	
	/**
	 * Gets the cost of a given cost function.
	 * 
	 * @param index the index of the cost function
	 * @return the value of the cost function
	 */
	public double getDetailedCost(int index) {
		return cost[index];
	}
	
	/**
	 * Sets the cost of a given cost function.
	 * 
	 * @param index the index of the cost function
	 * @param value the value of the cost function
	 */
	public void setDetailedCost(int index, double value) {
		this.cost[index] = value;
	}
	
	/**
	 * Gets the value of the worst case latency for a given loop.
	 * 
	 * @param index the index of the loop
	 * @return the value of the worst case latency
	 */
	public double getLoopDeadline(int index) {
		return loopDeadline[index];
	}
	
	/**
	 * Sets the value of the worst case latency for a given loop.
	 * 
	 * @param index the index of the loop
	 * @param value the value of the worst case latency
	 */
	public void setLoopDeadline(int index, double value) {
		this.loopDeadline[index] = value;
	}
	
	/**
	 * Gets the value of the worst case latency in the migration of a given application module.
	 * 
	 * @param index the index of the application module
	 * @return the value of the worst case latency
	 */
	public double getMigrationDeadline(int index) {
		return migrationDeadline[index];
	}
	
	/**
	 * Sets the value of the worst case latency in the migration of a given application module.
	 * 
	 * @param index the index of the application module
	 * @param value the value of the worst case latency
	 */
	public void setMigrationDeadline(int index, double value) {
		this.migrationDeadline[index] = value;
	}
	
	/**
	 * Gets the number of violated constraints.
	 * 
	 * @return the number of violated constraints
	 */
	public double getConstraint() {
		return constraint;
	}
	
	/**
	 * Sets the number of violated constraints.
	 * 
	 * @param constraint the number of violated constraints
	 */
	public void setConstraint(double constraint) {
		this.constraint = constraint;
	}
	
	/**
	 * Verifies whether the solution is valid (respects all constraints).
	 * 
	 * @return true if the solution is valid, otherwise false
	 */
	public boolean isValid() {
		return constraint == 0;
	}
	
}
