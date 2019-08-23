package org.fog.placement.algorithm;


import org.fog.core.Config;

/**
 * Class representing the solution of the multiple objective optimization algorithm.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class MultiObjectiveJob extends Job {

	/** Vector contains the details of the cost function (i.e., the cost of each objective) */
	private double[] detailedCost;
	
	/**
	 * Creates a solution based on the application module placement, tuple routing, and virtual machine migration
	 * routing tables (all of which being binary tables).
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param modulePlacementMap the module placement matrix (binary)
	 * @param tupleRoutingVectorMap the tuple routing matrix (binary)
	 * @param migrationRoutingVectorMap the migration routing matrix (binary)
	 */
	public MultiObjectiveJob(Algorithm algorithm, int[][] modulePlacementMap, int[][][] tupleRoutingVectorMap, int[][][] migrationRoutingVectorMap) {
		super();
		
		int nrDependencies = tupleRoutingVectorMap.length;
		int nrNodes = algorithm.getNumberOfNodes();
		int nrModules = algorithm.getNumberOfModules();
		int[][] tupleRoutingMap = new int[nrDependencies][nrNodes];
		int[][] migrationRoutingMap = new int[nrModules][nrNodes];
		
		int iter;
		
		// Tuple routing map		
		for(int i = 0; i < nrDependencies; i++) {
			int from = Job.findModulePlacement(modulePlacementMap, algorithm.getStartModDependency(i));
			tupleRoutingMap[i][0] = from;
			iter = 1;
			
			boolean found = true;
			while(found) {
				found = false;
				
				for(int j = 0; j < nrNodes; j++) {
					if(tupleRoutingVectorMap[i][from][j] == 1) {
						tupleRoutingMap[i][iter++] = j;
						from = j;
						j = nrNodes;
						found = true;
					}
				}
			}
			
			for(int j = iter; j < nrNodes; j++) {
				tupleRoutingMap[i][j] = from;
			}
		}
		
		// Migration routing map
		for(int i = 0; i < nrModules; i++) {			
			int from = Job.findModulePlacement(algorithm.isFirstOptimization() ? modulePlacementMap : algorithm.getCurrentPositionInt(), i);
			migrationRoutingMap[i][0] = from;
			iter = 1;
			
			boolean found = true;
			while(found) {
				found = false;
				
				for(int j = 0; j < nrNodes; j++) {
					if(migrationRoutingVectorMap[i][from][j] == 1) {
						migrationRoutingMap[i][iter++] = j;
						from = j;
						j = nrNodes;
						found = true;
					}
				}
			}
			
			for(int j = iter; j < nrNodes; j++) {
				migrationRoutingMap[i][j] = from;
			}
		}
		
		this.modulePlacementMap = modulePlacementMap;
		this.tupleRoutingMap = tupleRoutingMap;
		this.migrationRoutingMap = migrationRoutingMap;
		this.detailedCost = new double[Config.NR_OBJECTIVES];
		this.setValid(true);
	}
	
	/**
	 * Gets the vector which contains the details of the cost function.
	 * 
	 * @return the vector which contains the details of the cost function
	 */
	public double getDetailedCost(int index) {
		return detailedCost[index];
	}
	
	/**
	 * Sets the vector which contains the details of the cost function.
	 * 
	 * @param detailedCost the vector which contains the details of the cost function.
	 */
	public void setDetailedCost(int index, double value) {
		this.detailedCost[index] = value;
	}

}
