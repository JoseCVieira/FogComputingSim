package org.fog.placement.algorithms.placement;

import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;

public abstract class Algorithm {
	private double fMipsPrice[];
	private double fRamPrice[];
	private double fMemPrice[];
	private double fBwPrice[];
	
	private String fName[];
	private double fMips[];
	private double fRam[];
	private double fMem[];
	private double fBw[];
	private PowerModel fPwModel[];
	
	private String mName[];
	private double mMips[];
	private double mRam[];
	private double mMem[];
	private double mBw[];
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications) {
		fName = new String[fogDevices.size()];
		fMips = new double[fogDevices.size()];
		fRam = new double[fogDevices.size()];
		fMem = new double[fogDevices.size()];
		fBw = new double[fogDevices.size()];
		fPwModel = new PowerModel[fogDevices.size()];
		
		fMipsPrice = new double[fogDevices.size()];
		fRamPrice = new double[fogDevices.size()];
		fMemPrice = new double[fogDevices.size()];
		fBwPrice = new double[fogDevices.size()];
		
		int i = 0;
		for(FogDevice fogDevice : fogDevices) {
			getfName()[i] = fogDevice.getName();
			
			for(Pe pe : fogDevice.getHost().getPeList())
				getfMips()[i] += pe.getMips();
			
			getfRam()[i] = fogDevice.getHost().getRam();
			getfMem()[i] = fogDevice.getHost().getStorage();
			getfBw()[i] = fogDevice.getHost().getBw();
			getfPwModel()[i] = fogDevice.getHost().getPowerModel();
			
			FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) fogDevice.getCharacteristics();
			getfMipsPrice()[i] = characteristics.getCostPerMips();
			getfRamPrice()[i] = characteristics.getCostPerMem();
			getfMemPrice()[i] = characteristics.getCostPerStorage();
			getfBwPrice()[i++] = characteristics.getCostPerBw();
		}
		
		for(i = 0; i < fogDevices.size(); i++) {
			FogDevice fDevice = null;
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(getfName()[i]))
					fDevice = fogDevice;
			
			System.out.println("Id: " + fDevice.getId() + " fName: " + getfName()[i]);
			System.out.println("fMips: " + getfMips()[i]);
			System.out.println("fRam: " + getfRam()[i]);
			System.out.println("fMem: " + getfMem()[i]);
			System.out.println("fBw: " + getfBw()[i]);
			System.out.println("fMipsPrice: " + getfMipsPrice()[i]);
			System.out.println("fRamPrice: " + getfRamPrice()[i]);
			System.out.println("fMemPrice: " + getfMemPrice()[i]);
			System.out.println("fBwPrice: " + getfBwPrice()[i]);
			System.out.println("Neighbors: " +  fDevice.getNeighborsIds());
			System.out.println("LatencymMap: " + fDevice.getLatencyMap() + "\n");
		}
		
		int size = 0;
		for(Application application : applications)
			size += application.getModules().size();
		
		mName = new String[size];
		mMips = new double[size];
		mRam = new double[size];
		mMem = new double[size];
		mBw = new double[size];
		
		i = 0;
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				getmName()[i] = module.getName();
				getmMips()[i] = module.getMips();
				getmRam()[i] = module.getRam();
				getmMem()[i] = module.getSize();
				getmBw()[i++] = module.getBw();
			}
		}
		
		for(i = 0; i < size; i++) {
			System.out.println("mName: " + getmName()[i]);
			System.out.println("mMips: " + getmMips()[i]);
			System.out.println("mRam: " + getmRam()[i]);
			System.out.println("mMem: " + getmMem()[i]);
			System.out.println("mBw: " + getmBw()[i] + "\n");
		}
	}
	
	public abstract Map<String, List<String>> execute();

	public double[] getfMipsPrice() {
		return fMipsPrice;
	}

	public double[] getfRamPrice() {
		return fRamPrice;
	}

	public double[] getfMemPrice() {
		return fMemPrice;
	}

	public double[] getfBwPrice() {
		return fBwPrice;
	}

	public String[] getfName() {
		return fName;
	}

	public double[] getfMips() {
		return fMips;
	}

	public double[] getfRam() {
		return fRam;
	}

	public double[] getfMem() {
		return fMem;
	}

	public double[] getfBw() {
		return fBw;
	}

	public String[] getmName() {
		return mName;
	}

	public double[] getmMips() {
		return mMips;
	}

	public double[] getmRam() {
		return mRam;
	}

	public double[] getmMem() {
		return mMem;
	}

	public double[] getmBw() {
		return mBw;
	}

	public PowerModel[] getfPwModel() {
		return fPwModel;
	}
	
}
