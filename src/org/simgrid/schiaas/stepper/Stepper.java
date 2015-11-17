package org.simgrid.schiaas.stepper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.schiaas.Compute;
import org.simgrid.schiaas.SchIaaS;
import org.simgrid.schiaas.exceptions.VMSchedulingException;
/**
 * This Process allows to Schiaas to receive instructions on the fly via a socket.
 *  Instructions are executed in the simulations at times that map the real time.
 *    
 * @author luke
 *
 */
public abstract class Stepper extends Process {
	/**
	 * Constructor for the Stepper
	 * @param host
	 * 			host on which to pin the process
	 * @param port
	 * 			port on which to listen for commands
	 * @param cloudId
	 * 			Id of the Schiaas cloud on which to run the commands
	 */
	public Stepper(Host host, int port, String cloudId){
		super(host,"Schiaas-Stepper",new String[]{Integer.toString(port),cloudId});
	}
	

	/** The date of the first connection */
	private double startDate=-1;
	/** The cloud on which we operate */
	Compute compute = null;
	/** The port on which we listen */
	int port = 1907;
	/** Should be keep listening */
	private boolean listening = true;
	
	
	@Override
	/**
	 * The main thread of the process.
	 * @author luke
	 * @param args
	 * 		the string array should contain {"port_num","schiaas_cloud_id"}
	 */
	public void main(String[] args) throws MsgException {
		this.port = Integer.parseInt(args[0]);
		this.compute = SchIaaS.getCloud(args[1]).getCompute();
		
		
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			Msg.info("Listening on port "+port);

			while(listening){
				try (
					Socket clientSocket = serverSocket.accept();
					PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
					BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				) {
					String received;
					this.setStart();
					while ((received = in.readLine()) != null) {
						this.step();
						String response = execute(received);
						if(response != null){
							out.println(response);
						}else{
							break;
						}
					}
				} catch (VMSchedulingException e) {
					//TODO: check if this is the right thing to do
					Msg.info("Some instances failed to started because: "+e.getMessage());
				}
			}
		} catch (IOException e) {
			Msg.critical("Failled to open socket.");
			e.printStackTrace();
		}
	}

	/**
	 * Sets the startDates from which every other dates are mesured
	 */
	private void setStart(){
		if(this.startDate == -1)
			startDate = System.currentTimeMillis(); 
	}
	
	/**
	 * Tells the stepper to stop waiting for new connection
	 */
	protected void stopListening(){
		this.listening  = false;
	}
	
	/**
	 * Move the simulation to the date corresponding to the time elapsed since startDate
	 * @throws HostFailureException
	 */
	protected void step() throws HostFailureException{
		double now = System.currentTimeMillis();
		this.waitFor(((now-this.startDate)/1000)-Msg.getClock());
	}

	/**
	 * Receives the message send by the client and execute the corresponding command
	 * @param input
	 * 			The string send as a command by the client
	 * @return
	 * 			The response to send to the client
	 * @throws HostFailureException
	 * @throws VMSchedulingException 
	 */
	protected abstract String execute(String input) throws HostFailureException, VMSchedulingException;

	
}
