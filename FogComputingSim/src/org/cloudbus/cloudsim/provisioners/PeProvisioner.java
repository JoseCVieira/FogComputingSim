package org.cloudbus.cloudsim.provisioners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

public class PeProvisioner {
	private double overbookingRatioMips = 4.0;
	
	private double mips;
	private double availableMips;
	private Map<String, List<Double>> peTable;

	public PeProvisioner(double mips) {
		this(mips, 1.0);
	}
	
	public PeProvisioner(double mips, double overbookingRatioMips) {
		setMips(mips);
		setAvailableMips(getOverbookedMips(mips));
		setPeTable(new HashMap<String, List<Double>>());
	}
	
	public boolean allocateMipsForVm(Vm vm, double mips) {
		return allocateMipsForVm(vm.getUid(), mips);
	}

	public boolean allocateMipsForVm(String vmUid, double mips) {
		if (getAvailableMips() < mips) {
			return false;
		}

		List<Double> allocatedMips;

		if (getPeTable().containsKey(vmUid)) {
			allocatedMips = getPeTable().get(vmUid);
		} else {
			allocatedMips = new ArrayList<Double>();
		}

		allocatedMips.add(mips);

		setAvailableMips(getAvailableMips() - mips);
		getPeTable().put(vmUid, allocatedMips);

		return true;
	}
	
	public boolean allocateMipsForVm(Vm vm, List<Double> mips) {
		int totalMipsToAllocate = 0;
		for (double _mips : mips) {
			totalMipsToAllocate += _mips;
		}

		if (getAvailableMips() + getTotalAllocatedMipsForVm(vm) < totalMipsToAllocate) {
			return false;
		}

		setAvailableMips(getAvailableMips() + getTotalAllocatedMipsForVm(vm) - totalMipsToAllocate);

		getPeTable().put(vm.getUid(), mips);

		return true;
	}
	
	public void deallocateMipsForAllVms() {
		setAvailableMips(getOverbookedMips(getMips()));
		getPeTable().clear();
	}

	public double getAllocatedMipsForVmByVirtualPeId(Vm vm, int peId) {
		if (getPeTable().containsKey(vm.getUid())) {
			try {
				return getPeTable().get(vm.getUid()).get(peId);
			} catch (Exception e) {
			}
		}
		return 0;
	}
	
	public List<Double> getAllocatedMipsForVm(Vm vm) {
		if (getPeTable().containsKey(vm.getUid())) {
			return getPeTable().get(vm.getUid());
		}
		return null;
	}
	
	public double getTotalAllocatedMipsForVm(Vm vm) {
		if (getPeTable().containsKey(vm.getUid())) {
			double totalAllocatedMips = 0.0;
			for (double mips : getPeTable().get(vm.getUid())) {
				totalAllocatedMips += mips;
			}
			return totalAllocatedMips;
		}
		return 0;
	}
	
	public void deallocateMipsForVm(Vm vm) {
		if (getPeTable().containsKey(vm.getUid())) {
			for (double mips : getPeTable().get(vm.getUid())) {
				setAvailableMips(getAvailableMips() + mips);
			}
			getPeTable().remove(vm.getUid());
		}
	}
	
	public double getMips() {
		return mips;
	}
	
	public void setMips(double mips) {
		this.mips = mips;
	}
	
	public double getAvailableMips() {
		return availableMips;
	}
	
	protected void setAvailableMips(double availableMips) {
		this.availableMips = availableMips;
	}
	
	public double getTotalAllocatedMips() {
		double totalAllocatedMips = getMips() - getAvailableMips();
		
		if (totalAllocatedMips > 0)
			return totalAllocatedMips;
		
		return 0;
	}
	
	public double getUtilization() {
		return getTotalAllocatedMips() / getMips();
	}
	
	protected Map<String, List<Double>> getPeTable() {
		return peTable;
	}
	
	protected void setPeTable(Map<String, List<Double>> peTable) {
		this.peTable = peTable;
	}

	public double getOverbookedMips(double availableMips) {
		return availableMips * overbookingRatioMips;		
	}

}
