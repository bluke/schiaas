package org.simgrid.schiaas.engine.compute.rice;

import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.engine.compute.rice.RiceHost.IMGSTATUS;

/**
 * Process handling Compute commands from the node side.
 * @author julien.gossa@unistra.fr
 */
public class RiceNodeProcess extends Process {
		
	protected Rice rice;
	protected RiceHost riceHost;
	
	/**
	 * Constructor.
	 * @param rice The RICE concerned by the command.
	 * @param riceHost The host concerned by the command.
	 */
	public RiceNodeProcess(Rice rice, RiceHost riceHost) {
		super(riceHost.host, rice.getCompute().getId()+"-Rice-Node-"+riceHost.getHost().getName());
		this.rice = rice;
		this.riceHost = riceHost;
	} 
	
	/**
	 * MSG's main: receives the task corresponding to the command from the controller 
	 * and execute this command.
	 * @throws MsgException
	 */
	@Override
	public void main(String[] args) throws MsgException {
		
		RiceTask riceTask = (RiceTask) Task.receive(riceHost.messageBox());
		RiceInstance riceInstance = riceTask.riceInstance;
				
		switch (riceTask.command) {
		case START:
			
			// Image caching management
			if (riceHost.imagesCache.get(riceInstance.getImage()) == null) {
				switch (rice.imgCaching) {
				case ON:
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.TRANSFERRING);
					rice.imgStorage.get("RICEIMG-"+riceInstance.getImage().getId());
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.AVAILABLE);
					break;
				case OFF:
					rice.imgStorage.get("RICEIMG-"+riceInstance.getImage().getId());
				case PRE:
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.AVAILABLE);
				}				
			}
			else while (riceHost.imagesCache.get(riceInstance.getImage()) == IMGSTATUS.TRANSFERRING) {
				waitFor(1);
			}
			
			// Boot delay management
			/*
			while(Msg.getClock()<riceHost.lastBootDate+rice.interBootDelay) {
				Msg.info("waiting my turn");
				waitFor(riceHost.lastBootDate+rice.interBootDelay-Msg.getClock());
			}
			*/
			//Msg.info("bootmutex "+riceInstance.getId()+" : "+riceHost.getHost().getName());
			riceHost.bootMutex.acquire();
			
			// Boot
			riceHost.lastBootDate=Msg.getClock();
			Msg.verb(riceInstance.getId()+" is booting");
			riceInstance.start();
			
			waitFor(rice.interBootDelay);
			riceHost.bootMutex.release();
			break;
			
		case SHUTDOWN:
			// TODO: May be an issue when the slot is reused immediately and there is penalty for start/shutdown
			riceHost.removeInstance(riceInstance);
			while(riceInstance.isPending() || riceInstance.vm().isMigrating() == 1) { 
				waitFor(1);
			}
			if (riceInstance.vm().isRunning() == 1) {
				riceInstance.vm().shutdown();
			}
			riceInstance.vm().finalize();
			break;
		case SUSPEND:
			riceInstance.vm().suspend();
			break;
		case RESUME:
			riceInstance.vm().resume();
			break;
		case REBOOT:
			riceInstance.vm().shutdown();		
			riceInstance.vm().start();
			break;
		}			
	}

}
