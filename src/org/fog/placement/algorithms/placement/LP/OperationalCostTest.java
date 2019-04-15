package org.fog.placement.algorithms.placement.LP;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class OperationalCostTest {

	public static void main(String[] args) {
		final int NR_MODULES = 5;
		final int NR_FOG_NODES = 2;
		
		// fog nodes' characteristics price
		int fMipsPrice[] = {100, 20};
		int fRamPrice[] = {100, 20};
		int fMemPrice[] = {100, 20};
		int fBwPrice[] = {100, 20};
		
		// fog nodes' characteristics
		int fMips[] = {50000, 50000};
		int fRam[] = {5000, 5000};
		int fMem[] = {50000, 50000};
		int fBw[] = {2000, 2000};
		
		// modules' characteristics
		int mMips[] = {3000, 3500, 4000, 4500, 5000};
		int mRam[] = {1000, 1000, 1000, 1000, 1000};
		int mMem[] = {5000, 5000, 5000, 5000, 5000};
		int mBw[] = {100, 100, 100, 100, 100};
		
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
					double aux = fMipsPrice[i]*mMips[j] + fRamPrice[i]*mRam[j] + fMemPrice[i]*mMem[j] + fBwPrice[i]*mBw[j];
					objective.addTerm(var[i][j], aux);
				}
			}
			
			//cplex.addMaximize(objective);
			cplex.addMinimize(objective);

			// define constraints
			IloLinearNumExpr[] usedMipsCapacity = new IloLinearNumExpr[NR_FOG_NODES];
			IloLinearNumExpr[] usedRamCapacity = new IloLinearNumExpr[NR_FOG_NODES];
			IloLinearNumExpr[] usedMemCapacity = new IloLinearNumExpr[NR_FOG_NODES];
			IloLinearNumExpr[] usedBwCapacity = new IloLinearNumExpr[NR_FOG_NODES];
			for (int i = 0; i < NR_FOG_NODES; i++) {
				usedMipsCapacity[i] = cplex.linearNumExpr();
				usedRamCapacity[i] = cplex.linearNumExpr();
				usedMemCapacity[i] = cplex.linearNumExpr();
				usedBwCapacity[i] = cplex.linearNumExpr();
				
        		for (int j = 0; j < NR_MODULES; j++) {
        			usedMipsCapacity[i].addTerm(var[i][j], mMips[j]);
        			usedRamCapacity[i].addTerm(var[i][j], mRam[j]);
        			usedMemCapacity[i].addTerm(var[i][j], mMem[j]);
        			usedBwCapacity[i].addTerm(var[i][j], mBw[j]);
        		}
			}
			
			for (int i = 0; i < NR_FOG_NODES; i++) {
        		cplex.addLe(usedMipsCapacity[i], fMips[i]);
        		cplex.addLe(usedRamCapacity[i], fRam[i]);
        		cplex.addLe(usedMemCapacity[i], fMem[i]);
        		cplex.addLe(usedBwCapacity[i], fBw[i]);
			}
			
			//sum by columns
			IloNumVar[][] aux = new IloNumVar[NR_MODULES][NR_FOG_NODES];
			for(int i = 0; i < NR_FOG_NODES; i++)
				for(int j = 0; j < NR_MODULES; j++)
					aux[j][i] = var[i][j];
			
			for(int i = 0; i < NR_MODULES; i++)
				cplex.addEq(cplex.sum(aux[i]), 1.0);
			
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
