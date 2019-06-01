package org.fog.utils;

public class Location {
	private double x;
	private double y;
	
	public Location(double x, double y){
		this.setX(x);
		this.setY(y);
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
