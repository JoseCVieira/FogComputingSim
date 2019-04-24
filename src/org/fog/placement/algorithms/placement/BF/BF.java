package org.fog.placement.algorithms.placement.BF;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.AlgorithmUtils;
import org.fog.placement.algorithms.placement.Job;

public class BF extends Algorithm {
	private int[][] bestPlacementMap = null;
	private int[][] bestRoutingMap = null;
	private double bestCost = Double.MAX_VALUE;
	
	public BF(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		solveDeployment(new int[NR_NODES][NR_MODULES], 0);
		
		Job solution = new Job(this, bestPlacementMap, bestRoutingMap);
		
		if(PRINT_DETAILS)
			AlgorithmUtils.printResults(this, solution);
		
		return solution;
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
					//Trying the following placement map
			        
			        List<Integer> initialNodes = new ArrayList<Integer>();
			        List<Integer> finalNodes = new ArrayList<Integer>();
					
					for(int j = 0; j < NR_MODULES; j++) {
						for(int z = 0; z < NR_MODULES; z++) {
							if(getmDependencyMap()[j][z] != 0) {
								initialNodes.add(findModulePlacement(modulePlacementMap, j));
								finalNodes.add(findModulePlacement(modulePlacementMap, z));
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
				bestPlacementMap = copyPlacementMap(modulePlacementMap);
				bestRoutingMap = copyPlacementMap(routingMap);
			}
		}else {
			int previousNode = routingMap[row][col-1];
			
			for(int i = 0; i < NR_NODES; i++) {
				
				if(getfLatencyMap()[previousNode][i] < Double.MAX_VALUE) {
					routingMap[row][col] = i;
				
					if(col < max_c-1)
						solveRouting(new Job(this, modulePlacementMap, routingMap), row, col + 1);
					else
						solveRouting(new Job(this, modulePlacementMap, routingMap), row + 1, 1);
				}
			}
		}
	}
	
	private static int findModulePlacement(int[][] chromosome, int colomn) {
		for(int i = 0; i < chromosome.length; i++)
			if(chromosome[i][colomn] == 1)
				return i;
		return -1;
	}
	
	private int[][] convertListToMatrix(List<Integer> initials, List<Integer> finals) {
	    int[][] ret = new int[initials.size()][NR_NODES - 1];
	    
	    for(int i = 0; i < initials.size(); i++) {
	    	ret[i][0] = initials.get(i);
	    	ret[i][NR_NODES-2] = finals.get(i);
	    }
	    
	    return ret;
	}
	
	private int[][] copyPlacementMap(int[][] modulePlacementMap) {
		int r = modulePlacementMap.length;
		int c = modulePlacementMap[0].length;
		
		int[][] result = new int[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ; j++)
				result[i][j] = modulePlacementMap[i][j];
				
		return result;
	}
	
}