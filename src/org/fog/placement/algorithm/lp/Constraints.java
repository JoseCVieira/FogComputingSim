package org.fog.placement.algorithm.lp;

import org.fog.core.Config;
import org.fog.placement.algorithm.Algorithm;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

/**
 * Class in which constraints are defined for the problem defined using CPLEX framework.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since   July, 2019
 */
class Constraints {
	/**
	 * Verifies if all constraints are met.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param placementVar matrix which represents the next module placement (binary)
	 * @param routingVar matrix which contains the routing for each module pair dependency (binary)
	 * @param migrationRoutingVar matrix which contains the routing for each module migration (binary)
	 */
	static void checkConstraints(IloCplex cplex, final Algorithm algorithm, final IloNumVar[][] placementVar,
			final IloNumVar[][][] routingVar, final IloNumVar[][][] migrationRoutingVar) {		
		checkResourcesExceeded(cplex, algorithm, placementVar);
		checkPossiblePlacement(cplex, algorithm, placementVar);
		checkMultiplePlacement(cplex, algorithm, placementVar);
		checkBandwidth(cplex, algorithm, routingVar);
		checkDependencies(cplex, algorithm, placementVar, routingVar);
		checkMigration(cplex, algorithm, placementVar, migrationRoutingVar);
	}
	
	/**
	 * Check whether the solutions placement contains modules which are exceeding the machines' resources.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param placementVar matrix which represents the next module placement
	 */
	private static void checkResourcesExceeded(IloCplex cplex, final Algorithm al, final IloNumVar[][] placementVar) {
		int nrNodes = al.getNumberOfNodes();
		int nrModules = al.getNumberOfModules();
		
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
	    			usedMipsCapacity[i].addTerm(placementVar[i][j], al.getmMips()[j]);
	    			usedRamCapacity[i].addTerm(placementVar[i][j], al.getmRam()[j]);
	    			usedStrgCapacity[i].addTerm(placementVar[i][j], al.getmStrg()[j]);
	    		}
	    		
	    		cplex.addLe(usedMipsCapacity[i], al.getfMips()[i]);
	    		cplex.addLe(usedRamCapacity[i], al.getfRam()[i]);
	    		cplex.addLe(usedStrgCapacity[i], al.getfStrg()[i]);
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check whether the solutions placement respects the possible deployment matrix.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param placementVar matrix which represents the next module placement
	 */
	private static void checkPossiblePlacement(IloCplex cplex, final Algorithm al, final IloNumVar[][] placementVar) {
		int nrNodes = al.getNumberOfNodes();
		int nrModules = al.getNumberOfModules();
		
		try {
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrModules; j++) {
					cplex.addLe(placementVar[i][j], al.getPossibleDeployment()[i][j]);
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check whether the solutions placement contains modules which have not been deployed or have been placed in multiple machines.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param placementVar matrix which represents the next module placement
	 */
	private static void checkMultiplePlacement(IloCplex cplex, final Algorithm al, final IloNumVar[][] placementVar) {
		int nrNodes = al.getNumberOfNodes();
		int nrModules = al.getNumberOfModules();
		
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
	 * Check whether bandwidth usage is exceeded.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param routingVar matrix which contains the routing for each module pair dependency
	 */
	private static void checkBandwidth(IloCplex cplex, final Algorithm al, final IloNumVar[][][] routingVar) {
		int nrNodes = al.getNumberOfNodes();
		
		try {
			IloLinearNumExpr[][] bwUsage = new IloLinearNumExpr[nrNodes][nrNodes];
			
			// Bandwidth usage in each link can not be exceeded
			for(int i = 0; i < nrNodes; i++) {
				for(int j = 0; j < nrNodes; j++) {
					bwUsage[i][j] = cplex.linearNumExpr();
					
					for(int z = 0; z < al.getNumberOfDependencies(); z++) {
						double bwNeeded = al.getmBandwidthMap()[al.getStartModDependency(z)][al.getFinalModDependency(z)];
						bwUsage[i][j].addTerm(routingVar[z][i][j], bwNeeded);
					}
					
					cplex.addLe(bwUsage[i][j], al.getfBandwidthMap()[i][j] * Config.BW_PERCENTAGE_TUPLES);
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Check whether all dependencies are accomplished.
	 * 
	 * @param cplex the model
	 * @param al object which contains all information about the topology and which algorithm was used
	 * @param placementVar matrix which represents the next module placement
	 * @param routingVar routingVar matrix which contains the routing for each module pair dependency
	 */
	private static void checkDependencies(IloCplex cplex, final Algorithm al, final IloNumVar[][] placementVar,
			final IloNumVar[][][] routingVar) {
		int nrNodes = al.getNumberOfNodes();
		
		try {
			IloNumVar[][][] transposeR = new IloNumVar[al.getNumberOfDependencies()][nrNodes][nrNodes];
			for(int i = 0; i < al.getNumberOfDependencies(); i++) {
				for(int j = 0; j < nrNodes; j++) {
					for(int z = 0; z < nrNodes; z++) {
						transposeR[i][z][j] = routingVar[i][j][z];
					}
				}
			}
			
			// Defining the required start and end nodes for each dependency
			for(int i = 0; i < al.getNumberOfDependencies(); i++) {
				for(int j = 0; j < nrNodes; j++) {
					cplex.addEq(cplex.diff(cplex.sum(routingVar[i][j]), cplex.sum(transposeR[i][j])), 
							cplex.diff(placementVar[j][al.getStartModDependency(i)], placementVar[j][al.getFinalModDependency(i)]));
				}
			}
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
	private static void checkMigration(IloCplex cplex, final Algorithm al, final IloNumVar[][] placementVar,
			final IloNumVar[][][] migrationRoutingVar) {
		int nrNodes = al.getNumberOfNodes();
		int nrModules = al.getNumberOfModules();
		
		try {
			// If its the first time, its not necessary to compute migration routing tables
			if(!al.isFirstOptimization()) {
				IloNumVar[][][] transposeMigrationR = new IloNumVar[al.getNumberOfModules()][nrNodes][nrNodes];
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
								cplex.diff(al.getCurrentPlacement()[j][i] , placementVar[j][i]));
					}
				}
			}			
		}catch (IloException e) {
			e.printStackTrace();
		}
	}
	
}
