package popt4jlib.PS;

import parallel.*;
import java.util.*;
import java.io.Serializable;

/**
 * used in migrating individuals between islands in DPSO (or other ps-based
 * approaches, eg FA). Not part of the public API (though the class is public as
 * it is also used from the FA sub-package).
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class RecvIndTask implements TaskObject {
  private final static long serialVersionUID = 5070730321003905773L;
  Vector _immpool;
  Vector _imms;
  private boolean _done;

  /**
   * public constructor.
   * @param immpool Vector
   * @param imms Vector
   */
  public RecvIndTask(Vector immpool, Vector imms) {
    _immpool = immpool;
    _imms = imms;
    _done=false;
  }


  /**
   * adds all immigants objects specified in the second argument of the
   * constructor, to the pool of immigrants specified in the first constructor
   * argument.
   * @return Serializable this object.
   */
  public Serializable run() {
    _immpool.addAll(_imms);
    synchronized (this) {
      _done = true;
    }
    return this;
  }


  /**
   * returns true iff the run() method has already been invoked.
   * @return boolean
   */
  public synchronized boolean isDone() { return _done; }


  /**
   * performs a shallow copy of the immigrants, pool, and "done" status of the
   * object passed in to this object.
   * @param t TaskObject
   * @throws IllegalArgumentException if the arg. passed is not a RecvIndTask.
   */
  public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
    if (t instanceof RecvIndTask == false)
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    RecvIndTask t2 = (RecvIndTask) t;
    _immpool = t2._immpool;
    _imms = t2._imms;
    _done = t2._done;
  }

}

