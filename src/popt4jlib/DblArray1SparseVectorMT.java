package popt4jlib;

import parallel.*;
import parallel.distributed.PDBatchTaskExecutor;
import utils.DataMgr;
import java.util.List;
import java.util.Vector;
import java.util.ArrayList;
import java.io.Serializable;

/**
 * sparse double[] implementation of VectorIntf, maintaining two arrays,
 * an index array, and a values array. Useful when dealing with very
 * high-dimensional vectors that are sparse to a significant degree (e.g. in
 * Information Retrieval, one may create vectors in 50.000 dimensions, but
 * each vector may have only a few tens or hundreds of non-zero components).
 * Notice that getCoord/setCoord operations can be much more costly than the
 * same operations when operating with a DblArray1Vector (where the operations
 * are O(1) -constant time simple memory accesses).
 * Notice also that the class is thread-safe but quite expensive. A faster but
 * not thread-safe implementation is DblArray1SparseVector. Also notice that 
 * this class does not support default-values other than zero for components.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DblArray1SparseVectorMT extends DblArray1SparseVector {
  private static final long serialVersionUID = -303574319696853810L;
  private static PDBatchTaskExecutor _executor=null;
  private static final double MIN_REQ_ILEN = 1000;  // min length to warrant 
	                                                  // parallel processing
  private DMCoordinator _rwLocker=null;  // if a thread has any lock on this
	                                       // object while it is serialized, an
	                                       // exception will be thrown, as the
	                                       // _rwLocker will contain references to
	                                       // Thread objects that don't serialize


  /**
   * constructs the zero sparse vector in n-dimensional space.
   * @param n int the number of dimensions
   * @throws IllegalArgumentException if n&le;0
   */
  public DblArray1SparseVectorMT(int n) throws IllegalArgumentException {
    super(n);
    _rwLocker = DMCoordinator.getInstance("DblArray1SparseVectorMT"+
			                                    DataMgr.getUniqueId());
  }


  /**
   * public constructor.
   * @param indices int[] must be in ascending order
   * @param values double[] corresponds to values for each index in the indices 
	 * array
   * @param n int total length of the vector
   * @throws IllegalArgumentException if any of indices or values is null or if
	 * they have different lengths or if n&le;indices[indices.length-1].
   */
  public DblArray1SparseVectorMT(int[] indices, double[] values, int n) 
		throws IllegalArgumentException {
    super(indices, values, n);
    _rwLocker = DMCoordinator.getInstance("DblArray1SparseVectorMT"+
			                                    DataMgr.getUniqueId());
  }


  /**
   * public constructor making a copy of the vector passed in, and multiplying
   * each element by the multFactor passed in.
   * @param indices int[] elements must be in ascending order
   * @param values double[]
   * @param n int total length of the vector
   * @param multFactor double
   * @throws IllegalArgumentException same as 3-argument constructor 
   */
  public DblArray1SparseVectorMT(int[] indices, double[] values, int n, 
		                             double multFactor) 
		throws IllegalArgumentException {
    super(indices, values, n, multFactor);
    _rwLocker = DMCoordinator.getInstance("DblArray1SparseVectorMT"+
			                                    DataMgr.getUniqueId());
  }

	
  /**
   * private constructor.
   * @param indices int[] must be in ascending order
   * @param values double[] corresponds to values for each index in the indices 
	 * array
   * @param n int total length of the vector
	 * @param ilen int
   * @throws IllegalArgumentException if any of indices or values is null or if
	 * they have different lengths or if n&le;indices[indices.length-1].
   */
  private DblArray1SparseVectorMT(int[] indices, double[] values, int n, 
		                              int ilen) throws IllegalArgumentException {
    super(0.0, indices, values, n, ilen);
    _rwLocker = DMCoordinator.getInstance("DblArray1SparseVectorMT"+
			                                    DataMgr.getUniqueId());
  }

	
  /**
   * private constructor making a copy of the vector passed in and multiplying
   * each element by the multFactor passed in.
   * @param indices int[] elements must be in ascending order
   * @param values double[]
   * @param n int total length of the vector
   * @param multFactor double
	 * @param ilen int
   * @throws IllegalArgumentException same as 3-argument constructor 
   */
  private DblArray1SparseVectorMT(int[] indices, double[] values, int n, 
		                              double multFactor, int ilen) 
		throws IllegalArgumentException {
    super(indices, values, n, multFactor, 0.0, ilen);
    _rwLocker = DMCoordinator.getInstance("DblArray1SparseVectorMT"+
			                                    DataMgr.getUniqueId());
  }


  /**
   * acquire the read-lock associated with this vector.
   */
  public void getReadLock() {
    _rwLocker.getReadAccess();
  }


  /**
   * release the read-lock associated with this vector.
   * @throws ParallelException if the current thread doesn't have the read-lock
   */
  public void releaseReadLock() throws ParallelException {
    _rwLocker.releaseReadAccess();
  }


  /**
   * acquire the write-lock associated with this vector.
   * @throws ParallelException if the current thread already has the read-lock
   * for this object, and there are other threads also having a read-lock on
   * this object.
   */
  public void getWriteLock() throws ParallelException {
    _rwLocker.getWriteAccess();
  }


  /**
   * release the read-lock associated with this vector.
   * @throws ParallelException if the current thread doesn't have the write-lock
   */
  public void releaseWriteLock() throws ParallelException {
    _rwLocker.releaseWriteAccess();
  }


  /**
   * return new VectorIntf object containing a copy of the data of this object.
   * @return VectorIntf
   */
  public VectorIntf newCopy() {
    try {
      _rwLocker.getReadAccess();
      if (getIndices()==null) {
        return new DblArray1SparseVectorMT(getNumCoords());
      } else return new DblArray1SparseVectorMT(getIndices(), getValues(),
                                                getNumCoords(), getILen());
    }
    finally {
      try {
        _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * create new copy of this vector, and multiply each component by the
   * multFactor argument.
   * @param multFactor double
   * @return VectorIntf
   */
  public VectorIntf newCopyMultBy(double multFactor) {
    try {
      _rwLocker.getReadAccess();
      if (getIndices()==null) 
				return new DblArray1SparseVectorMT(getNumCoords());
      else return new DblArray1SparseVectorMT(getIndices(), getValues(),
                                              getNumCoords(), multFactor, 
				                                      getILen());
    }
    finally {
      try {
        _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }

	
	/**
   * return a new VectorIntf object containing a copy of the data of this object
	 * guaranteeing that the returned object is un-managed (not part of any pool).
	 * @return VectorIntf
	 */
	public VectorIntf newInstance() {
    try {
      _rwLocker.getReadAccess();
      if (getIndices()==null) {
        return new DblArray1SparseVectorMT(getNumCoords());
      } else return new DblArray1SparseVectorMT(getIndices(), getValues(),
                                                getNumCoords(), getILen());
    }
    finally {
      try {
        _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }		
	}
	

  /**
   * return a DblArray1SparseVectorMT object containing as data the arg passed
   * in. The length of the argument specifies the number of dimensions of the
   * returned <CODE>DblArray1SparseVectorMT</CODE> object.
   * @param arg double[]
   * @throws IllegalArgumentException if arg==null
   * @return VectorIntf a DblArray1SparseVectorMT object.
   */
  public VectorIntf newInstance(double[] arg) throws IllegalArgumentException {
    if (arg==null) throw new IllegalArgumentException("null arg");
    final int n = arg.length;
    Vector inds = new Vector();
    Vector vals = new Vector();
    for (int i=0; i<n; i++) {
      if (Double.compare(arg[i], 0.0) != 0) {
        inds.add(new Integer(i));
        vals.add(new Double(arg[i]));
      }
    }
    final int ilen = inds.size();
    int[] indices = new int[ilen];
    for (int i=0; i<ilen; i++) indices[i] = ((Integer) inds.get(i)).intValue();
    double[] values = new double[ilen];
    for (int i=0; i<ilen; i++) values[i] = ((Double) vals.get(i)).doubleValue();
    //return new DblArray1SparseVectorMT(indices, values, n);
		// faster way below
		DblArray1SparseVectorMT r = new DblArray1SparseVectorMT(n);
		r.setIndices(indices);
		r.setValues(values);
		r.setILen(ilen);
		return r;
  }


  /**
   * return a double[] representing this VectorIntf object. Should not really
   * be used as it defeats the purpose of this implementation, but if the
   * array representation is absolutely needed, then this method will return it.
   * @return double[]
   */
  public double[] getDblArray1() {
    double[] x = new double[getNumCoords()];
    try {
      _rwLocker.getReadAccess();
      if (getIndices()==null) {  // vector at origin
        return x;
      }
			boolean exec_ok = false;
			synchronized (DblArray1SparseVectorMT.class) {
				exec_ok = _executor!=null && _executor.isLive();
			}
      if (exec_ok) {
        final int nt = _executor.getNumThreads();
        final int chunk_size = getILen()/nt;
        int j=0;
        List tasks = new ArrayList();
        for (int i=0; i<nt-1; i++) {
          PopulateTask ti = new PopulateTask(j, j+chunk_size-1, x);
          tasks.add(ti);
          j += chunk_size;
        }
        // last task
        PopulateTask tlast = new PopulateTask(j, getILen()-1, x);
        tasks.add(tlast);
        try {
          _executor.executeBatch(tasks);
          return x;
        }
        catch (ParallelException e) {
          e.printStackTrace();  // could happen if _executor is shutDown
          // revert to same-thread computation
          for (int i = 0; i < getILen(); i++) x[getIndices()[i]] = getValues()[i];
          return x;
        }
      } else {  // single-threaded computation
        for (int i = 0; i < getILen(); i++) x[getIndices()[i]] = getValues()[i];
        return x;
      }
    }
    finally {
      try {
        _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * return the i-th coordinate of this VectorIntf (i must be in the set
   * {0,1,2,...<CODE>getNumCoords()</CODE>-1}). Has O(log(_ilen)) worst-case
   * time-complexity where _ilen is the (max.) number of non-zeros in this
   * vector.
   * @param i int
   * @throws IndexOutOfBoundsException if i is not in the set mentioned above
   * @return double
   */
  public double getCoord(int i) throws IndexOutOfBoundsException {
    if (i<0 || i>=getNumCoords()) throw new IndexOutOfBoundsException("index "+i+" out of bounds");
    try {
      _rwLocker.getReadAccess();
      final int[] indices = getIndices();
      if (indices==null) return 0.0;  // vector at origin
      final double[] values = getValues();
      final int ilen = getILen();
      // requires a binary search in the indices.
      if (ilen==0 || i < indices[0] || i > indices[ilen - 1])return 0.0;
      else if (i == indices[0])return values[0];
      else if (i == indices[ilen - 1])return values[ilen - 1];
      int i1 = 0;
      int i2 = ilen;
      int ih = (i1 + i2) / 2;
      while (i1 < i2 && i1 < ih) {
        if (indices[ih] == i)return values[ih];
        else if (indices[ih] < i) i1 = ih;
        else i2 = ih;
        ih = (i1 + i2) / 2;
      }
      return 0.0;
    }
    finally {
      try {
        _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * set the i-th coordinate of this VectorIntf (i must be in the set
   * {0,1,2,...<CODE>getNumCoords()</CODE>-1}). Repeated calls to this method
   * for indices not in the original contruction of this vector will eventually
   * destroy its sparsity. Has O(_ilen/NT) worst-case time-complexity where 
	 * _ilen is the (max.) number of non-zeros in the array and NT is the number
   * of cores in the system. The time-complexity reduces to O(log(_ilen)) if the
   * element to be set, is already non-zero before the operation.
   * @param i int
   * @param val double
   * @throws IndexOutOfBoundsException if i is not in the set mentioned above
   * @throws ParallelException if a read-lock for this object is already
   * acquired by the current thread and some-other thread as well.
   */
  public void setCoord(int i, double val) 
		throws IndexOutOfBoundsException, ParallelException {
    if (i<0 || i>=getNumCoords()) 
			throw new IndexOutOfBoundsException("index "+i+" out of bounds");
    boolean got_lock=false;
    try {
			final boolean is_val_0 = Double.compare(val, 0.0)==0;
      _rwLocker.getWriteAccess();
      got_lock=true;
      // binary search in indices
      final int[] indices = getIndices();
      if (indices==null) {  // vector at origin
				if (is_val_0) return;
        int[] indices2 = new int[1];
        indices2[0] = i;
        double[] values2 = new double[1];
        values2[0] = val;
        setIndices(indices2);
        setValues(values2);
        incrILen();
        return;
      }
			if (getILen()==0) {  // but _indices, _values not null
	      if (is_val_0) return;
				final int[] inds = getIndices();
				final double[] values = getValues();
				inds[0]=i;
				values[0]=val;
				incrILen();
				return;
			}
      final double[] values = getValues();
      final int ilen = getILen();
      int i1 = 0;
      int i2 = ilen;
      if (indices[0] == i) {
        values[0] = val;
        return;
      }
      else if (indices[ilen - 1] == i) {
        values[ilen - 1] = val;
        return;
      }
      int ih = (i1 + i2) / 2;
      while (i1 < i2 && i1 < ih) {
        if (indices[ih] == i)break;
        else if (indices[ih] < i) i1 = ih;
        else i2 = ih;
        ih = (i1 + i2) / 2;
      }
      if (indices[ih] == i) {
        values[ih] = val;
        return;
      }
      else if (is_val_0) return; // no change
      // change is necessary
      // if needed, create new arrays to insert the value for <i,val> pair.
      if (ilen == indices.length) { // increase arrays' capacity 20%
        int[] indices2 = new int[ilen + ilen / 5 + 1];
        double[] values2 = new double[ilen + ilen / 5 + 1];
				boolean exec_ok = false;
				synchronized (DblArray1SparseVectorMT.class) {
					exec_ok = _executor!=null && _executor.isLive();
				}
        if (exec_ok && ilen > MIN_REQ_ILEN) {
          final int nt = _executor.getNumThreads();
          final int chunk_size = getILen() / nt;
          int k = 0;
          List tasks = new ArrayList();
          for (int ii = 0; ii < nt - 1; ii++) {
            SetCoordTask ti = new SetCoordTask(k, k + chunk_size - 1, indices2,
                                               values2, i, val);
            tasks.add(ti);
            k += chunk_size;
          }
          // last task
          SetCoordTask tlast = new SetCoordTask(k, getILen() - 1, indices2,
                                                values2, i, val);
          tasks.add(tlast);
          try {
            _executor.executeBatch(tasks);
            setIndices(indices2);
            setValues(values2);
            incrILen();
            return;
          }
          catch (ParallelException e) {
            e.printStackTrace(); // could happen if _executor is shutDown
            // revert to single-thread computation
            boolean first_time = true;
            for (int j = 0; j < ilen; j++) {
              if (indices[j] < i) {
                indices2[j] = indices[j];
                values2[j] = values[j];
              }
              else if (first_time) { // insert <i,val> pair
                indices2[j] = i;
                values2[j] = val;
                first_time = false;
                j--;
              }
              else {
                indices2[j + 1] = indices[j];
                values2[j + 1] = values[j];
              }
            }
            if (first_time) {
              indices2[ilen] = i;
              values2[ilen] = val;
            }
            setIndices(indices2);
            setValues(values2);
            incrILen();
          }
        }
        else {  // single-threaded computation
          boolean first_time = true;
          for (int j = 0; j < ilen; j++) {
            if (indices[j] < i) {
              indices2[j] = indices[j];
              values2[j] = values[j];
            }
            else if (first_time) { // insert <i,val> pair
              indices2[j] = i;
              values2[j] = val;
              first_time = false;
              j--;
            }
            else {
              indices2[j + 1] = indices[j];
              values2[j + 1] = values[j];
            }
          }
          if (first_time) {
            indices2[ilen] = i;
            values2[ilen] = val;
          }
          setIndices(indices2);
          setValues(values2);
          incrILen();
        }
      }
      else { // use same arrays as there is capacity
				boolean exec_ok = false;
				synchronized (DblArray1SparseVectorMT.class) {
					exec_ok = _executor!=null && _executor.isLive();
				}
        if (exec_ok && ilen > MIN_REQ_ILEN) {
          try {
            final int nt = _executor.getNumThreads();
            final int chunk_size = getILen() / nt;
            int k = 0;
            List tasks = new ArrayList();
            for (int ii = 0; ii < nt-1; ii++) {
              SetCoordTask2 ti = new SetCoordTask2(k, k+chunk_size-1, i, val);
              tasks.add(ti);
              k += chunk_size;
            }
            // last task
            SetCoordTask2 tlast = new SetCoordTask2(k, getILen()-1, i, val);
            tasks.add(tlast);
            _executor.executeBatch(tasks);
            incrILen();
            return;
          }
          catch (ParallelException e) {
            e.printStackTrace();
            // revert to single-thread computation
            int j;
            for (j = ilen - 1; j >= 0; j--) {
              if (indices[j] > i) {
                indices[j + 1] = indices[j];
                values[j + 1] = values[j];
              }
              else break;
            }
            indices[j + 1] = i;
            values[j + 1] = val;
            incrILen();
          }
        } else {  // single-threaded computation
          int j;
          for (j = ilen - 1; j >= 0; j--) {
            if (indices[j] > i) {
              indices[j + 1] = indices[j];
              values[j + 1] = values[j];
            }
            else break;
          }
          indices[j + 1] = i;
          values[j + 1] = val;
          incrILen();
        }
      }
    }
    finally {
        if (got_lock) _rwLocker.releaseWriteAccess();
    }
  }


  /**
   * the purpose of this routine is to allow a traversal of the non-zeros of
   * this object as follows:
   * <br>
	 * <pre>
   * <CODE>
   * sparsevector.getReadLock();
   * for (int i=0; i&lt;sparsevector.getNumNonZeros(); i++) {
   *   int    pos = sparsevector.getIthNonZeroPos(i);
   *   double val = sparsevector.getCoord(pos);
   *   // do whatever must be done...
   * }
   * sparsevector.releaseReadLock();
   * </CODE>
	 * </pre>
   * It is possible that one or more of the positions returned contain zero
   * values (but in the past they must have had non-zero value). But no position
   * with a current non-zero value will be missed.
   * @param i int
   * @throws IndexOutOfBoundsException if i is out-of-bounds. Always throws if
   * this is the zero vector.
   * @return int
   */
  public int getIthNonZeroPos(int i) throws IndexOutOfBoundsException {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock=true;
      if (i < 0 || i >= getILen())throw new IndexOutOfBoundsException(
          "index " + i + " out of bounds[0," + getILen() + "]");
      return getIndices()[i];
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * modifies this VectorIntf by adding the quantity m*other to it. This
   * operation may destroy the sparse nature of this object.
   * @param m double
   * @param other VectorIntf
   * @throws IllegalArgumentException if other is null or does not have the
   * same dimensions as this vector or if m is NaN
   * @throws ParallelException if the current thread and at least another have
   * already a read-lock on this object
   */
  public void addMul(double m, VectorIntf other) throws IllegalArgumentException,
      ParallelException {
    if (other==null || other.getNumCoords()!=getNumCoords() || Double.isNaN(m))
      throw new IllegalArgumentException("cannot call addMul(m,v) with v "+
                                         "having different dimensions than "+
                                         "this vector or with m being NaN.");
    boolean got_lock=false;
    try {
      _rwLocker.getWriteAccess();
      got_lock=true;
      final int n = getNumCoords();
      for (int i = 0; i < n; i++) {
        setCoord(i, getCoord(i) + m * other.getCoord(i));
      }
    }
    finally {
      if (got_lock) _rwLocker.releaseWriteAccess();
    }
  }


  /**
   * divide the components of this vector by the argument h.
   * @param h double
   * @throws IllegalArgumentException if h is zero
   * @throws ParallelException if the current thread and at least another have
   * already a read-lock on this object
   */
  public void div(double h) throws IllegalArgumentException, ParallelException {
    if (Double.isNaN(h) || Math.abs(h)<1.e-120)
      throw new IllegalArgumentException("division by (almost) zero or NaN");
    boolean got_lock = false;
    try {
      _rwLocker.getWriteAccess();
      got_lock=true;
      final int ilen = getILen();
      final double[] values = getValues();
      for (int i = 0; i < ilen; i++) {
        values[i] /= h;
      }
    }
    finally {
      if (got_lock) _rwLocker.releaseWriteAccess();
    }
  }


  /**
   * return true iff all components are zero.
   * @return boolean
   */
  public boolean isAtOrigin() {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final double[] values = getValues();
      final int ilen = getILen();
      for (int i = 0; i < ilen; i++) {
        if (Double.compare(values[i], 0.0) != 0)return false;
      }
      return true;
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * return a String representation of this VectorIntf object.
   * @return String
   */
  public String toString() {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock=true;
      return super.toString();
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * returns the number of non-zero elements in this vector.
   * @return int
   */
  public int getNumNonZeros() {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final int ilen = getILen();
      final double[] values = getValues();
      int res = 0;
      for (int i = 0; i < ilen; i++)
        if (Double.compare(values[i], 0.0) != 0) ++res;
      return res;
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * compute the inner product of this vector with the argument passed in.
   * The operation should be fast when this vector is sparse enough, as it only
   * goes through the non-zero elements of the vector.
   * @param other VectorIntf
   * @throws IllegalArgumentException
   * @return double
   */
  public double innerProduct(VectorIntf other) throws IllegalArgumentException {
    if (other==null || other.getNumCoords()!=getNumCoords())
      throw new IllegalArgumentException("dimensions don't match or null argument passed in");
    boolean got_lock=false;
    boolean do_single_thread=true;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final int ilen = getILen();
      final double[] values = getValues();
      final int[] indices = getIndices();
      if (indices==null) return 0;  // short-cut: vector at origin
			boolean exec_ok = false;
			synchronized (DblArray1SparseVectorMT.class) {
				exec_ok = _executor!=null && _executor.isLive();
			}
      if (exec_ok && ilen > MIN_REQ_ILEN) {
        try {
          do_single_thread=false;
          int nt = _executor.getNumThreads();
          int chunk_size = ilen/nt;
          List tasks = new ArrayList();
          int j=0;
          for (int i=0; i<nt-1; i++) {
            IPTask ti = new IPTask(j, j+chunk_size-1, other);
            tasks.add(ti);
            j += chunk_size;
          }
          // last task
          IPTask last = new IPTask(j, ilen-1, other);
          tasks.add(last);
          Vector sums = _executor.executeBatch(tasks);
          double sum = 0.0;
          for (int i=0; i<sums.size(); i++) {
            sum += ((Double) sums.elementAt(i)).doubleValue();
          }
          return sum;
        }
        catch (ParallelException e) {
          e.printStackTrace();
          do_single_thread=true;
        }
      }
      if (do_single_thread) {
        double sum = 0.0;
        for (int i = 0; i < ilen; i++) {
          sum += values[i] * other.getCoord(indices[i]);
        }
        return sum;
      }
      else return 0.0;  // avoid compiler incorrectly issuing error...
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * return the k-th norm of this vector.
   * @param k int
   * @throws IllegalArgumentException if k&le;0
   * @return double
   */
  public double norm(int k) throws IllegalArgumentException {
    if (k<=0) throw new IllegalArgumentException("k<=0");
    if (k==2) return norm2();  // faster computation
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final int ilen = getILen();
      final double[] values = getValues();
      double res = 0.0;
      for (int i = 0; i < ilen; i++) {
        double absxi = Math.abs(values[i]);
        res += Math.pow(absxi, k);
      }
      res = Math.pow(res, 1.0 / (double) k);
      return res;
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * short-cut for norm(2). Faster too.
   * @return double
   */
  public double norm2() {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final int ilen = getILen();
      final double[] values = getValues();
      double res2 = 0.0;
      for (int i = 0; i < ilen; i++) {
        double xi = values[i];
        res2 += (xi * xi);
      }
      return Math.sqrt(res2);
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * computes the infinity norm of this vector.
   * @return double
   */
  public double normInfinity() {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      final int ilen = getILen();
      final double[] values = getValues();
      double res = 0.0;
      for (int i = 0; i < ilen; i++) {
        final double absxi = Math.abs(values[i]);
        if (absxi > res) res = absxi;
      }
      return res;
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * return true iff the other vector is exactly equal to this, component-wise. 
	 * When invoked with another DblArray1SparseVectorMT argument, the read-locks 
	 * of both objects (this, and the argument) will be requested, and released in 
	 * the same order they were requested. Notice that the class inherits the
	 * <CODE>hashCode()</CODE> implementation from its super-class.
   * @param other Object
   * @return boolean
   */
  public boolean equals(Object other) {
    if (other==null || other instanceof VectorIntf == false) return false;
    VectorIntf o = (VectorIntf) other;
    if (o.getNumCoords() != getNumCoords())return false;
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      if (o instanceof DblArray1SparseVectorMT)
        return ((DblArray1SparseVectorMT) o).equalsAux(this);
      else return o.equals(this);
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  /**
   * should be called once, before use of the class instances. Sets the executor
   * to be used only if no other executor already exists.
   * @param numthreads int
   * @throws ParallelException
   */
  public synchronized static void setExecutor(int numthreads) 
		throws ParallelException {
    if (_executor==null) {
      _executor = PDBatchTaskExecutor.newPDBatchTaskExecutor(numthreads);
    }
  }


  /**
   * shuts down the associated executor (if previously set by a call to
   * <CODE>setExecutor(numthreads)</CODE>).
   * @throws ParallelException
   */
  public static synchronized void shutDownExecutor() throws ParallelException {
    if (_executor!=null) _executor.shutDown();
  }


  private boolean equalsAux(DblArray1SparseVectorMT o) {
    boolean got_lock=false;
    try {
      _rwLocker.getReadAccess();
      got_lock = true;
      for (int i=0; i<getILen(); i++) {
        if (Double.compare(getValues()[i], o.getCoord(getIndices()[i]))!=0) 
					return false;
      }
      for (int i=0; i<o.getILen(); i++) {
        if (Double.compare(o.getValues()[i], getCoord(o.getIndices()[i]))!=0) 
					return false;
      }
      return true;
    }
    finally {
      try {
        if (got_lock) _rwLocker.releaseReadAccess();
      }
      catch (ParallelException e) {
        e.printStackTrace();  // never gets here
      }
    }
  }


  // inner class
  class PopulateTask implements TaskObject {
    private static final long serialVersionUID = 2538498502236692883L;
    private int _start;
    private int _end;
    private double[] _x;
    private boolean _isDone;

    PopulateTask(int s, int e, double[] x) {
      _start = s;
      _end = e;
      _x = x;
    }

    public Serializable run() {
      for (int i=_start; i<=_end; i++) {
        _x[getIndices()[i]] = getValues()[i];
      }
      synchronized (this) {
        _isDone=true;
      }
      return this;
    }

    public synchronized boolean isDone() { return _isDone; }
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("operation not supported");
    }
  }


  class SetCoordTask implements TaskObject {
    private static final long serialVersionUID = 4947884997990379073L;
    private int _start;
    private int _end;
    private int _i;
    private double _val;
    private int[] _inds;
    private double[] _vals;
    private boolean _isDone;

    SetCoordTask(int s, int e, int[] inds, double[] vals, int i, double val) {
      _start = s;
      _end = e;
      _inds = inds;
      _vals = vals;
      _i = i;
      _val = val;
    }

    public Serializable run() {
      final int[] indices = getIndices();
      final double[] values = getValues();
      final int ilen = getILen();
      boolean first_time = true;
      if (indices[_start]>_i) first_time=false;  // another thread responsible for insertion
      for (int j=_start; j<=_end; j++) {
        if (indices[j] < _i) {
          _inds[j] = indices[j];
          _vals[j] = values[j];
        }
        else if (first_time) { // insert <i,val> pair
          _inds[j] = _i;
          _vals[j] = _val;
          first_time = false;
          j--;
        }
        else {
          _inds[j + 1] = indices[j];
          _vals[j + 1] = values[j];
        }
      }
      if (first_time && _end==ilen-1) {
        _inds[ilen] = _i;
        _vals[ilen] = _val;
      }
      synchronized (this) {
        _isDone=true;
      }
      return this;
    }

    public synchronized boolean isDone() { return _isDone; }
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("operation not supported");
    }
  }

  // inner class
  class SetCoordTask2 implements TaskObject {
    private static final long serialVersionUID = 5120583233723325394L;
    private int _start;
    private int _end;
    private int _i;
    private double _val;
    private boolean _isDone;

    SetCoordTask2(int s, int e, int i, double val) {
      _start = s;
      _end = e;
      _i = i;
      _val = val;
    }

    public Serializable run() {
      final int[] indices = getIndices();
      final double[] values = getValues();
      int j;
      for (j = _end; j >= _start; j--) {
        if (indices[j] > _i) {
          indices[j + 1] = indices[j];
          values[j + 1] = values[j];
        }
        else break;
      }
      if (j==-1 || indices[j]<_i) {
        indices[j + 1] = _i;
        values[j + 1] = _val;
      }
      synchronized (this) {
        _isDone=true;
      }
      return this;
    }

    public synchronized boolean isDone() { return _isDone; }
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("operation not supported");
    }
  }

  // inner class
  class IPTask implements TaskObject {
    private static final long serialVersionUID = 3477483560519826353L;
    private int _start;
    private int _end;
    private VectorIntf _other;
    private boolean _isDone;

    IPTask(int s, int e, VectorIntf other) {
      _start = s;
      _end = e;
      _other = other;
    }

    public Serializable run() {
      final int[] indices = getIndices();
      final double[] values = getValues();
      double sum = 0.0;
      for (int i=_start; i<=_end; i++) 
				sum += values[i]*_other.getCoord(indices[i]);
      synchronized (this) {
        _isDone=true;
      }
      return new Double(sum);
    }


    public synchronized boolean isDone() { return _isDone; }
    public void copyFrom(TaskObject t) throws IllegalArgumentException {
      throw new IllegalArgumentException("operation not supported");
    }
  }

}

