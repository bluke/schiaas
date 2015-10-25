package org.simgrid.schiaas.stepper;

import java.util.Collection;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;

public class DemoStepper extends Stepper {

	public DemoStepper(Host host, int port, String cloudId) {
		super(host, port, cloudId);
	}

	@Override
	protected String execute(String received) throws HostFailureException {
		String[] command = received.split(" ");
		String response = "Invalid Command";
		if(command[0].matches("VMimage")){
			Msg.info(command[0]+" "+command[1]);
			Image img = this.compute.describeImage(command[1]);
			response = img.getId();

		} else if(command[0].matches("VMimageID")){
			Image img = this.compute.describeImage(command[1]);
			response = img.getId();

		} else if(command[0].matches("VMcreate")){
			response = this.compute.runInstance(command[2], command[3]).getId();

		}else if(command[0].matches("VMlocalIP")){
			response = "0.0.0.0";

		}else if(command[0].matches("VMdelete")){
			this.compute.terminateInstance(command[1]);
			response = "true";

		}else if(command[0].matches("VMstatus")){
			Instance inst = this.compute.describeInstance(command[1]);
			if (inst.isRunning()!=0){
				response = "32";
			}else{
				response = "0";
			}

		}else if(command[0].matches("VMlist")){
			Collection<Instance> instances = this.compute.describeInstances();
			response = "";
			for(Instance inst : instances){
				if(!inst.isTerminating())
					response = inst.getName() +" "+response;
			}

		}else if(command[0].matches("isBooted")){
			Instance inst = this.compute.describeInstance(command[1]);
			if (inst.isRunning()!=0){
				response = "true";
			}else{
				response = "false";
			}

		}else if(command[0].matches("KILL")){
			this.compute.terminate();
			this.stopListening();
			return null;
		}else{
			response="Unrecognise command";
		}
		
		return response;
		
	}
	
	/*
	 * VMimage id
	 * > id
	 * 
	 * VMimageID id
	 * > id
	 * 
	 * VMcreate name imgID instance Type
	 * > VMid
	 * 
	 * VMlocalIP VMid
	 * > 192.0.0.0
	 * 
	 * VMdelete id
	 * > true
	 * 
	 * VMstatus
	 * >32(booted)
	 * >0(not booted)
	 * 
	 * VMlist
	 * >id id id
	 * 
	 * isBooted id
	 * > true / false
	 * 
	 */

	
}
