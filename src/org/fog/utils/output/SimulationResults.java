package org.fog.utils.output;

import java.text.DecimalFormat;
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
import org.fog.entities.FogDevice;
import org.fog.placement.Controller;
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
	private static final int MAX_COLUMN_SIZE = 85;
	
	/** Object which holds the information needed to display the simulation results */
	private Controller controller;
	
	/**
	 * Prints the results obtained in the simulation execution.
	 * 
	 * @param controller the controller used in the simulation
	 */
	public SimulationResults(Controller controller) {
		this.controller = controller;
		
		printTimeDetails();
		printLoopDetails();
		printTupleDetails();
		printMigrationDetails();
		printEnergyDetails();
		printCPUDetails();
		printCostDetails();
		printNetworkUsageDetails();
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
	 * Prints the loops timing details obtained in the simulation execution.
	 */
	private void printLoopDetails() {
		int col1 = MAX_COLUMN_SIZE-4;
		int col2 = MAX_COLUMN_SIZE/5;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("LOOP", col1);
		subtitles.put("CPU", col2);
		subtitles.put("LATENCY", col2);
		subtitles.put("BANDWIDTH", col2);
		subtitles.put("QUEUE", col2);
		subtitles.put("TOTAL", col2);
		
		
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			List<String> modules = getListForLoopId(controller, loopId);
			String name = modules.toString();
			double cpu = 0, lat = 0, bw = 0, nw = 0;
			
			for(int i = 0; i < modules.size()-1; i++) {
				String startModule = modules.get(i);
				String destModule = modules.get(i+1);
				
				for(String tupleType : getTupleTypeForDependency(controller, startModule, destModule)) {
					if(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().containsKey(tupleType)) {
						cpu += TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
					}
					
					if(TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().containsKey(tupleType)) {
						Map<Double, Integer> map = TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().get(tupleType);
						double totalLat = map.entrySet().iterator().next().getKey();
						int counter = map.entrySet().iterator().next().getValue();
						lat += totalLat/counter;
						
						map = TimeKeeper.getInstance().getLoopIdToCurrentNwBwAverage().get(tupleType);
						double totalBw = map.entrySet().iterator().next().getKey();
						bw += totalBw/counter;
						
						map = TimeKeeper.getInstance().getLoopIdToCurrentNwAverage().get(tupleType);
						double totalTime = map.entrySet().iterator().next().getKey();
						nw += totalTime/counter;
					}
				}
			}
			
			String cpuStr = doubleToString(cpu, false);
			String latStr = doubleToString(lat, false);
			String bwStr = doubleToString(bw, true);
			String queueStr = doubleToString(nw-lat-bw, true);
			String totalStr = doubleToString(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId), false);
			
			content += "|" + Util.centerString(col1, name);
			content += "|" + Util.centerString(col2, cpuStr);
			content += "|" + Util.centerString(col2, latStr);
			content += "|" + Util.centerString(col2, bwStr);
			content += "|" + Util.centerString(col2, queueStr);
			content += "|" + Util.centerString(col2, totalStr) + "|\n";
		}
		
		table("APPLICATION LOOP DELAYS [s]", content, subtitles);
	}
	
	/**
	 * Prints the tuple timing details obtained in the simulation execution.
	 */
	private void printTupleDetails() {
		int col1 = MAX_COLUMN_SIZE-4;
		int col2 = MAX_COLUMN_SIZE/5;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("TUPLE", col1);
		subtitles.put("CPU", col2);
		subtitles.put("LATENCY", col2);
		subtitles.put("BANDWIDTH", col2);
		subtitles.put("QUEUE", col2);
		subtitles.put("TOTAL", col2);
		
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
			double cpu = 0, lat = 0, bw = 0, nw = 0;
			
			cpu = TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
			
			if(TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().containsKey(tupleType)) {
				Map<Double, Integer> map = TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().get(tupleType);			
				int counter = map.entrySet().iterator().next().getValue();	
				lat = map.entrySet().iterator().next().getKey()/counter;
				
				map = TimeKeeper.getInstance().getLoopIdToCurrentNwBwAverage().get(tupleType);
				bw = map.entrySet().iterator().next().getKey();
				
				map = TimeKeeper.getInstance().getLoopIdToCurrentNwAverage().get(tupleType);
				nw = map.entrySet().iterator().next().getKey()/counter;
			}
			
			String cpuStr = doubleToString(cpu, false);
			String latStr = doubleToString(lat, false);
			String bwStr = doubleToString(bw, true);
			String queueStr = doubleToString(nw-lat-bw, true);
			String totalStr = doubleToString(cpu + nw, false);
			
			content += "|" + Util.centerString(col1, tupleType);
			content += "|" + Util.centerString(col2, cpuStr);
			content += "|" + Util.centerString(col2, latStr);
			content += "|" + Util.centerString(col2, bwStr);
			content += "|" + Util.centerString(col2, queueStr);
			content += "|" + Util.centerString(col2, totalStr) + "|\n";
		}
			
		
		table("APPLICATION TUPLE DELAYS [s]", content, subtitles);
	}
	
	/**
	 * Prints the application module migration timing details obtained in the simulation execution.
	 */
	private void printMigrationDetails() {
		int col1 = MAX_COLUMN_SIZE-4;
		int col2 = MAX_COLUMN_SIZE/6;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("NAME", col1);
		subtitles.put("LATENCY", col2);
		subtitles.put("BANDWIDTH", col2);
		subtitles.put("QUEUE", col2);
		subtitles.put("BOOT", col2);
		subtitles.put("TOTAL", col2);
		subtitles.put("# MIG", col2);
		
		for(String appName : controller.getApplications().keySet()) {
			Application application = controller.getApplications().get(appName);
			for(AppModule appModule : application.getModules()) {
				String name = appModule.getName();
				double lat = 0, bw = 0, nw = 0, boot = 0;
				int cnt = 0;
				
				if(TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().containsKey(name)) {
					Map<Double, Integer> map = TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().get(name);			
					cnt = map.entrySet().iterator().next().getValue();	
					lat = map.entrySet().iterator().next().getKey()/cnt;
					
					map = TimeKeeper.getInstance().getLoopIdToCurrentNwBwAverage().get(name);
					bw = map.entrySet().iterator().next().getKey()/cnt;
					
					map = TimeKeeper.getInstance().getLoopIdToCurrentNwAverage().get(name);
					nw = map.entrySet().iterator().next().getKey()/cnt;
				}
				
				if(cnt != 0) boot = Config.SETUP_VM_TIME;
				
				String latStr = doubleToString(lat, false);
				String bwStr = doubleToString(bw, true);
				String queueStr = doubleToString(nw-lat-bw, true);
				String bootStr = doubleToString(boot, false);
				String totalStr = doubleToString(nw + boot, false);
				String cntStr = Integer.toString(cnt);
				
				content += "|" + Util.centerString(col1, name);
				content += "|" + Util.centerString(col2, latStr);
				content += "|" + Util.centerString(col2, bwStr);
				content += "|" + Util.centerString(col2, queueStr);
				content += "|" + Util.centerString(col2, bootStr);
				content += "|" + Util.centerString(col2, totalStr);
				content += "|" + Util.centerString(col2, cntStr) + "|\n";
			}
		}
		
		table("APPLICATION MODULE MIGRATION DELAYS [s]", content, subtitles);
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
		subtitles.put("VALUE", col3);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String energy = doubleToString(fogDevice.getEnergyConsumption(), false);
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, energy) + "|\n";
			
			total += fogDevice.getEnergyConsumption();
		}
		String totalStr = doubleToString(total, false);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "TOTAL = " + totalStr) + "|\n";
		
		table("ENERGY CONSUMED [W]", content, subtitles);
	}
	
	/**
	 * Prints both counters for the ordered and processed MIs.
	 */
	private void printCPUDetails() {
		long totalOdered = 0;
		long totalProcessed = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE;
		int col3 = MAX_COLUMN_SIZE/4;
		int col4 = MAX_COLUMN_SIZE/4;
		int col5 = MAX_COLUMN_SIZE/3-4;
		String content = "";
		
		Map<String, Integer> subtitles = new LinkedHashMap<String, Integer>();
		subtitles.put("ID", col1);
		subtitles.put("NAME", col2);
		subtitles.put("ORDERED", col3);
		subtitles.put("PROCESSED", col4);
		subtitles.put("ORDERED >= PROCESSED", col5);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String odered = Long.toString(fogDevice.getProcessorMonitor().getOrderedMI());
			String processed = Long.toString(fogDevice.getProcessorMonitor().getProcessedMI());
			String flag = Boolean.toString(fogDevice.getProcessorMonitor().getOrderedMI() >= fogDevice.getProcessorMonitor().getProcessedMI());
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, odered);
			content += "|" + Util.centerString(col4, processed);
			content += "|" + Util.centerString(col5, flag) + "|\n";
			
			totalOdered += fogDevice.getProcessorMonitor().getOrderedMI();
			totalProcessed += fogDevice.getProcessorMonitor().getProcessedMI();
		}		
		String oderedStr = Long.toString(totalOdered);
		String processedStr = Long.toString(totalProcessed);
		String flagStr = Boolean.toString(totalOdered >= totalProcessed);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(col1+col2+1, "TOTAL");
		content += "|" + Util.centerString(col3, oderedStr);
		content += "|" + Util.centerString(col4, processedStr);
		content += "|" + Util.centerString(col5, flagStr) + "|\n";
		
		table("PROCESSOR [MI]", content, subtitles);
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
		subtitles.put("VALUE", col3);
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String cost = doubleToString(fogDevice.getTotalCost(), false);
			
			content += "|" + Util.centerString(col1, id);
			content += "|" + Util.centerString(col2, name);
			content += "|" + Util.centerString(col3, cost) + "|\n";
			
			total += fogDevice.getTotalCost();
		}
		String costStr = doubleToString(total, false);
		
		content += newDetailsField('-', true);
		content += "|" + Util.centerString(MAX_COLUMN_SIZE*2+1, "TOTAL = " + costStr) + "|\n";
		
		table("COST OF EXECUTION [€]", content, subtitles);
	}
	
	/**
	 * Prints the network usage details obtained in the simulation execution.
	 */
	private void printNetworkUsageDetails() {
		String valueStr = doubleToString(NetworkMonitor.getNetworkUsage(), true);
		String content = "|" + Util.centerString((MAX_COLUMN_SIZE*2+1), valueStr) + "|\n";
		table("NETWORK USAGE [s]", content, null);
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
	 * Converts and formats a given double number to string.
	 * 
	 * @param value the value to be converted
	 * @param exp the flag which defines if the format is given in the exponential form
	 * @return the string
	 */
	private String doubleToString(double value, boolean exp) {
		DecimalFormat df = new DecimalFormat("0.00000");
		if(exp) df = new DecimalFormat("0.#####E0");
		
		String str = df.format(0);
		if(value > Constants.EPSILON) {
			str = df.format(value);
		}
		
		return str;
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
