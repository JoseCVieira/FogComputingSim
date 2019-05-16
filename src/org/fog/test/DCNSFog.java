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

public class DCNSFog extends FogTest {
	static int numOfAreas = 1;
	static int numOfCamerasPerArea = 4;
	private static double CAMERA_TRANSMISSION_TIME = 5;
	
	public DCNSFog() {
		System.out.println("Generating DCNS topology...");
		
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
		
		for(int i = 0; i < numOfAreas; i++){
			FogDevice dept = createFogDevice("d-"+i, 2800, 4000, 1000000, 10000, 107.339, 83.4333, 0.0, 0.05, 0.001, 0.0);
			
			fogDevices.add(dept);
			
			connectFogDevices(proxy, dept, 2.0, 2.0, 10000.0, 10000.0);
			
			
			for(int j = 0; j < numOfCamerasPerArea; j++){
				FogDevice mobile = createFogDevice("m-"+i+"-"+j, 1000, 1000, 1000000, 10000, 87.53, 82.44, 0.0, 0.05, 0.001, 0.0);
				
				fogDevices.add(mobile);
				
				connectFogDevices(dept, mobile, 2.0, 2.0, 10000.0, 10000.0);
			}
		}
	}
	
	private static void createClients() {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().startsWith("m"))
				createClient(fogDevice, "CAMERA:", new DeterministicDistribution(CAMERA_TRANSMISSION_TIME), 1.0, "PTZ_CONTROL:", 1.0);
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplication() {
		Application application = new Application("DCNS", -1);
		application.addAppModule("object_detector", 100, false, false);
		application.addAppModule("motion_detector", 100, true, false);
		application.addAppModule("object_tracker", 100, false, false);
		application.addAppModule("user_interface", 100, false, false);
		
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", AppEdge.SENSOR);
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", AppEdge.MODULE);
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", AppEdge.MODULE);
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", AppEdge.MODULE);
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", AppEdge.ACTUATOR);
		
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);add(loop2);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
}