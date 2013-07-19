package org.simgrid.schiaas.api;



adrien lebre






public class InstanceType implements Serializable {
/**
	* 
*/
private static final long serialVersionUID = 2691159847528386205L;

private String id;
private int ramSize;
private int coreNumber;
private int diskSize;
private String diskPath;
private int netBW;


// TODO: rendre inmutable


public InstanceType(String name) {
	this.setName(name);
}

public InstanceType(String name, int core, double CUperCore, long memory, long disk) {
	this.setName(name);
	this.setMemory(memory);
	this.setCore(core);
	this.setCUperCore(CUperCore);
	this.setDisk(disk);
	this.diskPath = "";
	this.netBW = 1250000000;
	id=idCpt;
	idCpt++;
}

public InstanceType(String name, int core, double CUperCore, long memory, long disk, String diskPath, long netBW) {
	this.setName(name);
	this.setMemory(memory);
	this.setCore(core);
	this.setCUperCore(CUperCore);
	this.setDisk(disk);
	this.diskPath = diskPath;
	this.netBW = netBW;
	 
	id=idCpt;
	idCpt++;
}

public long getMemory() {
	return memory;
}

public void setMemory(long memory) {
	this.memory = memory;
}

public int getCore() {
	return core;
}

public void setCore(int core) {
	this.core = core;
}

public double getCUperCore() {
	return CUperCore;
}

public void setCUperCore(double cUperCore) {
	CUperCore = cUperCore;
}

public long getDisk() {
	return disk;
}

public void setDisk(long disk) {
	this.disk = disk;
}

public String getName() {
	return name;
}

public void setName(String name) {
	 this.name = name;
}

public Integer getId() {
	return id;
}

public void setId(Integer id) {
	 this.id = id;
}

public boolean isClustercompte() {
	return clustercompte;
}

public void setClustercompte(boolean clustercompte) {
	this.clustercompte = clustercompte;
}

public double getMinPrice() {
	return minPrice;
}

public void setMinPrice(double minPrice) {
	this.minPrice = minPrice;
}

public double getMaxPrice() {
	return maxPrice;
}

public void setMaxPrice(double maxPrice) {
	this.maxPrice = maxPrice;
}

public double getLastAlea() {
	return lastAlea;
}

public void setLastAlea(double lastAlea) {
	this.lastAlea = lastAlea;
}

public String getTrace() {
	return trace;
}

public void setTrace(String trace) {
	this.trace = trace;
}

public Integer getLastLineRead() {
	return lastLineRead;
}

public void setLastLineRead(Integer lastLineRead) {
	this.lastLineRead = lastLineRead;
}

public InstanceType clone() {
	InstanceType tmpl = new InstanceType(name, core, CUperCore, memory, disk, diskPath, netBW);
	tmpl.setId(id);
	tmpl.setClustercompte(clustercompte);
	 
	return tmpl;
}

public String getDiskPath() {
	return diskPath;
}

public void setDiskPath(String diskPath) {
	this.diskPath = diskPath;
}

public long getNetBW() {
	return netBW;
}

public void setNetBW(long netBW) {
	this.netBW = netBW;
}

public void setBootComputeDuration(Double compute) {
	this.bootcomputeduration = compute;
}

public Double getBootComputeDuration() {
	return bootcomputeduration;
}

public void setShutdownComputeDuration(Double compute) {
	this.shutdowncomputeduration = compute;
}

public Double getShutdownComputeDuration() {
	return shutdowncomputeduration;
}
public void setSuspendComputeDuration(Double compute) {
	this.suspendcomputeduration = compute;
}

public Double getSuspendComputeDuration() {
	return suspendcomputeduration;
}
public void setResumeComputeDuration(Double compute) {
	this.resumecomputeduration = compute;
}

public Double getResumeComputeDuration() {
	return resumecomputeduration;
}
public String toString() {
	return this.id+" "+core+" "+CUperCore+" "+memory+" "+disk;
}
}

