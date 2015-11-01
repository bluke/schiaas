package org.simgrid.schiaas.examples.loadinjector.injectors;

import java.util.Random;
import java.util.Vector;

import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Cloud;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.examples.loadinjector.AbstractInjector;
import org.simgrid.schiaas.examples.loadinjector.LoadedInstance;
import org.simgrid.schiaas.tracing.Trace;


public class SinInjector extends AbstractInjector {

	private double duration, period;
	private double instancePeriod, instanceMin, instanceMax; 
	private double loadPeriod, loadMin, loadMax;
	
	private String imageId, instanceTypeId;
	
	public SinInjector(Cloud cloud, String[] args) {
		super(cloud, args);

		this.duration = Double.parseDouble(args[1]);
		this.period = Double.parseDouble(args[2]);
		this.instancePeriod = Double.parseDouble(args[3]);
		this.instanceMin = Double.parseDouble(args[4]);
		this.instanceMax = Double.parseDouble(args[5]);
		this.loadPeriod = Double.parseDouble(args[6]);
		this.loadMin = Double.parseDouble(args[7]);
		this.loadMax = Double.parseDouble(args[8]);
		
		this.imageId = args[9];
		this.instanceTypeId = args[10];
	}

	private double sinusoid(double t, double period, double min, double max) {
		return (Math.sin(Math.PI * ((t/period) % 2.0)) + 1) * (max-min)/2 + min;  
	}
	
	@Override
	public void run() throws HostFailureException {
		
		Vector<LoadedInstance> loadedInstances = new Vector<LoadedInstance>(); 
		
		// Injections
		double startTime = Msg.getClock();
		double t = 0;
		
		while (t < this.duration) {
			t = Msg.getClock() - startTime;
			
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
