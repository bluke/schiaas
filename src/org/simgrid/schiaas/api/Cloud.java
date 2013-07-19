package org.simgrid.schiaas.api;

/**
 * This class encapsulates all services provided by a particular instance of a cloud.
 */
public class Cloud {



    private Compute compute ;  // Computation as a service such as EC2.
    private Collection<Storage> Storage; // Storage as a service such as S3, EBS; ....
    private Network network; // Network As a Service


    // TODO AL -> JG/JRC, please backport the XML parser
    public Cloud(...){
        // TODO
    }


    public Compute getCompute() {
        return compute;
    }

    public Collection<Storage> getStorage() {
        return Storage;
    }

    public Network getNetwork() {
        return network;
    }

}
