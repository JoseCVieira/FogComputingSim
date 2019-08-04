package org.fog.utils;

/**
 * Class which is responsible for counting both the number of packages dropped and successfully delivered during the whole simulation.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
 * @see     tecnico.ulisboa.pt
 * @since   July, 2019
 */
public class Analysis {
	private static int packetDrop;
	private static int packetSuccess;
	
	/**
	 * Increment the number of tuples dropped.
	 */
	public static void incrementPacketDrop() {
		Analysis.packetDrop++;
	}
	
	/**
	 * Get the number of tuples dropped.
	 * 
	 * @return the number of tuples dropped
	 */
	public static int getPacketDrop() {
		return packetDrop;
	}
	
	/**
	 * Increment the number of tuples successfully delivered.
	 */
	public static void incrementPacketSuccess() {
		Analysis.packetSuccess++;
	}
	
	/**
	 * Get the number of tuples successfully delivered.
	 * 
	 * @return the number of tuples successfully delivered
	 */
	public static int getPacketSuccess() {
		return packetSuccess;
	}
	
}
