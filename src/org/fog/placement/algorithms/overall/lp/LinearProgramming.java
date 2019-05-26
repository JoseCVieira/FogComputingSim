package org.fog.placement.algorithms.overall.lp;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.overall.Algorithm;
import org.fog.placement.algorithms.overall.Job;
import org.fog.placement.algorithms.overall.util.AlgorithmUtils;

import ilog.concert.*;
import ilog.cplex.*;

public class LinearProgramming extends Algorithm {
	List<Integer> initialModules = new ArrayList<Integer>();
	List<Integer> finalModules = new ArrayList<Integer>();
	
	public LinearProgramming(final List<FogDevice> fogDevices, final List<Application> applications,
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
		
		double opBest = computeOpBest();
		double pwBest = computePwBest();
		double prBest = computePrBest();
		double ltBest = computeLtBest();
		double bwBest = computeBwBest();
		
		if(opBest < 0 || pwBest < 0 || prBest < 0 || ltBest < 0 || bwBest < 0) {
			System.out.println("Model not solved");
			return null;
		}
		
		opBest = opBest == 0 ? 1 : opBest;
		pwBest = pwBest == 0 ? 1 : pwBest;
		prBest = prBest == 0 ? 1 : prBest;
		ltBest = ltBest == 0 ? 1 : ltBest;
		bwBest = bwBest == 0 ? 1 : bwBest;
		
		System.out.println("\nopBest: " + opBest);
		System.out.println("pwBest: " + pwBest);
		System.out.println("prBest: " + prBest);
		System.out.println("ltBest: " + ltBest);
		System.out.println("bwBest: " + bwBest+"\n");
		
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
					
					double pwCost = Config.PW_W*(getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i]);
					
					double prCost = Config.PR_W*(getmMips()[j]/getfMips()[i]);
					
					objective.addTerm(placementVar[i][j], opCost/opBest);	// Operational cost
					objective.addTerm(placementVar[i][j], pwCost/pwBest);	// Power cost
					objective.addTerm(placementVar[i][j], prCost/prBest);	// Processing cost
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double dependencies = getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double latencyCost = Config.LT_W*(getfLatencyMap()[j][z]*dependencies);
						double bandwidthCost =  Config.BW_W*(bwNeeded/(getfBandwidthMap()[j][z] + Constants.EPSILON));
						double txOpCost = Config.OP_W*(getfBwPrice()[j]*bwNeeded);
						
						// Transmission cost + transmission operational cost + transition cost
						objective.addTerm(routingVar[i][j][z], latencyCost/ltBest);
						objective.addTerm(routingVar[i][j][z], bandwidthCost/bwBest);
						objective.addTerm(routingVar[i][j][z], txOpCost/opBest);
					}
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, placementVar, routingVar);
			
			// Display option
			cplex.setParam(IloCplex.Param.Simplex.Display, 0);

			long start = System.currentTimeMillis();
			
			// Solve
			if (cplex.solve()) {
				System.out.println("\nopBest: " + opBest);
				System.out.println("pwBest: " + pwBest);
				System.out.println("prBest: " + prBest);
				System.out.println("ltBest: " + ltBest);
				System.out.println("bwBest: " + bwBest);
				System.out.println("LP result = " + cplex.getObjValue() + "\n");
				
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
	
	private void constraints(IloCplex cplex, IloNumVar[][] placementVar, IloNumVar[][][] routingVar) {
		try {
			
			if(placementVar != null) {
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
			}
			
			if(routingVar != null) {
				IloNumVar[][][] transposeR = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
				for(int i = 0; i < getNumberOfDependencies(); i++)
					for(int j = 0; j < NR_NODES; j++)
						for(int z = 0; z < NR_NODES; z++)
							transposeR[i][z][j] = routingVar[i][j][z];
				
				if(placementVar != null) {
					// Defining the required start and end nodes for each dependency
					for(int i = 0; i < getNumberOfDependencies(); i++) {
						for(int j = 0; j < NR_NODES; j++) {
							cplex.addEq(cplex.diff(cplex.sum(routingVar[i][j]), cplex.sum(transposeR[i][j])), cplex.diff(placementVar[j][initialModules.get(i)], placementVar[j][finalModules.get(i)]));
							//cplex.addLe(cplex.sum(routingVar[i][j]), 1);
						}
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
			}
		} catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	
	private double computeOpBest() {
		try {
			IloCplex cplex = new IloCplex();
			IloLinearNumExpr objective = cplex.linearNumExpr();
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			IloNumVar[][][] routingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					placementVar[i][j] = cplex.intVar(0, 1);
			
			for(int i = 0; i < getNumberOfDependencies(); i++)
				for(int j = 0; j < NR_NODES; j++)
					for(int z = 0; z < NR_NODES; z++)
						routingVar[i][j][z] = cplex.intVar(0, 1);
			
			
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					double opCost = Config.OP_W*(getfMipsPrice()[i]*getmMips()[j] +
							getfRamPrice()[i]*getmRam()[j] + getfMemPrice()[i]*getmMem()[j]);
					objective.addTerm(placementVar[i][j], opCost);
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double txOpCost = Config.OP_W*(getfBwPrice()[j]*bwNeeded);
						objective.addTerm(routingVar[i][j][z], txOpCost);
					}
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, placementVar, routingVar);

			if (cplex.solve()) {
				double value = cplex.getObjValue();
				cplex.end();
				return value;
			}
			cplex.end();
			return -1;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return -1;
		}
	}
	
	private double computePwBest() {
		try {
			IloCplex cplex = new IloCplex();
			IloLinearNumExpr objective = cplex.linearNumExpr();
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					placementVar[i][j] = cplex.intVar(0, 1);
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					double pwCost = Config.PW_W*(getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i]);
					objective.addTerm(placementVar[i][j], pwCost);
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, placementVar, null);

			if (cplex.solve()) {
				double value = cplex.getObjValue();
				cplex.end();
				return value;
			}
			cplex.end();
			return -1;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return -1;
		}
	}
	
	private double computePrBest() {
		try {
			IloCplex cplex = new IloCplex();
			IloLinearNumExpr objective = cplex.linearNumExpr();
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					placementVar[i][j] = cplex.intVar(0, 1);
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					objective.addTerm(placementVar[i][j], Config.PR_W*(getmMips()[j]/getfMips()[i]));
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, placementVar, null);

			if (cplex.solve()) {
				double value = cplex.getObjValue();
				cplex.end();
				return value;
			}
			cplex.end();
			return -1;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return -1;
		}
	}
	
	private double computeLtBest() {
		try {
			IloCplex cplex = new IloCplex();
			IloLinearNumExpr objective = cplex.linearNumExpr();
			IloNumVar[][][] routingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			
			for(int i = 0; i < getNumberOfDependencies(); i++)
				for(int j = 0; j < NR_NODES; j++)
					for(int z = 0; z < NR_NODES; z++)
						routingVar[i][j][z] = cplex.intVar(0, 1);
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double dependencies = getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double latencyCost = Config.LT_W*(getfLatencyMap()[j][z]*dependencies);
						objective.addTerm(routingVar[i][j][z], latencyCost);
					}
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, null, routingVar);

			if (cplex.solve()) {
				double value = cplex.getObjValue();
				cplex.end();
				return value;
			}
			cplex.end();
			return -1;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return -1;
		}
	}
	
	private double computeBwBest() {
		try {
			IloCplex cplex = new IloCplex();
			IloLinearNumExpr objective = cplex.linearNumExpr();
			IloNumVar[][][] routingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			
			for(int i = 0; i < getNumberOfDependencies(); i++)
				for(int j = 0; j < NR_NODES; j++)
					for(int z = 0; z < NR_NODES; z++)
						routingVar[i][j][z] = cplex.intVar(0, 1);
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double bandwidthCost =  Config.BW_W*(bwNeeded/(getfBandwidthMap()[j][z] + Constants.EPSILON));
						objective.addTerm(routingVar[i][j][z], bandwidthCost);
					}
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, null, routingVar);

			if (cplex.solve()) {
				double value = cplex.getObjValue();
				cplex.end();
				return value;
			}
			cplex.end();
			return -1;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return -1;
		}
	}
	
}
