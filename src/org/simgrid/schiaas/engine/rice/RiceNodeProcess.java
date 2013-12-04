package org.simgrid.schiaas.engine.rice;

import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.engine.rice.RiceHost.IMGSTATUS;

/**
 * Useless for now.
 * @author julien
 *
 */
public class RiceNodeProcess extends Process {
		
	protected Rice rice;
	protected RiceHost riceHost;
	
	public RiceNodeProcess(Rice rice, RiceHost riceHost) {
		super(riceHost.host, rice.getCompute().getId()+" Rice Controller");
		this.rice = rice;
		this.riceHost = riceHost;
	} 
	
	public void main(String[] args) throws MsgException {
		
		RiceTask riceTask = (RiceTask) Task.receive(riceHost.messageBox());
		RiceInstance riceInstance = riceTask.riceInstance;
				
		switch (riceTask.command) {
		case START:
			
			// Image caching management
			if (riceHost.imagesCache.get(riceInstance.getImage()) == null) {
				switch (rice.imgCaching) {
				case ON:
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.TRANSFERING);
					rice.imgStorage.get("RICEIMG-"+riceInstance.getImage().getId());
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.AVAILABLE);
					break;
				case OFF:
					rice.imgStorage.get("RICEIMG-"+riceInstance.getImage().getId());
				case PRE:
					riceHost.imagesCache.put(riceInstance.getImage(), IMGSTATUS.AVAILABLE);
				}				
			}
			else while (riceHost.imagesCache.get(riceInstance.getImage()) == IMGSTATUS.TRANSFERING) {
				waitFor(rice.interBootDelay);
			}
			
			// Boot delay management
			/*
			while(Msg.getClock()<riceHost.lastBootDate+rice.interBootDelay) {
				Msg.info("waiting my turn");
				waitFor(riceHost.lastBootDate+rice.interBootDelay-Msg.getClock());
			}
			*/
			riceHost.bootMutex.acquire();
			
			// Boot
			riceHost.lastBootDate=Msg.getClock();
			Msg.verb(riceInstance.getName()+" is booting");
			riceInstance.start();
			
			waitFor(rice.interBootDelay);
			riceHost.bootMutex.release();
			break;
			
		case SHUTDOWN:
			riceInstance.shutdown();
			riceInstance.destroy();
			break;
		case SUSPEND:
			riceInstance.suspend();
			break;
		case RESUME:
			riceInstance.resume();
			break;
		case REBOOT:
			// TODO: Adrien will rename these VM methods to match their
			riceInstance.shutdown();		
			riceInstance.start();
			break;
		}			
	}

}
