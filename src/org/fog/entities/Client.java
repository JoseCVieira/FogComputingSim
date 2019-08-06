package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.utils.FogEvents;
import org.fog.utils.Movement;

/**
 * Class representing client nodes (although it's similar to a fog device it needs to handle some extra events).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Client extends FogDevice {
	/** The list containing all a associated actuators along with their latencies (latency is always zero) */
	private List<Pair<Integer, Double>> associatedActuatorIds;
	
	/**
	 * Creates a new client node.
	 * 
	 * @param name the name of the client node
	 * @param characteristics the characteristics of the client node (e.g., its resources and prices)
	 * @param vmAllocationPolicy the virtual machine allocation policy of the client node
	 * @param storageList the storage list of the client node
	 * @param schedulingInterval the scheduling interval of the client node
	 * @param movement the movement of the client node (i.e., position, velocity and direction)
	 * @throws Exception if the total number of PEs is zero
	 */
	public Client(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, Movement movement) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, movement);
		
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
	}
	
	/**
	 * Processes the events that can occur in a client.
	 */
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.ACTUATOR_JOINED:
			processActuatorJoined(ev);
			break;
		default:
			super.processOtherEvent(ev);
		}
	}
	
	/**
	 * Processes the event of actuator joined.
	 * 
	 * @param ev the event that just occurred
	 */
	protected void processActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
		double delay = (double)ev.getData();
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
	}
	
	/**
	 * Processes the event of tuple arrival.
	 * 
	 * @param ev the event that just occurred
	 */
	@Override
	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		if(this instanceof Client && tuple.getDirection() == Tuple.ACTUATOR) {
			((Client) this).sendTupleToActuator(tuple);
			return;
		}
		
		super.processTupleArrival(ev);
	}
	
	/**
	 * Sends a tuple to the respective actuator.
	 * 
	 * @param tuple the tuple to be sent
	 */
	protected void sendTupleToActuator(Tuple tuple){
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
			
			if(tuple.getDestModuleName().equals(actuatorType)){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		
		Map<String, String> communication = new HashMap<String, String>();
		communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
		sendTo(tuple, getTupleRoutingTable().get(communication));
	}
	
	/**
	 * Gets the list containing all a associated actuators along with their latencies (latency is always zero).
	 * 
	 * @return the list containing all a associated actuators
	 */
	public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
		return associatedActuatorIds;
	}
	
	/**
	 * Sets the list containing all a associated actuators along with their latencies (latency is always zero).
	 * 
	 * @param associatedActuatorIds the list containing all a associated actuators along with their latencies
	 */
	public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
		this.associatedActuatorIds = associatedActuatorIds;
	}

}
