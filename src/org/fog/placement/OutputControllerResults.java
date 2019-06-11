package org.fog.placement;

import java.text.DecimalFormat;
import java.util.Calendar;

import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.entities.FogDevice;
import org.fog.placement.algorithm.Algorithm;
import org.fog.placement.algorithm.overall.util.MatlabChartUtils;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class OutputControllerResults {
	private static final int MAX_COLUMN_SIZE = 50;
	
	private Controller controller;
	private Util u;
	
	public static boolean isDisplayingPlot = false; // Used when displaying a plot to wait until user presses the ENTER key to terminate
	
	public OutputControllerResults(Controller controller) {
		this.controller = controller;
		this.u = new Util();
		
		printTimeDetails();
		printPowerDetails();
		printCostDetails();
		printNetworkUsageDetails();
	}
	
	private void printTimeDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		
		System.out.println("\n\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "EXECUTION TIME") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), String.valueOf(Calendar.getInstance().getTimeInMillis() -
				TimeKeeper.getInstance().getSimulationStartTime())) + "|");
		newDetailsField(2, '=');
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "APPLICATION LOOP DELAYS") + "|");
		newDetailsField(2, '-');
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), getStringForLoopId(loopId) + " ---> "+
					df.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)).toString()) + "|");
		}
		newDetailsField(2, '=');

		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TUPLE CPU EXECUTION DELAY") + "|");
		newDetailsField(2, '-');
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet())
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE, tupleType) + "|" +
					u.centerString(MAX_COLUMN_SIZE,
							df.format(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType)).toString()) + "|\n");
		newDetailsField(2, '=');
	}
	
	private void printNetworkUsageDetails() {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL NETWORK USAGE") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "" +
				NetworkUsageMonitor.getNetworkUsage()/Constants.MAX_SIMULATION_TIME) + "|");
		newDetailsField(2, '=');
	}
	
	private void printCostDetails(){
		DecimalFormat df = new DecimalFormat("0.00"); 
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "COST OF EXECUTION") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				u.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : controller.getFogDevices()) {
			aux += fogDevice.getTotalCost();
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					u.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getTotalCost()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	private void printPowerDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "ENERGY CONSUMED") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				u.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : controller.getFogDevices()) {
			aux += fogDevice.getEnergyConsumption();
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					u.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getEnergyConsumption()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	private static void newDetailsField(int nrColumn, char character) {
		for (int i=0; i<MAX_COLUMN_SIZE*nrColumn+(nrColumn); i++)
		    System.out.print(character);
		System.out.println(character);
	}
	
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
	
	public static void plotResult(Algorithm algorithm, String title) {
		isDisplayingPlot = true;
		MatlabChartUtils matlabChartUtils = new MatlabChartUtils(algorithm, title);
    	matlabChartUtils.setVisible(true);
	}
	
}
