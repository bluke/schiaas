package org.simgrid.schiaas.engine.compute.rice;


import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Vector;

import org.simgrid.msg.Host;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.msg.TaskCancelledException;
import org.simgrid.schiaas.Instance;

/**
 * Process to handle scheduling of the migrations and offloads
 * @author julien.gossa@unistra.fr
 */
public class MigrationController extends Process {
	
	/**
	 * Struct describing one off-load
	 * @author julien.gossa@unistra.fr
	 */
	private class OffLoad {
		double date;
		Host host;
		public OffLoad(double date, Host host) {
			this.date = date;
			this.host = host;
		}
	}
		
	/** The set of migrations */
	private Vector<OffLoad> offLoads;
	
	/** The controller */
	private Rice rice;
	

	
	/**
	 * Process to handle one off-load 
	 * @author julien.gossa@unistra.fr
	 */
	private class OffLoadProcess extends Process {
		private Host host;		
		
		protected OffLoadProcess(Host host) throws HostNotFoundException {
			super(rice.controller, "OffLoadProcess:"+host);
			this.host = host;
			try {
				this.start();
			} catch(HostNotFoundException e) {
				Msg.critical("Something bad happend in the OffLoadProcess of RICE"+e.getMessage());
			}
		}

		public void main(String[] arg0) throws MsgException {
			Msg.info("OffLoad of "+host.getName()+" started.");
			rice.offLoad(host);
			Msg.info("OffLoad of "+host.getName()+" complete.");
		}
	}
	
	/**
	 * Constructor
	 * @param rice the RICE of this
	 * @param offloadsFileName the file containing offloads dates and infos
	 * @throws FileNotFoundException 
	 * @throws HostNotFoundException 
	 */
	protected MigrationController(Rice rice, String offloadsFileName) throws FileNotFoundException, HostNotFoundException {
		super(rice.controller, "SchloudMigrationController");

		this.rice = rice;
		
		Scanner scf = new Scanner(new File(offloadsFileName));		

		this.offLoads = new Vector<OffLoad>();
		while (scf.hasNextLine()) {
			Scanner sc = new Scanner(scf.nextLine());			
			addOffLoad(sc.nextDouble(), sc.next());
			sc.close();
		}
		scf.close();
		
		try {
			this.start();
		} catch(HostNotFoundException e) {
			Msg.critical("Something bad happend in the MigrationController of RISE"+e.getMessage());
		}
	}

	/**
	 * Add an off-load
	 * @param date the date of the migration
	 * @param hostname the name of the host to off-load
	 * @throws HostNotFoundException 
	 */
	protected void addOffLoad(double date, String hostname) throws HostNotFoundException {
		offLoads.add(new OffLoad(date, Host.getByName(hostname)));
	}

	
	/**
	 * Execute off-loads until there is no more.
	 */
	public void main(String[] arg0) throws MsgException {
		while (!offLoads.isEmpty()) {
			OffLoad offLoad = offLoads.remove(0);
			waitFor(offLoad.date-Msg.getClock());
			new OffLoadProcess(offLoad.host);
		}
	}
}
