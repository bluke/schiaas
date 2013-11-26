package org.simgrid.simiaas.api;

import org.simgrid.msg.Comm;
import org.simgrid.msg.HostNotFoundException;
import org.simgrid.msg.Msg;
import org.simgrid.msg.MsgException;
import org.simgrid.msg.Mutex;
import org.simgrid.msg.Process;
import org.simgrid.msg.Task;
import org.simgrid.simiaas.api.Cloud.VMHost;


/**
 * Represents an image of VM
 * @author julien
 */
public class Image {

	/** Name/ID of the image. */
	protected String name;
	
	/** Size (in B) of the image. */
	protected double size;
	
	/** Compute duration (in flops) of the boot. */
	private double bootComputeDuration;
	
	/** Compute duration (in flops) of the shutdown. */
	protected double shutdownComputeDuration;
	
	/** Compute duration (in flops) of the suspend. */
	protected double suspendComputeDuration;
	
	/** Compute duration (in flops) of the resume. */
	protected double resumeComputeDuration;
	
	
	/** Default compute duration (in flops) of the boot. */
	protected static final double defaultBootComputeDuration = 1e06;
	
	/** Default compute duration (in flops) of the shutdown. */
	protected static final double defaultShutdownComputeDuration = 1e04;
	
	/** Default compute duration (in flops) of the suspend. */
	protected static final double defaultSuspendComputeDuration  = 1e03;
	
	/** Default compute duration (in flops) of the resume. */
	protected static final double defaultResumeComputeDuration  = 1e03;
	
	
	/**
	 * Minimal constructor.
	 * @param size Size of the image (in B).
	 */
	public Image(String name, double size)
	{
		this(name, size, defaultBootComputeDuration);
	}

	/**
	 * Constructor.
	 * @param size Size of the image (in B).
	 * @param bootComputeDuration Boot compute duration (in flops).
	 */
	public Image(String name, double size, double bootComputeDuration)
	{
		this(name, size, bootComputeDuration, defaultShutdownComputeDuration);
	}
	
	/**
	 * Constructor.
	 * @param size Size of the image (in B).
	 * @param bootComputeDuration Boot compute duration (in flops).
	 * @param shutdownComputeDuration Shutdown compute duration (in flops).
	 */
	public Image(String name, double size, double bootComputeDuration, double shutdownComputeDuration) {
		this (name, size, bootComputeDuration, shutdownComputeDuration, defaultSuspendComputeDuration, defaultResumeComputeDuration);
	}

	/**
	 * Complete constructor.
	 * @param size Size of the image (in B).
	 * @param bootComputeDuration Boot compute duration (in flops).
	 * @param shutdownComputeDuration Shutdown compute duration (in flops).
	 * @param suspendComputeDuration Suspend compute duration (in flops).
	 * @param resumeComputeDuration Resume compute duration (in flops).
	 */
	public Image(String name, double size, double bootComputeDuration, double shutdownComputeDuration, double suspendComputeDuration, double resumeComputeDuration) {
		this.name = name;
		this.size = size;
		this.bootComputeDuration     = bootComputeDuration;
		this.shutdownComputeDuration = shutdownComputeDuration;
		this.suspendComputeDuration  = suspendComputeDuration;
		this.resumeComputeDuration   = resumeComputeDuration;
	}

	public double getBootComputeDuration() {
		return bootComputeDuration;
	}

	protected Mutex createTransferProcess(VMHost h) {
		ImageTransferProcess itp = new ImageTransferProcess(h,this);
		return itp.imageTransferMutex;
	}
	
	protected class ImageTransferProcess extends Process {
		
		protected VMHost vmhost;
		protected Image image; 
		protected Mutex imageTransferMutex;
		
		public ImageTransferProcess(VMHost vmhost, Image image) {
			super(vmhost.host, "ImageTransferProcess");
			this.vmhost = vmhost;
			this.image = image;
			this.imageTransferMutex = new Mutex();
			try {
				this.start();
			} catch (HostNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void main(String[] arg0) throws MsgException {
			imageTransferMutex.acquire();
			Msg.info("XXXXXXXXXXXXXXXXXXXXXXXX wait for data");
			Comm comm = Task.irecv(image.getTransferMessageBox());
			comm.waitCompletion();
			Msg.info("XXXXXXXXXXXXXXXXXXXXXXXX data received");
			imageTransferMutex.release();
			Msg.info("XXXXXXXXXXXXXXXXXXXXXXXX data received");
		}
		
	}

	public String getTransferMessageBox() {
		return name+"_ImageTransferMessageBox";
	}


}
