package org.fog.utils;

public class Analysis {
	private static int packetDrop;
	private static int packetSuccess;
	
	public static void incrementPacketDrop() {
		Analysis.packetDrop++;
	}
	
	public static int getPacketDrop() {
		return packetDrop;
	}
	
	public static void incrementPacketSuccess() {
		Analysis.packetSuccess++;
	}
	
	public static int getPacketSuccess() {
		return packetSuccess;
	}
	
}
