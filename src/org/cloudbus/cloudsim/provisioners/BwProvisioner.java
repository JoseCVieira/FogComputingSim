package org.cloudbus.cloudsim.provisioners;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

public class BwProvisioner extends ResourceProvisioner{
	private double overbookingRatioBw = 1.0;
	
	private long bw;
	private long availableBw;
	private Map<String, Long> bwTable;
	
	public BwProvisioner(long bw) {
		this(bw, 1.0);
	}
	
	public BwProvisioner(long bw, double overbookingRatioBw) {
		this.overbookingRatioBw = overbookingRatioBw;
		setBw(bw);
		setAvailableBw((long) getOverbookedBw(bw));
		setBwTable(new HashMap<String, Long>());
	}
	
	@Override
	public boolean allocateResourcesForVm(Vm vm, Number value) {
		long maxBw = vm.getBw();
		if ((long) value >= maxBw) value = maxBw;
		
		deallocateResourcesForVm(vm);

		if (getAvailableBw() >= (long) value) {
			setAvailableBw(getAvailableBw() - (long) value);
			getBwTable().put(vm.getUid(), (long) value);
			vm.setCurrentAllocatedBw((long) getAllocatedResourcesForVm(vm));
			return true;
		}

		vm.setCurrentAllocatedBw((long) getAllocatedResourcesForVm(vm));
		return false;
	}
	
	@Override
	public Number getAllocatedResourcesForVm(Vm vm) {
		if (getBwTable().containsKey(vm.getUid()))
			return getBwTable().get(vm.getUid());
		return 0;
	}
	
	@Override
	public void deallocateResourcesForVm(Vm vm) {
		if (getBwTable().containsKey(vm.getUid())) {
			long amountFreed = getBwTable().remove(vm.getUid());
			setAvailableBw(getAvailableBw() + amountFreed);
			vm.setCurrentAllocatedBw(0);
		}
	}
	
	@Override
	public void deallocateResourcesForAllVms() {
		setAvailableBw((long) getOverbookedBw(bw));
		getBwTable().clear();
	}
	
	@Override
	public boolean isSuitableForVm(Vm vm, Number value) {
		long allocatedBw = (long) getAllocatedResourcesForVm(vm);
		boolean result = allocateResourcesForVm(vm, value);
		deallocateResourcesForVm(vm);
		
		if (allocatedBw > 0)
			allocateResourcesForVm(vm, allocatedBw);
		
		return result;
	}
	
	public long getBw() {
		return bw;
	}

	protected void setBw(long bw) {
		this.bw = bw;
	}

	public long getAvailableBw() {
		return availableBw;
	}

	protected void setAvailableBw(long availableBw) {
		this.availableBw = availableBw;
	}
	
	public Map<String, Long> getBwTable() {
		return bwTable;
	}

	public void setBwTable(Map<String, Long> bwTable) {
		this.bwTable = bwTable;
	}

	public long getUsedBw() {
		return bw - availableBw;
	}
	
	public double getOverbookedBw(long capacity) {
		return capacity * overbookingRatioBw;		
	}
	
}
