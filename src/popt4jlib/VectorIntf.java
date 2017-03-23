package popt4jlib;

import parallel.ParallelException;
import java.io.Serializable;


/**
 * defines vectors in R^n.
 * Extends the <CODE>java.io.Serializable</CODE> interface so that implementing 
 * objects can be transported across JVMs in distributed computation.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface VectorIntf extends Serializable {
  /**
   * return a new VectorIntf object given the double[] arg passed in
   * @param arg double[]
   * @throws IllegalArgumentException
   * @return VectorIntf
   */
  public VectorIntf newInstance(double[] arg) throws IllegalArgumentException;
	
	
	/**
	 * return a new (un-managed) <CODE>VectorIntf</CODE> object that is an exact
	 * copy of this object. The only difference between this method and the method 
	 * <CODE>newCopy()</CODE> is that the latter method may choose to return an
	 * object that is gotten from an object-pool, thus requiring invocation of 
	 * <CODE>release()</CODE> methods eventually, if the implementing class also
	 * implements the <CODE>PoolableObjectIntf</CODE> interface.
	 * @return VectorIntf 
	 */
	public VectorIntf newInstance();

	
  /**
   * return a new <CODE>VectorIntf</CODE> that is an exact copy of this object.
   * @return VectorIntf
   */
  public VectorIntf newCopy();


  /**
   * return a new <CODE>VectorIntf</CODE> that is an exact copy of this object
   * but each coordinate of the new object is then multiplied by the value
   * passed in the argument.
   * @param multFactor double
   * @return VectorIntf
   */
  public VectorIntf newCopyMultBy(double multFactor);


  /**
   * get the i-th coordinate of this <CODE>VectorIntf</CODE> object.
   * @param coord int -must be in the set {0,1,...<CODE>getNumCoords()</CODE>-1}
   * @throws IndexOutOfBoundsException if the index is out of range
   * @return double
   */
  public double getCoord(int coord) throws IndexOutOfBoundsException;


  /**
   * set the value of the i-th coordinate of this object.
   * @param i int -must be in the set {0,1,...<CODE>getNumCoords()</CODE>-1}
   * @param val double
   * @throws IndexOutOfBoundsException if the index is out of range
   * @throws ParallelException if the implementation supports multi-threading
   * and a vector tries to advance a read-lock to a write-lock under unfavorable
   * conditions.
   */
  public void setCoord(int i, double val) throws IndexOutOfBoundsException, ParallelException;


  /**
   * modifies this VectorIntf by adding the quantity m*other to it.
   * @param m double
   * @param other VectorIntf
   * @throws IllegalArgumentException if other is null or does not have the
   * same dimensions as this vector
   * @throws ParallelException if the implementation supports multi-threading
   * and a vector tries to advance a read-lock to a write-lock under unfavorable
   * conditions.
   */
  public void addMul(double m, VectorIntf other) throws IllegalArgumentException, ParallelException;


  /**
   * divide the components of this vector by the argument h.
   * @param h double
   * @throws IllegalArgumentException if h is zero
   * @throws ParallelException if the implementation supports multi-threading
   * and a vector tries to advance a read-lock to a write-lock under unfavorable
   * conditions.
   */
  public void div(double h) throws IllegalArgumentException, ParallelException;


  /**
   * return the number of components (dimensionality) of this vector.
   * @return int
   */
  public int getNumCoords();


  /**
   * return true iff all components are null (zero)
   * @return boolean
   */
  public boolean isAtOrigin();


  /**
   * return a double[] representation of this vector.
   * @return double[]
   */
  public double[] getDblArray1();
}

