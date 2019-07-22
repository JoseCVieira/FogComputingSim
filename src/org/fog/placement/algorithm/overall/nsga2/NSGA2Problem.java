package org.fog.placement.algorithm.overall.nsga2;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.core.Constants;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryIntegerVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

public class NSGA2Problem extends AbstractProblem {	
	private Algorithm algorithm;
	private int nrVars;
	private int nrObjs;
	
	private final int NR_PLACEMENT_VARS;
	private final int NR_TUPLE_ROUTING_VARS;
	private final int NR_VM_ROUTING_VARS;
	
	private final int NR_NODES;
	private final int NR_MODULES;
	private final int NR_DEPENDENCIES;
	
	List<Integer> initialModules;
	List<Integer> finalModules;
	
	public NSGA2Problem(Algorithm algorithm, int nrVars, int nrObjs) {
		//super(# decision variables, # objectives, # constraints);
		super(nrVars, nrObjs, 1);
		
		this.algorithm = algorithm;
		this.nrVars = nrVars;
		this.nrObjs = nrObjs;
		
		NR_NODES = algorithm.getNumberOfNodes();
		NR_MODULES = algorithm.getNumberOfModules();
		NR_DEPENDENCIES = algorithm.getNumberOfDependencies();
		
		NR_PLACEMENT_VARS = NR_MODULES;
		NR_TUPLE_ROUTING_VARS = NR_DEPENDENCIES*NR_NODES;
		NR_VM_ROUTING_VARS = NR_MODULES*NR_NODES;
		
		initialModules = new ArrayList<Integer>();
		finalModules = new ArrayList<Integer>();
	}

	@Override
	public void evaluate(Solution solution) {		
		for(int i = 0; i < NR_MODULES; i++) {
			for (int j = 0; j < NR_MODULES; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
		
		calculateOperationalCost(solution);
		calculatePowerCost(solution);
		calculateProcessingCost(solution);
		calculateLatencyCost(solution);
		calculateBandwidthCost(solution);
		//calculateMigrationCost(solution);
		
		solution.setConstraint(0, 0);
		
		//defineConstraints(solution);
	}
	
	private void calculateOperationalCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			int value = pValue(solution, i);
			cost += algorithm.getfMipsPrice()[value] * algorithm.getmMips()[i] + 
					algorithm.getfRamPrice()[value] * algorithm.getmRam()[i] +
					algorithm.getfStrgPrice()[value] * algorithm.getmStrg()[i];
		}
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j) != tValue(solution, i, j-1)) {
					cost += algorithm.getfBwPrice()[tValue(solution, i, j-1)]*bwNeeded;
				}
			}
		}
		
		solution.setObjective(0, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void calculatePowerCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			int value = pValue(solution, i);
			cost += (algorithm.getfBusyPw()[value]-algorithm.getfIdlePw()[value])*(algorithm.getmMips()[i]/algorithm.getfMips()[value]);
		}
		
		solution.setObjective(1, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void calculateProcessingCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			int value = pValue(solution, i);
			cost += algorithm.getmMips()[i]/algorithm.getfMips()[value];
		}
		
		solution.setObjective(2, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void calculateLatencyCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j) != tValue(solution, i, j-1)) {
					cost += algorithm.getfLatencyMap()[tValue(solution, i, j-1)][tValue(solution, i, j)] * dependencies;
				}
			}
		}
		
		solution.setObjective(3, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void calculateBandwidthCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j) != tValue(solution, i, j-1)) {
					cost += bwNeeded/(algorithm.getfBandwidthMap()[tValue(solution, i, j-1)][tValue(solution, i, j)] + Constants.EPSILON);
				}
			}
		}
		
		solution.setObjective(4, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void calculateMigrationCost(Solution solution) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 1; j < NR_NODES; j++) {
				if(vValue(solution, i, j) != vValue(solution, i, j-1)) {
					cost += 1;
				}
			}
		}
		
		solution.setObjective(5, cost > Constants.REFERENCE_COST ? (Constants.REFERENCE_COST + random()): cost);
	}
	
	private void defineConstraints(Solution solution) {
		int violatedConstraintNum = 0;
		
		// If placement does not respects possible deployment matrix
		for(int i = 0; i < NR_MODULES; i++) {
			int value = pValue(solution, i);
			
			if(algorithm.getPossibleDeployment()[value][i] == 0)
				violatedConstraintNum += 100;
		}
		
		// If dependencies are not accomplished
		int iter = 0;
		for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_MODULES; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					if(tValue(solution, iter, 0) != pValue(solution, i))
						violatedConstraintNum += 100;
					
					if(tValue(solution, iter, NR_NODES - 1) != pValue(solution, j))
						violatedConstraintNum += 100;
					iter++;
				}
			}
		}
		
		// If fog node's resources are exceeded
		for(int i = 0; i < NR_NODES; i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalStrg = 0;
			
			for(int j = 0; j < NR_MODULES; j++) {
				if(pValue(solution, j) == i) {
					totalMips += algorithm.getmMips()[j];
					totalRam += algorithm.getmRam()[j];
					totalStrg += algorithm.getmStrg()[j];
					
					if(totalMips > algorithm.getfMips()[i] || totalRam > algorithm.getfRam()[i] || totalStrg > algorithm.getfStrg()[i])
						violatedConstraintNum += 100;
				}
			}
		}
		
		// If there is no link between the nodes
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j-1) != tValue(solution, i, j)) {
					if(algorithm.getfLatencyMap()[tValue(solution, i, j-1)][tValue(solution, i, j)] == Constants.INF) {
						violatedConstraintNum += 100;
					}
				}
			}
		}
		
		// If bandwidth links usage are exceeded
		/*double bwUsage[][] = new double[NR_NODES][NR_NODES];
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j-1) != tValue(solution, i, j)) {
					bwUsage[tValue(solution, i, j-1)][tValue(solution, i, j)] += bwNeeded;
				}
			}
		}
		
		for(int i = 0; i < NR_NODES; i++) {
			for(int j = 0; j < NR_NODES; j++) {
				if(bwUsage[i][j] > algorithm.getfBandwidthMap()[i][j])
					violatedConstraintNum++;
			}
		}
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(solution, i, j-1) != tValue(solution, i, j)) {
					if(algorithm.getfLatencyMap()[tValue(solution, i, j-1)][tValue(solution, i, j)] == Constants.INF)
						violatedConstraintNum++;
				}
			}
		}*/
		
		/*for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 1; j < NR_NODES; j++) {
				if(vValue(solution, i, j-1) != vValue(solution, i, j)) {
					if(algorithm.getfLatencyMap()[vValue(solution, i, j-1)][vValue(solution, i, j)] == Constants.INF) {
						vInv[i][j] = true;
					}
				}
			}
		}*/
		
		//System.out.println("violatedConstraintNum: " + violatedConstraintNum);
		solution.setConstraint(0, violatedConstraintNum);
		
		/*for(int i = 0; i < NR_MODULES; i++) {
			for(int j = 0; j < NR_NODES; j++) {
				if(vInv[i][j])
					solution.setConstraint(NR_PLACEMENT_VARS + NR_TUPLE_ROUTING_VARS + i*NR_NODES + j, IVALID);
				else
					solution.setConstraint(NR_PLACEMENT_VARS + NR_TUPLE_ROUTING_VARS + i*NR_NODES + j, VALID);
			}
		}*/
	}
	
	@Override
	public Solution newSolution() {
		Solution solution = new Solution(nrVars, nrObjs, 1);
		
		int[][] placement = Job.generateRandomPlacement(algorithm, NR_NODES, NR_MODULES);
		int[][] tupleRouting = Job.generateRandomTupleRouting(algorithm, placement, NR_NODES);
		
		for(int i = 0; i < NR_PLACEMENT_VARS; i++) {
			//solution.setVariable(i, EncodingUtils.newInt(0, NR_NODES-1));
			solution.setVariable(i, new BinaryIntegerVariable(findPlacement(placement, i), 0, NR_NODES-1));
		}
		
		for(int i = 0; i < NR_TUPLE_ROUTING_VARS; i++) {
			int j = (int)i/tupleRouting[0].length;
			int k = i-((int)i/tupleRouting[0].length)*tupleRouting[0].length;
			
			solution.setVariable(NR_PLACEMENT_VARS + i, new BinaryIntegerVariable(tupleRouting[j][k], 0, NR_NODES-1));
			//solution.setVariable(NR_PLACEMENT_VARS + i, EncodingUtils.newInt(0, NR_NODES-1));
		}
		
		/*for(int i = 0; i < NR_VM_ROUTING_VARS; i++) {
			solution.setVariable(NR_PLACEMENT_VARS + NR_TUPLE_ROUTING_VARS + i, EncodingUtils.newInt(0, NR_NODES-1));
		}*/
		
		return solution;
	}
	
	private int pValue(Solution solution, int module) {
		return EncodingUtils.getInt(solution.getVariable(module));
	}
	
	private int tValue(Solution solution, int dependency, int node) {
		return EncodingUtils.getInt(solution.getVariable(NR_PLACEMENT_VARS + dependency*NR_NODES + node));
	}
	
	private int vValue(Solution solution, int module, int node) {
		return EncodingUtils.getInt(solution.getVariable(NR_PLACEMENT_VARS + NR_TUPLE_ROUTING_VARS + module*NR_NODES + node));
	}
	
	private int findPlacement(int[][] placement, int index) {
		for(int i = 0; i < NR_NODES; i++) {
			if(placement[i][index] == 1){
				return i;
			}
		}
		return -1;
	}
	
	private double random() {
		return new Random().nextDouble() + new Random().nextDouble();
	}

}