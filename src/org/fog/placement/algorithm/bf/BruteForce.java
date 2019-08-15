package org.fog.placement.algorithm.bf;

import java.util.LinkedList;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.util.routing.Vertex;

/**
 * Class in which defines and executes the brute force algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class BruteForce extends Algorithm {
	/** Best solution found by the algorithm */
	private Job bestSolution;
	
	/** Best cost found by the algorithm */
	private double bestCost;
	
	/** Current iteration of the algorithm */
	private int iteration;
	
	/** Time at the beginning of the execution of the algorithm */
	private long start;
	
	/** Time at the end of the execution of the algorithm */
	private long finish;
	
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
	public Job execute() {
		iteration = 0;
		bestCost = Constants.REFERENCE_COST;
		bestSolution = null;
		
		// Time at the beginning of the execution of the algorithm
		start = System.currentTimeMillis();
		
		// Generate the Dijkstra graph
		generateDijkstraGraph();
		
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
							tupleRoutingMap[tmp][0] = Job.findModulePlacement(modulePlacementMap, j);
							tupleRoutingMap[tmp++][getNumberOfNodes()-1] = Job.findModulePlacement(modulePlacementMap, z);
						}
					}
				}
		        
		        int[][] migrationRoutingMap = new int[getNumberOfModules()][getNumberOfNodes()];
		        
				for(int j = 0; j < getNumberOfModules(); j++) {
					migrationRoutingMap[j][0] = isFirstOptimization() ? Job.findModulePlacement(modulePlacementMap, j) : Job.findModulePlacement(currentPositionInt, j);
					migrationRoutingMap[j][getNumberOfNodes()-1] = Job.findModulePlacement(modulePlacementMap, j);
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
					if(!isValidHop(i, migrationRoutingMap[row][getNumberOfNodes()-1], getNumberOfNodes() - col)) continue;
					
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
			Job job = new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
			
			if(job.getCost() < bestCost) {
				bestCost = job.getCost();
				bestSolution = new Job(job);
    			getValueIterMap().put(iteration, bestCost);
    			
    			if(Config.PRINT_ALGORITHM_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestCost);
			}
			
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
					if(!isValidHop(i, tupleRoutingMap[row][getNumberOfNodes()-1], getNumberOfNodes() - col)) continue;
					
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
	 * Checks whether the node is valid. It is valid if, and only if, from the current node index towards the final node index
	 * there is a path which has lower or equal number of hops as the provided maximum distance.
	 * 
	 * @param nodeIndex the current node index
	 * @param finalNodeIndex the final node index
	 * @param maxDistance the maximum allowed distance
	 * @return true is it's a valid node. False, otherwise.
	 */
	private boolean isValidHop(final int nodeIndex, final int finalNodeIndex, final int maxDistance) {
			getDijkstra().execute(getDijkstraNodes().get(nodeIndex));
			LinkedList<Vertex> path = getDijkstra().getPath(getDijkstraNodes().get(finalNodeIndex));
			
			// If path is null, means that both start and finish refer to the same node, thus it can be added
	        if((path != null && path.size() <= maxDistance) || path == null)
	        	return true;
	        
	        return false;
	}
	
}
