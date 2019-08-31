package org.fog.placement.algorithm.util;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.FogDevice;
import org.fog.placement.Controller;
import org.fog.placement.algorithm.Algorithm;
import org.fog.utils.NetworkMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class OutputControllerResults {
	private static final int MAX_COLUMN_SIZE = 75;
	
	/** Object which holds the information needed to display the simulation results */
	private Controller controller;
	
	/** Defines whether the plot was displayed to the user. If it's true, the program does no terminates until the user presses the ENTER key */
	public static boolean isDisplayingPlot = false;
	
	private static DecimalFormat df = new DecimalFormat("0.000");
	
	/**
	 * Prints the results obtained in the simulation execution.
	 * 
	 * @param controller
	 */
	public OutputControllerResults(Controller controller) {
		this.controller = controller;
		
		printTimeDetails();
		printLoopDetails();
		printEnergyDetails();
		printCPUDetails();
		printCostDetails();
		printNetworkUsageDetails();
		printNetworkDetails();
		
		if(Config.EXPORT_RESULTS_EXCEL) {
			try {
				ExcelUtils.writeExcel(controller);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Prints the time details obtained in the simulation execution.
	 */
	private void printTimeDetails() {
		long time = Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime();
		
		printTitle("EXECUTION TIME [s]");
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), String.valueOf(time)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the loops timing details obtained in the simulation execution.
	 */
	private void printLoopDetails() {
		int col1 = MAX_COLUMN_SIZE*6/5-3;
		int col2 = MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE/5;
		int col4 = MAX_COLUMN_SIZE/5;
		int col5 = MAX_COLUMN_SIZE/5;
		
		printTitle("APPLICATION LOOP DELAYS [s]");
		System.out.print("|" + Util.centerString(col1, "LOOP") + "|" + Util.centerString(col2, "CPU") + "|" + Util.centerString(col3, "LATENCY") + "|" + Util.centerString(col4, "BANDWIDTH") + "|" + Util.centerString(col5, "TOTAL") + "|\n");
		newDetailsField(2, '-');
		
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			String name = getStringForLoopId(loopId);
			List<String> modules = getListForLoopId(loopId);
			double cpu = 0;
			double lat = 0;
			double bw = 0;		
			
			for(int i = 0; i < modules.size()-1; i++) {
				String startModule = modules.get(i);
				String destModule = modules.get(i+1);
				
				for(String tupleType : getTupleTypeForDependency(startModule, destModule)) {
					if(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().containsKey(tupleType))
						cpu += TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
				}
			}
			
			for(int i = 0; i < modules.size()-1; i++) {
				String startModule = modules.get(i);
				String destModule = modules.get(i+1);
				
				for(String tupleType : getTupleTypeForDependency(startModule, destModule)) {
					if(TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().containsKey(tupleType)) {
						Map<Double, Integer> map = TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().get(tupleType);
						double totalLat = map.entrySet().iterator().next().getKey();
						int counter = map.entrySet().iterator().next().getValue();
						lat += totalLat/counter;
						
						map = TimeKeeper.getInstance().getLoopIdToCurrentNwBwAverage().get(tupleType);
						double totalBw = map.entrySet().iterator().next().getKey();
						bw += totalBw/counter;
					}
				}
			}
			
			df = new DecimalFormat("0.######E0");
			String bwf = df.format(bw);
			df = new DecimalFormat("0.000");
			String total = df.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId));
			
			System.out.print("|" + Util.centerString(col1, name) + "|" + Util.centerString(col2, df.format(cpu)) + "|" +
			Util.centerString(col3, df.format(lat)) + "|" + Util.centerString(col4, bwf) + "|" + Util.centerString(col5, total) + "|\n");
		}
		newDetailsField(2, '=');
		
		printTitle("TUPLE CPU EXECUTION DELAY [s]");
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
			String delay = df.format(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType));
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, tupleType) + "|" + Util.centerString(MAX_COLUMN_SIZE, delay) + "|\n");
		}
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the energy details obtained in the simulation execution.
	 */
	private void printEnergyDetails() {
		double total = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE;
		
		printTitle("ENERGY CONSUMED [W]");
		System.out.print("|" + Util.centerString(col1, "ID") + "|" + Util.centerString(col2, "NAME") + "|" + Util.centerString(col3, "VALUE") + "|\n");
		newDetailsField(2, '-');
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String energy = df.format(fogDevice.getEnergyConsumption());
			System.out.print("|" + Util.centerString(col1, id) + "|" + Util.centerString(col2, name) + "|" + Util.centerString(col3, energy) + "|\n");
			total += fogDevice.getEnergyConsumption();
		}
		newDetailsField(2, '-');
		
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(total)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints both counters for the ordered and processed MIs.
	 */
	private void printCPUDetails() {
		double totalOdered = 0;
		double totalProcessed = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/2;
		int col4 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/2-2;
		
		printTitle("PROCESSOR [MI]");
		System.out.print("|" + Util.centerString(col1, "ID") + "|" + Util.centerString(col2, "NAME") + "|" + Util.centerString(col3, "ORDERED") + "|" + Util.centerString(col4, "PROCESSED") + "|\n");
		newDetailsField(2, '-');
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String odered = df.format(fogDevice.getProcessorMonitor().getOrderedMI());
			String processed = df.format(fogDevice.getProcessorMonitor().getProcessedMI());
			System.out.print("|" + Util.centerString(col1, id) + "|" + Util.centerString(col2, name) + "|" + Util.centerString(col3, odered) + "|" + Util.centerString(col4, processed) + "|\n");
			totalOdered += fogDevice.getProcessorMonitor().getOrderedMI();
			totalProcessed += fogDevice.getProcessorMonitor().getProcessedMI();
		}
		newDetailsField(2, '-');
		
		String tOdered = df.format(totalOdered);
		String tProcessed = df.format(totalProcessed);
		
		System.out.print("|" + Util.centerString(col1+col2+1, "TOTAL") + "|" + Util.centerString(col3, tOdered) + "|" + Util.centerString(col4, tProcessed) + "|\n");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the monetary cost details obtained in the simulation execution.
	 */
	private void printCostDetails() {
		double total = 0;
		int col1 = MAX_COLUMN_SIZE/5-1;
		int col2 = MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5;
		int col3 = MAX_COLUMN_SIZE;
		
		printTitle("COST OF EXECUTION [€]");
		System.out.print("|" + Util.centerString(col1, "ID") + "|" + Util.centerString(col2, "NAME") + "|" + Util.centerString(col3, "VALUE") + "|\n");
		newDetailsField(2, '-');
		
		for(FogDevice fogDevice : controller.getFogDevices()) {
			String id = Integer.toString(fogDevice.getId());
			String name = fogDevice.getName();
			String cost = df.format(fogDevice.getTotalCost());
			System.out.print("|" + Util.centerString(col1, id) + "|" + Util.centerString(col2, name) + "|" + Util.centerString(col3, cost) + "|\n");
			total += fogDevice.getTotalCost();
		}
		newDetailsField(2, '-');
		
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(total)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the network usage details obtained in the simulation execution.
	 */
	private void printNetworkUsageDetails() {
		double value = NetworkMonitor.getNetworkUsage();
		
		printTitle("NETWORK USAGE TIME [s]");
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "" + df.format(value)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the tile of the table
	 */
	private void printTitle(String title) {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), title) + "|");
		newDetailsField(2, '-');
	}
	
	/**
	 * Prints both the number of packet drop and packet successfully delivered in order to check the QoS degradation.
	 */
	private void printNetworkDetails() {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "NETWORK DETAILS") + "|");
		newDetailsField(2, '-');
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Packet success counter") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(NetworkMonitor.getPacketSuccess())) + "|\n");
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Packet drop counter") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(NetworkMonitor.getPacketDrop())) + "|\n");
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Handover counter") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(controller.getNrHandovers())) + "|\n");
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Migration counter") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(controller.getNrMigrations())) + "|\n");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints a separator with a given character.
	 * 
	 * @param nrColumn value used to compute the number of times the character is printed
	 * @param character the character to be printed
	 */
	private static void newDetailsField(int nrColumn, char character) {
		for (int i = 0; i < MAX_COLUMN_SIZE*nrColumn+(nrColumn); i++)
		    System.out.print(character);
		System.out.println(character);
	}
	
	/**
	 * Converts a loop with a given id into a string.
	 * 
	 * @param loopId the id of the loop
	 * @return the parsed loop string
	 */
	private String getStringForLoopId(int loopId) {
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}
	
	private List<String> getListForLoopId(int loopId) {
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules();
			}
		}
		return null;
	}
	
	private List<String> getTupleTypeForDependency(String startModule, String destModule) {
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
	
	/**
	 * Plots the iteration/value map.
	 * 
	 * @param algorithm the object which holds all the information needed to run the optimization algorithm
	 * @param title the title to be displayed in the dialog
	 */
	public static void plotResult(Algorithm algorithm, String title) {
		isDisplayingPlot = true;
		MatlabChartUtils matlabChartUtils = new MatlabChartUtils(algorithm, title);
    	matlabChartUtils.setVisible(true);
	}
	
}
