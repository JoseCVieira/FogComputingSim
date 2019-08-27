package org.fog.placement.algorithm;

import org.fog.core.Config;
import org.fog.core.Constants;

/**
 * Class in which defines the single optimization problem cost function.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @since   July, 2019
 */
public class SingleObjectiveCostFunction extends CostFunction {
	
	/**
	 * Computes the cost of the best solution found by running the optimization algorithm.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param solution the best solution found by the execution of the optimization algorithm
	 * @return the result of the cost function
	 */
	@Override
	public double computeCost(Algorithm algorithm, Job solution) {
		int[][] modulePlacementMap = solution.getModulePlacementMap();
		int[][] tupleRoutingMap = solution.getTupleRoutingMap();
		
		double cost = 0;
		
		// Resources
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++) {
				double tmp = algorithm.getfMipsPrice()[i] * algorithm.getmMips()[j] +
							 algorithm.getfRamPrice()[i] * algorithm.getmRam()[j] +
							 algorithm.getfStrgPrice()[i] * algorithm.getmStrg()[j];
							 
				cost += tmp*modulePlacementMap[i][j];
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[algorithm.getStartModDependency(i)][algorithm.getFinalModDependency(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++)
				if(tupleRoutingMap[i][j] != tupleRoutingMap[i][j-1]) {
					cost += algorithm.getfBwPrice()[tupleRoutingMap[i][j-1]]*bwNeeded;
				}
		}
		
		double power = 0;
		for(int i = 0; i < algorithm.getNumberOfNodes(); i++) {
			for(int j = 0; j < algorithm.getNumberOfModules(); j++){
				power = modulePlacementMap[i][j]*(algorithm.getfBusyPw()[i]-algorithm.getfIdlePw()[i]) * (algorithm.getmMips()[j]/algorithm.getfMips()[i]);
				cost += power*algorithm.getfPwPrice()[i];
			}
		}
		
		for(int i = 0; i < algorithm.getNumberOfDependencies(); i++) {
			double bwNeeded = algorithm.getmBandwidthMap()[algorithm.getStartModDependency(i)][algorithm.getFinalModDependency(i)];
			
			for(int j = 1; j < algorithm.getNumberOfNodes(); j++) {
				if(tupleRoutingMap[i][j-1] != tupleRoutingMap[i][j]) {
					double txPower = algorithm.getfTxPw()[tupleRoutingMap[i][j-1]]*algorithm.getfPwPrice()[tupleRoutingMap[i][j-1]];
					double bwAvailable = algorithm.getfBandwidthMap()[tupleRoutingMap[i][j-1]][tupleRoutingMap[i][j]] * Config.BW_PERCENTAGE_TUPLES;
					
					power = bwNeeded/(bwAvailable + Constants.EPSILON)*txPower;
					cost += power*algorithm.getfPwPrice()[tupleRoutingMap[i][j-1]];
				}
			}
		}
		
		return cost;
	}
	
}
