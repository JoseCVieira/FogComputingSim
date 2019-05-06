package org.fog.test;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.FogTest;
import org.fog.entities.FogDevice;
import org.fog.utils.distribution.DeterministicDistribution;

public class VRGameFog extends FogTest {	
	private static int numOfDepts = 4;
	private static int numOfMobilesPerDept = 6;
	private static double EEG_TRANSMISSION_TIME = 5.1;
	
	public VRGameFog() {
		System.out.println("Generating VRGame topology...");
		
		createExampleApplication();
		createFogDevices();
		createClients();
		createController();
		createApplications();
	}

	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 1000000, 10000, 16*103, 16*83.25, 0.01, 0.05, 0.001, 0.0);
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		connectFogDevices(cloud, proxy, 100.0, 100.0, 10000.0, 10000.0);
		
		for(int i = 0; i < numOfDepts; i++){
			FogDevice dept = createFogDevice("d-"+i, 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
			
			fogDevices.add(dept);
			
			connectFogDevices(proxy, dept, 4.0, 4.0, 10000.0, 10000.0);
			
			
			for(int j = 0; j < numOfMobilesPerDept; j++){
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1000, 1000000, 10000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 2.0, 2.0, 10000.0, 10000.0);
			}
		}
	}
	
	private static void createClients() {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().startsWith("m"))
				createClient(fogDevice, "EEG:", new DeterministicDistribution(EEG_TRANSMISSION_TIME), 6.0, "DISPLAY:", 1.0);
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplication() {		
		Application application = new Application("VRGame", -1);
		application.addAppModule("client", 100, true);
		application.addAppModule("calculator", 100, false);
		application.addAppModule("connector", 100, false);
		
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", AppEdge.SENSOR);
		application.addAppEdge("client", "calculator", 3500, 500, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("calculator", "client", 14, 500, "CONCENTRATION", AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));
		
		final AppLoop loop = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
	
}
