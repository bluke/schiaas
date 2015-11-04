package org.simgrid.schiaas.loadinjector.injectors;

import java.util.Map;
import java.util.Vector;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.loadinjector.AbstractInjector;
import org.simgrid.schiaas.loadinjector.LoadedInstance;


public class SinInjector extends AbstractInjector {

	private double startDate;
	private double duration, period;
	private double instancePeriod, instanceMin, instanceMax; 
	private double loadPeriod, loadMin, loadMax;
	
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
		return (Math.sin(Math.PI * ((t/period) % 2.0)) + 1) * (max-min)/2 + min;  
	}
	
	@Override
	public void run() throws HostFailureException {
		
		Vector<LoadedInstance> loadedInstances = new Vector<LoadedInstance>();
		
		if (Msg.getClock() > this.startDate) {
			Msg.warn("The injector "+this.id+" is late (as usual): starting at " 
					+Msg.getClock()+" instead of "+this.startDate);
		} 
		if (Msg.getClock() < this.startDate) {
			Process.getCurrentProcess().waitFor(startDate-Msg.getClock());
		}

		// Injections
		this.startDate = Msg.getClock();
		double t = 0;
		
		while (t < this.duration) {
			t = Msg.getClock() - this.startDate;
			
			// Adjusting the number of instances 
			int targetInstance = (int) sinusoid(t, instancePeriod, instanceMin, instanceMax);
			this.trace.addEvent("instances_count", ""+targetInstance);
			int dInstance = targetInstance-loadedInstances.size();
			if (dInstance < 0) {
				for (int i=0; i<-dInstance; i++) {
					loadedInstances.remove(random.nextInt(loadedInstances.size()))
						.terminate();
				}
			} else {
				for (int i=0; i<dInstance; i++) {
					loadedInstances.add(new LoadedInstance(cloud.getCompute(), imageId, instanceTypeId) );
				}
			}
			
			double targetLoad = sinusoid(t, loadPeriod, loadMin, loadMax);
			this.trace.addEvent("instances_load", ""+targetLoad);
			for (LoadedInstance loadedInstance : loadedInstances) {
				loadedInstance.setLoad(targetLoad);
			}
			
			Process.getCurrentProcess().waitFor((int)this.period);
		}
	}
}
