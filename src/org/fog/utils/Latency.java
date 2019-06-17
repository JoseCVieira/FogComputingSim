package org.fog.utils;

import org.fog.core.Config;
import org.fog.entities.FogDevice;

public class Latency {

	// can be modified to anything
	public static double computeConnectionLatency(FogDevice f1, FogDevice f2){
		double latency = Location.computeDistance(f1, f2)*0.001;
		if(latency < Config.MIN_LATENCY)
			return Config.MIN_LATENCY;
		return latency;
	}
	
}
