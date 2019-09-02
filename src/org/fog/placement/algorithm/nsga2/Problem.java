package org.fog.placement.algorithm.nsga2;

import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Constraints;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.SingleObjectiveCostFunction;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryIntegerVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

public class Problem extends AbstractProblem {
	private final Algorithm algorithm;
	
	private final int nrNodes;
	private final int nrModules;
	private final int nrDependencies;

	public Problem(Algorithm algorithm, int nrVars) {
		super(nrVars, 1, 4);
		this.algorithm = algorithm;
		this.nrNodes = algorithm.getNumberOfNodes();
		this.nrModules = algorithm.getNumberOfModules();
		this.nrDependencies = algorithm.getNumberOfDependencies();
	}
	
	@Override
	public Solution newSolution() {
		Solution solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(), getNumberOfConstraints());
		 
		for (int i = 0; i < getNumberOfVariables(); i++) {
			solution.setVariable(i, new BinaryIntegerVariable(0, nrNodes-1));
	    }
	 
	    return solution;
	}
	

	@Override
	public void evaluate(Solution solution) {
		int[] x = EncodingUtils.getInt(solution);
		
		int[][] modulePlacementMap = extractModulePlacement(x, nrNodes, nrModules);
		int[][] tupleRoutingMap = extractTupleRouting(x, nrNodes, nrModules, nrDependencies);
		int[][] migrationRoutingMap = extractModuleRouting(x, nrNodes, nrModules, nrDependencies);
		
		Job job = new Job(algorithm, new SingleObjectiveCostFunction(), modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		double constraints = Constraints.checkConstraints(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		double cost = job.getCost()-constraints;
		solution.setObjective(0, cost);
		
		double constraint = Constraints.checkResourcesExceeded(algorithm, modulePlacementMap);
		constraint += Constraints.checkPossiblePlacement(algorithm, modulePlacementMap);
		solution.setConstraint(0, constraint);
		
		constraint = Constraints.checkDependencies(algorithm, modulePlacementMap, tupleRoutingMap);
		constraint += Constraints.checkBandwidth(algorithm, tupleRoutingMap);
		solution.setConstraint(1, constraint);
		
		constraint = Constraints.checkMigration(algorithm, modulePlacementMap, migrationRoutingMap);
		solution.setConstraint(2, constraint);
		
		constraint = Constraints.checkDeadlines(algorithm, modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
		solution.setConstraint(3, constraint);
	}
	
	static int[][] extractModulePlacement(final int[] x, int nrNodes, int nrModules) {
		int[][] result = new int[nrNodes][nrModules];
		
		for(int i = 0; i < nrModules; i++) {
			result[x[i]][i] = 1;
		}
		
		return result;
	}
	
	static int[][] extractTupleRouting(final int[] x, int nrNodes, int nrModules, int nrDependencies) {
		int[][] result = new int[nrDependencies][nrNodes];
		
		for(int i = 0; i < nrDependencies; i++) {
			for(int j = 0; j < nrNodes; j++) {
				result[i][j] = tValue(x, i, j, nrNodes, nrModules);
			}
		}
		
		return result;
	}
	
	static int[][] extractModuleRouting(final int[] x, int nrNodes, int nrModules, int nrDependencies) {
		int[][] result = new int[nrModules][nrNodes];
		
		for(int i = 0; i < nrModules; i++) {
			for(int j = 0; j < nrNodes; j++) {
				result[i][j] = vValue(x, i, j, nrNodes, nrModules, nrDependencies);
			}
		}
		
		return result;
	}
	
	private static int tValue(int[] x, int dependency, int node, int nrNodes, int nrModules) {
		return x[nrModules + dependency*nrNodes + node];
	}
	
	private static int vValue(int[] x, int module, int node, int nrNodes, int nrModules, int nrDependencies) {
		return x[nrModules + nrDependencies*nrNodes + module*nrNodes + node];
	}
	
}
