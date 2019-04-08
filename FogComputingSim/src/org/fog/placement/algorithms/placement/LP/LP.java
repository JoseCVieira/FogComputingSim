package org.fog.placement.algorithms.placement.LP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;

import ilog.concert.*;
import ilog.cplex.*;

public class LP {	
	private double fMipsPrice[];
	private double fRamPrice[];
	private double fMemPrice[];
	private double fBwPrice[];
	
	private String fName[];
	private double fMips[];
	private double fRam[];
	private double fMem[];
	private double fBw[];
	
	private String mName[];
	private double mMips[];
	private double mRam[];
	private double mMem[];
	private double mBw[];
	
	public LP(List<FogDevice> fogDevices, List<Application> applications) {
		fName = new String[fogDevices.size()];
		fMips = new double[fogDevices.size()];
		fRam = new double[fogDevices.size()];
		fMem = new double[fogDevices.size()];
		fBw = new double[fogDevices.size()];
		
		fMipsPrice = new double[fogDevices.size()];
		fRamPrice = new double[fogDevices.size()];
		fMemPrice = new double[fogDevices.size()];
		fBwPrice = new double[fogDevices.size()];
		
		int i = 0;
		for(FogDevice fogDevice : fogDevices) {
			fName[i] = fogDevice.getName();
			
			for(Pe pe : fogDevice.getHost().getPeList())
				fMips[i] += pe.getMips();
			
			fRam[i] = fogDevice.getHost().getRam();
			fMem[i] = fogDevice.getHost().getStorage();
			fBw[i] = fogDevice.getHost().getBw();
			
			
			FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) fogDevice.getCharacteristics();
			
			fMipsPrice[i] = characteristics.getCostPerMips();
			fRamPrice[i] = characteristics.getCostPerMem();
			fMemPrice[i] = characteristics.getCostPerStorage();
			fBwPrice[i++] = characteristics.getCostPerBw();
		}
		
		for(i = 0; i < fogDevices.size(); i++) {
			FogDevice fDevice = null;
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(fName[i]))
					fDevice = fogDevice;
			
			System.out.println("Id: " + fDevice.getId() + " fName: " + fName[i]);
			System.out.println("fMips: " + fMips[i]);
			System.out.println("fRam: " + fRam[i]);
			System.out.println("fMem: " + fMem[i]);
			System.out.println("fBw: " + fBw[i]);
			System.out.println("fMipsPrice: " + fMipsPrice[i]);
			System.out.println("fRamPrice: " + fRamPrice[i]);
			System.out.println("fMemPrice: " + fMemPrice[i]);
			System.out.println("fBwPrice: " + fBwPrice[i]);
			System.out.println("Neighbors: " +  fDevice.getNeighborsIds());
			System.out.println("LatencymMap: " + fDevice.getLatencyMap() + "\n");
		}
		
		int size = 0;
		for(Application application : applications)
			size += application.getModules().size();
		
		mName = new String[size];
		mMips = new double[size];
		mRam = new double[size];
		mMem = new double[size];
		mBw = new double[size];
		
		i = 0;
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				mName[i] = module.getName();
				mMips[i] = module.getMips();
				mRam[i] = module.getRam();
				mMem[i] = module.getSize();
				mBw[i++] = module.getBw();
			}
		}
		
		for(i = 0; i < size; i++) {
			System.out.println("mName: " + mName[i]);
			System.out.println("mMips: " + mMips[i]);
			System.out.println("mRam: " + mRam[i]);
			System.out.println("mMem: " + mMem[i]);
			System.out.println("mBw: " + mBw[i] + "\n");
		}
	}
	
	public Map<String, List<String>> Execute() {
		final int NR_FOG_NODES = fMips.length;
		final int NR_MODULES = mMips.length;
		
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
					double aux = fMipsPrice[i]*mMips[j] +
								 fRamPrice[i]*mRam[j] +
								 fMemPrice[i]*mMem[j] +
								 fBwPrice[i]*mBw[j];
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
							modules.add(mName[j]);
					
					resMap.put(fName[i], modules);
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
