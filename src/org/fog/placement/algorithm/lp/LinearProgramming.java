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
		
		try {
			// Define new model
			IloCplex cplex = new IloCplex();
			/*cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.1);
			cplex.setParam(IloCplex.Param.TimeLimit, 3600);*/
			
			// Variables
			IloNumVar[][] placementVar = new IloNumVar[NR_NODES][NR_MODULES];
			IloNumVar[][][] tupleRoutingVar = new IloNumVar[getNumberOfDependencies()][NR_NODES][NR_NODES];
			IloNumVar[][][] migrationRoutingVar = new IloNumVar[NR_MODULES][NR_NODES][NR_NODES];
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					placementVar[i][j] = cplex.intVar(0, 1);
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						tupleRoutingVar[i][j][z] = cplex.intVar(0, 1);
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
			
			// Define objective
			IloLinearNumExpr objective = cplex.linearNumExpr();
			
			for(int i = 0; i < NR_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					
					double opCost = Config.OP_W*(getfMipsPrice()[i]*getmMips()[j] + getfRamPrice()[i]*getmRam()[j] + getfStrgPrice()[i]*getmStrg()[j]);
					double pwCost = Config.PW_W*(getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i]);					
					double prCost = Config.PR_W*(getmMips()[j]/getfMips()[i]);
					
					objective.addTerm(placementVar[i][j], opCost);	// Operational cost
					objective.addTerm(placementVar[i][j], pwCost);	// Power cost
					objective.addTerm(placementVar[i][j], prCost);	// Processing cost
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
						objective.addTerm(tupleRoutingVar[i][j][z], latencyCost);						// Latency cost
						objective.addTerm(tupleRoutingVar[i][j][z], bandwidthCost);						// Bandwidth cost
						objective.addTerm(tupleRoutingVar[i][j][z], txOpCost);							// Operational cost
						objective.addTerm(tupleRoutingVar[i][j][z], latencyCost*getfTxPwMap()[j][z]);	// Power cost
						objective.addTerm(tupleRoutingVar[i][j][z], bandwidthCost*getfTxPwMap()[j][z]);	// Power cost
					}
				}
			}
			
			for(int i = 0; i < NR_MODULES; i++) {
				double size = getmStrg()[i] + getmRam()[i];
				
				for(int j = 0; j < NR_NODES; j++) {
					for(int z = 0; z < NR_NODES; z++) {
						double mg = getfLatencyMap()[j][z] + size/(getfBandwidthMap()[j][z] + Constants.EPSILON);
						objective.addTerm(migrationRoutingVar[i][j][z], mg);
					}
				}
			}
			
			cplex.addMinimize(objective);
			constraints(cplex, placementVar, tupleRoutingVar, migrationRoutingVar);
			
			// Display option
			if(Config.PRINT_DETAILS)
				cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			else
				cplex.setOut(null);

			long start = System.currentTimeMillis();
			
			// Solve
			if (cplex.solve()) {
				long finish = System.currentTimeMillis();
				elapsedTime = finish - start;
				
				int[][] modulePlacementMap = new int[NR_NODES][NR_MODULES];
				int[][][] tupleRoutingMap = new int[getNumberOfDependencies()][NR_NODES][NR_NODES];
				int[][][] migrationRoutingMap = new int[NR_MODULES][NR_NODES][NR_NODES];
				
				for(int i = 0; i < NR_NODES; i++) {
					for(int j = 0; j < NR_MODULES; j++) {
						modulePlacementMap[i][j] = (int) Math.round(cplex.getValue(placementVar[i][j]));
					}
				}
				
				for(int i = 0; i < getNumberOfDependencies(); i++) {
					for(int j = 0; j < NR_NODES; j++) {
						for(int z = 0; z < NR_NODES; z++) {
							tupleRoutingMap[i][j][z] = (int) Math.round(cplex.getValue(tupleRoutingVar[i][j][z]));
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
				
				Job solution = new Job(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap, currentPlacement);
				
				valueIterMap.put(0, solution.getCost());
				
				if(Config.PRINT_DETAILS) {
			    	System.out.println("\n\nLP RESULTS:\n");
					
					double totalOp = 0, totalPw = 0, totalPr = 0, totalLt = 0, totalBw = 0;
					for(int i = 0; i < NR_NODES; i++) {
						for(int j = 0; j < NR_MODULES; j++) {
							totalOp += Config.OP_W*(getfMipsPrice()[i]*getmMips()[j] + getfRamPrice()[i]*getmRam()[j] + getfStrgPrice()[i]*getmStrg()[j])*cplex.getValue(placementVar[i][j]);
							totalPw += Config.PW_W*(getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i])*cplex.getValue(placementVar[i][j]);
							totalPr += Config.PR_W*(getmMips()[j]/getfMips()[i])*cplex.getValue(placementVar[i][j]);
						}
					}
					
					for(int i = 0; i < getNumberOfDependencies(); i++) {
						double dependencies = getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
						double bwNeeded = getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
						
						for(int j = 0; j < NR_NODES; j++) {
							for(int z = 0; z < NR_NODES; z++) {
								totalLt += Config.LT_W*(getfLatencyMap()[j][z]*dependencies)*cplex.getValue(tupleRoutingVar[i][j][z]);
								totalBw += Config.BW_W*(bwNeeded/(getfBandwidthMap()[j][z] + Constants.EPSILON))*cplex.getValue(tupleRoutingVar[i][j][z]);
								totalOp += Config.OP_W*(getfBwPrice()[j]*bwNeeded)*cplex.getValue(tupleRoutingVar[i][j][z]);
								
								
								totalPw += Config.LT_W*(getfLatencyMap()[j][z]*dependencies)*cplex.getValue(tupleRoutingVar[i][j][z])*getfTxPwMap()[j][z];
								totalPw += Config.BW_W*(bwNeeded/(getfBandwidthMap()[j][z] + Constants.EPSILON))*cplex.getValue(tupleRoutingVar[i][j][z])*getfTxPwMap()[j][z];
							}
						}
					}
					
					AlgorithmUtils.print("asdsadaaddasasddsa", getfTxPwMap());
					
					System.out.println("\ntotalOp: " + totalOp);
					System.out.println("totalPw: " + totalPw);
					System.out.println("totalPr: " + totalPr);
					System.out.println("totalLt: " + totalLt);
					System.out.println("totalBw: " + totalBw);
					System.out.println("LP result1 = " + cplex.getObjValue() + "\n\n");
					
			    	System.out.println("\nSOLUTION (JOB) RESULTS:\n");
			    	System.out.println("LP result = " + solution.getCost() + "\n\n");
			    	AlgorithmUtils.printResults(this, solution);
			    }
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
			if(placementVar != null) {
				// Define constraints
				IloLinearNumExpr[] usedMipsCapacity = new IloLinearNumExpr[NR_NODES];
				IloLinearNumExpr[] usedRamCapacity = new IloLinearNumExpr[NR_NODES];
				IloLinearNumExpr[] usedStrgCapacity = new IloLinearNumExpr[NR_NODES];
				
				for (int i = 0; i < NR_NODES; i++) {
					usedMipsCapacity[i] = cplex.linearNumExpr();
					usedRamCapacity[i] = cplex.linearNumExpr();
					usedStrgCapacity[i] = cplex.linearNumExpr();
					
		    		for (int j = 0; j < NR_MODULES; j++) {
		    			usedMipsCapacity[i].addTerm(placementVar[i][j], getmMips()[j]);
		    			usedRamCapacity[i].addTerm(placementVar[i][j], getmRam()[j]);
		    			usedStrgCapacity[i].addTerm(placementVar[i][j], getmStrg()[j]);
		    		}
				}
				
				// Sum of the resources needed should not pass the capacity
				for (int i = 0; i < NR_NODES; i++) {
		    		cplex.addLe(usedMipsCapacity[i], getfMips()[i]);
		    		cplex.addLe(usedRamCapacity[i], getfRam()[i]);
		    		cplex.addLe(usedStrgCapacity[i], getfStrg()[i]);
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
