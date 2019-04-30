package org.fog.placement.algorithms.placement.LP;

import ilog.concert.*;
import ilog.cplex.*;

public class ShortestPathTest {
	private static final int NR_NODES = 4;
	private static final int INITIAL = 0;
	private static final int FINAL = 3;
	private static final int[][] M = {{0, 4, 3, 40},
									  {2, 0, 3, 2},
									  {1, 5, 0, 2},
									  {2, 4, 5, 0}};
	
	public static void main(String[] args) {		
		try {
			// Define new model
			IloCplex cplex = new IloCplex();
			
			// Variables
			IloNumVar[][] routingVar = new IloNumVar[4][4];
			
			for(int i = 0; i < 4; i++)
				for(int j = 0; j < 4; j++)
					routingVar[i][j] = cplex.boolVar();
			
			// Define objective
			IloLinearNumExpr objective = cplex.linearNumExpr();
			
			for(int i = 0; i < NR_NODES; i++)
				for(int j = 0; j < NR_NODES; j++)
					objective.addTerm(routingVar[i][j], M[i][j]);
			
			cplex.addMinimize(objective);
			
			// Define constraints
			
			IloNumVar[][] transposeR = new IloNumVar[4][4];
			for(int i = 0; i < 4; i++)
				for(int j = 0; j < 4; j++)
					transposeR[j][i] = routingVar[i][j];
			
			IloNumVar[] aux = new IloNumVar[4];
			for(int i = 0; i < 4; i++)
				aux[i] = cplex.intVar(-1, 1);
			
			cplex.addEq(aux[INITIAL], 1);
			cplex.addEq(aux[FINAL], -1);
			cplex.addEq(aux[1], 0);
			cplex.addEq(aux[2], 0);			
			
			for(int i = 0; i < NR_NODES; i++)
				cplex.addEq(cplex.diff(cplex.sum(routingVar[i]), cplex.sum(transposeR[i])), aux[i]);
			
			// Display option
			cplex.setParam(IloCplex.Param.Simplex.Display, 0);
			
			// Solve
			if (cplex.solve()) {
				System.out.println("\nValue = " + cplex.getObjValue() + "\n");
				
				for(int i = 0; i < 4; i++) {						
					for(int j = 0; j < 4; j++) {
						if(cplex.getValue(routingVar[i][j]) == 0)
							System.out.print("- ");
						else
							System.out.print("1 ");
					}
					System.out.println();
				}
			}else
				System.out.println("Model not solved");
			cplex.end();
		}
		catch (IloException exc) {
			exc.printStackTrace();
		}
	}
}
