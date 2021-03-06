package org.simgrid.schiaas.engine.storage.rise;

import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Data;
import org.simgrid.schiaas.engine.storage.StorageEngine;

/**
 * Process handling Storage commands.
 * @author julien.gossa@unistra.fr
 */
public class RiseProcess extends Process {
	
	protected Rise rise;
	protected StorageEngine.REQUEST request;
	protected Data data;
	
	public RiseProcess(Rise rise, StorageEngine.REQUEST request, Data data) {
		super(	rise.controller, 
				"RISEProcess:"+rise.getStorage().getCloud()+":"
				+rise.getStorage().getId()+":"+request);
		
		this.rise = rise;
		this.request = request;
		this.data = data;
		
		this.start();

	} 
	
	/**
	 * MSG's main: receives and handle one storage request.
	 * @throws MsgException
	 */
	@Override
	public void main(String[] args) throws MsgException {
		
		// receive the request
		RiseTask reqTask = (RiseTask) RiseTask.receive(rise.requestMessageBox());
		
		switch (request) {
		case PUT:
		case DELETE:
			break;
		case GET: 
		case LIST:
			// Send the response
			RiseTask respTask = new RiseTask(request, data, true);
			respTask.send(rise.responseMessageBox(reqTask));
			break;
		}
	}
}
