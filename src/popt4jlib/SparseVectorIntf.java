package popt4jlib;

/**
 * this interface extends the standard VectorIntf in that it provides two
 * methods to allow for (fast) loops over the non-zero elements of a sparse
 * vector.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public interface SparseVectorIntf extends VectorIntf {
  /**
   * get the number of non-zero elements of this vector.
   * @return int
   */
  public int getNumNonZeros();

  /**
   * return the index of the i-th non-zero element of this vector. For example,
   * for the vector v=[0 0 1 2 0], the method <CODE>getIthNonZeroPos(0)</CODE>
   * will return 2, and the call <CODE>getIthNonZeroPos(1)</CODE> will return
   * 3.
   * @param i int
   * @throws IndexOutOfBoundsException if the argument i is &lt;0 or
   * &ge;getNumCoords(). Also, if this is the zero vector, the method will
   * always throw regardless of the argument passed in.
   * @return int
   */
  public int getIthNonZeroPos(int i) throws IndexOutOfBoundsException;
}
