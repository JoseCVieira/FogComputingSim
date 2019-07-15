package org.fog.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author jcrv
 * 
 *	Model for 4G-LTE bandwidth communications based on several modulations
 */
public class MobileBandwidthModel {
	private final static String[] NAMES = {"64 QAM w/ MIMO 2x2", "16 QAM w/ MIMO 2x2", "64 QAM w/ SIMO 1x2", "16 QAM w/ SIMO 1x2", "64 QAM w/ SISO 1x1", "16 QAM w/ SISO 1x1"};
	private final static double[] A = {0.1365, 0.2673, 0.3111, 0.7105, 0.1915, 0.3917};
	private final static double[] c = {0.1808, 0.1939, 0.2029, 0.2528, 0.2032, 0.2521};
	private final static double[] RREF = {6.0204, 7.8873, 6.9442, 10.0372, 4.2814, 5.5495};
	
	private final static double BANDWIDTH_SIGNAL = 20.0;		// 5-20 MHz
	private final static double NOISE_FACTOR = 7;				// 6,11 dB
	private final static double EFFECTIVE_NOISE_POWER = 174;	// dBm/MHz
	
	private final static double INTERFERENCE_MARGIN = 1;		// 1-3 dB
	
	public static Map<String, Double> computeCommunicationBandwidth(int nSub, double signalPower) {
		double noisePower = -EFFECTIVE_NOISE_POWER + 10*Math.log10(nSub*BANDWIDTH_SIGNAL) + NOISE_FACTOR;
		double snr = signalPower - noisePower - INTERFERENCE_MARGIN;
		
		int modulationIndex = -1;
		double communicationSpeed = -1;		
		for(int i = 0; i < NAMES.length; i++) {
			double tmp = RREF[i] / (A[i] + Math.exp(-c[i]*snr));
			
			if(communicationSpeed < tmp) {
				communicationSpeed = tmp;
				modulationIndex = i;
			}
		}
		
		Map<String, Double> retVal = new HashMap<String, Double>();
		retVal.put(NAMES[modulationIndex], communicationSpeed*1024*1024);
		
		return retVal;
	}
	
}
