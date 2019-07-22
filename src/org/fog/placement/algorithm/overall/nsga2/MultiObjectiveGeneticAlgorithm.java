package org.fog.placement.algorithm.overall.nsga2;

import java.util.List;
import java.util.Scanner;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.overall.util.AlgorithmUtils;
import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class MultiObjectiveGeneticAlgorithm extends Algorithm {
	
	public MultiObjectiveGeneticAlgorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}

	@Override
	public Job execute() {
		int nrVars = NR_MODULES + getNumberOfDependencies()*NR_NODES /*+ NR_MODULES*NR_NODES*/;
		int nrObjs = 5; //6;
		
		NondominatedPopulation result = new Executor()
			    .withProblemClass(Problem.class, this, nrVars, nrObjs)
			    .withAlgorithm("NSGAII") // GDE3
			    .withMaxEvaluations(10000)
			    /*.withTerminationCondition(new TerminationCondition() {
					
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
				})*/
			    .run();
		
		for (Solution solution : result) {
			if(solution.violatesConstraints()) continue;
  
			int[] x = EncodingUtils.getInt(solution);
			AlgorithmUtils.print("", x);

			for(int i = 0; i < nrObjs; i++) {
				System.out.print(solution.getObjective(i) + " ");
			}
			System.out.println();
		}
		
		/*NondominatedPopulation result = new Executor()
				.withAlgorithm("NSGAII")
				.withProblemClass(NSGAPlacement.class, this, nrVars, nrObjs)
				//.withProperty("UM.rate", 0.4)		// Mutation
				//.withProperty("UX.rate", 0.6)		// Crossover
				//.checkpointEveryIteration()
				//.withMaxEvaluations(100000)
				//.withMaxTime(15000)
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
                .run();*/
		
		/*for (Solution solution : result) {
			if (!solution.violatesConstraints()){
			
				System.out.println(solution.getObjective(0) + " " + solution.getObjective(1) + " " + solution.getObjective(2)/* + " "
						+ solution.getObjective(3) + " " + solution.getObjective(4) + " " + solution.getObjective(5)*//*);
				
				int start = 0;
				System.out.println("\nPlacement: ");
				for(int i = 0; i < NR_MODULES; i++) {
					System.out.print(EncodingUtils.getInt(solution.getVariable(start + i)) + " ");
				}
				System.out.println("\n");*/
				
				/*start += NR_MODULES;
				System.out.println("Tuple routing: ");
				for(int i = 0; i < getNumberOfDependencies(); i++) {
					for(int j = 0; j < NR_NODES; j++) {
						System.out.print(EncodingUtils.getInt(solution.getVariable(start + i*NR_NODES + j)) + " ");
					}
					System.out.println();
				}
				System.out.println("\n\n");*/
				
				/*start += getNumberOfDependencies()*NR_NODES;
				System.out.println("VM routing: ");
				for(int i = 0; i < NR_MODULES; i++) {
					for(int j = 0; j < NR_NODES; j++) {
						System.out.print(EncodingUtils.getInt(solution.getVariable(start + i*NR_NODES + j)) + " ");
					}
					System.out.println();
				}
				System.out.println("\n\n");*/
			/*}
		}*/
		
		System.out.println("END");
		Scanner reader = new Scanner(System.in);
		reader.nextInt();
		reader.close();
		return null;
	}

}