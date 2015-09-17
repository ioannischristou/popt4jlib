package parallel;

import java.io.Serializable;

/**
 * interface defining what a TaskObject must support.
 * It is defined to extend Serializable so that it can be distributed over
 * sockets to other JVMs as well.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface TaskObject extends Serializable {

  /**
   * a computation to be carried out, possibly returning some result (or itself,
   * or null ...)
   * @return Serializable
   */
  public Serializable run();


  /**
   * returns true only if the computation carried out by the run() method has
   * been completed.
   * @return boolean
   */
  public boolean isDone();


  /**
   * support the ability to copy the state of other into this TaskObject.
   * @param other TaskObject the object whose state must be copied onto this.
   * @throws IllegalArgumentException if other can't be copied onto this object.
   */
  public void copyFrom(TaskObject other) throws IllegalArgumentException;
}
