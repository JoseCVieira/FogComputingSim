package org.fog.placement.algorithm.overall.nsga2;

import java.util.ArrayList;
import java.util.List;

import org.fog.core.Constants;
import org.fog.placement.algorithm.Algorithm;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.BinaryIntegerVariable;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

public class Problem extends AbstractProblem {
	private final Algorithm algorithm;
	
	private final int NR_NODES;
	private final int NR_MODULES;
	private final int NR_DEPENDENCIES;
	
	private final List<Integer> initialModules;
	private final List<Integer> finalModules;

	public Problem(Algorithm algorithm, int nrVars, int nrObjs) {
		super(nrVars, nrObjs, 1);
		this.algorithm = algorithm;
		this.NR_NODES = algorithm.getNumberOfNodes();
		this.NR_MODULES = algorithm.getNumberOfModules();
		this.NR_DEPENDENCIES = algorithm.getNumberOfDependencies();
		
		initialModules = new ArrayList<Integer>();
		finalModules = new ArrayList<Integer>();
		for(int i = 0; i < NR_MODULES; i++) {
			for (int j = 0; j < NR_MODULES; j++) {
				if(algorithm.getmDependencyMap()[i][j] != 0) {
					initialModules.add(i);
					finalModules.add(j);
				}
			}
		}
	}
	
	@Override
	public Solution newSolution() {
		Solution solution = new Solution(getNumberOfVariables(), getNumberOfObjectives(), 1);
		 
	    for (int i = 0; i < NR_MODULES; i++) {
	    	List<Integer> possibleValues = new ArrayList<Integer>();
	    	
	    	int lowerBound = -1, upperBound = -1;
	    	
	    	for(int j = 0; j < algorithm.getNumberOfNodes(); j++) {
	    		if(algorithm.getPossibleDeployment()[j][i] == 1)
	    			possibleValues.add(j);
	    		
	    		if(lowerBound == -1 && algorithm.getPossibleDeployment()[j][i] == 1)
	    			lowerBound = j;
	    		
	    		if(algorithm.getPossibleDeployment()[j][i] == 1)
	    			upperBound = j;
	    	}
	    	
	    	if(lowerBound == upperBound && lowerBound != 0)
	    		lowerBound--;
	    	else if(lowerBound == upperBound && lowerBound == 0)
	    		upperBound++;
	    	
	    	solution.setVariable(i, new BinaryIntegerVariable(lowerBound, upperBound));
	    }
	    
	    for (int i = 0; i < NR_DEPENDENCIES*NR_NODES; i++) {			
			solution.setVariable(NR_MODULES + i, new BinaryIntegerVariable(0, NR_NODES-1));
	    }
	 
	    return solution;
	}
	

	@Override
	public void evaluate(Solution solution) {
		int[] x = EncodingUtils.getInt(solution);
	    double[] f = new double[numberOfObjectives], c;
	    
	    f[0] = calculateOperationalCost(x);
	    f[1] = calculatePowerCost(x);
	    f[2] = calculateProcessingCost(x);
	    f[3] = calculateLatencyCost(x);
	    f[4] = calculateBandwidthCost(x);
	    c = defineConstraints(x, numberOfVariables);
	    
	    solution.setConstraints(c);
	    solution.setObjectives(f);
	}
	
	private double calculateOperationalCost(int[] x) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			cost += algorithm.getfMipsPrice()[x[i]] * algorithm.getmMips()[i] + 
					algorithm.getfRamPrice()[x[i]] * algorithm.getmRam()[i] +
					algorithm.getfStrgPrice()[x[i]] * algorithm.getmStrg()[i];
		}
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(x, i, j) != tValue(x, i, j-1)) {
					cost += algorithm.getfBwPrice()[tValue(x, i, j-1)]*bwNeeded;
				}
			}
		}
		
		return cost;
	}
	
	private double calculatePowerCost(int[] x) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			cost += (algorithm.getfBusyPw()[x[i]]-algorithm.getfIdlePw()[x[i]])*(algorithm.getmMips()[i]/algorithm.getfMips()[x[i]]);
		}
		
		return cost;
	}
	
	private double calculateProcessingCost(int[] x) {
		double cost = 0;
		
		for(int i = 0; i < NR_MODULES; i++) {
			cost += algorithm.getmMips()[i]/algorithm.getfMips()[x[i]];
		}
		
		return cost;
	}
	
	private double calculateLatencyCost(int[] x) {
		double cost = 0;
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double dependencies = algorithm.getmDependencyMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(x, i, j) != tValue(x, i, j-1)) {
					cost += algorithm.getfLatencyMap()[tValue(x, i, j-1)][tValue(x, i, j)] * dependencies;
				}
			}
		}
		
		return cost;
	}
	
	private double calculateBandwidthCost(int[] x) {
		double cost = 0;
		
		for(int i = 0; i < NR_DEPENDENCIES; i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[initialModules.get(i)][finalModules.get(i)];
			
			for(int j = 1; j < NR_NODES; j++) {
				if(tValue(x, i, j) != tValue(x, i, j-1)) {
					cost += bwNeeded/(algorithm.getfBandwidthMap()[tValue(x, i, j-1)][tValue(x, i, j)] + Constants.EPSILON);
				}
			}
		}
		
		return cost;
	}
	
	private double[] defineConstraints(int[] x, int numberOfVariables) {
		double constraints[] = new double[1];
		
		// If placement does not respects possible deployment matrix
		for(int i = 0; i < numberOfVariables; i++) {
			if(x[i] < 0 || x[i] > algorithm.getNumberOfNodes()-1) {
				constraints[0]++;
				continue;
			}
			
			if(algorithm.getPossibleDeployment()[x[i]][i] == 0) {
				constraints[0]++;
			}
		}
		
		// If fog node's resources are exceeded
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			double totalMips = 0;
			double totalRam = 0;
			double totalStrg = 0;
			
			for(int j = 0; j < numberOfVariables; j++) {
				if(x[j] != i) continue;
				
				if(x[i] < 0 || x[i] > algorithm.getNumberOfNodes()-1) {
					constraints[0]++;
					continue;
				}
					
				totalMips += algorithm.getmMips()[j];
				totalRam += algorithm.getmRam()[j];
				totalStrg += algorithm.getmStrg()[j];
				
				if(totalMips < algorithm.getfMips()[i] && totalRam < algorithm.getfRam()[i] && totalStrg < algorithm.getfStrg()[i])
					continue;
				
				constraints[0]++;
			}
		}
		
		return constraints;
	}
	
	private int tValue(int[] x, int dependency, int node) {
		return x[NR_MODULES + dependency*NR_NODES + node];
	}
	
}
