/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel.distributed;

import java.io.IOException;
import java.io.ObjectOutputStream;
import parallel.FairDMCoordinator;
import parallel.ParallelException;

/**
 * implements a DMsgIntf object that asks a DRWLockSrv identified by name to 
 * release from the current thread a global read-lock. Not for use as part of
 * the normal public API. Only works with 
 * DActiveMsgPassingCoordinatorLongLivedConn[Clt/Srv] objects.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DRLockReleaseRequest implements DMsgIntf {
  // private final static long serialVersionUID = ...L;
  private String _rwlname;  // unique global name to identify read-write lock
	
	
  /**
   * constructor.
   * @param rwlockname String name of the read-write lock in the receiving DRWLockSrv.
   */
  public DRLockReleaseRequest(String rwlockname) {
    _rwlname = rwlockname;
  }


  /**
   * release the read-lock (via sockets) from the FairDMCoordinator object named 
	 * in the constructor. These methods work only when invoked on the server from 
	 * the same thread for each remote thread connecting to the server, thus the 
	 * need for the long-lived connection objects mentioned in the header comments.
   * @param oos ObjectOutputStream
   * @throws IOException
   */
  public void execute(ObjectOutputStream oos) throws IOException {
		try {
	    FairDMCoordinator.getInstance(_rwlname).releaseReadAccess();
		  // ok, handling thread on server added
			oos.writeObject(new OKReply());
			oos.flush();
		}
    catch (ParallelException e) {
      oos.writeObject(new FailedReply());
      oos.flush();
      // throw e;  // don't throw as this would destroy the long-lived socket
			// connection between clt and srv.
    }
  }

  public String toString() {
    return "DRLockReleaseRequest("+_rwlname+")";
  }

}
