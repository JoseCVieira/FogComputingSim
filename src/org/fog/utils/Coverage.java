package org.fog.utils;

import org.fog.entities.FogDevice;

public class Coverage {
	private double radius;
	
	public Coverage(double radius) {
		this.setRadius(radius);
	}
	
	public boolean covers(FogDevice f1, FogDevice f2, double percentage) {
		Location l1 = f1.getMovement().getLocation();
		Location l2 = f2.getMovement().getLocation();
		
		double c1 = Math.pow(l1.getX() - l2.getX(), 2);
		double c2 = Math.pow(l1.getY() - l2.getY(), 2);
		
		if(Math.sqrt(c1+c2) < radius * (1-percentage))
			return true;
		return false;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	@Override
	public String toString() {
		return "Coverage [radius=" + radius + "]";
	}
	
}
