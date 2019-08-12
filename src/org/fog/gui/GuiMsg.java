package org.fog.gui;

/**
 * Class which holds all messages used within the GUI.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GuiMsg {
	public static final String TipDevName = "Name of the fog device";
	public static final String TipDevLevel = "Defines the position at the graphical interface (it is not used by the simulation itself)";
	public static final String TipDevMips = "Available MIPS of the machine";
	public static final String TipDevRam = "Available ram in the machine";
	public static final String TipDevStrg = "Available storage in the machine";
	public static final String TipDevMipsPrice = "Price that will be charged by using processing resources";
	public static final String TipDevRamPrice = "Price that will be charged by using memory resources";
	public static final String TipDevStrgPrice = "Price that will be charged by using storage resources";
	public static final String TipDevBwPrice = "Price that will be charged by bandwidth resources";
	public static final String TipDevEnPrice = "Price that will be charged by spending energy";
	public static final String TipDevBusyPw = "Power value while using the full processing capacity of the machine";
	public static final String TipDevIdlePw = "Power value while using no processing resources in the machine";
	public static final String TipDevXCoord = "X coordinate of the machine";
	public static final String TipDevYCoord = "Y coordinate of the machine";
	public static final String TipDevDir = "Defines the direction which the machine is following (if it is a fixed node this value is ignored)";
	public static final String TipDevVel = "Defines the velocity of the machine (if it is a fixed node this value is ignored)";
	public static final String TipDevApp = "Defines the application (if it has an applications it means that it is a client)";
	public static final String TipDevDist = "Defines the time interval (deterministic or not) when the sensor will generate new tuples";
	public static final String TipDevnMean = "Mean of the normal distribution";
	public static final String TipDevnStd = "Standard deviation of the normal distribution";
	public static final String TipDevuLow = "Minimum value of the uniform distribution";
	public static final String TipDevuUp = "Maximum value of the uniform distribution";
	public static final String TipDevdVal = "Value of the deterministic distribution";
	
	/**
	 * Concatenate the missing error message to the provided string.
	 * 
	 * @param str the provided string
	 * @return the final error message
	 */
	public static String errMissing(String str) {
		return "Missing " + str + "\n";
	}
	
	/**
	 * Concatenate the format error message to the provided string.
	 * 
	 * @param str the provided string
	 * @return the final error message
	 */
	public static String errFormat(String str) {
		return "\n" + str + " should be a positive number";
	}
	
}
