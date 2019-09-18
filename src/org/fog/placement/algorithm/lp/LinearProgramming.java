package org.fog.placement.algorithm.lp;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.MultiObjectiveJob;

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
	private Job bestSolution;
	
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
	public Job execute() {
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
	
	public Job solve() {
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
			
			// Define variables
			IloNumVar[][] placementVar = new IloNumVar[nrNodes][nrModules];
			IloNumVar[][][] tupleRoutingVar = new IloNumVar[getNumberOfDependencies()][nrNodes][nrNodes];
			IloNumVar[][][] migrationRoutingVar = new IloNumVar[nrModules][nrNodes][nrNodes];
			
			//IloNumExpr[] latency = new IloNumExpr[getLoops().length];
			
			// Define objectives
			IloNumExpr opObjective = cplex.numExpr();
			IloNumExpr pwObjective = cplex.numExpr();
			IloNumExpr prObjective = cplex.numExpr();
			IloNumExpr bwObjective = cplex.numExpr();
			IloNumExpr mgObjective = cplex.numExpr();
			
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					placementVar[i][j] = cplex.intVar(0, 1);
					
					double pr = getmMips()[j]/getfMips()[i];
					double pw = (getfBusyPw()[i]-getfIdlePw()[i])*pr;
					double op = getfMipsPrice()[i]*getmMips()[j] + getfRamPrice()[i]*getmRam()[j] + getfStrgPrice()[i]*getmStrg()[j] + pw*getfPwPrice()[i];
					
					opObjective = cplex.sum(opObjective, cplex.prod(placementVar[i][j], op));	// Operational cost
					pwObjective = cplex.sum(pwObjective, cplex.prod(placementVar[i][j], pw));	// Power cost
					prObjective = cplex.sum(prObjective, cplex.prod(placementVar[i][j], pr));	// Processing cost	
				}
			}
			
			for(int i = 0; i < nrDependencies; i++) {
				double bandwidth = getmBandwidthMap()[getStartModDependency(i)][getFinalModDependency(i)];
				
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						tupleRoutingVar[i][j][z] = cplex.intVar(0, 1);
						
						double bw = bandwidth/(getfBandwidthMap()[j][z]*Config.BW_PERCENTAGE_TUPLES + Constants.EPSILON);
						double pw = bw*getfTxPw()[j];
						double op = pw*getfPwPrice()[j] + bandwidth*getfBwPrice()[j];
						
						bwObjective = cplex.sum(bwObjective, cplex.prod(tupleRoutingVar[i][j][z], bw));	// Bandwidth cost
						opObjective = cplex.sum(opObjective, cplex.prod(tupleRoutingVar[i][j][z], op));	// Operational cost
						pwObjective = cplex.sum(pwObjective, cplex.prod(tupleRoutingVar[i][j][z], pw));	// Power cost						
					}
				}
			}
			
			for(int i = 0; i < nrModules; i++) {
				double size = getmStrg()[i] + getmRam()[i];
				
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						migrationRoutingVar[i][j][z] = cplex.intVar(0, 1);
						
						double linkBw = getfBandwidthMap()[j][z]*(1-Config.BW_PERCENTAGE_TUPLES) + Constants.EPSILON;
						double totalDep = 0;
						for(int l = 0; l < getNumberOfModules(); l++) {
							totalDep += getmDependencyMap()[l][i];
						}
						
						double mg = size/linkBw*totalDep;
						
						mgObjective = cplex.sum(mgObjective, cplex.prod(migrationRoutingVar[i][j][z], mg));	// Migration cost
					}
				}
			}
			
			defineConstraints(cplex, placementVar, tupleRoutingVar, migrationRoutingVar/*, latency*/);
			
			IloObjective opCost = cplex.minimize(opObjective);
			IloObjective pwCost = cplex.minimize(pwObjective);
			IloObjective prCost = cplex.minimize(prObjective);
			IloObjective bwCost = cplex.minimize(bwObjective);
			IloObjective mgCost = cplex.minimize(mgObjective);
			
			IloNumExpr[] objArray = new IloNumExpr[Config.NR_OBJECTIVES];
			objArray[Config.OPERATIONAL_COST] = opCost.getExpr();
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
				
				MultiObjectiveJob solution = new MultiObjectiveJob(this, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
				solution.setDetailedCost(Config.OPERATIONAL_COST, cplex.getValue(opObjective));
				solution.setDetailedCost(Config.POWER_COST, cplex.getValue(pwObjective));
				solution.setDetailedCost(Config.PROCESSING_COST, cplex.getValue(prObjective));
				solution.setDetailedCost(Config.BANDWIDTH_COST, cplex.getValue(bwObjective));
				solution.setDetailedCost(Config.MIGRATION_COST, cplex.getValue(mgObjective));
				
				/*if(Config.PRINT_ALGORITHM_CONSTRAINTS) {
					for(int i = 0; i < getLoops().length; i++) {
						System.out.print("\nLoop " + i + ": [ " );
						
						for(int j = 0; j < getNumberOfModules(); j++) {
							if(j == getNumberOfModules() - 1 || getLoops()[i][j+1] == -1) {
								System.out.print(getmName()[getLoops()[i][j]] + " ]");
								break;
							}
							System.out.print(getmName()[getLoops()[i][j]] + " -> ");
						}
						
						System.out.print("\t Worst case latency: " + cplex.getValue(latency[i]) + " sec\t Deadline: " + getLoopsDeadline()[i] + " sec\n");
					}
				}*/
				
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
	 * @param al the object which contains all information about the topology and which algorithm was used
	 * @param placementVar the matrix which represents the next module placement (binary)
	 * @param routingVar the matrix which contains the routing for each module pair dependency (binary)
	 * @param migrationRoutingVar the matrix which contains the routing for each module migration (binary)
	 */
	private void defineConstraints(IloCplex cplex, final IloNumVar[][] placementVar,
			final IloNumVar[][][] tupleRoutingVar, final IloNumVar[][][] migrationRoutingVar/*, IloNumExpr[] latency*/) {
		defineResourcesExceeded(cplex, placementVar);
		definePossiblePlacement(cplex, placementVar);
		defineSinglePlacement(cplex, placementVar);
		defineBandwidth(cplex, tupleRoutingVar);
		defineDependencies(cplex, placementVar, tupleRoutingVar);
		defineMigration(cplex, placementVar, migrationRoutingVar);
		//defineDeadlines(cplex, placementVar, tupleRoutingVar, migrationRoutingVar, latency);
	}
	
	/**
	 * Solutions cannot exceed the machines' resources.
	 * 
	 * @param cplex the model
	 * @param al the object which contains all information about the topology and which algorithm was used
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
	    		
	    		cplex.addLe(usedMipsCapacity[i], getfMips()[i]);
	    		cplex.addLe(usedRamCapacity[i], getfRam()[i]);
	    		cplex.addLe(usedStrgCapacity[i], getfStrg()[i]);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Defines the possible module deployment.
	 * 
	 * @param cplex the model
	 * @param al the object which contains all information about the topology and which algorithm was used
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
	 * @param al object the which contains all information about the topology and which algorithm was used
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
	 * @param al the object which contains all information about the topology and which algorithm was used
	 * @param routingVar the matrix which contains the routing for each module pair dependency
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
					
					cplex.addLe(bwUsage[i][j], getfBandwidthMap()[i][j] * Config.BW_PERCENTAGE_TUPLES);
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
	 * @param al the object which contains all information about the topology and which algorithm was used
	 * @param placementVar the matrix which represents the next module placement
	 * @param routingVar the routingVar matrix which contains the routing for each module pair dependency
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
	 * @param al the object which contains all information about the topology and which algorithm was used
	 * @param placementVar the matrix which represents the next module placement
	 * @param migrationRoutingVar  the matrix which contains the routing for each module migration
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
	
	private void defineDeadlines(IloCplex cplex, final IloNumVar[][] placementVar, final IloNumVar[][][] tupleRoutingVar,
			final IloNumVar[][][] migrationRoutingVar, IloNumExpr[] latency) {
		
		try {
			for(int i = 0; i < getLoops().length; i++) { // Loop index
				latency[i] = cplex.numExpr();
				ArrayList<Integer> computedMigrations = new ArrayList<Integer>();
				
				for(int j = 0; j < getNumberOfModules(); j++) {
					int source = getLoops()[i][j];
					int depIndex = -1;
					
					// Virtual machine migration latency
					if(!computedMigrations.contains(source)) {
						computedMigrations.add(source);
						
						double vmSize = getmStrg()[source] + getmRam()[source];
						
						for (int l = 0; l < getNumberOfNodes(); l++) {
							for (int k = 0; k < getNumberOfNodes(); k++) {
								double linkLat = getfLatencyMap()[l][k];
								double linkBw = getfBandwidthMap()[l][k];
								latency[i] = cplex.sum(latency[i], cplex.prod(migrationRoutingVar[source][l][k], linkLat + vmSize/(linkBw + Constants.EPSILON)));
							}
						}
						
						if(!isFirstOptimization()) {
							int prevNodeIndex = Job.findModulePlacement(getCurrentPositionInt(), source);
							
							// If the virtual machine was migrated, then sum a given setup time
							latency[i] = cplex.sum(latency[i], cplex.prod(cplex.diff(1, placementVar[prevNodeIndex][source]), Config.SETUP_VM_TIME));
						}
					}
					
					if(j == getNumberOfModules()-1 || getLoops()[i][j+1] == -1) break;
					
					int dest = getLoops()[i][j+1];
					for (int l = 0; l < getNumberOfDependencies(); l++) {
						if(getStartModDependency(l) == source && getFinalModDependency(l) == dest) {
							depIndex = l;
							break;
						}
					}
					
					if(depIndex == -1)
						FogComputingSim.err("Should not happen (Constraints linear programming)");
					
					double depBw = getmBandwidthMap()[source][dest];
					double depNw = getmNWMap()[source][dest];
					double depCpu = getmCPUMap()[source][dest];
					double modMips = getmMips()[dest];
					
					// Processing latency (sensor and actuator modules does not count)
					if(modMips != 0) latency[i] = cplex.sum(latency[i], depCpu/modMips);
					
					// Tuple transmission latency
					for (int l = 0; l < getNumberOfNodes(); l++) {
						for (int k = 0; k < getNumberOfNodes(); k++) {
							double linkLat = getfLatencyMap()[l][k];
							
							// Sensor and actuator modules does not count
							if(depBw != 0)
								latency[i] = cplex.sum(latency[i], cplex.prod(tupleRoutingVar[depIndex][l][k], linkLat + depNw/depBw));
						}
						
					}
				}
				
				cplex.addLe(latency[i], getLoopsDeadline()[i]);
			}
			
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
}
