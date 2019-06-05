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
import org.fog.core.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.Movement;

public class Client extends FogDevice {
	private List<Pair<Integer, Double>> associatedActuatorIds;

	public Client(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, Movement movement) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval, movement);
		
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
	}
	
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
	
	protected void processActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
		double delay = (double)ev.getData();
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
	}
	
	@Override
	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		if(this instanceof Client && tuple.getDirection() == Tuple.ACTUATOR) {
			((Client) this).sendTupleToActuator(tuple);
			return;
		}
		
		super.processTupleArrival(ev);
	}
	
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
		
		if(Config.PRINT_COMMUNICATION_DETAILS) printCommunication(tuple);
		
		Map<String, String> communication = new HashMap<String, String>();
		communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
		sendTo(tuple, getRoutingTable().get(communication));
	}
	
	public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
		return associatedActuatorIds;
	}

	public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
		this.associatedActuatorIds = associatedActuatorIds;
	}

}
