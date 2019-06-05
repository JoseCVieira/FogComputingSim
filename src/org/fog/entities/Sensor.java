package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.Distribution;

public class Sensor extends SimEntity{
	
	private static final long OUTPUT_SIZE = 3;
	
	private String appId;
	private String tupleType;
	private String sensorName;
	private String destModuleName;
	private int userId;
	private int gatewayDeviceId;
	private double latency;
	private Application app;
	private Distribution transmitDistribution;
	
	/**
	 * This constructor is called from the code that generates PhysicalTopology from JSON
	 * @param name
	 * @param tupleType
	 * @param string 
	 * @param userId
	 * @param appId
	 * @param transmitDistribution
	 */
	public Sensor(String name, String tupleType, int userId, String appId,
			Distribution transmitDistribution, int gatewayDeviceId, double latency) {
		
		super(name);
		this.setAppId(appId);
		this.setTransmitDistribution(transmitDistribution);
		setTupleType(tupleType);
		setSensorName(tupleType);
		setUserId(userId);
		setGatewayDeviceId(gatewayDeviceId);
		setLatency(latency);
	}
	
	public void transmit(){
		AppEdge _edge = null;
		for(AppEdge edge : getApp().getEdges())
			if(edge.getSource().equals(getTupleType()))
				_edge = edge;

		long cpuLength = (long) _edge.getTupleCpuLength();
		long nwLength = (long) _edge.getTupleNwLength();
		
		Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), cpuLength, 1,
				nwLength, OUTPUT_SIZE, new UtilizationModelFull(), new UtilizationModelFull(),
				new UtilizationModelFull());
		
		tuple.setUserId(getUserId());
		tuple.setTupleType(getTupleType());
		tuple.setDestModuleName(_edge.getDestination());
		tuple.setSrcModuleName(getSensorName());
		tuple.setActualTupleId(updateTimings(getSensorName(), tuple.getDestModuleName()));
		
		Logger.debug(getName(), "Sending tuple with tupleId = " + tuple.getCloudletId());
		send(gatewayDeviceId, getLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	private int updateTimings(String src, String dest){
		Application application = getApp();
		for(AppLoop loop : application.getLoops()){
			if(loop.hasEdge(src, dest)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				return tupleId;
			}
		}
		return -1;
	}
	
	@Override
	public void startEntity() {
		send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.EMIT_TUPLE:
			transmit();
			send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
			break;
		}
			
	}

	@Override
	public void shutdownEntity() {
	}

	public int getGatewayDeviceId() {
		return gatewayDeviceId;
	}

	public void setGatewayDeviceId(int gatewayDeviceId) {
		this.gatewayDeviceId = gatewayDeviceId;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getTupleType() {
		return tupleType;
	}

	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public String getSensorName() {
		return sensorName;
	}

	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getDestModuleName() {
		return destModuleName;
	}

	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}

	public Distribution getTransmitDistribution() {
		return transmitDistribution;
	}

	public void setTransmitDistribution(Distribution transmitDistribution) {
		this.transmitDistribution = transmitDistribution;
	}

	public Application getApp() {
		return app;
	}

	public void setApp(Application app) {
		this.app = app;
	}

	public Double getLatency() {
		return latency;
	}

	public void setLatency(Double latency) {
		this.latency = latency;
	}

}
