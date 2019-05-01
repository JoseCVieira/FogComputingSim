package org.fog.application;

import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.power.PowerVm;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.utils.FogUtils;

/**
 * Class representing an application module, the processing elements of the application model of iFogSim.
 * @author Harshit Gupta
 *
 */
public class AppModule extends PowerVm{

	private String name;
	private String appId;
	private boolean clientModule;
	private Map<Pair<String, String>, SelectivityModel> selectivityMap;
	
	public AppModule(int id, String name, String appId, int userId, double mips, int ram, long bw, long size, String vmm,
			CloudletScheduler cloudletScheduler, Map<Pair<String, String>, SelectivityModel> selectivityMap, boolean clientModule) {
		super(id, userId, mips, 1, ram, bw, size, 1, vmm, cloudletScheduler, 300);
		
		setName(name);
		setId(id);
		setAppId(appId);
		setUserId(userId);
		setUid(getUid(userId, id));
		setMips(mips);
		setNumberOfPes(1);
		setRam(ram);
		setBw(bw);
		setSize(size);
		setVmm(vmm);
		setCloudletScheduler(cloudletScheduler);
		setInMigration(false);
		setBeingInstantiated(true);
		setCurrentAllocatedBw(0);
		setCurrentAllocatedMips(null);
		setCurrentAllocatedRam(0);
		setCurrentAllocatedSize(0);
		setSelectivityMap(selectivityMap);
		setClientModule(clientModule);
	}
	
	public AppModule(AppModule operator) {
		super(FogUtils.generateEntityId(), operator.getUserId(), operator.getMips(), 1, operator.getRam(), operator.getBw(),
				operator.getSize(), 1, operator.getVmm(), new CloudletSchedulerTimeShared(), operator.getSchedulingInterval());
		
		setName(operator.getName());
		setAppId(operator.getAppId());
		setInMigration(false);
		setBeingInstantiated(true);
		setCurrentAllocatedBw(0);
		setCurrentAllocatedMips(null);
		setCurrentAllocatedRam(0);
		setCurrentAllocatedSize(0);
		setSelectivityMap(operator.getSelectivityMap());
		setClientModule(operator.isClientModule());
	}
	
	public void setValues(String name, double mips, int ram, long size, long bw, boolean clientModule) { //Added
		setName(name);
		setMips(mips);
		setRam(ram);
		setBw(bw);
		setSize(size);
		setClientModule(clientModule);
	}
	
	public void setValues(String name, int ram, long size, boolean clientModule) { //Added
		setName(name);
		setRam(ram);
		setSize(size);
		setClientModule(clientModule);
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Map<Pair<String, String>, SelectivityModel> getSelectivityMap() {
		return selectivityMap;
	}
	
	public void setSelectivityMap(Map<Pair<String, String>, SelectivityModel> selectivityMap) {
		this.selectivityMap = selectivityMap;
	}
	
	public String getAppId() {
		return appId;
	}
	
	public void setAppId(String appId) {
		this.appId = appId;
	}

	public boolean isClientModule() {
		return clientModule;
	}

	public void setClientModule(boolean clientModule) {
		this.clientModule = clientModule;
	}
	
	@Override
	public String toString() {
		return "AppModule [name=" + name + ", appId=" + appId +"]";
	}
	
}