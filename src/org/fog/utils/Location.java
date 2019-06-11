package org.fog.utils;

import org.fog.entities.FogDevice;

public class Location {
	private double x;
	private double y;
	
	public Location(double x, double y){
		this.setX(x);
		this.setY(y);
	}
	
	public static double computeDistance(FogDevice f1, FogDevice f2) {
		Location l1 = f1.getMovement().getLocation();
		Location l2 = f2.getMovement().getLocation();
		
		double first = Math.pow(l1.getX() - l2.getX(), 2);
		double second = Math.pow(l1.getY() - l2.getY(), 2);
		return Math.sqrt(first + second);
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return "Location [x=" + x + ", y=" + y + "]";
	}
	
}
