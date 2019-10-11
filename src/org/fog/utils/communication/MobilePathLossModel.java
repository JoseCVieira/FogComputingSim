package org.fog.utils.communication;


/**
 * Class which defines the path loss model used to compute the received signal power in the mobile 4G-LTE communications based on isotropic antennas.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class MobilePathLossModel {
	public final static double TX_POWER = 199.5262315E-3;	// 200 mW ~= 23 dBm
	public final static double LATENCY = 50E-3;				// 50 ms
	private final static double GAMMA = 3.1;
	
	/**
	 * 
	 * GAMMA:
	 * 	- Free space:2
	 *	- Urban area cellular radio: 2.7 to 3.5
	 *	- Shadowed cellular radio: 3 to 5
	 *	- In building line-of-sight: 1.6 to 1.8
	 *	- Obstructed in building: 4 to 6
	 *	- Obstructed in factories: 2 to 3
	 *
	 */
		
	/**
	 * Computes the received signal power at the receiver based on the distance.
	 * Note: 4*Math.PI*R^2 is the sphere area, thus at a given point the received  power is given by transmitted power divided by the its area.
	 * Because power of 2 is an optimistic value, power of 3.1 is used instead.
	 * 
	 * @param distance
	 * @return the power at the receiver
	 */
	public static double computeReceivedPower(double distance) {
		// 4*Math.PI*R^2 => sphere area, thus (4*Math.PI*Math.pow(distance, GAMMA)) means that at a given point the received
		// power is given by transmitted power divided by the its sphere area
		return TX_POWER / (4*Math.PI*Math.pow(distance, GAMMA));
	}
}
