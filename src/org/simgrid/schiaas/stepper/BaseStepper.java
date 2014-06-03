package org.simgrid.schiaas.stepper;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.schiaas.Image;
import org.simgrid.schiaas.Instance;
import org.simgrid.schiaas.InstanceType;

/**
 * Basic example of a stepper command system
 * Available commands are (arguments are mandatory) :
 *  - describeInstanceTypes
 *  - describeInstanceType [type]
 *  - describeImages
 *  - describeImage [img]
 *  - describeAvailability [type]
 *  - runInstance [img] [type]
 *  - runInstances [img] [type] [num]
 *  - describeInstances
 *  - describeInstance [inst]
 *  - suspendInstance [inst]
 *  - resumeInstance [inst]
 *  - terminateInstance [inst]
 *  - terminate
 *  - help
 * 
 * @author luke
 *
 */
public class BaseStepper extends Stepper {


	public BaseStepper(Host host, int port, String cloudId) {
		super(host, port, cloudId);
	}

	@Override
	protected String execute(String input) throws HostFailureException {
		
		String output = "Unknown Command";
		String[] args = input.split(" ");
		String command = args[0];
		
		try{
		
			if(command.matches("describeInstanceTypes")){
		
				Collection<InstanceType> collection = this.compute.describeInstanceTypes();
				Iterator<InstanceType> i = collection.iterator();
				output = "["+i.next().getId();
				while(i.hasNext()){
					output+=","+i.next().getId();
				}
				output+="]";
			
			}else if(command.matches("describeInstanceType")){
				
				InstanceType type = compute.describeInstanceType(args[1]);
				output = type.toString();

			
			}else if(command.matches("describeImages")){
				
				Map<String,Image> map = this.compute.describeImages();
				Iterator<String> i = map.keySet().iterator();
				output = "["+i.next();
				while(i.hasNext()){
					output+=","+i.next();
				}
				output+="]";
				
			}else if(command.matches("describeImage")){
				

				Image image = compute.describeImage(args[1]);
				output = image.toString();
			
			}else if(command.matches("describeAvailability")){
				
				output = Integer.toString(compute.describeAvailability(args[1]));
				
			}else if(command.matches("describeInstance")){
				
				Instance instance = compute.describeInstance(args[1]);
				output = instance.toString();
				
			}else if(command.matches("describeInstances")){
				
				Collection<Instance> collection = compute.describeInstances();
				Iterator<Instance> i = collection.iterator();
				output = "["+i.next().getId();
				while(i.hasNext()){
					output+=","+i.next().getId();
				}
				output+="]";
				
			}else if(command.matches("runInstance")){
				
				output = compute.runInstance(args[1], args[2]);
				
			}else if(command.matches("runInstances")){
				
				String[] ids = compute.runInstances(args[1], args[2], Integer.parseInt(args[3]));
				output = ids.toString();
				
			}else if(command.matches("terminateInstance")){
				
				compute.terminateInstance(args[1]);
				output = "understood";
				
			}else if(command.matches("terminate")){
				
				compute.terminate();
				stopListening();
				output = null;
				
			}else if(command.matches("resumeInstance")){
				
				compute.resumeInstance(args[1]);
				output = "understood";
				
			}else if(command.matches("suspendInstance")){
				
				compute.suspendInstance(args[1]);
				output = "understood";
				
			}else if(command.matches("help")){
				
				output = "Good luck;";
				
			}
			
			
		
		}catch(NullPointerException e){
			output = "Internal Error : Wrong argument(s)";
		}catch(ArrayIndexOutOfBoundsException e){
			output = "Internal Error : Missing argument(s)";
		}catch(Exception e){
			output = "Internal Error : "+e.toString();
		}
		
		return output;
	}

}
