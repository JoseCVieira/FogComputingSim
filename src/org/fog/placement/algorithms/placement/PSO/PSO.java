package org.fog.placement.algorithms.placement.PSO;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.AlgorithmConstants;
import org.fog.placement.algorithms.placement.AlgorithmMathUtils;
import org.fog.placement.algorithms.placement.AlgorithmUtils;
import org.fog.placement.algorithms.placement.Job;
import org.fog.utils.Util;

/**
 * Represents a swarm of particles from the Particle Swarm Optimization algorithm.
 */
public class PSO extends Algorithm {
    private double bestEval = Util.INF;
    private Job bestPosition = null;
    
    
    public PSO(final List<FogBroker> fogBrokers, final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogBrokers, fogDevices, applications, sensors, actuators);
	}
    
    @Override
	public Job execute() {
    	// Initialize
    	Particle[] particles = new Particle[AlgorithmConstants.POPULATION_SIZE];
    	
    	for (int i = 0; i < AlgorithmConstants.POPULATION_SIZE; i++) {
            Particle particle = new Particle(this, getNumberOfNodes(), getNumberOfModules());
            particles[i] = particle;
            updateGlobalBest(particle);
        }

        double oldEval = bestEval;
        System.out.println("--------------------------EXECUTING-------------------------");
        System.out.println("Global Best Evaluation (Epoch " + 0 + "):\t"  + bestEval);

        for (int i = 0; i < AlgorithmConstants.MAX_ITER; i++) {

            if (bestEval < oldEval) {
                System.out.println("Global Best Evaluation (Epoch " + (i + 1) + "):\t" + bestEval);
                oldEval = bestEval;
            }

            for (Particle p : particles) {
                p.updatePersonalBest();
                updateGlobalBest(p);
            }

            for (Particle p : particles) {
                updateVelocity(p);
                p.updatePosition();
            }
        }

        System.out.println("---------------------------RESULT---------------------------");
        System.out.println("Cost = " + bestPosition.getCost());
        System.out.println("Final Best Evaluation: " + bestEval);
        System.out.println("---------------------------COMPLETE-------------------------");
        
        if(bestPosition.getCost() == Double.MAX_VALUE)
	    	return null;
	    
	    if(PRINT_DETAILS)
	    	AlgorithmUtils.printResults(this, bestPosition);
        
		return bestPosition;
	}

    /**
     * Update the global best solution if a the specified particle has
     * a better solution
     * @param particle  the particle to analyze
     */
    private void updateGlobalBest (Particle particle) {
        if (particle.getBestEval() < bestEval) {
            bestPosition = particle.getBestPosition();
            bestEval = particle.getBestEval();
        }
    }

    /**
     * Update the velocity of a particle using the velocity update formula
     * @param particle  the particle to update
     */
    private void updateVelocity (Particle particle) {
    	int[] oldVelocityPlacement = particle.getVelocityPlacement();
    	int[][] oldVelocityRouting = particle.getVelocityRouting();
    	
    	Job pBest = new Job(particle.getBestPosition());
    	Job gBest = new Job(bestPosition);
    	Job pos = new Job(particle.getPosition());
    	
    	int[][] pBestM = pBest.getModulePlacementMap();
    	int[][] gBestM = gBest.getModulePlacementMap();
    	int[][] posM = pos.getModulePlacementMap();
    	
    	int[][] pBestR = pBest.getRoutingMap();
    	int[][] gBestR = gBest.getRoutingMap();
    	int[][] posR = pos.getRoutingMap();
    	
    	/*
    	 * The first product of the formula: 	velocity =  DEFAULT_INERTIA		* oldVelocity
    	 * The second product of the formula: 	velocity += DEFAULT_COGNITIVE	* random * (pbest - position)
    	 * The third product of the formula: 	velocity += DEFAULT_SOCIAL		* random * (gbest - position)
    	 */

    	/**
    	 * Placement velocity
    	 */
    	
        // The first product of the formula
    	Number[] nOldVelocityPlacement = AlgorithmMathUtils.toNumber(Util.copy(oldVelocityPlacement));
    	double[] newVelocityPlacement = AlgorithmMathUtils.scalarMultiplication(nOldVelocityPlacement, AlgorithmConstants.DEFAULT_INERTIA);

        // The second product of the formula
    	Number[] npBestM =  AlgorithmMathUtils.toNumber(parseModulePlacement(pBestM));
    	Number[] nposM =  AlgorithmMathUtils.toNumber(parseModulePlacement(posM));
    	
        double[] aux1 = AlgorithmMathUtils.subtract(npBestM, nposM);
        aux1 = AlgorithmMathUtils.scalarMultiplication(AlgorithmMathUtils.toNumber(aux1), AlgorithmConstants.DEFAULT_COGNITIVE * new Random().nextDouble());
        newVelocityPlacement = AlgorithmMathUtils.sum(AlgorithmMathUtils.toNumber(newVelocityPlacement), AlgorithmMathUtils.toNumber(aux1));
        		
        // The third product of the formula
        Number[] ngBestM =  AlgorithmMathUtils.toNumber(parseModulePlacement(gBestM));
        
        aux1 = AlgorithmMathUtils.subtract(ngBestM, nposM);
        aux1 = AlgorithmMathUtils.scalarMultiplication(AlgorithmMathUtils.toNumber(aux1), AlgorithmConstants.DEFAULT_SOCIAL * new Random().nextDouble());
        newVelocityPlacement = AlgorithmMathUtils.sum(AlgorithmMathUtils.toNumber(newVelocityPlacement), AlgorithmMathUtils.toNumber(aux1));
        int[] finalVelocityPlacement = verifyPositioning(parseModulePlacement(posM), AlgorithmMathUtils.toInt(newVelocityPlacement));
        
        /**
         * Routing velocity
         */
        
        // The first product of the formula
        Number[][] nOldVelocityRouting = AlgorithmMathUtils.toNumber(Util.copy(oldVelocityRouting));
        double[][] newVelocityRouting = AlgorithmMathUtils.scalarMultiplication(nOldVelocityRouting, AlgorithmConstants.DEFAULT_INERTIA);
        
        // The second product of the formula   
        Number[][] npBestR =  AlgorithmMathUtils.toNumber(pBestR);
    	Number[][] nposR =  AlgorithmMathUtils.toNumber(posR);
        
        double[][] aux2 = AlgorithmMathUtils.subtract(npBestR, nposR);
        aux2 = AlgorithmMathUtils.scalarMultiplication(AlgorithmMathUtils.toNumber(aux2), AlgorithmConstants.DEFAULT_COGNITIVE * new Random().nextDouble());
        newVelocityRouting = AlgorithmMathUtils.sum(AlgorithmMathUtils.toNumber(newVelocityRouting), AlgorithmMathUtils.toNumber(aux2));
        
        // The third product of the formula
        Number[][] ngBestR =  AlgorithmMathUtils.toNumber(gBestR);
        
        aux2 = AlgorithmMathUtils.subtract(ngBestR, nposR);
        aux2 = AlgorithmMathUtils.scalarMultiplication(AlgorithmMathUtils.toNumber(aux2), AlgorithmConstants.DEFAULT_SOCIAL * new Random().nextDouble());
        newVelocityRouting = AlgorithmMathUtils.sum(AlgorithmMathUtils.toNumber(newVelocityRouting), AlgorithmMathUtils.toNumber(aux2));
        int[][] finalVelocityRouting = verifyRouting(parseModulePlacement(posM), finalVelocityPlacement, posR,
        		AlgorithmMathUtils.toInt(newVelocityRouting));
        
        particle.setVelocityPlacement(finalVelocityPlacement);
        particle.setVelocityRouting(finalVelocityRouting);
    }
    
    public int[] parseModulePlacement(int[][] modulePlacementMap) {
    	int[] result = new int[modulePlacementMap[0].length];
    	
    	for(int j = 0; j < modulePlacementMap[0].length; j++) {
    		for(int i = 0; i < modulePlacementMap.length; i++) {
        		if(modulePlacementMap[i][j] == 1) {
        			result[j] = i;
        			break;
        		}
        	}
    	}
    	
    	return result;
    }
    
    public int[][] parseModulePlacement(int[] modulePlacement) {
    	int[][] result = new int[getNumberOfNodes()][getNumberOfModules()];
    	
    	for(int i = 0; i < getNumberOfNodes(); i++)
    		result[modulePlacement[i]][i] = 1;
    	
    	return result;
    }
    
    private int[] verifyPositioning(int[] position, int[] velocity) {
    	int[][] possibleDeployment = AlgorithmMathUtils.toInt(getPossibleDeployment());
    	
    	for(int i = 0; i < getNumberOfModules(); i++) {
    		int nextPos = position[i] + velocity[i];
    		
    		if(nextPos < 0)
    			nextPos = 0;
    		else if(nextPos > getNumberOfNodes()-1)
    			nextPos = getNumberOfNodes()-1;
    		
    		if(possibleDeployment[nextPos][i] != 1) {
    			
    			List<Integer> validValues = new ArrayList<Integer>();
    			
    			for(int j = 0; j < getNumberOfNodes(); j++)
    				if(possibleDeployment[j][i] == 1)
    					validValues.add(j);
    			
    			nextPos = validValues.get(new Random().nextInt(validValues.size()));
    			velocity[i] = nextPos - position[i];
    		}
    	}
    	
    	return velocity;
    }
    
    private int[][] verifyRouting(int[] positionPlacement, int[] velocityPlacement, int[][] positionRouting, int[][] velocityRouting) {
    	double[][] dependencyMatrix = getmDependencyMap();
    	
    	List<Integer> initialNodes = new ArrayList<Integer>();
		List<Integer> finalNodes = new ArrayList<Integer>();
		
		int[] actualPositioning = new int [getNumberOfModules()];
		for(int i = 0; i < getNumberOfModules(); i++) {
			int nextPos = positionPlacement[i] + velocityPlacement[i];
			
			if(nextPos < 0)
    			nextPos = 0;
    		else if(nextPos > getNumberOfNodes()-1)
    			nextPos = getNumberOfNodes()-1;
			
			actualPositioning[i] = nextPos;
		}
			
		
		for(int i = 0; i < dependencyMatrix.length; i++) {
			for(int j = 0; j < dependencyMatrix[0].length; j++) {
				if(dependencyMatrix[i][j] != 0) {
					initialNodes.add(actualPositioning[i]);
					finalNodes.add(actualPositioning[j]);
				}
			}
		}
		
		for(int i  = 0; i < initialNodes.size(); i++) {
			velocityRouting[i][0] = initialNodes.get(i) - positionRouting[i][0];
			velocityRouting[i][getNumberOfNodes()-2] = initialNodes.get(i) - positionRouting[i][getNumberOfNodes()-2];
		}
    	
    	return velocityRouting;
    }

}