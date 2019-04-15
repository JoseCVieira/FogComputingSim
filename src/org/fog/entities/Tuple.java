package org.fog.entities;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

public class Tuple extends Cloudlet{
	private static final int NOT_ACTUATOR = 1;
	public static final int ACTUATOR = 2;
	
	private String appId;
	private String tupleType;
	private String destModuleName;
	private String srcModuleName;
	private int actualTupleId;
	private int direction;
	private int sourceModuleId;
	private int clientId;
	
	/**
	 * Map to keep track of which module instances has a tuple traversed.
	 * Map from moduleName to vmId of a module instance
	 */
	private Map<String, Integer> moduleCopyMap;
	
	public Tuple(String appId, int cloudletId, long cloudletLength, int pesNumber,
			long cloudletFileSize, long cloudletOutputSize, UtilizationModel utilizationModelCpu,
			UtilizationModel utilizationModelRam, UtilizationModel utilizationModelBw, int clientId) {
		
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
				utilizationModelCpu, utilizationModelRam, utilizationModelBw);
		
		setAppId(appId);
		setClientId(clientId);
		setDirection(NOT_ACTUATOR);
		setModuleCopyMap(new HashMap<String, Integer>());
	}

	public int getActualTupleId() {
		return actualTupleId;
	}

	public void setActualTupleId(int actualTupleId) {
		this.actualTupleId = actualTupleId;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getTupleType() {
		return tupleType;
	}

	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	public String getDestModuleName() {
		return destModuleName;
	}

	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}

	public String getSrcModuleName() {
		return srcModuleName;
	}

	public void setSrcModuleName(String srcModuleName) {
		this.srcModuleName = srcModuleName;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public Map<String, Integer> getModuleCopyMap() {
		return moduleCopyMap;
	}

	public void setModuleCopyMap(Map<String, Integer> moduleCopyMap) {
		this.moduleCopyMap = moduleCopyMap;
	}

	public int getSourceModuleId() {
		return sourceModuleId;
	}

	public void setSourceModuleId(int sourceModuleId) {
		this.sourceModuleId = sourceModuleId;
	}
	
	public int getClientId() {
		return clientId;
	}

	public void setClientId(int clientId) {
		this.clientId = clientId;
	}
	
	@Override
	public String toString() {
		String str = "";
		
		str = "\nappId: " + appId + "\n"+
		"tupleType: " + tupleType + "\n"+
		"TupleCpuLength: " + getCloudletLength() + "\n"+
		"destModuleName: " + destModuleName + "\n"+
		"srcModuleName: " + srcModuleName + "\n"+
		"actualTupleId: " + actualTupleId + "\n"+
		"direction: " + direction + "\n"+
		"sourceModuleId: " + sourceModuleId + "\n\n";
		return str;
	}
	
}
