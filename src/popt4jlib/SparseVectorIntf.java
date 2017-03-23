package popt4jlib;

/**
 * this interface extends the standard VectorIntf in that it provides two
 * methods to allow for (fast) loops over the non-zero (non-default-valued more 
 * generally) elements of a sparse vector.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 2.0
 */
public interface SparseVectorIntf extends VectorIntf {
  /**
   * get the number of non-zero elements of this vector. In case the result of
	 * <CODE>getDefVal()</CODE> is not-zero, then the method must return the 
	 * number of non-default-valued components.
   * @return int
   */
  public int getNumNonZeros();

  /**
   * return the index of the i-th non-zero element of this vector. For example,
   * for the vector v=[0 0 1 2 0], the method <CODE>getIthNonZeroPos(0)</CODE>
   * will return 2, and the call <CODE>getIthNonZeroPos(1)</CODE> will return
   * 3.
	 * <p>In case the result of <CODE>getDefVal()</CODE> is not-zero, then the 
	 * method must return the i-th non-default-valued element of this vector.</p>
   * @param i int
   * @throws IndexOutOfBoundsException if the argument i is &lt;0 or
   * &ge;getNumCoords(). Also, if this is the zero vector, the method will
   * always throw regardless of the argument passed in.
   * @return int
   */
  public int getIthNonZeroPos(int i) throws IndexOutOfBoundsException;
	
	
	/**
	 * return the default value for components of this vector (usually, this must
	 * be zero). An invariant property is that the number returned must be the 
	 * same constant for any object throughout its life-time.
	 * @return double
	 */
	public double getDefaultValue();
	
}
