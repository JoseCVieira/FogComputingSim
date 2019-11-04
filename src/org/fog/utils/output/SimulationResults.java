package org.fog.utils.output;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Client;
import org.fog.entities.FogDevice;
import org.fog.placement.Controller;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.Solution;
import org.fog.utils.NetworkMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

/**
 * Class which is responsible to print the simulation results.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class SimulationResults {
	private static final int MAX_COLUMN_SIZE = 120;
	
	/** Object which holds the information needed to display the simulation results */
	private Controller controller;
	
	/** The algorithm object */
	private Algorithm al;
	
	/** The final solution */
	private Solution solution;
	
	
	/**
	 * Prints the results obtained in the simulation execution.
	 * 
	 * @param controller the controller used in the simulation
	 */
	public SimulationResults(Controller controller) {
		this.controller = controller;
		this.al = controller.getControllerAlgorithm().getAlgorithm();
		this.solution = controller.getControllerAlgorithm().getSolution();
		
		printTimeDetails();
		printLoopDetailsAverage();
		printTupleDetails();
		printLoopDetails();
		printMigrationDetailsAverage();
		printMigrationDetails();
		printEnergyDetails();
		printCPUDetails();
		printNetworkUsageDetails();
		printCostDetails();
		printNetworkDetails();
	}
	
	/**
	 * Prints the simulation execution elapsed time.
	 */
	private void printTimeDetails() {
		long time = Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime();
		String content = "|" + Util.centerString((MAX_COLUMN_SIZE*2+1), String.valueOf(time)) + "|\n";
		table("EXECUTION TIME [s]", content, null);
	}
	
	/**
	 * Prints the loops average timing details obtained in the simulation execution.
	 */
	private void printLoopDetailsAverage() {		
		int col1 = MAX_COLUMN_SIZE-2;
		int col2 = MAX_COLUMN_SIZE/5;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("LOOP", col1);
		subtitles.put("CPU [s]", col2);
		subtitles.put("NW [s]", col2);
		subtitles.put("TOTAL [s]", col2);
		subtitles.put("DEADLINE [s]", col2);
		subtitles.put("VIOLATED", col2-2);
		
		for(String appName : controller.getApplications().keySet()) {
			Application application = controller.getApplications().get(appName);
			
			for(AppLoop loop : application.getLoops()) {
				int loopId = loop.getLoopId();
				
				List<String> modules = getListForLoopId(controller, loopId);
				String name = modules.toString();
				double cpu = 0, nw = 0;
				
				for(int i = 0; i < modules.size()-1; i++) {
					String startModule = modules.get(i);
					String destModule = modules.get(i+1);
					
					for(String tupleType : getTupleTypeForDependency(controller, startModule, destModule)) {
						if(TimeKeeper.getInstance().getTupleTotalCpu().containsKey(tupleType)) {
							Map<Double, Integer> map = TimeKeeper.getInstance().getTupleTotalCpu().get(tupleType);
							int counter = map.entrySet().iterator().next().getValue();
							double totalTime = map.entrySet().iterator().next().getKey();
							cpu += totalTime/counter;
						}
						
						if(TimeKeeper.getInstance().getTupleTotalNw().containsKey(tupleType)) {
							Map<Double, Integer> map = TimeKeeper.getInstance().getTupleTotalNw().get(tupleType);
							int counter = map.entrySet().iterator().next().getValue();
							double totalTime = map.entrySet().iterator().next().getKey();
							nw += totalTime/counter;
						}
					}
				}
				
				String cpuStr = Util.doubleToString(19, 15, cpu);
				String nwStr = Util.doubleToString(19, 15, nw);
				String totalStr = Util.doubleToString(19, 15, nw + cpu);
				
				String deadline = "inf";
				if(loop.getDeadline() != Constants.INF)
					deadline = Util.doubleToString(19, 15, loop.getDeadline());
				
				String violated = "false";
				if(loop.getDeadline() < nw + cpu) {
					violated = "true";
				}
				
				content += "|" + Util.centerString(col1, name);
				content += "|" + Util.centerString(col2, cpuStr);
				content += "|" + Util.centerString(col2, nwStr);
				content += "|" + Util.centerString(col2, totalStr);
				content += "|" + Util.centerString(col2, deadline);
				content += "|" + Util.centerString(col2-2, violated) + "|\n";
			}
		}
		
		table("APPLICATION LOOP DELAYS (AVERAGE VALUES)", content, subtitles);
	}
	
	/**
	 * Prints the tuple average timing details obtained in the simulation execution.
	 */
	private void printTupleDetails() {
		int col1 = MAX_COLUMN_SIZE-2;
		int col2 = MAX_COLUMN_SIZE/3;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("TUPLE", col1);
		subtitles.put("CPU [s]", col2);
		subtitles.put("NW [s]", col2);
		subtitles.put("TOTAL [s]", col2);
		
		for(String tupleType : TimeKeeper.getInstance().getTupleTotalCpu().keySet()) {
			double cpu = 0, nw = 0;
			
			if(TimeKeeper.getInstance().getTupleTotalCpu().containsKey(tupleType)) {
				Map<Double, Integer> map = TimeKeeper.getInstance().getTupleTotalCpu().get(tupleType);			
				int counter = map.entrySet().iterator().next().getValue();
				cpu = map.entrySet().iterator().next().getKey()/counter;
			}
			
			if(TimeKeeper.getInstance().getTupleTotalNw().containsKey(tupleType)) {
				Map<Double, Integer> map = TimeKeeper.getInstance().getTupleTotalNw().get(tupleType);			
				int counter = map.entrySet().iterator().next().getValue();
				nw = map.entrySet().iterator().next().getKey()/counter;
			}
			
			String cpuStr = Util.doubleToString(19, 15, cpu);
			String nwStr = Util.doubleToString(19, 15, nw);
			String totalStr = Util.doubleToString(19, 15, nw + cpu);
			
			content += "|" + Util.centerString(col1, tupleType);
			content += "|" + Util.centerString(col2, cpuStr);
			content += "|" + Util.centerString(col2, nwStr);
			content += "|" + Util.centerString(col2, totalStr) + "|\n";
		}
			
		
		table("APPLICATION TUPLE DELAYS (AVERAGE VALUES)", content, subtitles);
	}
	
	/**
	 * Prints the loops timing details obtained in the simulation execution.
	 */
	private void printLoopDetails() {
		int col1 = MAX_COLUMN_SIZE-2;
		int col2 = MAX_COLUMN_SIZE/6;
		String content = "";
		double totalLoopCount = 0;
		double totalLoopDiff = 0;
		double loopAverageDev = 0;
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("LOOP", col1);
		subtitles.put("MIN [s]", col2);
		subtitles.put("AVG [s]", col2);
		subtitles.put("MAX [s]", col2);
		subtitles.put("MAX [s]", col2);
		subtitles.put("S.D. [s]", col2);
		subtitles.put("# LOOPS", col2);
		subtitles.put("# VIOLATED", col2-3);
		
		Map<List<String>, List<Double>> loopValues = TimeKeeper.getInstance().getLoopValues();
		
		for(List<String> path : loopValues.keySet()) {
			List<Double> values = loopValues.get(path);
			int nrLoops = values.size();
			int nrViolated = 0;
			double deadline = -1;
			double max = values.get(0);
			double min = values.get(0);
			double avg = 0;
			double s = 0;
			
			for(String appName : controller.getApplications().keySet()) {
				Application application = controller.getApplications().get(appName);
				
				for(AppLoop appLoop : application.getLoops()) {
					if(appLoop.getModules().equals(path)) {
						deadline = appLoop.getDeadline();
						break;
					}
				}
			}
			
			if(deadline == -1) FogComputingSim.err("SimulationResults Err: Should not happen");
			
			double wcs = -1;
			for(int i = 0; i < al.getNumberOfLoops(); i++) {
				ArrayList<String> loopModules = new ArrayList<String>();
				
				for(int j = 0; j < al.getNumberOfModules(); j++) {
					if(j == al.getNumberOfModules() - 1 || al.getLoops()[i][j+1] == -1) {
						loopModules.add(al.getmName()[al.getLoops()[i][j]]);
						break;
					}
					loopModules.add(al.getmName()[al.getLoops()[i][j]]);
				}
				
				if(loopModules.equals(path)) {
					wcs = solution.getLoopDeadline(i);
					break;
				}
			}
			
			if(wcs == -1) FogComputingSim.err("SimulationResults Err: Should not happen");
			
			for(double v : values) {
				avg += v;
				if(v > deadline)
					nrViolated++;
				
				if(max < v)
					max = v;
				
				if(min > v)
					min = v;
				
				if(v > wcs && wcs <= deadline)
					totalLoopDiff += v - wcs;
			}
			
			if(wcs <= deadline) totalLoopCount += values.size();
			
			avg /= nrLoops;
			
			for(double v : values) {
				s += Math.pow(avg-v, 2);
			}
			
			s /= nrLoops;
			s = Math.sqrt(s);
			
			content += "|" + Util.centerString(col1, path.toString());
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, min));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, avg));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, max));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, s));
			content += "|" + Util.centerString(col2, Integer.toString(nrLoops));
			content += "|" + Util.centerString(col2-3, Integer.toString(nrViolated)) + "|\n";
		}
		
		if(totalLoopCount != 0)
			loopAverageDev = totalLoopDiff/totalLoopCount;
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "Loop average deviation [s] = " + loopAverageDev) + "|\n";
		
		table("APPLICATION LOOP DELAYS", content, subtitles);
		
		
	}
	
	/**
	 * Prints the application module migration average timing details obtained in the simulation execution.
	 */
	private void printMigrationDetailsAverage() {
		int col = 2*MAX_COLUMN_SIZE/7;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("NAME", col);
		subtitles.put("NW [s]", col);
		subtitles.put("BOOT [s]", col);
		subtitles.put("TOTAL [s]", col);
		subtitles.put("DEADLINE [s]", col);
		subtitles.put("VIOLATED", col);
		subtitles.put("# MIG", col-3);
		
		for(String appName : controller.getApplications().keySet()) {
			Application application = controller.getApplications().get(appName);
			for(AppModule appModule : application.getModules()) {
				String name = appModule.getName();
				double nw = 0, boot = 0;
				int cnt = 0;
				
				if(TimeKeeper.getInstance().getTupleTotalNw().containsKey(name)) {
					Map<Double, Integer> map = TimeKeeper.getInstance().getTupleTotalNw().get(name);			
					cnt = map.entrySet().iterator().next().getValue();
					nw = map.entrySet().iterator().next().getKey()/cnt;
				}
				
				if(cnt != 0) boot = Config.SETUP_VM_TIME;
				
				String nwStr = Util.doubleToString(19, 15, nw);
				String bootStr = Util.doubleToString(19, 15, boot);
				String totalStr = Util.doubleToString(19, 15, nw + boot);
				String cntStr = Integer.toString(cnt);
				
				String deadline = "inf";
				if(appModule.getMigrationDeadline() != Constants.INF)
					deadline = Util.doubleToString(19, 15, appModule.getMigrationDeadline());
				
				String violated = "false";
				if(appModule.getMigrationDeadline() < nw + boot) {
					violated = "true";
				}
				
				content += "|" + Util.centerString(col, name);
				content += "|" + Util.centerString(col, nwStr);
				content += "|" + Util.centerString(col, bootStr);
				content += "|" + Util.centerString(col, totalStr);
				content += "|" + Util.centerString(col, deadline);
				content += "|" + Util.centerString(col, violated);
				content += "|" + Util.centerString(col-3, cntStr) + "|\n";
			}
		}
		
		table("APPLICATION MODULE MIGRATION DELAYS (AVERAGE VALUES)", content, subtitles);
	}
	
	/**
	 * Prints the application module migration timing details obtained in the simulation execution.
	 */
	private void printMigrationDetails() {
		int col1 = MAX_COLUMN_SIZE-2;
		int col2 = MAX_COLUMN_SIZE/6;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("NAME", col1);
		subtitles.put("MIN [s]", col2);
		subtitles.put("AVG [s]", col2);
		subtitles.put("MAX [s]", col2);
		subtitles.put("MAX [s]", col2);
		subtitles.put("S.D. [s]", col2);
		subtitles.put("# MIG", col2);
		subtitles.put("# VIOLATED", col2-3);
		
		
		Map<String, List<Double>> migValues = TimeKeeper.getInstance().getMigrationValues();
		
		for(String moduleName : migValues.keySet()) {
			List<Double> values = migValues.get(moduleName);
			int nrLoops = values.size();
			int nrViolated = 0;
			double deadline = -1;
			double max = values.get(0);
			double min = values.get(0);
			double avg = 0;
			double s = 0;
			
			for(String appName : controller.getApplications().keySet()) {
				Application application = controller.getApplications().get(appName);
				
				for(AppModule appModule : application.getModules()) {
					if(appModule.getName().equals(moduleName)) {
						deadline = appModule.getMigrationDeadline();
						break;
					}
				}
			}
			
			if(deadline == -1) FogComputingSim.err("SimulationResults Err: Should not happen");
			
			for(double v : values) {
				avg += v;
				if(v > deadline)
					nrViolated++;
				
				if(max < v)
					max = v;
				
				if(min > v)
					min = v;
			}
			
			avg /= nrLoops;
			
			for(double v : values) {
				s += Math.pow(avg-v, 2);
			}
			
			s /= nrLoops;
			s = Math.sqrt(s);
			
			content += "|" + Util.centerString(col1, moduleName);
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, min));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, avg));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, max));
			content += "|" + Util.centerString(col2, Util.doubleToString(19, 15, s));
			content += "|" + Util.centerString(col2, Integer.toString(nrLoops));
			content += "|" + Util.centerString(col2-3, Integer.toString(nrViolated)) + "|\n";
		}
		
		table("APPLICATION MODULE MIGRATION DELAYS", content, subtitles);
	}
	
	/**
	 * Prints the energy details obtained in the simulation execution.
	 */
	private void printEnergyDetails() {
		double total = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("ID", col1);
		subtitles.put("NAME", col2);
		subtitles.put("VALUE [J]", col3);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String energy = Util.doubleToString(30, 15, fogDevice.getEnergyConsumption());
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, energy) + "|\n";
			
			total += fogDevice.getEnergyConsumption();
		}
		String totalStr = Util.doubleToString(30, 15, total);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "TOTAL [J] = " + totalStr) + "|\n";
		
		table("ENERGY", content, subtitles);
	}
	
	/**
	 * Prints both counters for the ordered and processed MIs.
	 */
	private void printCPUDetails() {
		long totalCapacity = 0;
		long totalProcessed = 0;
		double totalOccupation = 0;
		double jfiNum = 0;
		double jfiDen = 0;
		int nrFogDevices = 0;
		
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE - col1;
		int col3 = MAX_COLUMN_SIZE/3;
		int col4 = MAX_COLUMN_SIZE/3;
		int col5 = MAX_COLUMN_SIZE/3-3;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("ID", col1);
		subtitles.put("NAME", col2);
		subtitles.put("CAPACITY [MI]", col3);
		subtitles.put("PROCESSED [MI]", col4);
		subtitles.put("OCCUPATION [%]", col5);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			long c = fogDevice.getHost().getTotalMips();
			long p = fogDevice.getProcessorMonitor().getProcessedMI();
			double o = (double) 100*p/(c*Config.MAX_SIMULATION_TIME);
			
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String capacity = Util.longToString(11, 6, c*Config.MAX_SIMULATION_TIME);
			String processed = Util.longToString(11, 6, p);
			String occ = Util.doubleToString(19, 15, o);
			
			if(fogDevice.getProcessorMonitor().getOrderedMI() < p)
				FogComputingSim.err("SimulationResults Err: Should not happen");
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, capacity);
			content += "|" + Util.centerString(col4, processed);
			content += "|" + Util.centerString(col5, occ) + "|\n";
			
			int isFogDevice = fogDevice instanceof Client ? 0 : 1;
			
			totalCapacity += c*Config.MAX_SIMULATION_TIME*isFogDevice;
			totalProcessed += p*isFogDevice;
			totalOccupation += o*isFogDevice;
			
			jfiNum += o*isFogDevice;
			jfiDen += Math.pow(o*isFogDevice, 2);
			nrFogDevices += isFogDevice;
		}
		
		jfiNum = Math.pow(jfiNum, 2);
		jfiDen *= nrFogDevices;
		
		String wcs = Util.doubleToString(7, 5, (double)1/nrFogDevices);
		String jfi = Util.doubleToString(7, 5, (double)jfiNum/jfiDen);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(col1+col2+1, "TOTAL (FOG/CLOUD DEVICES)");
		content += "|" + Util.centerString(col3, Util.longToString(11, 6, totalCapacity));
		content += "|" + Util.centerString(col4, Util.longToString(11, 6, totalProcessed));
		content += "|" + Util.centerString(col5, Util.doubleToString(19, 15, totalOccupation)) + "|\n";
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "JFI (FOG/CLOUD DEVICES) [ wcs = " + wcs + " ; bcs = 1.0 ] = " + jfi) + "|\n";
		
		table("PROCESSOR", content, subtitles);
	}
	
	/**
	 * Prints the network usage details obtained in the simulation execution.
	 */
	private void printNetworkUsageDetails() {
		long totalCapacity = 0;
		long totalUsed = 0;
		double totalOccupation = 0;
		double jfiNum = 0;
		double jfiDen = 0;
		int nrConnections= 0;
		
		int col1 = MAX_COLUMN_SIZE;
		int col2 = MAX_COLUMN_SIZE/3;
		int col3 = MAX_COLUMN_SIZE/3;
		int col4 = MAX_COLUMN_SIZE/3-2;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("FROM -> TO", col1);
		subtitles.put("CAPACITY [B]", col2);
		subtitles.put("TRANSFERRED [B]", col3);
		subtitles.put("OCCUPATION [%]", col4);
		
		for(FogDevice f1 : controller.getFogDevices()) {
			double totalTime = 0;
			
			int isFogDevice = f1 instanceof Client ? 0 : 1;
			
			for(FogDevice f2 : controller.getFogDevices()) {
				double time = NetworkMonitor.getTotalConnectionTime(f1.getId(), f2.getId());
				double bw = NetworkMonitor.getConnectionVelocity(f1.getId(), f2.getId());
				long size = NetworkMonitor.getNetworkUsageMap(f1.getId(), f2.getId());
				if(time == -1) continue;
				
				double o = 100*size/(time*bw);
				String max = Util.longToString(11, 6, (long) (time*bw));
				String occ = Util.doubleToString(19, 15, o);
				String s = Util.longToString(11, 6, size);
				
				totalCapacity += time*bw*isFogDevice*isFogDevice;
				totalUsed += size*isFogDevice*isFogDevice;
				totalOccupation += o*isFogDevice;
				
				content += "|" + Util.centerString(col1, f1.getName() + " -> " + f2.getName());
				content += "|" + Util.centerString(col2, max);
				content += "|" + Util.centerString(col3, s);
				content += "|" + Util.centerString(col4, occ) + "|\n";
				
				totalTime += time;
				
				jfiNum += o*isFogDevice;
				jfiDen += Math.pow(o*isFogDevice, 2);
				nrConnections += isFogDevice;
			}
			
			/**
			 * Each dynamic node should have always one an only one connection during the whole simulation.
			 * Note that it can be connected to several static nodes during the simulation.
			 */
			if(!f1.isStaticNode() && totalTime != Config.MAX_SIMULATION_TIME)
				FogComputingSim.err("SimulationResults Err: Should not happen");
		}
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(col1, "TOTAL (FOG/CLOUD DEVICES)");
		content += "|" + Util.centerString(col2, Util.longToString(11, 6, totalCapacity));
		content += "|" + Util.centerString(col3, Util.longToString(11, 6, totalUsed));
		content += "|" + Util.centerString(col4, Util.doubleToString(19, 15, totalOccupation)) + "|\n";
		
		jfiNum = Math.pow(jfiNum, 2);
		jfiDen *= nrConnections;
		
		String wcs = Util.doubleToString(7, 5, (double)1/nrConnections);
		String jfi = Util.doubleToString(7, 5, (double)jfiNum/jfiDen);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "JFI (FOG/CLOUD DEVICES) [ wcs = " + wcs + " ; bcs = 1.0 ] = " + jfi) + "|\n";
		
		table("NETWORK", content, subtitles);
	}
	
	/**
	 * Prints the monetary cost details obtained in the simulation execution.
	 */
	private void printCostDetails() {
		double total = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("ID", col1);
		subtitles.put("NAME", col2);
		subtitles.put("VALUE [€]", col3);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String cost = Util.doubleToString(30, 15, fogDevice.getTotalCost());
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, cost) + "|\n";
			
			total += fogDevice.getTotalCost();
		}
		String costStr = Util.doubleToString(30, 15, total);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "TOTAL [€] = " + costStr) + "|\n";
		
		table("COST", content, subtitles);
	}
	
	/**
	 * Prints both the number of packet drop and packet successfully delivered in order to check the QoS degradation.
	 */
	private void printNetworkDetails() {
		String successStr = Integer.toString(NetworkMonitor.getPacketSuccess());
		String dropStr = Integer.toString(NetworkMonitor.getPacketDrop());
		String handoverStr = Integer.toString(controller.getNrHandovers());
		String migrationStr = Integer.toString(controller.getNrMigrations());
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("COUNTER", MAX_COLUMN_SIZE);
		subtitles.put("VALUE", MAX_COLUMN_SIZE);
		
		String content = "|" + Util.centerString(MAX_COLUMN_SIZE, "Packet success");
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, successStr) + "|\n";
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, "Packet drop");
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, dropStr) + "|\n";
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, "Handover");
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, handoverStr) + "|\n";
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, "Migration");
		content += "|" + Util.centerString(MAX_COLUMN_SIZE, migrationStr) + "|\n";
		
		table("NETWORK DETAILS", content, subtitles);
	}
	
	/**
	 * Prints a new table.
	 */
	private void table(String title, String content, Map<String, Integer> subtitles) {
		System.out.println("\n");
		newDetailsField('=', false);
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), title) + "|");
		newDetailsField('-', false);
		
		if(subtitles != null) {
			for(String str : subtitles.keySet()) {
				int size = subtitles.get(str);
				System.out.print("|" + Util.centerString(size, str));
			}
			System.out.println("|");
			newDetailsField('-', false);
		}
		
		System.out.print(content);
		
		newDetailsField('=', false);
	}
	
	/**
	 * Prints or returns a separator with a given character.
	 * 
	 * @param character the character composing the separator
	 * @param ret the flag which defines whether the separator is to be returned in the string form or to be printed
	 * @return the separator in the string form if is to be returned; null if is to be printed
	 */
	private String newDetailsField(char character, boolean ret) {
		String str = "";
		
		if(!ret) {
			for (int i = 0; i < MAX_COLUMN_SIZE*2+2; i++)
			    System.out.print(character);
			System.out.println(character);
		}else {
			for (int i = 0; i < MAX_COLUMN_SIZE*2+2; i++)
				str += character;
			str += character + "\n";
		}
		
		return str;
	}
	
	/**
	 * Gets the list of application module names inside a given loop.
	 * 
	 * @param controller the controller used in the simulation
	 * @param loopId the id of the loop
	 * @return the list of application module names inside the loop
	 */
	static List<String> getListForLoopId(Controller controller, int loopId) {
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppLoop loop : app.getLoops()) {
				if(loop.getLoopId() == loopId)
					return loop.getModules();
			}
		}
		return null;
	}
	
	/**
	 * Gets the list of tuple types for a given dependency.
	 * 
	 * @param controller the controller used in the simulation
	 * @param startModule the start module of the dependency
	 * @param destModule the destiny module of the dependency
	 * @return the list of tuple types for the dependency
	 */
	static List<String> getTupleTypeForDependency(Controller controller, String startModule, String destModule) {
		List<String> tupleTypes = new ArrayList<String>();
		
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppEdge appEdge : app.getEdges()){
				if(!appEdge.getSource().equals(startModule) || !appEdge.getDestination().equals(destModule)) continue;
				if(tupleTypes.contains(appEdge.getTupleType())) continue;
				tupleTypes.add(appEdge.getTupleType());
			}
		}
		return tupleTypes;
	}
	
}
