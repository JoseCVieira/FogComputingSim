package org.fog.placement.algorithm.bf;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;
import org.fog.placement.algorithm.util.routing.Vertex;

/**
 * Class in which defines and executes the brute force algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class BruteForce extends Algorithm {
	/** Best solution found by the algorithm */
	private Solution bestSolution;
	
	/** Current iteration of the algorithm */
	private int iteration;
	
	/** Time at the beginning of the execution of the algorithm */
	private long start;
	
	/** Time at the end of the execution of the algorithm */
	private long finish;
	
	/** Map containing the hop count between any two nodes */
	private Map<Map<Integer, Integer>, Integer> hopCountMap;
	
	public BruteForce(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	/**
	 * Executes the brute force algorithm in order to find the best solution (the solution with the lower cost which respects all constraints).
	 * 
	 * @return the best solution; can be null
	 */
	@Override
	public Solution execute() {
		iteration = 0;
		bestSolution = null;
		getValueIterMap().clear();
		
		// Time at the beginning of the execution of the algorithm
		start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
		// Compute the hop count between any two nodes
		createHopCountMap();
		
		// Solve the problem
		solveModulePlacement(new int[getNumberOfNodes()][getNumberOfModules()], 0);
		
		// Time at the end of the execution of the algorithm
		finish = System.currentTimeMillis();
		
		setElapsedTime(finish - start);
		
		return bestSolution;
	}
	
	/**
	 * Solves the module placement map.
	 * 
	 * @param modulePlacementMap the current binary module placement map
	 * @param index the index of the module to be filled up
	 */
	private void solveModulePlacement(int[][] modulePlacementMap, final int index) {
		int[][] currentPositionInt = getCurrentPositionInt();
		
		for(int i = 0; i < getNumberOfNodes(); i++) {
			// If its not a valid placement continue
			if(getPossibleDeployment()[i][index] == 0) continue;
			
			// Clear other placements of the module and place it in the correct node index
			for(int j = 0; j < getNumberOfNodes(); j++) {
				modulePlacementMap[j][index] = j == i ? 1 : 0;
			}
			
			if(checkResourcesExceeded(modulePlacementMap, i)) continue;
			
			// If its not the final module, solve the next one
			if(index != getNumberOfModules() - 1) {
				solveModulePlacement(modulePlacementMap, index + 1);
			}
			
			// Tries the current module placement map
			else {
				int[][] tupleRoutingMap = new int[getNumberOfDependencies()][getNumberOfNodes()];
				
				int tmp = 0;
				for(int j = 0; j < getNumberOfModules(); j++) {
					for(int z = 0; z < getNumberOfModules(); z++) {
						if(getmDependencyMap()[j][z] != 0) {
							tupleRoutingMap[tmp][0] = Solution.findModulePlacement(modulePlacementMap, j);
							tupleRoutingMap[tmp++][getNumberOfNodes()-1] = Solution.findModulePlacement(modulePlacementMap, z);
						}
					}
				}
		        
		        int[][] migrationRoutingMap = new int[getNumberOfModules()][getNumberOfNodes()];
		        
				for(int j = 0; j < getNumberOfModules(); j++) {
					migrationRoutingMap[j][0] = Solution.findModulePlacement(isFirstOptimization() ? modulePlacementMap : currentPositionInt, j);
					migrationRoutingMap[j][getNumberOfNodes()-1] = Solution.findModulePlacement(modulePlacementMap, j);
				}
				
				solveVmRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, 0, 1);
			}
		}
	}
	
	/**
	 * Solves the virtual machine routing map.
	 * 
	 * @param modulePlacementMap the current binary module placement map
	 * @param tupleRoutingMap the current tuple routing map
	 * @param migrationRoutingMap the current virtual machine migration map
	 * @param row the index of the module to be filled up
	 * @param col the index of the node to be filled up
	 */
	private void solveVmRouting(final int[][] modulePlacementMap, final int[][] tupleRoutingMap,
			int[][] migrationRoutingMap, final int row, final int col) {
		int max_r = migrationRoutingMap.length - 1;
		int max_c = migrationRoutingMap[0].length - 1;
		
		// If VM routing matrix is already filled solve the tuple routing matrix
		if(row == max_r + 1 && col == 1) {
			solveTupleRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, 0, 1);
			
		// Otherwise, keep filling the VM routing matrix
		}else {
			int previousNode = migrationRoutingMap[row][col-1];
			
			// If it already find out the destination just fill the next hops with the destination index
			if(previousNode == migrationRoutingMap[row][getNumberOfNodes()-1]) {
				migrationRoutingMap[row][col] = previousNode;
				
				// If its not the final hop, fill the next hop for the current module/VM
				if(col < max_c-1)
					solveVmRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row, col + 1);
				
				// Otherwise, fill the next module/VM path
				else
					solveVmRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row + 1, 1);
				
			// Otherwise, keep filling the VM routing matrix
			}else {
				for(int i = 0; i < getNumberOfNodes(); i++) {
					if(getfLatencyMap()[previousNode][i] == Constants.INF) continue;
					if(getfLatencyMap()[previousNode][i] == 0) continue;
					
					Map<Integer,Integer> map = new HashMap<Integer, Integer>();
					map.put(i, migrationRoutingMap[row][getNumberOfNodes()-1]);
					
					if(hopCountMap.get(map) > getNumberOfNodes() - col) continue;
					
					migrationRoutingMap[row][col] = i;
				
					// If its not the final hop, fill the next hop for the current module/VM
					if(col < max_c-1)
						solveVmRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row, col + 1);
					
					// Otherwise, fill the next module/VM path
					else
						solveVmRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row + 1, 1);
				}
			}
		}
	}
	
	/**
	 *  Solves the tuple routing map.
	 * 
	 * @param modulePlacementMap the current binary module placement map
	 * @param tupleRoutingMap the current tuple routing map
	 * @param migrationRoutingMap the current virtual machine migration map
	 * @param row the index of the dependency to be filled up
	 * @param col the index of the node to be filled up
	 */
	private void solveTupleRouting(final int[][] modulePlacementMap, int[][] tupleRoutingMap,
			final int[][] migrationRoutingMap, final int row, final int col) {
		int max_r = tupleRoutingMap.length - 1;
		int max_c = tupleRoutingMap[0].length - 1;
		
		// If tuple routing matrix is already filled analyze the following solution
		if(row == max_r + 1 && col == 1) {			
			Solution solution = new Solution(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
			
			// Check whether the new individual is the new best solution
    		bestSolution = Solution.checkBestSolution(this, solution, bestSolution, iteration);
			
			iteration++;
			
		// Otherwise, keep filling the tuple routing matrix
		}else {
			int previousNode = tupleRoutingMap[row][col-1];
			
			// If it already find out the destination just fill the next hops with the destination index
			if(previousNode == tupleRoutingMap[row][getNumberOfNodes()-1]) {
				tupleRoutingMap[row][col] = previousNode;
				
				// If its not the final hop, fill the next hop for the current tuple
				if(col < max_c-1)
					solveTupleRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row, col + 1);
				
				// Otherwise, fill the next tuple path
				else
					solveTupleRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row + 1, 1);
				
				// Otherwise, keep filling the tuple routing matrix
			}else {
				for(int i = 0; i < getNumberOfNodes(); i++) {
					if(getfLatencyMap()[previousNode][i] == Constants.INF) continue;
					if(getfLatencyMap()[previousNode][i] == 0) continue;
					
					Map<Integer,Integer> map = new HashMap<Integer, Integer>();
					map.put(i, tupleRoutingMap[row][getNumberOfNodes()-1]);
					
					if(hopCountMap.get(map) > getNumberOfNodes() - col) continue;
					
					tupleRoutingMap[row][col] = i;
				
					// If its not the final hop, fill the next hop for the current tuple
					if(col < max_c-1)
						solveTupleRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row, col + 1);
					
					// Otherwise, fill the next tuple path
					else
						solveTupleRouting(modulePlacementMap, tupleRoutingMap, migrationRoutingMap, row + 1, 1);
				}
			}
		}
	}
	
	/**
	 * Verifies whether resources are exceeded in a given node for a given placement matrix.
	 * 
	 * @param modulePlacementMap the current binary module placement map
	 * @param node the node to verify
	 * @return true if its resources are being exceeded. 0, otherwise
	 */
	private boolean checkResourcesExceeded(final int[][] modulePlacementMap, int node) {
		double totalMips = 0;
		double totalRam = 0;
		double totalStrg = 0;
		
		for(int j = 0; j < getNumberOfModules(); j++) {
			totalMips += modulePlacementMap[node][j] * getmMips()[j];
			totalRam += modulePlacementMap[node][j] * getmRam()[j];
			totalStrg += modulePlacementMap[node][j] * getmStrg()[j];
		}
		
		if(totalMips > getfMips()[node] * Config.MIPS_PERCENTAGE_UTIL) return true;
		if(totalRam > getfRam()[node] * Config.MEM_PERCENTAGE_UTIL) return true;
		if(totalStrg > getfStrg()[node] * Config.STRG_PERCENTAGE_UTIL) return true;
		
		return false;
	}
	
	/**
	 * Computes and stores the hop count between any two nodes.
	 */
	private void createHopCountMap() {
		hopCountMap = new HashMap<Map<Integer,Integer>, Integer>();
		
		for(int i = 0; i < getNumberOfNodes(); i++) {
			for(int j = 0; j < getNumberOfNodes(); j++) {
				Map<Integer,Integer> map = new HashMap<Integer, Integer>();
				map.put(i, j);
				
				if(i != j) {
					getDijkstra().execute(getDijkstraNodes().get(i));
					LinkedList<Vertex> path = getDijkstra().getPath(getDijkstraNodes().get(j));
					hopCountMap.put(map, path.size());
				}else {
					hopCountMap.put(map, 0);
				}
			}
		}
	}
	
}
