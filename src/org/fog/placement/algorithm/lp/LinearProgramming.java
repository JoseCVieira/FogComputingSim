package org.fog.placement.algorithm.lp;

import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;

import ilog.concert.*;
import ilog.cplex.*;

/**
 * Class in which defines and executes the multiple objective linear programming.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class LinearProgramming extends Algorithm {
	/** Best solution found by the algorithm */
	private Solution bestSolution;
	
	/** Time at the beginning of the execution of the algorithm */
	private long start;
	
	/** Time at the end of the execution of the algorithm */
	private long finish;
	
	public LinearProgramming(final List<FogDevice> fogDevices, final List<Application> applications,
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
		bestSolution = null;
		getValueIterMap().clear();
		
		// Time at the beginning of the execution of the algorithm
		start = System.currentTimeMillis();
		
		// Solve the problem
		bestSolution = solve();
		
		// Time at the end of the execution of the algorithm
		finish = System.currentTimeMillis();
		
		setElapsedTime(finish - start);
		
		return bestSolution;
	}
	
	public Solution solve() {
		try {
			// Define model
			IloCplex cplex = new IloCplex();
			/*cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.05);
			cplex.setParam(IloCplex.Param.MIP.Strategy.HeuristicFreq, -1);
			cplex.setParam(IloCplex.Param.MIP.Strategy.Probe, 3);
			cplex.setParam(IloCplex.Param.TimeLimit, 5);*/
			
			int nrNodes = getNumberOfNodes();
			int nrModules = getNumberOfModules();
			int nrDependencies = getNumberOfDependencies();
			int nrLoops = getNumberOfLoops();
			
			// Define variables
			IloNumVar[][] placementVar = new IloNumVar[nrNodes][nrModules];
			IloNumVar[][][] tupleRoutingVar = new IloNumVar[nrDependencies][nrNodes][nrNodes];
			IloNumVar[][][] migrationRoutingVar = new IloNumVar[nrModules][nrNodes][nrNodes];
			
			// Define objectives
			IloNumExpr qsObjective = cplex.numExpr();
			IloNumExpr pwObjective = cplex.numExpr();
			IloNumExpr prObjective = cplex.numExpr();
			IloNumExpr bwObjective = cplex.numExpr();
			IloNumExpr mgObjective = cplex.numExpr();
			
			IloNumExpr[] latency = new IloNumExpr[nrLoops];
			IloNumExpr[] migLatency = new IloNumExpr[nrModules];
			
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					placementVar[i][j] = cplex.intVar(0, 1);
					
					double pr = getmMips()[j]/(getfMips()[i]*Config.MIPS_PERCENTAGE_UTIL);
					double pw = (getfBusyPw()[i]-getfIdlePw()[i])*pr;
					
					pwObjective = cplex.sum(pwObjective, cplex.prod(placementVar[i][j], pw*getfIsFogDevice()[i]));					// Power cost
					prObjective = cplex.sum(prObjective, cplex.prod(placementVar[i][j], pr*getfIsFogDevice()[i]));					// Processing cost	
				}
			}
			
			for(int i = 0; i < nrDependencies; i++) {
				double bandwidth = getmBandwidthMap()[getStartModDependency(i)][getFinalModDependency(i)];
				
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						tupleRoutingVar[i][j][z] = cplex.intVar(0, 1);
						
						double bw = bandwidth/(getfBandwidthMap()[j][z]*Config.BW_PERCENTAGE_UTIL + Constants.EPSILON);
						double pw = bw*getfTxPw()[j];
						
						bwObjective = cplex.sum(bwObjective, cplex.prod(tupleRoutingVar[i][j][z], bw*getfIsFogDevice()[j]));		// Bandwidth cost
						pwObjective = cplex.sum(pwObjective, cplex.prod(tupleRoutingVar[i][j][z], pw*getfIsFogDevice()[j]));		// Power cost
					}
				}
			}
			
			for(int i = 0; i < nrModules; i++) {
				double size = getmStrg()[i] + getmRam()[i];
				
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						migrationRoutingVar[i][j][z] = cplex.intVar(0, 1);
						
						double linkBw = getfBandwidthMap()[j][z]*(1-Config.BW_PERCENTAGE_UTIL) + Constants.EPSILON;
						/*double totalDep = 0;
						for(int l = 0; l < getNumberOfModules(); l++) {
							totalDep += getmDependencyMap()[l][i];
						}*/
						
						double mg = size/linkBw/**totalDep*/;
						
						mgObjective = cplex.sum(mgObjective, cplex.prod(migrationRoutingVar[i][j][z], mg*getfIsFogDevice()[j]));	// Migration cost
					}
				}
			}
			
			defineConstraints(cplex, placementVar, tupleRoutingVar, migrationRoutingVar, latency, migLatency);
			
			IloNumExpr[] ensureLoops = new IloNumExpr[nrNodes];
			for(int i = 0; i < nrLoops; i++) {
				ensureLoops[i] = cplex.numExpr();
				
				latency[i] = cplex.diff(latency[i], 2/*getLoopsDeadline()[i]*/);
				ensureLoops[i] = cplex.max(latency[i], 0);
				ensureLoops[i] = cplex.prod(ensureLoops[i], Integer.MAX_VALUE);
				ensureLoops[i] = cplex.min(ensureLoops[i], 1);
				
				qsObjective = cplex.sum(qsObjective, ensureLoops[i]);																// Quality of Service cost
			}
			
			//qsObjective = cplex.sum(qsObjective, cplex.max(latency));																// Quality of Service cost
			
			IloObjective qsCost = cplex.minimize(qsObjective);
			IloObjective pwCost = cplex.minimize(pwObjective);
			IloObjective prCost = cplex.minimize(prObjective);
			IloObjective bwCost = cplex.minimize(bwObjective);
			IloObjective mgCost = cplex.minimize(mgObjective);
			
			IloNumExpr[] objArray = new IloNumExpr[Config.NR_OBJECTIVES];
			objArray[Config.QOS_COST] = qsCost.getExpr();
			objArray[Config.POWER_COST] = pwCost.getExpr();
			objArray[Config.PROCESSING_COST] = prCost.getExpr();
			objArray[Config.BANDWIDTH_COST] = bwCost.getExpr();
			objArray[Config.MIGRATION_COST] = mgCost.getExpr();
			
			cplex.add(cplex.minimize(cplex.staticLex(objArray, Config.weights, Config.priorities, Config.absTols, Config.relTols, null)));
			
			// Display option
			if(Config.PRINT_DETAILS)
				cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			else
				cplex.setOut(null);
			
			// Solve
			if (cplex.solve()) {
				int[][] modulePlacementMap = new int[nrNodes][nrModules];
				int[][][] tupleRoutingMap = new int[nrDependencies][nrNodes][nrNodes];
				int[][][] migrationRoutingMap = new int[nrModules][nrNodes][nrNodes];
				
				for(int i = 0; i < nrNodes; i++) {
					for(int j = 0; j < nrModules; j++) {
						modulePlacementMap[i][j] = (int) Math.round(cplex.getValue(placementVar[i][j]));
					}
				}
				
				for(int i = 0; i < nrDependencies; i++) {
					for(int j = 0; j < nrNodes; j++) {
						for(int z = 0; z < nrNodes; z++) {
							tupleRoutingMap[i][j][z] = (int) Math.round(cplex.getValue(tupleRoutingVar[i][j][z]));
						}
					}
				}
				
				
				for(int i = 0; i < nrModules; i++) {
					for(int j = 0; j < nrNodes; j++) {
						for(int z = 0; z < nrNodes; z++) {
							migrationRoutingMap[i][j][z] = (int) Math.round(cplex.getValue(migrationRoutingVar[i][j][z]));
						}
					}
				}
				
				Solution solution = new Solution(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
				solution.setDetailedCost(Config.QOS_COST, cplex.getValue(qsObjective));
				solution.setDetailedCost(Config.POWER_COST, cplex.getValue(pwObjective));
				solution.setDetailedCost(Config.PROCESSING_COST, cplex.getValue(prObjective));
				solution.setDetailedCost(Config.BANDWIDTH_COST, cplex.getValue(bwObjective));
				solution.setDetailedCost(Config.MIGRATION_COST, cplex.getValue(mgObjective));
				
				for(int i = 0; i < nrLoops; i++)
					solution.setLoopDeadline(i, cplex.getValue(latency[i]) + 2);
				
				for(int i = 0; i < nrModules; i++)
					solution.setMigrationDeadline(i, cplex.getValue(migLatency[i]));
				
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
	
	/**
	 * Defines all constraints.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement (binary)
	 * @param routingVar the matrix which contains the routing for each module pair dependency (binary)
	 * @param migrationRoutingVar the matrix which contains the routing for each module migration (binary)
	 * @param latency the vector which represents the worst case scenario latency for each loop
	 */
	private void defineConstraints(IloCplex cplex, final IloNumVar[][] placementVar,
			final IloNumVar[][][] tupleRoutingVar, final IloNumVar[][][] migrationRoutingVar,
			IloNumExpr[] latency, IloNumExpr[] migLatency) {
		defineResourcesExceeded(cplex, placementVar);
		definePossiblePlacement(cplex, placementVar);
		defineSinglePlacement(cplex, placementVar);
		defineBandwidth(cplex, tupleRoutingVar);
		defineDependencies(cplex, placementVar, tupleRoutingVar);
		defineMigration(cplex, placementVar, migrationRoutingVar);
		defineDeadlines(cplex, placementVar, tupleRoutingVar, latency);
		defineMigrationDeadlines(cplex, placementVar, migrationRoutingVar, migLatency);
	}
	
	/**
	 * Solutions cannot exceed the machines' resources.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 */
	private void defineResourcesExceeded(IloCplex cplex, final IloNumVar[][] placementVar) {
		int nrNodes = getNumberOfNodes();
		int nrModules = getNumberOfModules();
		
		try {
			// Define constraints
			IloLinearNumExpr[] usedMipsCapacity = new IloLinearNumExpr[nrNodes];
			IloLinearNumExpr[] usedRamCapacity = new IloLinearNumExpr[nrNodes];
			IloLinearNumExpr[] usedStrgCapacity = new IloLinearNumExpr[nrNodes];
			
			for (int i = 0; i < nrNodes; i++) {
				usedMipsCapacity[i] = cplex.linearNumExpr();
				usedRamCapacity[i] = cplex.linearNumExpr();
				usedStrgCapacity[i] = cplex.linearNumExpr();
				
	    		for (int j = 0; j < nrModules; j++) {
	    			usedMipsCapacity[i].addTerm(placementVar[i][j], getmMips()[j]);
	    			usedRamCapacity[i].addTerm(placementVar[i][j], getmRam()[j]);
	    			usedStrgCapacity[i].addTerm(placementVar[i][j], getmStrg()[j]);
	    		}
	    		
	    		cplex.addLe(usedMipsCapacity[i], getfMips()[i]*Config.MIPS_PERCENTAGE_UTIL);
	    		cplex.addLe(usedRamCapacity[i], getfRam()[i]*Config.MEM_PERCENTAGE_UTIL);
	    		cplex.addLe(usedStrgCapacity[i], getfStrg()[i]*Config.STRG_PERCENTAGE_UTIL);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines the possible module deployment.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 */
	private void definePossiblePlacement(IloCplex cplex, final IloNumVar[][] placementVar) {
		int nrNodes = getNumberOfNodes();
		int nrModules = getNumberOfModules();
		
		try {
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					cplex.addLe(placementVar[i][j], getPossibleDeployment()[i][j]);
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines that each module can only be placed within one machine.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 */
	private void defineSinglePlacement(IloCplex cplex, final IloNumVar[][] placementVar) {
		int nrNodes = getNumberOfNodes();
		int nrModules = getNumberOfModules();
		
		try {
			IloNumVar[][] transposeP = new IloNumVar[nrModules][nrNodes];
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					transposeP[j][i] = placementVar[i][j];
				}
			}
			
			// One an only one placement
			for(int i = 0; i < nrModules; i++) {
				cplex.addEq(cplex.sum(transposeP[i]), 1.0);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines that the links bandwidth must not be exceeded.
	 * 
	 * @param cplex the model
	 * @param tupleRoutingVar the routingVar matrix which contains the routing for each module pair dependency
	 */
	private void defineBandwidth(IloCplex cplex, final IloNumVar[][][] tupleRoutingVar) {
		int nrNodes = getNumberOfNodes();
		int nrDependencies = getNumberOfDependencies();
		
		try {
			IloLinearNumExpr[][] bwUsage = new IloLinearNumExpr[nrNodes][nrNodes];
			
			// Bandwidth usage in each link can not be exceeded
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrNodes; j++) {
					bwUsage[i][j] = cplex.linearNumExpr();
					
					for(int z = 0; z < nrDependencies; z++) {
						double bwNeeded = getmBandwidthMap()[getStartModDependency(z)][getFinalModDependency(z)];
						bwUsage[i][j].addTerm(tupleRoutingVar[z][i][j], bwNeeded);
					}
					
					cplex.addLe(bwUsage[i][j], getfBandwidthMap()[i][j]*Config.BW_PERCENTAGE_UTIL);
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines that each dependency must be accomplished and well routed.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 * @param tupleRoutingVar the routingVar matrix which contains the routing for each module pair dependency
	 */
	private void defineDependencies(IloCplex cplex, final IloNumVar[][] placementVar, final IloNumVar[][][] tupleRoutingVar) {
		int nrNodes = getNumberOfNodes();
		int nrDependencies = getNumberOfDependencies();
		
		try {
			IloNumVar[][][] transposeR = new IloNumVar[nrDependencies][nrNodes][nrNodes];
			for(int i = 0; i < nrDependencies; i++) {
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						transposeR[i][z][j] = tupleRoutingVar[i][j][z];
					}
				}
			}
			
			// Defining the required start and end nodes for each dependency
			for(int i = 0; i < nrDependencies; i++) {
				for(int j = 0; j < nrNodes; j++) {
					cplex.addEq(cplex.diff(cplex.sum(tupleRoutingVar[i][j]), cplex.sum(transposeR[i][j])), 
							cplex.diff(placementVar[j][getStartModDependency(i)], placementVar[j][getFinalModDependency(i)]));
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines that each migration must must be accomplished and well routed according to the current placement.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 * @param migrationRoutingVar the matrix which contains the routing for each module migration
	 */
	private void defineMigration(IloCplex cplex, final IloNumVar[][] placementVar, final IloNumVar[][][] migrationRoutingVar) {
		int nrNodes = getNumberOfNodes();
		int nrModules = getNumberOfModules();
		
		try {
			// If its the first time, its not necessary to compute migration routing tables
			if(!isFirstOptimization()) {
				IloNumVar[][][] transposeMigrationR = new IloNumVar[nrModules][nrNodes][nrNodes];
				for(int i = 0; i < nrModules; i++) {
					for(int j = 0; j < nrNodes; j++) {
						for(int z = 0; z < nrNodes; z++) {
							transposeMigrationR[i][z][j] = migrationRoutingVar[i][j][z];
						}
					}
				}

				for(int i = 0; i < nrModules; i++) {
					for(int j = 0; j < nrNodes; j++) {
						cplex.addEq(cplex.diff(cplex.sum(migrationRoutingVar[i][j]), cplex.sum(transposeMigrationR[i][j])), 
								cplex.diff(getCurrentPlacement()[j][i], placementVar[j][i]));
					}
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines that each loop, in the worst case scenario, must accomplish the defined deadline.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 * @param tupleRoutingVar the routingVar matrix which contains the routing for each module pair dependency
	 * @param latency the vector which represents the worst case scenario latency for each loop
	 */
	private void defineDeadlines(IloCplex cplex, final IloNumVar[][] placementVar, IloNumVar[][][] tupleRoutingVar, IloNumExpr[] latency) {
		try {
			for(int i = 0; i < getNumberOfLoops(); i++) { // Loop index
				latency[i] = cplex.numExpr();
				
				for(int j = 0; j < getNumberOfModules() - 1; j++) {
					if(getLoops()[i][j+1] == -1) break;
					int startModuleIndex = getLoops()[i][j];
					int finalModuleIndex = getLoops()[i][j+1];
					
					processingLatency(cplex, placementVar, latency, i, startModuleIndex);
					dependencyLatency(cplex, tupleRoutingVar, latency, i, startModuleIndex, finalModuleIndex);
				}
				
				cplex.addLe(latency[i], Integer.MAX_VALUE/*getLoopsDeadline()[i]*/);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Computes the processing latency for the worst case scenario.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 * @param latency the vector which represents the worst case scenario latency for each loop
	 * @param loopIndex the loop index
	 * @param modIndex the module index
	 */
	private void processingLatency(IloCplex cplex, final IloNumVar[][] placementVar, IloNumExpr[] latency, final int loopIndex, final int modIndex) {
		if(getmMips()[modIndex] == 0) return;
		
		try {
			for(int i = 0; i < getNumberOfNodes(); i++) {
				for(int j = 0; j < getNumberOfModules(); j++) {
					for(int k = 0; k < getNumberOfModules(); k++) {
						if(getmMips()[k] == 0) continue; // Sensor and actuator modules does not count
						
						IloNumVar tmp = cplex.intVar(0, 1);
						cplex.add(cplex.ifThen(cplex.le(cplex.sum(placementVar[i][modIndex], placementVar[i][k]), 1), cplex.eq(tmp, 0)));
						cplex.add(cplex.ifThen(cplex.eq(cplex.sum(placementVar[i][modIndex], placementVar[i][k]), 2), cplex.eq(tmp, 1)));
						
						latency[loopIndex] = cplex.sum(latency[loopIndex], cplex.prod(tmp, getmCPUMap()[j][k]/(getfMips()[i]*Config.MIPS_PERCENTAGE_UTIL)));
						cplex.remove(tmp);
					}
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Computes the transmission tuple latency for the worst case scenario between a pair of modules.
	 * 
	 * @param cplex the model
	 * @param tupleRoutingVar the routingVar matrix which contains the routing for each module pair dependency
	 * @param latency the vector which represents the worst case scenario latency for each loop
	 * @param loopIndex the loop index
	 * @param moduleIndex1 the first module index
	 * @param moduleIndex2 the second module index
	 */
	private void dependencyLatency(IloCplex cplex, final IloNumVar[][][] tupleRoutingVar, IloNumExpr[] latency, final int loopIndex,
			final int moduleIndex1, final int moduleIndex2) {
		int depIndex = -1;
		
		// Find dependency index
		for (int i = 0; i < getNumberOfDependencies(); i++) {
			if(getStartModDependency(i) == moduleIndex1 && getFinalModDependency(i) == moduleIndex2) {
				depIndex = i;
				break;
			}
		}
		
		if(depIndex == -1)
			FogComputingSim.err("Should not happen (Linear programming constraints)");
		
		try {
			// For each Link, in the tuple routing map sum the total latency
			for (int i = 0; i < getNumberOfNodes(); i++) {
				for (int j = 0; j < getNumberOfNodes(); j++) {
					latency[loopIndex] = cplex.sum(latency[loopIndex], cplex.prod(tupleRoutingVar[depIndex][i][j], getfLatencyMap()[i][j]));
				}
			}
			
			for (int i = 0; i < getNumberOfDependencies(); i++) {
				double size = getmNWMap()[getStartModDependency(i)][getFinalModDependency(i)];
				
				for (int j = 0; j < getNumberOfNodes(); j++) {
					for (int k = 0; k < getNumberOfNodes(); k++) {
						IloNumVar tmp = cplex.intVar(0, 1);
						cplex.add(cplex.ifThen(cplex.le(cplex.sum(tupleRoutingVar[depIndex][j][k], tupleRoutingVar[i][j][k]), 1), cplex.eq(tmp, 0)));
						cplex.add(cplex.ifThen(cplex.eq(cplex.sum(tupleRoutingVar[depIndex][j][k], tupleRoutingVar[i][j][k]), 2), cplex.eq(tmp, 1)));
						
						double bw = getfBandwidthMap()[j][k]*Config.BW_PERCENTAGE_UTIL + Constants.EPSILON;
						latency[loopIndex] = cplex.sum(latency[loopIndex], cplex.prod(tmp, size/bw));
						cplex.remove(tmp);
					}
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines the maximum migration time in each migration.
	 * 
	 * @param cplex the model
	 * @param placementVar the matrix which represents the next module placement
	 * @param migrationRoutingVar the matrix which contains the routing for each module migration
	 * @param migLatency the vector which represents the worst case scenario latency for each migration
	 */
	private void defineMigrationDeadlines(IloCplex cplex, final IloNumVar[][] placementVar, final IloNumVar[][][] migrationRoutingVar,
			IloNumExpr[] migLatency) {
		try {
			//migLatency[loopIndex] = cplex.sum(migLatency[loopIndex], );
			for(int i = 0; i < getNumberOfModules(); i++) { // Loop index
				migLatency[i] = cplex.numExpr();
				double vmSize = getmStrg()[i] + getmRam()[i];
				
				for (int j = 0; j < getNumberOfNodes(); j++) {
					for (int k = 0; k < getNumberOfNodes(); k++) {
						double linkLat = getfLatencyMap()[j][k];
						double linkBw = getfBandwidthMap()[j][k]*(1-Config.BW_PERCENTAGE_UTIL) + Constants.EPSILON;
						migLatency[i] = cplex.sum(migLatency[i], cplex.prod(migrationRoutingVar[i][j][k], linkLat + vmSize/linkBw));
					}
				}
				
				if(!isFirstOptimization()) {
					int prevNodeIndex = Solution.findModulePlacement(getCurrentPositionInt(), i);
					
					// If the virtual machine was migrated, then sum a given setup time
					migLatency[i] = cplex.sum(migLatency[i], cplex.prod(cplex.diff(1, placementVar[prevNodeIndex][i]), Config.SETUP_VM_TIME));
				}
				
				cplex.addLe(migLatency[i], getmMigD()[i]);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
}
