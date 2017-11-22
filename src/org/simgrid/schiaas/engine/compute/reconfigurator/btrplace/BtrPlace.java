package org.simgrid.schiaas.engine.compute.reconfigurator.btrplace;

import org.btrplace.json.JSONConverterException;
import org.btrplace.json.model.InstanceConverter;
import org.btrplace.model.*;
import org.btrplace.model.constraint.MinMTTR;
import org.btrplace.model.constraint.Preserve;
import org.btrplace.model.constraint.SatConstraint;
import org.btrplace.model.view.ShareableResource;
import org.btrplace.plan.ReconfigurationPlan;
import org.btrplace.plan.event.*;
import org.btrplace.scheduler.SchedulerException;
import org.btrplace.scheduler.choco.ChocoScheduler;
import org.btrplace.scheduler.choco.DefaultChocoScheduler;
import org.simgrid.msg.HostFailureException;
import org.simgrid.msg.Msg;
import org.simgrid.schiaas.engine.compute.ComputeEngine;
import org.simgrid.schiaas.engine.compute.ComputeReconfigurator;
import org.simgrid.schiaas.engine.compute.ComputeReconfigurator.ReconfigurationHeuristic;
import org.simgrid.schiaas.engine.compute.ComputeHost;
import org.simgrid.schiaas.loadinjector.LoadedInstance;

import java.io.*;
import java.util.*;

/**
 * @author Adrien Lebre based on Adrian Fraisse proposal (historically in VMPlaceS)
 * Implementation of the Scheduler interface using the BtrPlace API
 */

public class BtrPlace extends ReconfigurationHeuristic{

    /**
     * The BtrPlace scheduler
     */
    private ChocoScheduler btrSolver;

    /**
     * Map to link BtrPlace nodes ids to XHosts
     */
    private Map<Integer, String> nodesMap;

    /**
     * Map to link BtrPlace vm ids to XVMs
     */
    private Map<Integer, String> vmMap;

    /**
     * The initial configuration.
     */
    private Model source;

    /**
     * The configuration's constraints
     */
    private Set<SatConstraint> constraints;

    /**
     * The computed reconfiguration plan
     */
    private ReconfigurationPlan reconfigurationPlan;


    public BtrPlace((ComputeEngine computeEngine, Map<String, String> config) {
        this(computeEngine.getComputeHosts(), new Random(Long.parseLong(config.get("random_seed"))).nextInt());

    }
    public BtrPlaceRP(Collection<ComputeHost> hostsToBeChecked, Integer id) {
        super();
        //this.id = id;
        this.btrSolver = new DefaultChocoScheduler();

        /**
         * Adrian - From BtrPlace doc :
         * By default, BtrPlace considers every VMs when it solves a model.
         * This may lead to a non-reasonable solving process duration when
         * a few number of constraints are violated.
         * The repair approach addresses that problem by trying to reduce as possible
         * the number of VMs to consider in the model.
         */

        this.btrSolver.doRepair(true);
        int timeLimit = hostsToBeChecked.size() / 8;
        this.btrSolver.setTimeLimit(timeLimit > 30 ? timeLimit : 30);

        this.extractConfiguration(hostsToBeChecked);

        // log the model
        // FIXME Adrien - JSON is for later
        /*
        try {
            File file = new File("logs/btrplace/configuration/" + id + "-" + System.currentTimeMillis() + ".txt");
            file.getParentFile().mkdirs();

            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
            Instance i = new Instance(source, new ArrayList<>(), new MinMTTR());
            InstanceConverter conv = new InstanceConverter();
            pw.write(conv.toJSON(i).toJSONString());
            pw.flush();
            pw.close();
        } catch (IOException | JSONConverterException e) {
            Msg.critical("BtrPlace : could not log the source model");
        }
        */

    }

  //FIXME
  /*public BtrPlaceRP(Collection<XHost> xHosts) {
        this(xHosts, new Random(SimulatorProperties.getSeed()).nextInt());
    }*/

    /**
     * Creates a Model and constraints for BtrPlace
     *
     * @param xHosts Collection of Xhosts declared as hosting nodes and that are turned on
     */
    private void extractConfiguration(Collection<ComputeHost> xHosts) {

        // Initialization
        this.source = new DefaultModel();
        Mapping mapping = this.source.getMapping();

        this.nodesMap = new HashMap<>();
        this.vmMap = new HashMap<>();

        // Creation of a view for defining CPU & Memory resources
        // FIXME need CPU and memory max value
        ShareableResource rcCPU = new ShareableResource("cpu", 8192, 0);
        ShareableResource rcMem = new ShareableResource("mem", 8192, 0);

        this.constraints = new HashSet<>();

        // Add nodes
        for (ComputeHost tmpH : xHosts) {
            // Creates a physical node
            Node n = this.source.newNode();
            this.nodesMap.put(n.id(), tmpH.getHost().getName());

            // Add physical node to mapping
            mapping.addOnlineNode(n);

            // Node's resources are explicitly set
            rcCPU.setCapacity(n, tmpH.getHost().getSpeed());
            // FIXME we need to add memory in ComputeHost
            rcMem.setCapacity(n, tmpH.getMemory());

            //FIXME we need to add a method isViolated
            if (tmpH.isViable()) {
                // If the host if viable, the model is exactly has the VM demand regarding cpu and memory usage
                // Declare running VMs mapping
                for (LoadedInstance tmpVM : tmpH.getHostedInstances()) {
                    VM v = this.source.newVM();
                    mapping.addRunningVM(v, n);
                    this.vmMap.put(v.id(), tmpVM.getName());
                    rcCPU.setConsumption(v, (int) tmpVM.getCPUDemand());
                    rcMem.setConsumption(v, tmpVM.getMemSize());
                }
            } else {
                // The host is not viable : we create a model based on a fair share of the host resources

                int cpuFairShare = tmpH.getCPUCapacity() / tmpH.getNbVMs();
                int memFairShare = tmpH.getMemSize() / tmpH.getNbVMs();

                for (XVM tmpVM : tmpH.getRunnings()) {
                    VM v = this.source.newVM();
                    mapping.addRunningVM(v, n);
                    this.vmMap.put(v.id(), tmpVM.getName());

                    // if the VM ask for more resource than what the host can provide, we allow only a fair share of the resources
                    rcCPU.setConsumption(v, Math.min((int) tmpVM.getCPUDemand(), cpuFairShare));
                    rcMem.setConsumption(v, Math.min(tmpVM.getMemSize(), memFairShare));

                    this.constraints.add(new Preserve(v, "cpu", (int) tmpVM.getCPUDemand()));
                    this.constraints.add(new Preserve(v, "mem", tmpVM.getMemSize()));
                }

            }

        }

        this.source.attach(rcCPU);
        this.source.attach(rcMem);

    }

    /**
     * Computes the reconfiguration plan and measure the duration of the computation.
     * @return The result of the computation
     */
    public ComputingResult computeReconfigurationPlan() {
        Msg.info("Nodes considered: " + source.getMapping().getAllNodes().toString());

        long timeToComputeVMRP = System.currentTimeMillis();
        try {
            reconfigurationPlan = this.btrSolver.solve(source, constraints);
            timeToComputeVMRP = System.currentTimeMillis() - timeToComputeVMRP;
        } catch (SchedulerException e) {
            timeToComputeVMRP = System.currentTimeMillis() - timeToComputeVMRP;
            reconfigurationPlan = null;
            Msg.critical("An error occurred while solving the model : " + e.getCause());
            return new ComputingResult(ComputingResult.State.RECONFIGURATION_FAILED, timeToComputeVMRP);
        }

        if (reconfigurationPlan == null)
            return new ComputingResult(ComputingResult.State.RECONFIGURATION_FAILED, timeToComputeVMRP);
        else if (reconfigurationPlan.getActions().isEmpty())
            return new ComputingResult(ComputingResult.State.NO_RECONFIGURATION_NEEDED, timeToComputeVMRP);
        else
            return new ComputingResult(ComputingResult.State.SUCCESS, timeToComputeVMRP, computeNbMigrations(), reconfigurationPlan.getDuration());

    }

    /**
     * Apply the reconfiguration plan using BtrPlace EventListeners
     */
    protected void applyReconfigurationPlan() {
        if (this.reconfigurationPlan != null && reconfigurationPlan.isApplyable()) {

            // We log the reconfiguration plan
            try {
                File file = new File("logs/btrplace/reconfigurationplan/" + id + "-" + System.currentTimeMillis() + ".txt");
                file.getParentFile().mkdirs();
                PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.write(this.reconfigurationPlan.toString());
                pw.flush();
                pw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /**
             * Adrian - We add a Action Committed Listener for it to also execute business code for VMPlaces
             */
            reconfigurationPlan.getReconfigurationApplier().addEventCommittedListener(new EventCommittedListener() {
                /*
                 * Those methods will be called upon completion of an action.
                 * It's just the right time for us to actually do the action in SimGrid !
                 */

                @Override
                public void committed(Allocate allocate) {}

                @Override
                public void committed(AllocateEvent allocateEvent) {}

                @Override
                public void committed(SubstitutedVMEvent substitutedVMEvent) {}

                @Override
                public void committed(BootNode bootNode) {}

                @Override
                public void committed(BootVM bootVM) {}

                @Override
                public void committed(ForgeVM forgeVM) {
                }

                @Override
                public void committed(KillVM killVM) {}

                @Override
                public void committed(MigrateVM migrateVM) {
                    XHost src = SimulatorManager.getXHostByName(nodesMap.get(migrateVM.getSourceNode().id()));
                    XHost dst = SimulatorManager.getXHostByName(nodesMap.get(migrateVM.getDestinationNode().id()));

                    if(dst.isOff())
                        SimulatorManager.turnOn(dst);

                    relocateVM(
                            vmMap.get(migrateVM.getVM().id()),
                            nodesMap.get(migrateVM.getSourceNode().id()),
                            nodesMap.get(migrateVM.getDestinationNode().id())
                    );



                    Msg.info(src.getName() + " has " + src.getRunnings().size() + " Vms");
                }

                @Override
                public void committed(ResumeVM resumeVM) {
                /*
                    // Not available right now due to the rpAborted semantic that needs to be clarified
                    resumeVM(

                            vmMap.get(resumeVM.getVM().id()),
                            nodesMap.get(resumeVM.getSourceNode().id())
                    );
                */
                }

                @Override
                public void committed(ShutdownNode shutdownNode) {}

                @Override
                public void committed(ShutdownVM shutdownVM) {}

                @Override
                public void committed(SuspendVM suspendVM) {
                /*
                    // Not available right now due to the rpAborted semantic that needs to be clarified
                    suspendVM(

                            vmMap.get(suspendVM.getVM().id()),
                            nodesMap.get(suspendVM.getSourceNode().id())
                    );
                 */
                }
            });

            // We now roll out the reconfiguration plan
            reconfigurationPlan.getResult();

            // If you reach that line, it means that either the execution of the plan has been completely launched or the
            // plan has been aborted. In both cases, we should wait for the completion of on-going migrations

            // Add a watch dog to determine infinite loop
            int watchDog = 0;

            while(this.ongoingMigrations()){
                try {
                    org.simgrid.msg.Process.getCurrentProcess().waitFor(1);
                    watchDog ++;
                    if (watchDog%100==0){
                        Msg.info("You're are waiting for a couple of seconds (already "+watchDog+" seconds)");
                        if(SimulatorManager.isEndOfInjection()){
                            Msg.critical("The reconfiguration is taking too long - Forcing termination...");
                            System.exit(42);
                        }
                    }
                } catch (HostFailureException e) {
                    Msg.critical("Host crashed while reconfiguring : " + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Get the number of migrations
     */
    private int computeNbMigrations(){
        int nbMigrations = 0;

        for (Action a : reconfigurationPlan.getActions()) {
            if (a instanceof MigrateVM) {
                nbMigrations++;
            }
        }

        return nbMigrations;
    }

}
