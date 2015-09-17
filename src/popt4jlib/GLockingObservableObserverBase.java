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
 * base class implementing both the SubjectIntf and ObserverIntf interfaces, with 
 * all implemented methods locking on the global "popt4jlib" w-lock. Necessarily 
 * therefore, the API <CODE>notifyChange(subject)</CODE> calls are not 
 * open-calls when the <CODE>notifyObservers()</CODE> method is invoked, but
 * instead are protected and execute sequentially via the serialization provided
 * by the "popt4jlib" global write lock.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public abstract class GLockingObservableObserverBase implements SubjectIntf, ObserverIntf {

  private HashMap _observers;  // map<ObserverIntf o, Vector<Object> newSols>
  private HashMap _subjects;  // map<SubjectIntf o, Vector<Object> newSols>
	
	
	public GLockingObservableObserverBase() {
		_subjects = new HashMap();
		_observers = new HashMap();
	}
	

  // SubjectIntf methods implementation
  /**
   * allows an Object that implements the ObserverIntf interface to register
   * with this object and thus be notified whenever new incumbent solutions
   * are produced. The ObserverIntf objects may then
   * independently produce their own new solutions and add them back into the
   * process via a call to addIncumbent(observer, functionarg).
   * The order of events cannot be uniquely defined and the experiment may not
   * always produce the same results.
   * @param observer ObserverIntf.
   * @return boolean returns always true.
   */
  public final boolean registerObserver(ObserverIntf observer) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      _observers.put(observer, new Vector());
      return true;
    }
    catch (ParallelException e) {
      e.printStackTrace();
      return false;
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
   * removes an Object that implements the ObserverIntf that has been registered
   * to listen for new solutions. Returns true if the observer was registered,
   * false otherwise.
   * @param observer ObserverIntf
   * @return boolean
   */
  public final boolean removeObserver(ObserverIntf observer) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      int size = _observers.size();
      _observers.remove(observer);
      return (size == _observers.size() + 1);
    }
    catch (ParallelException e) {
      e.printStackTrace();
      return false;
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
   * notifies every ObserverIntf object that was registered via a call to
   * registerObserver(obs) -and has not been removed since- by calling the
   * ObserverIntf object's method notifyChange(SubjectIntf this).
   */
  public final void notifyObservers() {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      Iterator it = _observers.keySet().iterator();
      while (it.hasNext()) {
        ObserverIntf oi = (ObserverIntf) it.next();
        try {
          oi.notifyChange(this);
        }
        catch (OptimizerException e) {
          e.printStackTrace(); // no-op
        }
      }
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
   * returns the current function that is being minimized (may be null). The 
	 * implementation of this function in subclasses should be normally 
	 * synchronized to ensure memory visibility etc.
   * @return FunctionIntf
   */
  public abstract FunctionIntf getFunction();
	
	
  /**
   * returns the currently best known function argument that  minimizes the _f
   * function. The ObserverIntf objects would need this method to get the
   * current incumbent (and use it as they please). Note: the method is not
   * synchronized, but it still executes atomically, and is in synch with
   * the DGAThreads executing setIncumbent(), by locking, as all other 
	 * implemented methods in this class, on the "popt4jlib" global w-lock, and
	 * then invoking the -abstract- method getIncumbentProtected() which is to be
	 * implemented by sub-classes, in an instance of the template method pattern.
   * @return Object
   */
  public final Object getIncumbent() {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
			return getIncumbentProtected();
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
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
   * allows an ObserverIntf object to add back into the DGA process an
   * improvement to the incumbent solution it was given. The method should only
   * be called by the ObserverIntf object that has been registered to improve
   * the current incumbent.
   * @param obs ObserverIntf
   * @param soln Object
   */
  public final void addIncumbent(ObserverIntf obs, Object soln) {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      // add new solution back
      Vector sols = (Vector) _observers.get(obs);
      if (sols == null) {
        return; // ObserverIntf was not registered or was removed
      }
      sols.addElement(soln);
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


  // ObserverIntf methods implementation
  /**
   * when a subject's thread calls the method notifyChange, in response, this
   * object will add the best solution found by the subject, in the _subjects'
   * solutions map, to be later picked up by the first optimizer Thread spawned 
	 * by this object.
   * @param subject SubjectIntf
   * @throws OptimizerException
   */
  public final void notifyChange(SubjectIntf subject) throws OptimizerException {
    try {
      DMCoordinator.getInstance("popt4jlib").getWriteAccess();
      Object arg = subject.getIncumbent();
      addSubjectIncumbent(subject, arg);  // add the solution found by the
      // subject to my solutions so that it will be picked up in the
      // next generation from _threads[0].
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
	 * subclasses must implement this method.
	 * @return Object
	 */
	protected abstract Object getIncumbentProtected();

	
	/**
	 * return the observers reference.
	 * @return HashMap
	 */
	protected final HashMap getObservers() {
		return _observers;
	}
	
	
	/**
	 * return the subjects reference.
	 * @return HashMap
	 */
	protected final HashMap getSubjects() {
		return _subjects;
	}
	
	
  /**
   * add the soln into a hash-map maintaining (SubjectIntf, Object soln) pairs.
   * Then, the soln can be normally inserted in the first optimizer's thread's 
	 * population in the next iteration. Only called from the 
	 * <CODE>notifyChange(subject)</CODE> method.
   * @param subject SubjectIntf
   * @param soln Object
   */
  private void addSubjectIncumbent(SubjectIntf subject, Object soln) {
    // add new solution back
    Vector sols = (Vector) _subjects.get(subject);
    if (sols == null) sols = new Vector();
    sols.addElement(soln);
    _subjects.put(subject, sols);
  }
	
}

