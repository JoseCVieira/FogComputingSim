package org.fog.placement.algorithms.placement.LP;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class LatencyCostTest {

	public static void main(String[] args) {
		final int NR_MODULES = 5;
		final int NR_FOG_NODES = 3;
		
		// fog nodes' characteristics price
		int fMipsPrice[] = {100, 20, 20};
		
		// fog nodes' characteristics
		int fMips[] = {50000, 50000, 50000};
		
		// modules' characteristics
		int mMips[] = {3000, 3500, 4000, 4500, 5000};
		
		int latMap[][] = {{0, 100, 50},{100, 0, 150},{50, 150, 0}};
		
		try {
			// define new model
			IloCplex cplex = new IloCplex();
			
			// variables
			IloNumVar[][] var = new IloNumVar[NR_FOG_NODES][NR_MODULES];
			for(int i = 0; i < NR_FOG_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					var[i][j] = cplex.boolVar();
			
			// define objective
			IloLinearNumExpr objective = cplex.linearNumExpr();
			
			for(int i = 0; i < NR_FOG_NODES; i++) {
				for(int j = 0; j < NR_MODULES; j++) {
					double aux = fMipsPrice[i]*mMips[j] + fBwPrice[i]*mBw[j];
					objective.addTerm(var[i][j], aux);
				}
			}
			
			/*IloNumVar latencyVar[] = cplex.numVarArray(NR_MODULES, 0, Double.MAX_VALUE);
			for(int i = 0; i < NR_MODULES; i++)
				objective.addTerm(1.0, latencyVar[i]);*/
			
			//cplex.addMaximize(objective);
			cplex.addMinimize(objective);

			// define constraints
			IloLinearNumExpr[] usedMipsCapacity = new IloLinearNumExpr[NR_FOG_NODES];
			for (int i = 0; i < NR_FOG_NODES; i++) {
				usedMipsCapacity[i] = cplex.linearNumExpr();
        		for (int j = 0; j < NR_MODULES; j++)
        			usedMipsCapacity[i].addTerm(var[i][j], mMips[j]);
			}
			
			for (int i = 0; i < NR_FOG_NODES; i++)
        		cplex.addLe(usedMipsCapacity[i], fMips[i]);
			
			//sum by columns
			IloNumVar[][] aux = new IloNumVar[NR_MODULES][NR_FOG_NODES];
			for(int i = 0; i < NR_FOG_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					aux[j][i] = var[i][j];
			
			// ensure just one placement for each module
			for(int i = 0; i < NR_MODULES; i++)
				cplex.addEq(cplex.sum(aux[i]), 1.0);
			
			// latency
			IloNumVar from[] = cplex.numVarArray(NR_MODULES, 0, NR_FOG_NODES-1);
			IloNumVar[][] mLatMap = new IloNumVar[NR_MODULES][NR_FOG_NODES];
			for(int i = 0; i < NR_MODULES; i++) {
				for(int j = 0; j < NR_FOG_NODES; j++) {
					cplex.ifThen(cplex.eq(aux[i][j], 1), cplex.eq(from[i], j));
					
					if(i == 0)
						cplex.eq(mLatMap[i][j], 0.0);
					else {
						cplex.ifThen(cplex.and(cplex.eq(aux[i][j], 1), cplex.eq(from[i-1], j)), cplex.eq(mLatMap[i][j], latMapj));
						
						cplex.ifThen(cplex.eq(aux[i][j], 0), cplex.eq(mLatMap[i][j], 0.0));
					}
				}
			}
			
			// display option
			cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			
			// solve
			if (cplex.solve()) {
				System.out.println("\nValue = " + cplex.getObjValue() + "\n");
				for(int i = 0; i < NR_FOG_NODES; i++) {
					for(int j = 0; j < NR_MODULES; j++)
						System.out.print(cplex.getValue(var[i][j]) + " ");
					System.out.println();
				}
				
				//System.out.println("latencyVar= " + cplex.getValue(latencyVar));
			}
			else
				System.out.println("Model not solved");
			cplex.end();
		}
		catch (IloException exc) {
			exc.printStackTrace();
		}
	}
}
