package simschlouder;

import java.util.LinkedList;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.Storage;

/**
 * Represents a cloud in the SimSchlouder system
 * @author julien.gossa@unistra.fr
 *
 */
public class SchloudCloud {
	/** Name of the cloud */
	public String name;
	
	/** Linear regression of boot time */
	protected double bootTimeB0;
	protected double bootTimeB1;
	
	/** Like EC2CU */
	protected double standardPower;
	
	/** Duration of the Billing Time Unit in s. e.g. 3600s for EC2 */
	protected double BTU;
	
	/** Margin for shutdown command: the instances are shuted down if idle at BTU-shutdownMargin */
	protected double shutdownMargin;
	
	/** Maximum instances allowed per user. NB: SimSchlouder is single-uuser for now. */
	protected int maxInstancesPerUser;
	
	/** Amount of currently booting instances */
	protected int bootCount;
	
	/** List of bootTimes, as exactly observed in real experiments */
	protected LinkedList<Integer> bootTimes;
	
	/** List of bootTime predictions, as exactly observed in real experiments */
	protected LinkedList<Integer> bootTimePredictions;
	
	/** The compute to use. */
	public Compute compute;
	
	/** The storage to use */
	public Storage storage;
	
	/**
	 * Enumerates the possible commands to control instances.
	 * @author julien.gossa@unistra.fr
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
	public SchloudCloud(String name, double bootTimeB0, double bootTimeB1, double BTU, double shutdownMargin, double standardPower, int maxInstances) throws NumberFormatException, Exception	{
		this.name=name;
		this.bootTimeB0=bootTimeB0;
		this.bootTimeB1=bootTimeB1;
		this.BTU=BTU;
		this.shutdownMargin=shutdownMargin;
		this.maxInstancesPerUser=maxInstances;

		this.bootCount = 0;
		this.compute = SchIaaS.getCloud(name).getCompute();
		this.storage = null;
		
		this.standardPower= standardPower;
		
		bootTimes = new LinkedList<Integer>();
		bootTimePredictions = new LinkedList<Integer>();
	}
		
	
	/**
	 * Set the storage to use for task data;
	 * @param storage The storage to use.
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
	 * Works only with "PRE" caching strategy.
	 * @return the predicted boot time in seconds, using predictions reported in experiments when available.
	 */
	public double getBootTimePrediction() {
		if (!bootTimePredictions.isEmpty())
			return bootTimePredictions.peekFirst();
		return (bootTimeB0+bootTimeB1*(bootCount+1));
	}
	
	/**
	 * Give the current availability of new instances on this.
	 * 
	 * @param instanceTypeId
	 *            The id of a type of instance
	 * @return The number of available instances of the given type
	 */
	public int describeAvailability(String instanceTypeId) {
		if(this.maxInstancesPerUser!=0){
			return Math.max(
					0, 
					Math.min(
							maxInstancesPerUser - SchloudController.nodes.size(), 
							compute.describeAvailability(instanceTypeId)));
		}
		return compute.describeAvailability(instanceTypeId);
	}

	/**
	 * Gets the BTU on the cloud
	 * @return
	 */
	public double getBtuTime() {
		return this.BTU;
	}
	
	/**
	 * Gets the shutdown time for one VM
	 * @return
	 */
	public double getShutdownMargin() {
		return this.shutdownMargin;
	}

}