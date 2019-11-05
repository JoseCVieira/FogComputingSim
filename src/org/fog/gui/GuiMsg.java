package org.fog.gui;

/**
 * Class which holds all messages used within the GUI.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GuiMsg {
	public static final String TipEdgeFrom = "Module where the tuples will be generated";
	public static final String TipEdgeTo = "Module where the tuples will be received";
	public static final String TipEdgeType = "Defines if the the tuple is generated by a sensor or a module or will be sent to an actuator";
	public static final String TipEdgePeri = "Defines if the the tuple is generated in a periodic or reactive way";
	public static final String TipEdgeSensor = "The name of the source sensor";
	public static final String TipEdgeActuator = "The name of the destination actuator";
	public static final String TipEdgePeriod = "The time interval between tuple generation";
	public static final String TipEdgeCPU = "The CPU size which will be needed to be processed by each tuple";
	public static final String TipEdgeNW = "The network tuple size needed to be sent";
	public static final String TipEdgeTupleType = "The name/label of the application edge";
	
	public static final String TipLoopDeadline = "The time acceptable by the user for the loop execution in the worst case scenario";
	
	public static final String TipModName = "Name of the application module";
	public static final String TipModStrg = "Storage needed to support this module";
	public static final String TipModRam = "Ram needed to support this module";
	public static final String TipModMig = "Maximum allowed time to spend in each migration";
	public static final String TipModClient = "Defines whether this module should specifically run inside the client node (e.g., GUI)";
	public static final String TipModGlobal = "Defines whether this module is used by all users running this application (e.g., server)";
	
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
	public static final String TipDevMov = "Defines type of movement of the machine";
	public static final String TipDevxL = "Defines the x length describing the rectangle each the machine follows";
	public static final String TipDevyL = "Defines the Y length describing the rectangle each the machine follows";
	public static final String TipDevVel = "Defines the velocity of the machine";
	public static final String TipDevApp = "Defines the application (if it has an applications it means that it is a client)";
	public static final String TipDevDist = "Defines the time interval (deterministic or not) when the sensor will generate new tuples";
	public static final String TipDevnMean = "Mean of the normal distribution";
	public static final String TipDevnStd = "Standard deviation of the normal distribution";
	public static final String TipDevuLow = "Minimum value of the uniform distribution";
	public static final String TipDevuUp = "Maximum value of the uniform distribution";
	public static final String TipDevdVal = "Value of the deterministic distribution";
	
	public static final String TipLinkSource = "One of the fog nodes to be added the connection";
	public static final String TipLinkDest = "Another fog node to be added the connection";
	public static final String TipLinkLat = "Latency which characterizes the connection";
	public static final String TipLinkBw = "Bandwidth which characterizes the connection";
	
	public static final String TipTupleMod = "The name of the module which will process the input tuple and randomly generate the output one";
	public static final String TipTupleIn = "The name/label of the input tuple";
	public static final String TipTupleOut = "The name/label of the output tuple";
	public static final String TipTupleProb = "The probability of the outuput tuple being generated upon the execution of the input one";
	
	public static final String TipSettPrintAlgIter = "Defines whether the best value is printed between iterations are printed";
	public static final String TipSettPrintAlgRes = "Defines whether the final result of the algorithm (i.e., the best solution) is printed";
	public static final String TipSettPlotAlgRes = "Defines whether the iteration-value map is plotted";
	public static final String TipSettExcel = "Defines whether the results of both the algorithm and the simulation are exported to the output excel file";
	public static final String TipSettDebug = "Defines whether the simulation runs in debug mode (i.e., prints the debug logs defined in the original version of iFogSim)";
	public static final String TipSettDetails = "Defines whether the simulation should print logs about the simulation (e.g., tuple transmission, migrations, processing, etc.)";
	public static final String TipSettCost = "Defines whether the simulation should print logs about the costs and resource usage";
	public static final String TipSettDynamic = "Defines whether the simulation is dynamic (i.e., mobile nodes actualy move around)";
	public static final String TipSettMigration = "Defines whether the simulation is allowed to perform migrations of VMs";
	
	public static final String TipRandNrFog = "Defines number of fog devices within the random topology";
	public static final String TipRandConnect = "Defines the probability of creating links (if a node is not connected it is a mobile node)";
	public static final String TipRandClient = "Defines the probability of a node being a client (has one application)";
	public static final String TipRandMobile = "Defines the probability of a node being mobile";
	public static final String TipRandLevelDec = "Defines the level decadency factor (mean and deviation  values are multiplied by 1/((level+1)*decadency factor)";
	
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
