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
	private Job bestSolution = null;
	private double bestCost = Constants.REFERENCE_COST;
	private int iteration = 0;
	
	public BruteForce(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		long start = System.currentTimeMillis();
		solveDeployment(new int[NR_NODES][NR_MODULES], 0);
		long finish = System.currentTimeMillis();
		elapsedTime = finish - start;
		
		if(Config.PRINT_DETAILS)
			AlgorithmUtils.printResults(this, bestSolution);
		
		return bestSolution;
	}
	
	private void solveDeployment(int[][] modulePlacementMap, int index) {
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
					if(!isPossibleModulePlacement(modulePlacementMap))
						continue;
					
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
					solveRouting(new Job(this, modulePlacementMap, convertListToMatrix(initialNodes, finalNodes)), 0, 1);
				}
			}
		}
	}
	
	private void solveRouting(Job job, int row, int col) {
		int[][] modulePlacementMap = job.getModulePlacementMap();
		int[][] routingMap = job.getRoutingMap();
		
		int max_r = routingMap.length - 1;
		int max_c = routingMap[0].length - 1;
		
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
			int previousNode = routingMap[row][col-1];
			
			if(previousNode == routingMap[row][NR_NODES-1]) {
				routingMap[row][col] = previousNode;
				
				if(col < max_c-1)
					solveRouting(new Job(this, modulePlacementMap, routingMap), row, col + 1);
				else
					solveRouting(new Job(this, modulePlacementMap, routingMap), row + 1, 1);
				
			}else {
				for(int i = 0; i < NR_NODES; i++) {
					
					if(getfLatencyMap()[previousNode][i] < Constants.INF) {
						
						boolean valid = true;
						for(int j = 0; j < col - 1; j++) {
							if(routingMap[row][j] == i && i != previousNode) {
								valid = false;
								break;
							}
						}
							
						if(!valid)
							continue;
						
						routingMap[row][col] = i;
					
						if(col < max_c-1)
							solveRouting(new Job(this, modulePlacementMap, routingMap), row, col + 1);
						else
							solveRouting(new Job(this, modulePlacementMap, routingMap), row + 1, 1);
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
	
	private boolean isPossibleModulePlacement(int[][] modulePlacementMap) {
		for(int i = 0; i < getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalMem = 0;
			
			for(int j = 0; j < getNumberOfModules(); j++) {
				totalMips += modulePlacementMap[i][j] * getmMips()[j];
				totalRam += modulePlacementMap[i][j] * getmRam()[j];
				totalMem += modulePlacementMap[i][j] * getmMem()[j];
			}
			
			if(totalMips > getfMips()[i] || totalRam > getfRam()[i] || totalMem > getfMem()[i])
				return false;
		}
		
		return true;
	}
	
}