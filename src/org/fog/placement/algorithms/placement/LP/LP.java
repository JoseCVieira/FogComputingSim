package org.fog.placement.algorithms.placement.LP;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.Job;
import org.fog.placement.algorithms.placement.util.AlgorithmUtils;

import ilog.concert.*;
import ilog.cplex.*;

public class LP extends Algorithm {	
	public LP(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
		List<Integer> initialModules = new ArrayList<Integer>();
		List<Integer> finalModules = new ArrayList<Integer>();
		
		for(int i = 0; i < getNumberOfModules(); i++) {
			for (int j = 0; j < getNumberOfModules(); j++) {
				if(getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		try {
			// Define new model
			IloCplex cplex = new IloCplex();
			/*cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.1);
			cplex.setParam(IloCplex.Param.TimeLimit, 3600);*/
			
			// Variables
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			IloNumVar[][][] routingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					placementVar[i][j] = cplex.intVar(0, 1);
			
			for(int i = 0; i < getNumberOfDependencies(); i++)
				for(int j = 0; j < NR_NODES; j++)
					for(int z = 0; z < NR_NODES; z++)
						routingVar[i][j][z] = cplex.intVar(0, 1);
			
			// Define objective
			IloLinearNumExpr objective = cplex.linearNumExpr();
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					
					double opCost = Config.OP_W*(getfMipsPrice()[i]*getmMips()[j] +
							getfRamPrice()[i]*getmRam()[j] + getfMemPrice()[i]*getmMem()[j]);
					
					double enCost = Config.EN_W*(getfBusyPw()[i]-getfIdlePw()[i])*
							(getmMips()[j]/getfMips()[i])*getfPwWeight()[i];
					
					double prCost = Config.PR_W*(getmMips()[j]/getfMips()[i]);
					
					objective.addTerm(placementVar[i][j], opCost);	// Operational cost
					objective.addTerm(placementVar[i][j], enCost);	// Energetic cost
					objective.addTerm(placementVar[i][j], prCost);	// Processing cost
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double dependencies = getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						
						double latencyCost = Config.LT_W*(getfLatencyMap()[j][z]*dependencies);
						double bandwidthCost = bwNeeded/(getfBandwidthMap()[j][z] + Config.EPSILON);
						double txOpCost = Config.OP_W*(getfBwPrice()[j]*bwNeeded);
						
						// Transmission cost + transmission operational cost + transition cost
						objective.addTerm(routingVar[i][j][z], latencyCost + bandwidthCost + txOpCost);
					}
				}
			}
			
			cplex.addMinimize(objective);
			
			// Define constraints
			IloLinearNumExpr[] usedMipsCapacity = new IloLinearNumExpr[NR_NODES];
			IloLinearNumExpr[] usedRamCapacity = new IloLinearNumExpr[NR_NODES];
			IloLinearNumExpr[] usedMemCapacity = new IloLinearNumExpr[NR_NODES];
			
			for (int i = 0; i < NR_NODES; i++) {
				usedMipsCapacity[i] = cplex.linearNumExpr();
				usedRamCapacity[i] = cplex.linearNumExpr();
				usedMemCapacity[i] = cplex.linearNumExpr();
				
        		for (int j = 0; j < NR_MODULES; j++) {
        			usedMipsCapacity[i].addTerm(placementVar[i][j], getmMips()[j]);
        			usedRamCapacity[i].addTerm(placementVar[i][j], getmRam()[j]);
        			usedMemCapacity[i].addTerm(placementVar[i][j], getmMem()[j]);
        		}
			}
			
			// Sum of the resources needed should not pass the capacity
			for (int i = 0; i < NR_NODES; i++) {
        		cplex.addLe(usedMipsCapacity[i], getfMips()[i]);
        		cplex.addLe(usedRamCapacity[i], getfRam()[i]);
        		cplex.addLe(usedMemCapacity[i], getfMem()[i]);
			}
			
			IloNumVar[][] transposeP = new IloNumVar[NR_MODULES][NR_NODES];
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					transposeP[j][i] = placementVar[i][j];
			
			// One an only one placement
			for(int i = 0; i < NR_MODULES; i++)
				cplex.addEq(cplex.sum(transposeP[i]), 1.0);
			
			// Each module has to be placed inside one of the possible fog nodes
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					cplex.addLe(placementVar[i][j], getPossibleDeployment()[i][j]);
			
			IloNumVar[][][] transposeR = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			for(int i = 0; i < getNumberOfDependencies(); i++)
				for(int j = 0; j < NR_NODES; j++)
					for(int z = 0; z < NR_NODES; z++)
						transposeR[i][z][j] = routingVar[i][j][z];
			
			// Defining the required start and end nodes for each dependency
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				for(int j = 0; j < NR_NODES; j++) {
					cplex.addEq(cplex.diff(cplex.sum(routingVar[i][j]), cplex.sum(transposeR[i][j])), cplex.diff(placementVar[j][initialModules.get(i)], placementVar[j][finalModules.get(i)]));
					//cplex.addLe(cplex.sum(routingVar[i][j]), 1);
				}
			}
			
			IloLinearNumExpr[][] bwUsage = new IloLinearNumExpr[NR_NODES][NR_NODES];
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_NODES; j++) {
					bwUsage[i][j] = cplex.linearNumExpr();
					
					for(int z = 0; z < getNumberOfDependencies(); z++) {
						double bwNeeded = getmBandwidthMap()[initialModules.get(z)][finalModules.get(z)];
						bwUsage[i][j].addTerm(routingVar[z][i][j], bwNeeded);
					}
					
					cplex.addLe(bwUsage[i][j], getfBandwidthMap()[i][j]);
				}
			}
			
			// Display option
			cplex.setParam(IloCplex.Param.Simplex.Display, 0);

			long start = System.currentTimeMillis();
			// Solve
			if (cplex.solve()) {
				//System.out.println("\nValue = " + cplex.getObjValue() + "\n");
				long finish = System.currentTimeMillis();
				elapsedTime = finish - start;
				
				int[][] modulePlacementMap = new int[NR_NODES][NR_MODULES];
				int[][][] routingMap = new int[getNumberOfDependencies()][NR_NODES][NR_NODES];
				
				for(int i = 0; i < NR_NODES; i++) {
					for(int j = 0; j < NR_MODULES; j++) {
						if(cplex.getValue(placementVar[i][j]) == 0)
							modulePlacementMap[i][j] = 0;
						else
							modulePlacementMap[i][j] = 1;
					}
				}
				
				for(int i = 0; i < getNumberOfDependencies(); i++)
					for(int j = 0; j < NR_NODES; j++)
						for(int z = 0; z < NR_NODES; z++)
							routingMap[i][j][z] = (int) cplex.getValue(routingVar[i][j][z]);
				
				Job solution = new Job(this, modulePlacementMap, routingMap);
				
				valueIterMap.put(0, solution.getCost());
			    
			    if(Config.PRINT_DETAILS)
			    	AlgorithmUtils.printResults(this, solution);
				
				cplex.end();
				return solution;
			}
			
			System.out.println("Model not solved");
			cplex.end();
			return null;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return null;
		}
	}
}
