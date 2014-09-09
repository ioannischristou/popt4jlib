package popt4jlib.GA;

import parallel.*;
import java.util.*;
import java.io.Serializable;

/**
 * used in migrating individuals between islands in DGA. Not a part of the
 * public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
class RecvIndTask implements TaskObject {
  private final static long serialVersionUID = -841697574519513689L;
  List _immpool;
  List _imms;
  boolean _isDone=false;

  /**
   * public sole constructor.
   * @param immpool List
   * @param imms List
   */
  public RecvIndTask(List immpool, List imms) {
    _immpool = immpool;
    _imms = imms;
  }


  /**
   * adds the immigrants passed in this object during construction, to the
   * immigrant pool passed in this object during construction.
   * @return Serializable this object.
   */
  public Serializable run() {
    _immpool.addAll(_imms);
    synchronized (this) {
      _isDone = true;
    }
    return this;
  }


  /**
   * synchronized method returns true iff the run() method has been executed.
   * @return boolean
   */
  public synchronized boolean isDone() { return _isDone; }


  /**
   * synchronized copying of each field from argument to this object.
   * @param t TaskObject
   * @throws IllegalArgumentException
   */
  public synchronized void copyFrom(TaskObject t) throws IllegalArgumentException {
    if (t instanceof RecvIndTask == false)
      throw new IllegalArgumentException("copyFrom(t) method not supported");
    RecvIndTask t2 = (RecvIndTask) t;
    _immpool = t2._immpool;
    _imms = t2._imms;
    _isDone = t2._isDone;
  }

}

