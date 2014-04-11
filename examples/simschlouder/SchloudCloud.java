package simschlouder;

import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Storage;

public class SchloudCloud {
	public String name;
	protected double bootTimeB0;
	protected double bootTimeB1;
	protected double standardPower;
	protected double BTU;
	protected double shutdownMargin;
	protected int maxInstancesPerUser;
	
	protected int bootCount;
	
	public Compute compute;
	public Storage storage;
	
	/**
	 * Enumerates the possible commands to control instances.
	 * @author julien
	 */
	public static enum COMMAND { RUN, SHUTDOWN, SUSPEND, RESUME, REBOOT, FINALIZE };

	

	/**
	 * Default constructor
	 * @param name the name of the cloud
	 * @param bootTimeB0 the boot time for TODO
	 * @param bootTimeB1 the boot time for TODO
	 * @param BTU the size of a BTU in seconds
	 * @param shutdownMargin the shutdown time
	 * @param maxInsances the maximum allowed Instances per User
	 * @throws Exception 
	 * @throws NumberFormatException 
	 */
	public SchloudCloud(String name, double bootTimeB0, double bootTimeB1, double BTU, double shutdownMargin,int maxInstances) throws NumberFormatException, Exception	{
		this.name=name;
		this.bootTimeB0=bootTimeB0;
		this.bootTimeB1=bootTimeB1;
		this.BTU=BTU;
		this.shutdownMargin=shutdownMargin;
		this.maxInstancesPerUser=maxInstances;

		this.bootCount = 0;
		this.compute = SchIaaS.getCloud(name).getCompute();
		this.storage = null;
		
		this.standardPower=Double.parseDouble(compute.getConfig("standard_power"));
	}
		
	
	/**
	 * Set the storage to use for task data;
	 */
	public void setStorage(String storage) {
		this.storage = SchIaaS.getCloud(name).getStorage(storage);
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
	
	public int describeAvailability(String instanceTypeId) {
		if(SchloudController.nodes.size()<maxInstancesPerUser){
			return compute.describeAvailability(instanceTypeId);
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Gets the shutdown time for one VM
	 * @return
	 */
	public double getShutdownMargin() {
		return this.shutdownMargin;
	}

}