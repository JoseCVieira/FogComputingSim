package org.fog.placement;

import java.text.DecimalFormat;
import java.util.Calendar;

import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.FogDevice;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.util.MatlabChartUtils;
import org.fog.utils.Analysis;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class OutputControllerResults {
	private static final int MAX_COLUMN_SIZE = 50;
	
	/** Object which holds the information needed to display the simulation results */
	private Controller controller;
	
	/** Defines whether the plot was displayed to the user. If it's true, the program does no terminates until the user presses the ENTER key */
	public static boolean isDisplayingPlot = false;
	
	/**
	 * Prints the results obtained in the simulation execution.
	 * 
	 * @param controller
	 */
	public OutputControllerResults(Controller controller) {
		this.controller = controller;
		
		printTimeDetails();
		printEnergyDetails();
		printCostDetails();
		printNetworkUsageDetails();
		printPacketDetails();
	}
	
	/**
	 * Prints the time details obtained in the simulation execution.
	 */
	private void printTimeDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "EXECUTION TIME") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), String.valueOf(Calendar.getInstance().getTimeInMillis() -
				TimeKeeper.getInstance().getSimulationStartTime())) + "|");
		newDetailsField(2, '=');
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "APPLICATION LOOP DELAYS") + "|");
		newDetailsField(2, '-');
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), getStringForLoopId(loopId) + " ---> "+
					df.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)).toString()) + "|");
		}
		newDetailsField(2, '=');

		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TUPLE CPU EXECUTION DELAY") + "|");
		newDetailsField(2, '-');
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet())
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, tupleType) + "|" +
					Util.centerString(MAX_COLUMN_SIZE,
							df.format(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType)).toString()) + "|\n");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the energy details obtained in the simulation execution.
	 */
	private void printEnergyDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "ENERGY CONSUMED") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				Util.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				Util.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : controller.getFogDevices()) {
			aux += fogDevice.getEnergyConsumption();
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					Util.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					Util.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getEnergyConsumption()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the monetary cost details obtained in the simulation execution.
	 */
	private void printCostDetails() {
		DecimalFormat df = new DecimalFormat("0.00"); 
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "COST OF EXECUTION") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				Util.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				Util.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : controller.getFogDevices()) {
			aux += fogDevice.getTotalCost();
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					Util.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					Util.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getTotalCost()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints the network usage details obtained in the simulation execution.
	 */
	private void printNetworkUsageDetails() {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL NETWORK USAGE") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "" +
				NetworkUsageMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME) + "|");
		newDetailsField(2, '=');
	}
	
	/**
	 * Prints both the number of packet drop and packet successfully delivered in order to check the QoS degradation.
	 */
	private static void printPacketDetails() {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + Util.centerString((MAX_COLUMN_SIZE*2+1), "PACKET COUTERS") + "|");
		newDetailsField(2, '-');
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Success") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(Analysis.getPacketSuccess())) + "|\n");
			System.out.print("|" + Util.centerString(MAX_COLUMN_SIZE, "Drop") + "|" +
					Util.centerString(MAX_COLUMN_SIZE, Integer.toString(Analysis.getPacketDrop())) + "|\n");
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
	private String getStringForLoopId(int loopId){
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
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
