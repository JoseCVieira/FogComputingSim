package org.fog.placement.algorithm.nsga2;

import java.util.List;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.SingleObjectiveCostFunction;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Population;
import org.moeaframework.core.Solution;
import org.moeaframework.core.TerminationCondition;
import org.moeaframework.core.variable.EncodingUtils;

public class NSGA2 extends Algorithm {
	
	public NSGA2(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}

	@Override
	public Job execute() {
		int nrModules = getNumberOfModules();
		int nrNodes = getNumberOfNodes();
		int nrDependencies = getNumberOfDependencies();
		
		// Without migration
		int nrVars = nrModules+ nrDependencies*nrNodes + nrModules*nrNodes;
		
		NondominatedPopulation result = new Executor()
			    .withProblemClass(Problem.class, this, nrVars)
			    .withAlgorithm("NSGAII")
			    .withProperty("populationSize", 100)
			    .withTerminationCondition(new TerminationCondition() {
					int generationsWithNoChange = 0;
					Population lastPopulation = null;
					
					@Override
					public boolean shouldTerminate(org.moeaframework.core.Algorithm arg0) {
						if (lastPopulation == null) {
							lastPopulation = arg0.getResult();
							generationsWithNoChange = 0;
							return false;
						} else {
							Population currentPopulation = arg0.getResult();
							
							if (areIdentical(currentPopulation, lastPopulation)) {
								generationsWithNoChange++;
								
								if (generationsWithNoChange > 10) {
									System.out.println("Terminating");
									return true;
								} else {
									return false;
								}
							} else {
								lastPopulation = currentPopulation;
								generationsWithNoChange = 0;
								return false;
							}
						}
					}
					
					@Override
					public void initialize(org.moeaframework.core.Algorithm arg0) {
						// Do nothing
					}
					
					public boolean areIdentical(Population p1, Population p2) {
						DistanceMeasure distance = new EuclideanDistance();
						boolean allFound = true;
						
						for (Solution s1 : p1) {
							boolean found = false;
							
							for (Solution s2 : p2) {
								if (distance.compute(s1.getObjectives(), s2.getObjectives()) < 0.001) {									
									if(DoubleStream.of(s1.getConstraints()).sum() == 0 || DoubleStream.of(s2.getConstraints()).sum() == 0) {
										found = true;
										break;
									}
								}								
							}
							
							if (!found) {
								allFound = false;
								break;
							}
						}
						
						return allFound;
					}
				})
			    .run();
		
		Solution bestSolution = null;
		double bestCost = Constants.REFERENCE_COST;
		for (Solution solution : result) {
			if(solution.violatesConstraints()) continue;
			if(bestCost < solution.getObjective(0)) continue;
			
			bestCost = solution.getObjective(0);
			bestSolution = solution;
		}
		
		if(bestSolution == null) return null;
		
		int[] x = EncodingUtils.getInt(bestSolution);
		int[][] modulePlacementMap = Problem.extractModulePlacement(x, nrNodes, nrModules);
		int[][] tupleRoutingMap = Problem.extractTupleRouting(x, nrNodes, nrModules, nrDependencies);
		int[][] migrationRoutingMap = Problem.extractModuleRouting(x, nrNodes, nrModules, nrDependencies);
		
		return new Job(this, new SingleObjectiveCostFunction(), modulePlacementMap, tupleRoutingMap, migrationRoutingMap);
	}

}