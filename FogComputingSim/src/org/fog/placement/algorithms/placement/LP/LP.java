package org.fog.placement.algorithms.placement.LP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.placement.AlgorithmResolution;

import ilog.concert.*;
import ilog.cplex.*;

public class LP extends AlgorithmResolution {
	
	public LP(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		super(fogDevices, applications, sensors, actuators);
	}
	
	@Override
	public Map<String, List<String>> execute() {
		final int NR_FOG_NODES = getfMips().length;
		final int NR_MODULES = getmMips().length;
		
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
					double aux = getfMipsPrice()[i]*getmMips()[j] +
								 getfRamPrice()[i]*getmRam()[j] +
								 getfMemPrice()[i]*getmMem()[j] +
								 getfBwPrice()[i]*getmBw()[j];
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
        			usedMipsCapacity[i].addTerm(var[i][j], getmMips()[j]);
        			usedRamCapacity[i].addTerm(var[i][j], getmRam()[j]);
        			usedMemCapacity[i].addTerm(var[i][j], getmMem()[j]);
        			usedBwCapacity[i].addTerm(var[i][j], getmBw()[j]);
        		}
			}
			
			for (int i = 0; i < NR_FOG_NODES; i++) {
        		cplex.addLe(usedMipsCapacity[i], getfMips()[i]);
        		cplex.addLe(usedRamCapacity[i], getfRam()[i]);
        		cplex.addLe(usedMemCapacity[i], getfMem()[i]);
        		cplex.addLe(usedBwCapacity[i], getfBw()[i]);
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
				Map<String, List<String>> resMap = new HashMap<>();
				
				System.out.println("\nValue = " + cplex.getObjValue() + "\n");
				
				for(int i = 0; i < NR_FOG_NODES; i++) {
					for(int j = 0; j < NR_MODULES; j++)
						System.out.print(cplex.getValue(var[i][j]) + " ");
					System.out.println();
				}
				
				for(int i = 0; i < NR_FOG_NODES; i++) {
					List<String> modules = new ArrayList<String>();
					
					for(int j = 0; j < NR_MODULES; j++)
						if(cplex.getValue(var[i][j]) == 1)
							modules.add(getmName()[j]);
					
					resMap.put(getfName()[i], modules);
				}
				
				cplex.end();
				return resMap;
			}
				
			System.out.println("Model not solved");
			cplex.end();
			return null;
		}
		catch (IloException exc) {
			exc.printStackTrace();
			return null;
		}
	}
}
