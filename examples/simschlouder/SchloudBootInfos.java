package simschlouder;

/**
 * This class is a basic struct to store infos about boot times
 * @author julien.gossa@unistra.fr
 */
public class SchloudBootInfos {
	public Double bootTime;
	public Double provisioningDate;
	public Double lagTime;
	public Double monitoringTime;
	
	/**
	 * Constructor making everything null
	 */
	public SchloudBootInfos() {
		this.bootTime = null;
		this.provisioningDate = null;
		this.lagTime = null;
		this.monitoringTime = null;
	}
}
