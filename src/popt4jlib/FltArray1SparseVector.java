package popt4jlib;

import parallel.ParallelException;
import java.util.Vector;


/**
 * sparse float[] implementation of VectorIntf, maintaining two arrays,
 * an index array, and a values array. Useful when dealing with very
 * high-dimensional vectors that are sparse to a significant degree (e.g. in
 * Information Retrieval, one may create vectors in 50.000 dimensions, but
 * each vector may have only a few tens or hundreds of non-zero components).
 * Notice that getCoord/setCoord operations can be much more costly than the
 * same operations when operating with a DblArray1Vector (where the operations
 * are O(1) -constant time simple memory accesses). The reason for representing
 * the values as a float[] is of course to save memory space (32-bits for
 * each float as opposed to 64-bit for every double).
 * Notice also that the class is not thread-safe, i.e. none of the methods can
 * be considered as reentrant in a multi-threaded environment. This choice is
 * made for speed considerations. Clients must therefore ensure on their own
 * that the application is race-condition free when using this class. 
 * Also notice that this class does not support default-values other than zero 
 * for components.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class FltArray1SparseVector implements SparseVectorIntf {
  //private static final long serialVersionUID = ;
  private int[] _indices=null;  // _indices[0] indicates the first non-zero element
  private float[] _values=null;  // _values[0] is the value of the _indices[0]-th element of the array
  private int _ilen;  // value of last non-zero element in _indices & _values arrays
  private int _n;  // vector dimension


  /**
   * constructs the zero sparse vector in n-dimensional space.
   * @param n int the number of dimensions
   * @throws IllegalArgumentException if n&le;0
   */
  public FltArray1SparseVector(int n) throws IllegalArgumentException {
    if (n<=0) throw new IllegalArgumentException("dimensions must be >= 1");
    _n = n;
  }


  /**
   * public constructor.
   * @param indices int[] must be in ascending order
   * @param values double[] corresponds to values for each index in the indices array
   * @param n int total length of the vector
   * @throws IllegalArgumentException if indices or values are null, or if their
	 * lengths differ, or if n&le;indices[indices.length-1] or if some component
	 * of values is zero
   */
  public FltArray1SparseVector(int[] indices, double[] values, int n) throws IllegalArgumentException {
    if (indices==null || values==null || indices.length!=values.length)
      throw new IllegalArgumentException("Arguments null or dimensions don't match");
    if (n<=indices[indices.length-1])
      throw new IllegalArgumentException("dimension mismatch");
    final int ilen = indices.length;
    _indices = new int[ilen];
    for (int i=0; i<ilen; i++) _indices[i] = indices[i];
    _values = new float[ilen];
    for (int i=0; i<ilen; i++) {
			_values[i] = (float) values[i];
			if (Float.compare(_values[i], 0.0f)==0)
				throw new IllegalArgumentException("zero component "+i);
		}
    _n = n;
    _ilen = ilen;
  }


  /**
   * public constructor making a copy of the vector passed in, and multiplying
   * each element by the multFactor passed in.
   * @param indices int[] elements must be in ascending order
   * @param values double[]
   * @param n int total length of vector
	 * @param ilen int
   * @param multFactor double
   * @throws IllegalArgumentException if indices or values are null, or if their
	 * lengths differ, or if n&le;indices[indices.length-1] or if some component 
	 * of values is zero
   */
  public FltArray1SparseVector(int[] indices, double[] values, int n, int ilen, double multFactor) throws IllegalArgumentException {
    if (indices==null || values==null || indices.length!=values.length)
      throw new IllegalArgumentException("Arguments null or dimensions don't match");
    if (n<=indices[indices.length-1])
      throw new IllegalArgumentException("dimension mismatch");
		if (Double.compare(multFactor, 0.0)==0) {
			_n = n;
			return;
		}
    //final int ilen = indices.length;
    _indices = new int[ilen];
    for (int i=0; i<ilen; i++) _indices[i] = indices[i];
    _values = new float[ilen];
    for (int i=0; i<ilen; i++) {
			_values[i] = (float) (values[i]*multFactor);
			if (Float.compare(_values[i], 0.0f)==0) 
				throw new IllegalArgumentException("zero component "+i);
		}
    _n = n;
    _ilen  = ilen;
  }


  /**
   * public constructor making a copy of the vector passed in, and multiplying
   * each element by the multFactor passed in.
   * @param indices int[] elements must be in ascending order
   * @param values float[]
   * @param n int total length of vector
	 * @param ilen int
   * @param multFactor float
   * @throws IllegalArgumentException if indices or values are null, or if their
	 * lengths differ, or if n&le;indices[indices.length-1] or if some component 
	 * of values is zero
   */
  public FltArray1SparseVector(int[] indices, float[] values, int n, int ilen, float multFactor) throws IllegalArgumentException {
    if (indices==null || values==null || indices.length!=values.length)
      throw new IllegalArgumentException("Arguments null or dimensions don't match");
    if (n<=indices[indices.length-1])
      throw new IllegalArgumentException("dimension mismatch");
		if (Float.compare(multFactor, 0.0f)==0) {
			_n=n;
			return;
		}
    //final int ilen = indices.length;
    _indices = new int[ilen];
    for (int i=0; i<ilen; i++) _indices[i] = indices[i];
    _values = new float[ilen];
    for (int i=0; i<ilen; i++) {
			_values[i] = values[i]*multFactor;
			if (Float.compare(_values[i], 0.0f)==0)
				throw new IllegalArgumentException("zero component "+i);
		}
    _n = n;
    _ilen  = ilen;
  }


  /**
   * return a new VectorIntf object containing a copy of the data of this object.
   * @return VectorIntf
   */
  public VectorIntf newCopy() {
    if (_indices==null) return new FltArray1SparseVector(_n);
    return new FltArray1SparseVector(_indices, _values, _n, _ilen, 1.0f);
  }


  /**
   * create new copy of this vector, and multiply each component by the
   * multFactor argument.
   * @param multFactor double
   * @return VectorIntf
   */
  public VectorIntf newCopyMultBy(double multFactor) {
    if (_indices==null) return new FltArray1SparseVector(_n);
    return new FltArray1SparseVector(_indices, _values, _n, _ilen, (float) multFactor);
  }
	
	
	/**
	 * return a new VectorIntf object containing a copy of the data of this object
	 * guaranteeing that the returned object is un-managed (not part of a pool).
	 * @return VectorIntf
	 */
	public VectorIntf newInstance() {
    if (_indices==null) return new FltArray1SparseVector(_n);
    return new FltArray1SparseVector(_indices, _values, _n, _ilen, 1.0f);		
	}


  /**
   * return a FltArray1SparseVector object containing as data the arg passed in.
   * Linear complexity in the length of the argument array.
   * @param arg double[]
   * @throws IllegalArgumentException
   * @return VectorIntf
   */
  public VectorIntf newInstance(double[] arg) throws IllegalArgumentException {
    if (arg==null) throw new IllegalArgumentException("null arg");
    final int n = arg.length;
    Vector inds = new Vector();
    Vector vals = new Vector();
    for (int i=0; i<n; i++) {
      if (Float.compare((float) arg[i], 0.0f) != 0) {
        inds.add(new Integer(i));
        vals.add(new Double(arg[i]));
      }
    }
    final int ilen = inds.size();
    int[] indices = new int[ilen];
    for (int i=0; i<ilen; i++) indices[i] = ((Integer) inds.elementAt(i)).intValue();
    double[] values = new double[ilen];
    for (int i=0; i<ilen; i++) values[i] = ((Double) vals.elementAt(i)).doubleValue();
    return new FltArray1SparseVector(indices, values, n);
  }


  /**
   * return the number of coordinates of this VectorIntf object.
   * @return int
   */
  public int getNumCoords() { return _n; }

	
	/**
	 * default value is always zero.
	 * @return double // zero
	 */
	public double getDefaultValue() {
		return 0;
	}
	

  /**
   * return a double[] representing this VectorIntf object. Should not really
   * be used as it defeats the purpose of this implementation, but if the
   * array representation is absolutely needed, then this method will return it.
   * @return double[]
   */
  public double[] getDblArray1() {
    double[] x = new double[_n];
    for (int i=0; i<_ilen; i++) x[_indices[i]] = _values[i];
    return x;
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
    if (i<0 || i>=_n) throw new IndexOutOfBoundsException("index "+i+" out of bounds");
    if (_indices==null) return 0.0;
    // requires a binary search in the indices.
    if (i<_indices[0] || i>_indices[_ilen-1]) return 0.0;
    else if (i==_indices[0]) return _values[0];
    else if (i==_indices[_ilen-1]) return _values[_ilen-1];
    int i1 = 0;
    int i2 = _ilen;
    int ih = (i1+i2)/2;
    while (i1<i2 && i1<ih) {
      if (_indices[ih] == i) return _values[ih];
      else if (_indices[ih] < i) i1 = ih;
      else i2 = ih;
      ih = (i1+i2)/2;
    }
    return 0.0;
  }


  /**
   * set the i-th coordinate of this VectorIntf (i must be in the set
   * {0,1,2,...<CODE>getNumCoords()</CODE>-1}). Repeated calls to this method
   * for indices not in the original construction of this vector will eventually
   * destroy its sparsity. Has O(_ilen) worst-case time-complexity where _ilen
   * is the (max.) number of non-zeros in the array. The time-complexity
   * reduces to O(log(_ilen)) if the element to be set, is already non-zero
   * before the operation.
   * @param i int
   * @param val double
   * @throws IndexOutOfBoundsException if i is not in the set mentioned above
   * @throws ParallelException -never throws this exception
   */
  public void setCoord(int i, double val) throws IndexOutOfBoundsException, ParallelException {
    if (i<0 || i>=_n) throw new IndexOutOfBoundsException("index "+i+" out of bounds");
    final boolean is_val_0 = Float.compare((float)val,0.0f)==0;
		if (_indices==null) {
      if (is_val_0) return;  // don't do anything
      _indices = new int[1];
      _indices[0] = i;
      _values = new float[1];
      _values[0] = (float) val;
      _ilen=1;
      return;
    }
		if (_ilen==0) {  // but _indices, _values not null
			if (is_val_0) return;
			_indices[0]=i;
			_values[0]=(float)val;
			_ilen=1;
			return;
		}    
    // binary search in indices
    final int ilen = _indices.length;
    int i1 = 0;
    int i2 = _ilen;
    if (_indices[0]==i) {
      if (is_val_0) shrink(0);
      else _values[0] = (float) val;
      return;
    } else if (_indices[_ilen-1] == i) {
      if (is_val_0) shrink(_ilen-1);
      else _values[_ilen-1] = (float) val;
      return;
    }
    int ih = (i1+i2)/2;
    while (i1<i2 && i1<ih) {
      if (_indices[ih] == i) break;
      else if (_indices[ih] < i) i1 = ih;
      else i2 = ih;
      ih = (i1+i2)/2;
    }
    if (_indices[ih]==i) {
      if (is_val_0) shrink(ih);
      else _values[ih] = (float) val;
      return;
    }
    else if (is_val_0) return;  // no change
    // change is necessary
    // if needed, create new arrays to insert the value for <i,val> pair.
    if (_ilen == ilen) {  // increase arrays' capacity 20%
      int[] indices = new int[ilen + ilen / 5 + 1];
      float[] values = new float[ilen + ilen / 5 + 1];
      boolean first_time = true;
      for (int j = 0; j < ilen; j++) {
        if (_indices[j] < i) {
          indices[j] = _indices[j];
          values[j] = _values[j];
        }
        else if (first_time) { // insert <i,val> pair
          indices[j] = i;
          values[j] = (float) val;
          first_time = false;
          j--;
        }
        else {
          indices[j + 1] = _indices[j];
          values[j + 1] = _values[j];
        }
      }
      if (first_time) {
        indices[ilen] = i;
        values[ilen] = (float) val;
      }
      _indices = indices;
      _values = values;
    }
    else {  // use same arrays as there is capacity
      int j;
      for (j=_ilen-1; j>=0; j--) {
        if (_indices[j]>i) {
          _indices[j+1] = _indices[j];
          _values[j+1] = _values[j];
        }
        else break;
      }
      _indices[j+1] = i;
      _values[j+1] = (float) val;
    }
    ++_ilen;
  }


  /**
   * the purpose of this routine is to allow a traversal of the non-zeros of
   * this object as follows:
   * <br>
	 * <pre>
   * <CODE>
   * for (int i=0; i&lt;sparsevector.getNumNonZeros(); i++) {
   *   int    pos = sparsevector.getIthNonZeroPos(i);
   *   double val = sparsevector.getIthNonZeroVal(i);
   * }
   * </CODE>
	 * </pre>
   * @param i int.
   * @throws IndexOutOfBoundsException if i is out-of-bounds. Always throws if
   * this is the zero vector.
   * @return int
   */
  public int getIthNonZeroPos(int i) throws IndexOutOfBoundsException {
    if (i<0 || i>=_ilen) throw new IndexOutOfBoundsException("index "+i+" out of bounds[0,"+_ilen+"] Vector is: "+this);
    return _indices[i];
  }

	
	/**
	 * get the i-th non-default value of this vector.
	 * @param i int
	 * @return double
	 * @throws IndexOutOfBoundsException if i is out-of-bounds. Always throws if
	 * this is the default vector.
	 */
	public double getIthNonZeroVal(int i) throws IndexOutOfBoundsException {
    if (i<0 || i>=_ilen) 
			throw new IndexOutOfBoundsException("index "+i+" out of bounds[0,"+
				                                  _ilen+"] Vector is: "+this);
    return _values[i];		
	}


  /**
   * modifies this VectorIntf by adding the quantity m*other to it. This
   * operation may destroy the sparse nature of this object.
   * @param m double
   * @param other VectorIntf
   * @throws IllegalArgumentException if other is null or does not have the
   * same dimensions as this vector or if m is NaN
   * @throws ParallelException -never throws this exception
   */
  public void addMul(double m, VectorIntf other) throws IllegalArgumentException, ParallelException {
    if (other==null || other.getNumCoords()!=_n || Double.isNaN(m))
      throw new IllegalArgumentException("cannot call addMul(m,v) with v "+
                                         "having different dimensions than "+
                                         "this vector or with m being NaN.");
    for (int i=0; i<_n; i++) {
      setCoord(i, getCoord(i)+m*other.getCoord(i));
    }
  }


  /**
   * divide the components of this vector by the argument h.
   * @param h double
   * @throws IllegalArgumentException if h is (almost) zero
   * @throws ParallelException -never throws this exception.
   */
  public void div(double h) throws IllegalArgumentException, ParallelException {
    if (Double.isNaN(h) || Math.abs(h)<1.e-120)
      throw new IllegalArgumentException("division by (almost) zero or NaN");
    float h1 = (float) h;
    for (int i=0; i<_ilen; i++) {
      _values[i] /= h1;
    }
  }


  /**
   * return true iff all components are zero.
   * @return boolean
   */
  public boolean isAtOrigin() {
    return _ilen==0;
  }


  /**
   * return a String representation of this VectorIntf object.
   * @return String
   */
  public String toString() {
    String x="[";
    for (int i=0; i<_ilen-1; i++) x += "("+_indices[i]+","+_values[i]+")"+", ";
    if (_ilen>0) x += "("+_indices[_ilen-1]+","+_values[_ilen-1]+")";
    x += "]";
    return x;
  }


  /**
   * returns the number of non-zero elements in this vector.
   * @return int
   */
  public int getNumNonZeros() {
    return _ilen;
  }


  /**
   * compute the inner product of this vector with the argument passed in.
   * The operation should be fast when this vector is sparse enough, as it only
   * goes through the non-zero elements of the vector.
   * @param other VectorIntf
   * @throws IllegalArgumentException if other is null of other's dimensions is
	 * different from this vector's.
   * @return double
   */
  public double innerProduct(VectorIntf other) throws IllegalArgumentException {
    if (other==null || other.getNumCoords()!=_n)
      throw new IllegalArgumentException("dimensions don't match or null argument passed in");
    double sum=0.0;
    for (int i=0; i<_ilen; i++) {
      sum += _values[i]*other.getCoord(_indices[i]);
    }
    return sum;
  }


  /**
   * return the k-th norm of this vector.
   * @param k int
   * @throws IllegalArgumentException if x==null or if k&le;0
   * @return double
   */
  public double norm(int k) throws IllegalArgumentException {
    if (k<=0) throw new IllegalArgumentException("k<=0");
    if (k==2) return norm2();  // faster computation
    double res = 0.0;
    for (int i=0; i<_ilen; i++) {
      double absxi = Math.abs(_values[i]);
      res += Math.pow(absxi,k);
    }
    res = Math.pow(res, 1.0/(double) k);
    return res;
  }


  /**
   * short-cut for norm(2). Faster too.
   * @throws IllegalArgumentException if x==null
   * @return double
   */
  public double norm2() throws IllegalArgumentException {
    double res2=0.0;
    for (int i=0; i<_ilen; i++) {
      double xi = _values[i];
      res2 += (xi * xi);
    }
    return Math.sqrt(res2);
  }


  /**
   * computes the infinity norm of this vector.
   * @return double
   */
  public double normInfinity() {
    double res = 0.0;
    for (int i=0; i<_ilen; i++) {
      final double absxi = Math.abs(_values[i]);
      if (absxi>res) res = absxi;
    }
    return res;
  }


  /**
   * return true iff the other vector is exactly equal to this.
   * @param other Object
   * @return boolean
   */
  public boolean equals(Object other) {
    if (other==null || other instanceof VectorIntf == false) return false;
    VectorIntf o = (VectorIntf) other;
    if (o.getNumCoords()!=_n) return false;
    if (other instanceof SparseVectorIntf) {  // short-cut
      SparseVectorIntf osv = (SparseVectorIntf) other;
      for (int i = 0; i < _ilen; i++)
        if (Double.compare(_values[i], osv.getCoord(_indices[i])) != 0)
          return false;
      for (int i = 0; i < osv.getNumNonZeros(); i++) {
        int pi = osv.getIthNonZeroPos(i);
        if (Double.compare(osv.getCoord(pi), getCoord(pi)) != 0)
          return false;
      }
      return true;
    }
    else return o.equals(this);  // no short-cut
  }
	
	
	/**
	 * return the integer part of the first value of the first non-zero element of 
	 * this vector, if it exists, else return zero.
	 * @return int
	 */
	public int hashCode() {
		return _ilen > 0 ? (int) _values[0] : 0;
	}


  /**
   * reduce the _indices and _values arrays and the _ilen value by removing
   * the value at position pos.
   * @param pos int
   */
  private void shrink(int pos) {
    for (int i=pos; i<_ilen-1; i++) {
      _indices[i] = _indices[i+1];
      _values[i] = _values[i+1];
    }
    --_ilen;
  }
}

