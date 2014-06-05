package livesim;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.stepper.BaseStepper;

public class LiveSim {

		public static void main(String[] args) throws HostNotFoundException {

			Msg.init(args);
			
			if(args.length<3){
				Msg.info("Usage : SimSocket platform.xml cloud.xml port");
				System.exit(-1);
			}

			Msg.createEnvironment(args[0]);
			SchIaaS.init(args[1]);
			
			BaseStepper s = new BaseStepper(Host.all()[0],Integer.parseInt(args[2]),"myCloud");
			s.start();
			
			Msg.info("Start sim");
			Msg.run();
		}

}
