package simschlouder;

import org.simgrid.simiaas.api.Compute;

public class Cloud {
	public String name;
	protected double bootTimeB0;
	protected double bootTimeB1;
	protected double standardPower;
	protected double BTU;
	protected double shutdownMargin;
	
	protected int bootCount;

	/**
	 * Default constructor
	 * @param name the name of the cloud
	 * @param bootTimeB0 the boot time for TODO
	 * @param bootTimeB1 the boot time for TODO
	 * @param BTU the size of a BTU in seconds
	 * @param shutdownMargin the shutdown time
	 */
	public Cloud(String name, double bootTimeB0, double bootTimeB1, double BTU, double shutdownMargin)	{
		this.name=name;
		this.bootTimeB0=bootTimeB0;
		this.bootTimeB1=bootTimeB1;
		this.BTU=BTU;
		this.shutdownMargin=shutdownMargin;
		this.standardPower=Compute.getStandardPower(name);
		
		this.bootCount=0;
	}
		
	/**
	 * Resets the number of booted VMs in this cloud
	 */
	public void resetBootCount() {
		bootCount=0;
	}
	
	/**
	 * Adds one VM to the list of booted machines
	 */
	public void incrementBootCount() {
		this.bootCount++;
	}
	
	/**
	 * Returns the predicted boot time.
	 * Works only with "PRE" caching strategy
	 * @return the predicted boot time in seconds
	 */
	public double getBootTimePrediction() {
		return (bootTimeB0+bootTimeB1*(bootCount+1));
	}		
	
	/**
	 * Gets the shutdown time for one VM
	 * @return
	 */
	public double getShutdownMargin() {
		return this.shutdownMargin;
	}

}