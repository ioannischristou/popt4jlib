package popt4jlib;

import java.util.Hashtable;

/**
 * The SubjectIntf implements the Subject Object's methods in the well-known
 * Observer Design Pattern. Also known as the Observable part of the pattern.
 * See the comments in the ObserverIntf class.
 *
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface SubjectIntf {
  /**
   * registers an observer to listen to changes to this subject
   * @param o ObserverIntf
   * @return boolean
   */
  public boolean registerObserver(ObserverIntf o);


  /**
   * removes an observer from this subject's registered observers
   * @param o ObserverIntf
   * @return boolean true iff the observer object was registered for this subject
   */
  public boolean removeObserver(ObserverIntf o);


  /**
   * calls the method <CODE>notifyChange(x)</CODE> for each of the registered
   * observers of this subject.
   */
  public void notifyObservers();


  /**
   * return the function associated with this subject.
   * @return FunctionIntf
   */
  public FunctionIntf getFunction();


  /**
   * returns the parameters associated with this subject.
   * @return Hashtable
   */
  public Hashtable getParams();


  /**
   * return the best solution found so far by this subject.
   * @return Object
   */
  public Object getIncumbent();


  /**
   * add back a new solution produced by the obs Observer
   * @param obs ObserverIntf
   * @param soln Object
   * @throws OptimizerException
   */
  public void addIncumbent(ObserverIntf obs, Object soln) throws OptimizerException;
}

