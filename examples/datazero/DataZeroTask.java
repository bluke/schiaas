/*
 * Copyright 2006-2012. The SimGrid Team. All rights reserved. 
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the license (GNU LGPL) which comes with this package. 
 */

package datazero;
import org.simgrid.msg.Task;

public class DataZeroTask extends Task {    	
   public DataZeroTask() {
      super("finalize",0,0);
   }
}
    
