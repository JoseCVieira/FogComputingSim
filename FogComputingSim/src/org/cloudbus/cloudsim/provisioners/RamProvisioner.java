package org.cloudbus.cloudsim.provisioners;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

public class RamProvisioner extends ResourceProvisioner{
	private double overbookingRatioRam = 1.0;
	
	private int ram;
	private int availableRam;
	private Map<String, Integer> ramTable;
	
	public RamProvisioner(int ram) {
		this(ram, 1.0);
	}
	
	public RamProvisioner(int ram, double overbookingRatioRam) {
		this.overbookingRatioRam = overbookingRatioRam;
		setRam(ram);
		setAvailableRam((int) getOverbookedRam(ram));
		setRamTable(new HashMap<String, Integer>());
	}
	
	@Override
	public boolean allocateResourcesForVm(Vm vm, Number value) {
		int maxRam = vm.getRam();
		if ((int) value >= maxRam) value = maxRam;

		deallocateResourcesForVm(vm);

		if (getAvailableRam() >= (int) value) {
			setAvailableRam(getAvailableRam() - (int) value);
			getRamTable().put(vm.getUid(), (int) value);
			vm.setCurrentAllocatedRam((int) getAllocatedResourcesForVm(vm));
			return true;
		}

		vm.setCurrentAllocatedRam((int) getAllocatedResourcesForVm(vm));
		return false;
	}
	
	@Override
	public Number getAllocatedResourcesForVm(Vm vm) {
		if (getRamTable().containsKey(vm.getUid()))
			return getRamTable().get(vm.getUid());
		return 0;
	}

	@Override
	public void deallocateResourcesForVm(Vm vm) {
		if (getRamTable().containsKey(vm.getUid())) {
			int amountFreed = getRamTable().remove(vm.getUid());
			setAvailableRam(getAvailableRam() + amountFreed);
			vm.setCurrentAllocatedRam(0);
		}
	}
	
	@Override
	public void deallocateResourcesForAllVms() {
		setAvailableRam((int) getOverbookedRam(ram));
		getRamTable().clear();
	}

	@Override
	public boolean isSuitableForVm(Vm vm, Number value) {
		int allocatedRam = (int) getAllocatedResourcesForVm(vm);
		boolean result = allocateResourcesForVm(vm, value);
		deallocateResourcesForVm(vm);
		
		if (allocatedRam > 0)
			allocateResourcesForVm(vm, allocatedRam);
		
		return result;
	}

	public int getRam() {
		return ram;
	}

	protected void setRam(int ram) {
		this.ram = ram;
	}

	public int getUsedRam() {
		return ram - availableRam;
	}

	public int getAvailableRam() {
		return availableRam;
	}

	protected void setAvailableRam(int availableRam) {
		this.availableRam = availableRam;
	}

	public Map<String, Integer> getRamTable() {
		return ramTable;
	}

	public void setRamTable(Map<String, Integer> ramTable) {
		this.ramTable = ramTable;
	}
	
	public double getOverbookedRam(int capacity) {
		return capacity * overbookingRatioRam;		
	}

}
