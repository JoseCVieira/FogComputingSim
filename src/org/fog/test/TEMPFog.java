package org.fog.test;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.core.Config;
import org.fog.core.FogTest;
import org.fog.entities.FogDevice;
import org.fog.utils.distribution.NormalDistribution;

public class TEMPFog extends FogTest {
	static int numOfRouters = 1;
	static int numOfMobilesPerRouter = 2;
	private static double TEMP_TRANSMISSION_TIME_MEAN = 10;
	private static double TEMP_TRANSMISSION_TIME_DEV = 2;
	
	public TEMPFog() {
		System.out.println("Generating DCNS topology...");
		
		createExampleApplication();
		createFogDevices();
		createClients();
		createController();
		createApplications();
	}
	
	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 100000, 10240, 1000000, 1000, 16*103, 16*83.25, Config.COST_PER_SEC, 10, 0.05, 0.001, 0.0);
		
		fogDevices.add(cloud);
		
		for(int i = 0; i < numOfRouters; i++){
			FogDevice dept = createFogDevice("d-"+i, 1000, 1024, 1000000, 1000, 107.339, 83.4333, Config.COST_PER_SEC, 0.0, 0.05, 0.001, 0.0);
			
			fogDevices.add(dept);
			
			connectFogDevices(cloud, dept, 50.0, 50.0, 1000.0, 1000.0);
			
			
			for(int j = 0; j < numOfMobilesPerRouter; j++){
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1024, 1000000, 1000, 87.53, 82.44, Config.COST_PER_SEC, 0.0, 0.05, 0.001, 0.0);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 10.0, 10.0, 1000.0, 1000.0);
			}
		}
	}
	
	private static void createClients() {	
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().startsWith("m"))
				createClient(fogDevice, "TEMP:", new NormalDistribution(TEMP_TRANSMISSION_TIME_MEAN, TEMP_TRANSMISSION_TIME_DEV), 2.0, "MOTOR:", 2.0);
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplication() {		
		Application application = new Application("TEMP", -1);
		application.addAppModule("client", 100, false);
		application.addAppModule("classifier", 100, false);
		application.addAppModule("tuner", 100, false);
	
		application.addAppEdge("TEMP", "client", 1000, 100, "TEMP", AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 8000, 100, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000000, 100, "HISTORY", AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", AppEdge.MODULE);
		application.addAppEdge("client", "MOTOR", 1000, 100, "ACTUATOR", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "TEMP", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
}