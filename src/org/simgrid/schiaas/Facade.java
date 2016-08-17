package org.simgrid.schiaas;

/**
 * Facade for the whole SCHIaaS/SimGrid simulator
 * @author julien.gossa@unistra.fr
 *
 */
public class Facade {
	
	/** internal clock of the simulator */
	private double msgClock;
	
	/**
	 * Basic constructor
	 */
	public Facade () {
		msgClock = 0;
	}
	
	/**
	 * Will be changed to Msg.getClock() once plugged to the simulator 
	 * @return the clock of the simgrid simulation
	 */
	private double getClock() {
		return msgClock;
	}
	
	/**
	 * Makes the current process sleep until time seconds have elapsed.
	 * Will be changed to Msg.Process.waitFor(seconds) once plugged to the simulator
	 * @param seconds The time the current process must sleep. 
	 */
	private void waitFor(double seconds) {
		assert seconds >= 0;
		msgClock += seconds;
	}
	
}
