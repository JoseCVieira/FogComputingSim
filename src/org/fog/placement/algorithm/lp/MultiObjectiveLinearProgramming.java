package org.fog.placement.algorithm.lp;

import java.util.List;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;

import ilog.concert.*;
import ilog.cplex.*;

/**
 * Class in which defines and executes the multiple objective linear programming.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since  July, 2019
 */
public class MultiObjectiveLinearProgramming extends Algorithm {
	/** Best solution found by the algorithm */
	private Job bestSolution;
	
	/** Time at the beginning of the execution of the algorithm */
	private long start;
	
	/** Time at the end of the execution of the algorithm */
	private long finish;
	
	public MultiObjectiveLinearProgramming(final List<FogDevice> fogDevices, final List<Application> applications,
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
			
			// Define variables
			IloNumVar[][] placementVar = new IloNumVar[nrNodes][nrModules];
			IloNumVar[][][] tupleRoutingVar = new IloNumVar[getNumberOfDependencies()][nrNodes][nrNodes];
			IloNumVar[][][] migrationRoutingVar = new IloNumVar[nrModules][nrNodes][nrNodes];
			
			// Define objectives
			IloNumExpr opObjective = cplex.numExpr();
			IloNumExpr pwObjective = cplex.numExpr();
			IloNumExpr prObjective = cplex.numExpr();
			IloNumExpr ltObjective = cplex.numExpr();
			IloNumExpr bwObjective = cplex.numExpr();
			IloNumExpr mgObjective = cplex.numExpr();
			
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					placementVar[i][j] = cplex.intVar(0, 1);
					
					double pw = (getfBusyPw()[i]-getfIdlePw()[i])*(getmMips()[j]/getfMips()[i] + CloudSim.getMinTimeBetweenEvents());
					double op = getfMipsPrice()[i]*getmMips()[j] + getfRamPrice()[i]*getmRam()[j] + getfStrgPrice()[i]*getmStrg()[j] + pw*getfEnPrice()[i];
					double pr = getmMips()[j]/getfMips()[i];
					
					opObjective = cplex.sum(opObjective, cplex.prod(placementVar[i][j], op));	// Operational cost
					pwObjective = cplex.sum(pwObjective, cplex.prod(placementVar[i][j], pw));	// Power cost
					prObjective = cplex.sum(prObjective, cplex.prod(placementVar[i][j], pr));	// Processing cost	
				}
			}
			
			for(int i = 0; i < getNumberOfDependencies(); i++) {
				double dependencies = getmDependencyMap()[getStartModDependency(i)][getFinalModDependency(i)];
				double bwNeeded = getmBandwidthMap()[getStartModDependency(i)][getFinalModDependency(i)];
				
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						tupleRoutingVar[i][j][z] = cplex.intVar(0, 1);
						
						double lt = getfLatencyMap()[j][z]*dependencies;
						double bw = bwNeeded/((getfBandwidthMap()[j][z] + Constants.EPSILON)*Config.BW_PERCENTAGE_TUPLES);
						double op = getfBwPrice()[j]*bwNeeded;
						double pw = bw*getfTxPw()[j];
						
						ltObjective = cplex.sum(ltObjective, cplex.prod(tupleRoutingVar[i][j][z], lt));	// Latency cost
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
						
						double mg = getfLatencyMap()[j][z] + size/((getfBandwidthMap()[j][z] + Constants.EPSILON)*(1-Config.BW_PERCENTAGE_TUPLES));
						
						mgObjective = cplex.sum(mgObjective, cplex.prod(migrationRoutingVar[i][j][z], mg));	// Migration cost
					}
				}
			}
			
			Constraints.checkConstraints(cplex, this, placementVar, tupleRoutingVar, migrationRoutingVar);
			
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
			
			cplex.add(cplex.minimize(cplex.staticLex(objArray, Config.weights, Config.priorities, Config.absTols, Config.relTols, null)));
			
			// Display option
			if(Config.PRINT_DETAILS)
				cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			else
				cplex.setOut(null);
			
			// Solve
			if (cplex.solve()) {
				
				int[][] modulePlacementMap = new int[nrNodes][nrModules];
				int[][][] tupleRoutingMap = new int[getNumberOfDependencies()][nrNodes][nrNodes];
				int[][][] migrationRoutingMap = new int[nrModules][nrNodes][nrNodes];
				
				for(int i = 0; i < nrNodes; i++) {
					for(int j = 0; j < nrModules; j++) {
						modulePlacementMap[i][j] = (int) Math.round(cplex.getValue(placementVar[i][j]));
					}
				}
				
				for(int i = 0; i < getNumberOfDependencies(); i++) {
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
				
				Job solution = new Job(this, null, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
				
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
