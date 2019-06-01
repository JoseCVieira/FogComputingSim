package org.fog.utils;

public class Coverage {
	private double radius;
	
	public Coverage(double radius) {
		this.setRadius(radius);
	}
	
	public boolean covers(Location l1, Location l2) {
		double c1 = Math.pow(l1.getX() - l2.getX(), 2);
		double c2 = Math.pow(l1.getY() - l2.getY(), 2);
		
		if(Math.sqrt(c1+c2) < radius)
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
