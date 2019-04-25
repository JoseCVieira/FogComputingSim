package org.fog.placement.algorithms.placement.PSO;

import org.fog.placement.algorithms.placement.Algorithm;
import org.fog.placement.algorithms.placement.Job;

/**
 * Represents a particle from the Particle Swarm Optimization algorithm.
 */
class Particle {
	private Job position;				// Current position
	private int[] velocityPlacement;	// Current velocity
	private int[][] velocityRouting;	// Current velocity
    private Job bestPosition;			// Personal best solution
    private double bestEval;			// Personal best value
    Algorithm algorithm;
    int nrOfNodes;
    int nrOfModules;

    Particle (Algorithm algorithm, int nrOfNodes, int nrOfModules) {
    	this.algorithm = algorithm;
    	this.nrOfNodes = nrOfNodes;
    	this.nrOfModules = nrOfModules;
    	
        position = Job.generateRandomJob(algorithm, nrOfNodes, nrOfModules);
        velocityPlacement = new int[nrOfModules];
        velocityRouting = new int[position.getRoutingMap().length][nrOfNodes-1];
        bestEval = position.getCost();
        bestPosition = new Job(position);
    }

    /**
     * Update the position of a particle by adding its velocity to its position.
     */
    void updatePosition () {
    	int[][] modulePlacementMap = position.getModulePlacementMap();
    	int[][] routingMap = position.getRoutingMap();
    	
    	int[] modulePlacement = ((PSO) algorithm).parseModulePlacement(modulePlacementMap);
    	for(int i = 0; i < algorithm.getNumberOfModules(); i++) {
    		modulePlacement[i] += velocityPlacement[i];
    		
    		if(modulePlacement[i] < 0)
    			modulePlacement[i] = 0;
			else if(modulePlacement[i] > nrOfNodes-1)
				modulePlacement[i] = nrOfNodes-1;
    	}
    	
    	modulePlacementMap = ((PSO) algorithm).parseModulePlacement(modulePlacement);
    	
    	for(int i  = 0; i < routingMap.length; i++) {
    		for(int j  = 0; j < routingMap[i].length; j++) {
    			routingMap[i][j] += velocityRouting[i][j];
    			
    			if(routingMap[i][j] < 0)
    				routingMap[i][j] = 0;
    			else if(routingMap[i][j] > nrOfNodes-1)
    				routingMap[i][j] = nrOfNodes-1;
    		}
    	}
    	
        position = new Job(algorithm, modulePlacementMap, routingMap);
    }
    
    /**
     * Update the personal best if the current evaluation is better.
     */
    void updatePersonalBest () {
        double eval = position.getCost();
        if (eval < bestEval) {
            bestPosition = new Job(position);
            bestEval = eval;
        }
    }
    
    Job getPosition () {
        return position;
    }
    
    /**
     * Get the value of the personal best solution.
     * @return  the evaluation
     */
    double getBestEval () {
        return bestEval;
    }
    
    /**
     * Set the velocity of the particle.
     * @param velocity  the new velocity
     */
    void setVelocityPlacement (int[] velocityPlacement) {
        this.velocityPlacement = velocityPlacement;
    }
    
    
    /**
     * Get a copy of the velocity of the particle.
     * @return  the velocity
     */
    int[] getVelocityPlacement () {
        return velocityPlacement;
    }
    
    /**
     * Set the velocity of the particle.
     * @param velocity  the new velocity
     */
    void setVelocityRouting (int[][] velocityRouting) {
        this.velocityRouting = velocityRouting;
    }
    
    /**
     * Get a copy of the velocity of the particle.
     * @return  the velocity
     */
    int[][] getVelocityRouting () {
        return velocityRouting;
    }
    
    Job getBestPosition() {
        return bestPosition;
    }

}