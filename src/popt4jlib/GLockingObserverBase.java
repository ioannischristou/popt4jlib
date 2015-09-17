/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package popt4jlib;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import parallel.DMCoordinator;
import parallel.ParallelException;


/**
 * base class implementing only the ObserverIntf interface -template method 
 * style-, with the implemented method locking on the global "popt4jlib" w-lock. 
 * Necessarily therefore, the API <CODE>notifyChange(subject)</CODE> call is not 
 * an open-call but instead is protected and executes sequentially via the 
 * serialization provided by the "popt4jlib" global write lock.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class GLockingObserverBase implements ObserverIntf {

	/**
	 * no-op in body of constructor.
	 */
	public GLockingObserverBase() {
	}
	

  /**
   * when a subject's thread calls the method notifyChange, in response, this
   * object will invoke the sub-class's implementation of the abstract
	 * <CODE>notifyChangeProtected(subject)</CODE> method (template method style).
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  public final void notifyChange(SubjectIntf subject) throws OptimizerException {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
			notifyChangeProtected(subject);
    }
    catch (ParallelException e) {
      e.printStackTrace();
    }
    finally {
      try {
        DMCoordinator.getInstance("popt4jlib").releaseWriteAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
    }
  }

	
	/**
	 * sub-classes must implement this method to actually act on the notification.
	 * @param subject SubjectIntf
	 * @throws OptimizerException
	 */
	protected abstract void notifyChangeProtected(SubjectIntf subject) throws OptimizerException;
	
}

