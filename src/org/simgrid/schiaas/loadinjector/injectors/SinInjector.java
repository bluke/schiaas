package org.simgrid.schiaas.loadinjector.injectors;

import java.util.Map;
import java.util.Vector;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.exceptions.VMSchedulingException;
import org.simgrid.schiaas.loadinjector.AbstractInjector;
import org.simgrid.schiaas.loadinjector.LoadedInstance;


public class SinInjector extends AbstractInjector {

	/** Date at which the injection start */
	protected double startDate;
	/** Duration of the injection */  
	protected double duration;
	/** Period at which the loads change */
	protected double period;
	/** Duration of one full sinusoid cycle for instances */
	protected double instancePeriod;
	/** Minimum number of instances */
	protected double instanceMin;
	/** Maximum number of instances */
	protected double instanceMax; 
	/** Duration of one full sinusoid cycle for CPU load */
	protected double loadPeriod;
	/** Minimum CPU load */
	protected double loadMin;
	/** Maximum CPU load */
	protected double loadMax;
	
	private String imageId, instanceTypeId;
	
	public SinInjector(String id, Cloud cloud, Map<String,String> config) {
		super(id, cloud, config);
				
		this.startDate = Double.parseDouble(config.get("start_date"));
		this.duration = Double.parseDouble(config.get("duration"));
		this.period = Double.parseDouble(config.get("period"));
		this.instancePeriod = Double.parseDouble(config.get("instance_period"));
		this.instanceMin = Double.parseDouble(config.get("instance_min"));
		this.instanceMax = Double.parseDouble(config.get("instance_max"));
		this.loadPeriod = Double.parseDouble(config.get("load_period"));
		this.loadMin = Double.parseDouble(config.get("load_min"));
		this.loadMax = Double.parseDouble(config.get("load_max"));
		
		this.imageId = config.get("image_id");
		this.instanceTypeId = config.get("instance_type_id");
	}

	private double sinusoid(double t, double period, double min, double max) {
		return (Math.sin(Math.PI * 2 * ((t/period) % 1.0)) + 1) * (max-min)/2 + min;  
	}
	
	@Override
	public void run() throws HostFailureException {
		
		Vector<LoadedInstance> loadedInstances = new Vector<>();
		
		if (Msg.getClock() > this.startDate) {
			Msg.warn("The injector "+this.id+" is late (as usual): starting at " 
					+Msg.getClock()+" instead of "+this.startDate);
		} 
		if (Msg.getClock() < this.startDate) {
			Process.getCurrentProcess().waitFor(startDate-Msg.getClock());
		}

		Msg.info("starting "+id);
		
		// Injections
		this.startDate = Msg.getClock();
		double t = 0;
		
		while (t < this.duration) {
			t = Msg.getClock() - this.startDate;
			
			// Adjusting the number of instances 
			int targetInstance = (int) sinusoid(t, instancePeriod, instanceMin, instanceMax);
			this.trace.addEvent("instances_count", ""+targetInstance);
			//Msg.info("intances count :"+targetInstance+" "+cloud.getCompute().describeAvailability(instanceTypeId)
			//		+" "+cloud.getCompute().describeInstances().size());
			int dInstance = targetInstance-loadedInstances.size();
			if (dInstance < 0) {
				for (int i=0; i<-dInstance; i++) {
					loadedInstances.remove(random.nextInt(loadedInstances.size()))
						.terminate();
				}
			} else {
				try {
					for (int i=0; i<dInstance; i++) {
						loadedInstances.add(new LoadedInstance(cloud.getCompute(), imageId, instanceTypeId) );
					}
				} catch (VMSchedulingException e) {
					Msg.info("The injector failed to start some instances because: "+e.getMessage());
				}
			}
			
			double targetLoad = sinusoid(t, loadPeriod, loadMin, loadMax);
			this.trace.addEvent("instances_load", ""+targetLoad);
			for (LoadedInstance loadedInstance : loadedInstances) {
				loadedInstance.setLoad(targetLoad);
			}
			
			Process.getCurrentProcess().waitFor((int)this.period);
		}
		
		// terminating
		for (LoadedInstance li : loadedInstances) {
			li.terminate();
		}
		this.trace.addEvent("instances_count", ""+0);
	}
}
