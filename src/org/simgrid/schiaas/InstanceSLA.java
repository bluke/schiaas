package org.simgrid.schiaas;

public class InstanceSLA {
	
	/** flop/s */
	public double speed;
	
	/* B */
	public int RAM;
	
	public InstanceSLA(double speed, int RAM) {
		this.speed = speed;
		this.RAM = RAM;
	}
	
	public InstanceSLA() {
		this.speed = 0;
		this.RAM = 0;
	}

}
