package org.fog.placement.algorithm.overall.lp;

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

import ilog.concert.*;
import ilog.cplex.*;

public class MultiObjectiveLinearProgramming extends Algorithm {
	List<Integer> initialModules = new ArrayList<Integer>();
	List<Integer> finalModules = new ArrayList<Integer>();
	
	public MultiObjectiveLinearProgramming(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Job execute() {
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
			/*cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.05);
			cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, -1);
			cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, 3);*/
			
			//cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.5);
			//cplex.setParam(IloCplex.Param.TimeLimit, 5);
			
			// Variables
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			IloNumVar[][][] routingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			IloNumVar[][][] migrationRoutingVar = new IloNumVar[NR_MODULES][NR_NODES][NR_NODES];
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					placementVar[i][j] = cplex.intVar(0, 1);
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						routingVar[i][j][z] = cplex.intVar(0, 1);
					}
				}
			}
			
			for(int i = 0; i < NR_MODULES; i++) {
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						migrationRoutingVar[i][j][z] = cplex.intVar(0, 1);
					}
				}
			}
			
			// Define objectives
			IloNumExpr opObjective = cplex.numExpr();
			IloNumExpr pwObjective = cplex.numExpr();
			IloNumExpr prObjective = cplex.numExpr();
			IloNumExpr ltObjective = cplex.numExpr();
			IloNumExpr bwObjective = cplex.numExpr();
			IloNumExpr mgObjective = cplex.numExpr();
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					double op = getfMipsPrice()[i]*getmMips()[j] + getfRamPrice()[i]*getmRam()[j] + getfMemPrice()[i]*getmMem()[j];
					double pw = (getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i]);
					double pr = getmMips()[j]/getfMips()[i];
					
					opObjective = cplex.sum(opObjective, cplex.prod(placementVar[i][j], op));		// Operational cost
					pwObjective = cplex.sum(pwObjective, cplex.prod(placementVar[i][j], pw));		// Power cost
					prObjective = cplex.sum(prObjective, cplex.prod(placementVar[i][j], pr));		// Processing cost	
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double dependencies = getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double lt = getfLatencyMap()[j][z]*dependencies;
						double bw = bwNeeded/(getfBandwidthMap()[j][z] + Constants.EPSILON);
						double op = getfBwPrice()[j]*bwNeeded;
						
						ltObjective = cplex.sum(ltObjective, cplex.prod(routingVar[i][j][z], lt));	// Latency cost
						bwObjective = cplex.sum(bwObjective, cplex.prod(routingVar[i][j][z], bw));	// Bandwidth cost
						opObjective = cplex.sum(opObjective, cplex.prod(routingVar[i][j][z], op));	// Operational cost
					}
				}
			}
			
			for(int i = 0; i < NR_MODULES; i++) {
				double size = getmMem()[i] + getmRam()[i];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double mg = getfLatencyMap()[j][z] + size/(getfBandwidthMap()[j][z] + Constants.EPSILON);
						mgObjective = cplex.sum(mgObjective, cplex.prod(migrationRoutingVar[i][j][z], mg));
					}
				}
			}
			
			//cplex.addMinimize(objective);
			constraints(cplex, placementVar, routingVar, migrationRoutingVar);
			
			IloObjective opCost = cplex.minimize(opObjective);
			IloObjective pwCost = cplex.minimize(pwObjective);
			IloObjective prCost = cplex.minimize(prObjective);
			IloObjective ltCost = cplex.minimize(ltObjective);
			IloObjective bwCost = cplex.minimize(bwObjective);
			IloObjective mgCost = cplex.minimize(mgObjective);
			
			IloNumExpr[] objArray = new IloNumExpr[] {
					opCost.getExpr(),
					pwCost.getExpr(),
					prCost.getExpr(),
					ltCost.getExpr(),
					bwCost.getExpr(),
					mgCost.getExpr()
			};
			
			cplex.add(cplex.minimize(cplex.staticLex(objArray, null, Config.priorities, null, null, null)));
			
			// Display option
			/*if(Config.PRINT_DETAILS)
				cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			else
				cplex.setOut(null);*/
			cplex.setParam(IloCplex.Param.Simplex.Display, 0);

			long start = System.currentTimeMillis();
			
			// Solve
			if (cplex.solve()) {				
				long finish = System.currentTimeMillis();
				elapsedTime = finish - start;
				
				int[][] modulePlacementMap = new int[NR_NODES][NR_MODULES];
				int[][][] routingMap = new int[getNumberOfDependencies()][NR_NODES][NR_NODES];
				int[][][] migrationRoutingMap = new int[NR_MODULES][NR_NODES][NR_NODES];
				
				for(int i = 0; i < NR_NODES; i++) {
					for(int j = 0; j < NR_MODULES; j++) {
						modulePlacementMap[i][j] = (int) Math.round(cplex.getValue(placementVar[i][j]));
					}
				}
				
				for(int i = 0; i < getNumberOfDependencies(); i++) {
					for(int j = 0; j < NR_NODES; j++) {
						for(int z = 0; z < NR_NODES; z++) {
							routingMap[i][j][z] = (int) Math.round(cplex.getValue(routingVar[i][j][z]));
						}
					}
				}
				
				for(int i = 0; i < NR_MODULES; i++) {
					for(int j = 0; j < NR_NODES; j++) {
						for(int z = 0; z < NR_NODES; z++) {
							migrationRoutingMap[i][j][z] = (int) Math.round(cplex.getValue(migrationRoutingVar[i][j][z]));
						}
					}
				}				
				
				Job solution = new Job(this, modulePlacementMap, routingMap, migrationRoutingMap, currentPlacement);
				
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
	
	private void constraints(IloCplex cplex, IloNumVar[][] placementVar, IloNumVar[][][] routingVar, IloNumVar[][][] migrationRoutingVar) {
		try {
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
			
			IloNumVar[][][] transposeR = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						transposeR[i][z][j] = routingVar[i][j][z];
					}
				}
			}
			
			// Defining the required start and end nodes for each dependency
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				for(int j = 0; j < NR_NODES; j++) {
					cplex.addEq(cplex.diff(cplex.sum(routingVar[i][j]), cplex.sum(transposeR[i][j])), 
							cplex.diff(placementVar[j][initialModules.get(i)], placementVar[j][finalModules.get(i)]));
				}
			}
			
			// If its the first time, its not necessary to compute migration routing tables
			if(!isFirstOptimization()) {
				IloNumVar[][][] transposeMigrationR = new IloNumVar[getNumberOfModules()][NR_NODES][NR_NODES];
				for(int i = 0; i < NR_MODULES; i++) {
					for(int j = 0; j < NR_NODES; j++) {
						for(int z = 0; z < NR_NODES; z++) {
							transposeMigrationR[i][z][j] = migrationRoutingVar[i][j][z];
						}
					}
				}
				
				for(int i = 0; i < NR_MODULES; i++) {
					for(int j = 0; j < NR_NODES; j++) {
						cplex.addEq(cplex.diff(cplex.sum(migrationRoutingVar[i][j]), cplex.sum(transposeMigrationR[i][j])), 
								cplex.diff(currentPlacement[j][i], placementVar[j][i]));
					}
				}
			}
			
			
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
}
