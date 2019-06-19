package org.fog.placement.algorithm.overall.bf;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;

public class BruteForce extends Algorithm {
	private Job bestSolution;
	private double bestCost;
	private int iteration;
	
	public BruteForce(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		bestSolution = null;
		bestCost = Constants.REFERENCE_COST;
		iteration = 0;
		
		long start = System.currentTimeMillis();
		solveDeployment(new int[NR_NODES][NR_MODULES], 0);
		long finish = System.currentTimeMillis();
		elapsedTime = finish - start;
		
		if(Config.PRINT_DETAILS)
			AlgorithmUtils.printResults(this, bestSolution);
		
		return bestSolution;
	}
	
	private void solveDeployment(int[][] modulePlacementMap, int index) {
		int[][] currentPositionInt = new int[NR_NODES][NR_MODULES];
        
		for(int j = 0; j < NR_NODES; j++) {
			for (int z = 0; z < NR_MODULES; z++) {
				currentPositionInt[j][z] = (int) currentPlacement[j][z];
			}
		}
		
		for(int i = 0; i < NR_NODES; i++) {
			if(possibleDeployment[i][index] == 1) {
				for(int j = 0; j < NR_NODES; j++) {
					if(j == i)
						modulePlacementMap[j][index] = 1;
					else
						modulePlacementMap[j][index] = 0;
				}
				
				if(index != NR_MODULES - 1)
					solveDeployment(modulePlacementMap, index + 1);
				else {					
					//Trying the following placement map
			        List<Integer> initialNodes = new ArrayList<Integer>();
			        List<Integer> finalNodes = new ArrayList<Integer>();
					
					for(int j = 0; j < NR_MODULES; j++) {
						for(int z = 0; z < NR_MODULES; z++) {
							if(getmDependencyMap()[j][z] != 0) {
								initialNodes.add(Job.findModulePlacement(modulePlacementMap, j));
								finalNodes.add(Job.findModulePlacement(modulePlacementMap, z));
							}
						}
					}
					
					int[][] tupleRoutingMap = convertListToMatrix(initialNodes, finalNodes);
					
					initialNodes = new ArrayList<Integer>();
			        finalNodes = new ArrayList<Integer>();
			        
					for(int j = 0; j < NR_MODULES; j++) {
						if(isFirstOptimization())
							initialNodes.add(Job.findModulePlacement(modulePlacementMap, j));
						else
							initialNodes.add(Job.findModulePlacement(currentPositionInt, j));
						finalNodes.add(Job.findModulePlacement(modulePlacementMap, j));
					}
					
					int[][] migrationRoutingMap = convertListToMatrix(initialNodes, finalNodes);
					solveVmRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), 0, 1);
				}
			}
		}
	}
	
	private void solveVmRouting(Job job, int row, int col) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		int[][] tupleRoutingMap = job.getTupleRoutingMap();
		int[][] migrationRoutingMap = job.getMigrationRoutingMap();
		
		int max_r = migrationRoutingMap.length - 1;
		int max_c = migrationRoutingMap[0].length - 1;
		
		if(row == max_r + 1 && col == 1) {
			solveTupleRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), 0, 1);
		}else {
			int previousNode = migrationRoutingMap[row][col-1];
			
			if(previousNode == migrationRoutingMap[row][NR_NODES-1]) {
				migrationRoutingMap[row][col] = previousNode;
				
				if(col < max_c-1)
					solveVmRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row, col + 1);
				else
					solveVmRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row + 1, 1);
			}else {
				for(int i = 0; i < NR_NODES; i++) {
					if(getfLatencyMap()[previousNode][i] < Constants.INF) {
						
						boolean valid = true;
						for(int j = 0; j < col - 1; j++) {
							if(migrationRoutingMap[row][j] == i && i != previousNode) {
								valid = false;
								break;
							}
						}
							
						if(!valid)
							continue;
						
						migrationRoutingMap[row][col] = i;
					
						if(col < max_c-1)
							solveVmRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row, col + 1);
						else
							solveVmRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row + 1, 1);
					}
				}
			}
		}
	}
	
	private void solveTupleRouting(Job job, int row, int col) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		int[][] tupleRoutingMap = job.getTupleRoutingMap();
		int[][] migrationRoutingMap = job.getMigrationRoutingMap();
		
		int max_r = tupleRoutingMap.length - 1;
		int max_c = tupleRoutingMap[0].length - 1;
		
		if(row == max_r + 1 && col == 1) {
			//Trying the following routing map
			
			if(job.getCost() < bestCost) {
				bestCost = job.getCost();
				bestSolution = new Job(job);
    			valueIterMap.put(iteration, bestCost);
    			
    			if(Config.PRINT_BEST_ITER)
    				System.out.println("iteration: " + iteration + " value: " + bestCost);
			}
			iteration++;
			
		}else {
			int previousNode = tupleRoutingMap[row][col-1];
			
			if(previousNode == tupleRoutingMap[row][NR_NODES-1]) {
				tupleRoutingMap[row][col] = previousNode;
				
				if(col < max_c-1)
					solveTupleRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row, col + 1);
				else
					solveTupleRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row + 1, 1);
			}else {
				for(int i = 0; i < NR_NODES; i++) {
					if(getfLatencyMap()[previousNode][i] < Constants.INF) {
						
						boolean valid = true;
						for(int j = 0; j < col - 1; j++) {
							if(tupleRoutingMap[row][j] == i && i != previousNode) {
								valid = false;
								break;
							}
						}
							
						if(!valid)
							continue;
						
						tupleRoutingMap[row][col] = i;
					
						if(col < max_c-1)
							solveTupleRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row, col + 1);
						else
							solveTupleRouting(new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap), row + 1, 1);
					}
				}
			}
		}
	}
	
	private int[][] convertListToMatrix(List<Integer> initials, List<Integer> finals) {
	    int[][] ret = new int[initials.size()][NR_NODES];
	    
	    for(int i = 0; i < initials.size(); i++) {
	    	ret[i][0] = initials.get(i);
	    	ret[i][NR_NODES-1] = finals.get(i);
	    }
	    
	    return ret;
	}
	
}