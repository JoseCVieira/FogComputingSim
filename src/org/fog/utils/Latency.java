package org.fog.utils;

import org.fog.entities.FogDevice;

public class Latency {

	// can be modified to anything
	public static double computeConnectionLatency(FogDevice f1, FogDevice f2){
		return Location.computeDistance(f1.getMovement().getLocation(), f2.getMovement().getLocation())*0.001;
	}
	
}
