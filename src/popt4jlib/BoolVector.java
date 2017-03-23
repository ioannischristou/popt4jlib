package popt4jlib;

//import parallel.ParallelBatchTaskExecutor;
//import parallel.TaskObject;
//import parallel.ParallelException;
import java.util.Set;
import java.util.Iterator;
import java.util.Arrays;
//import java.util.List;
//import java.util.ArrayList;
import java.io.Serializable;


/**
 * represents a vector of booleans (alternative to BitSet, or Set&lt;Long&gt;
 * that in fact may hold bit-sets of arbitrarily large size).
 * Original version by Daniel Lemire on GitHub:
 * https://github.com/lemire/Code-used-on-Daniel-Lemire-s-blog/blob/master/2012/11/13/src/StaticBitSet.java
 * which however is now greatly enhanced with significantly more functionality.
 * Notes:
 * <ul>
 * <li>The class is not thread-safe (to avoid paying locking costs).
 * Clients must ensure no race-conditions exist when using this class.
 * <li> 2016-01-27: The <CODE>and(BoolVector), or(BoolVector)</CODE> operations
 * execute in parallel in the face of very large data lengths (above 1mio bits
 * stored in both vectors to be and-ed). The implementation is modeled after the 
 * pattern in <CODE>popt4jlib.MSSC.GMeansMTClusterer</CODE> which turned out to 
 * be almost twice as fast as the generic 
 * <CODE>parallel.ParallelBatchTaskExecutor</CODE>.
 * <li> 2017-02-10: The class is made serializable so that it can be used with
 * <CODE>graph.packing.DBBNode*</CODE> to store the node-ids of a partial soln
 * instead of using expensive HashSet's.
 * </ul>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoolVector implements Serializable, Comparable {
  private final static int _MIN_CAPACITY_REQD_4_PARALLEL_OP = 1000000;
  private final static int _NUM_THREADS = 4;
  private long[] _data;
  private int _numSetBits = -1; // cache to cardinality
	private int _lastSetBit = -1;  // cache to last set bit

	
  private volatile static BVThread[] _threads = new BVThread[_NUM_THREADS];
  static {
    for (int i = 0; i < _NUM_THREADS; i++) {
      _threads[i] = new BVThread(new BVAux());
      _threads[i].setDaemon(true);
      _threads[i].start();
    }
  }

	
  /**
   * shut-down this class's thread-pool. Method is not thread-safe, and clients
   * must ensure that the method is called in a safe manner.
   */
  public static void shutDownThreadPool() {
    for (int i = 0; i < _NUM_THREADS; i++) {
      _threads[i].getBVAux().setFinish();
    }
    _threads = null;
  }

	
  /**
   * public constructor, specifying the max number of bits this vector may hold.
   * @param sizeinbits int
   */
  public BoolVector(int sizeinbits) {
    _data = new long[ (sizeinbits + 63) / 64];
    _numSetBits = 0;
  }

	
	/**
	 * public constructor, specifying both the max number of bits this vector may
	 * hold, and the initial bits set for it, held as integers in the first 
	 * argument.
	 * @param bits Set  // Set&lt;Integer&gt;
	 * @param sizeinbits int 
	 */
	public BoolVector(Set bits, int sizeinbits) {
		this(sizeinbits);
		Iterator it = bits.iterator();
		while (it.hasNext()) {
			int b = ((Integer) it.next()).intValue();
			set(b);
		}
	}

	
  /**
   * public copy constructor.
   * @param other BoolVector
   */
  public BoolVector(BoolVector other) {
    _data = new long[other._data.length];
    for (int i = 0; i < _data.length; i++) _data[i] = other._data[i];
		_numSetBits = other._numSetBits;
		_lastSetBit = other._lastSetBit;
  }
	
	
  /**
   * resets all bits in this vector.
   */
  public void clear() {
    Arrays.fill(_data, 0);
    _numSetBits = 0;
		_lastSetBit = -1;
  }

	
  /**
   * copy the contents of the other BoolVector into this object.
   * @param other BoolVector
   * @throws IllegalArgumentException if the argument is of different size than
   * this.
   */
  public void copy(BoolVector other) throws IllegalArgumentException {
    if (_data.length != other._data.length)
      throw new IllegalArgumentException(
          "argument has different size than this");
    // ok do the copy
    for (int i = 0; i < _data.length; i++) {
      _data[i] = other._data[i];
    }
    _numSetBits = other._numSetBits;
		_lastSetBit = other._lastSetBit;
  }

	
  /**
   * returns the maximum number of bits this vector may hold.
   * @return int
   */
  public int capacity() {
    return _data.length * 64;
  }

	
  /**
   * resize this vector to hold at least as many bits as the argument.
   * @param sizeinbits int
   */
  public void resize(int sizeinbits) {
    if (sizeinbits > _data.length * 64) {
      //_data = Arrays.copyOf(_data, (sizeinbits + 63) / 64);  // JDK 1.6 method
      long[] tmp = new long[ (sizeinbits + 63) / 64];
      for (int i = 0; i < _data.length; i++) tmp[i] = _data[i]; // the rest is zero
      _data = tmp;
      // _numSetBits = -1;  // no reason invalidate cache
    }
  }

	
  /**
   * returns the number of set bits in this vector.
   * @return int the number of set bits in this vector.
   */
  public int cardinality() {
    if (_numSetBits >= 0) return _numSetBits;
    int sum = 0;
    //for(long l : _data) {  // JDK 1.5 method
    for (int i = 0; i < _data.length; i++) {
      long l = _data[i];
      // sum += Long.bitCount(l);  // JDK 1.5 method
      sum += bitCount(l);
    }
    _numSetBits = sum;
    return sum;
  }

	
  /**
   * return true iff the i-th bit is set in this vector.
   * @param i int
   * @return boolean
   * @throws IndexOutOfBoundsException if i is not in [0,capacity()-1]
   */
  public boolean get(int i) throws IndexOutOfBoundsException {
    return (_data[i / 64] & (1l << (i % 64))) != 0;
  }

	
  /**
   * set the i-th bit of this vector.
   * @param i int
   * @throws IndexOutOfBoundsException if i is not in [0,capacity()-1]
   */
  public void set(int i) throws IndexOutOfBoundsException {
    _data[i / 64] |= (1l << (i % 64));
    _numSetBits = -1; // invalidate cache
		if (i>_lastSetBit && _lastSetBit>=0) _lastSetBit = i;
  }

	
  /**
   * unset the i-th bit of this vector.
   * @param i int
   * @throws IndexOutOfBoundsException if i is not in [0,capacity()-1]
   */
  public void unset(int i) throws IndexOutOfBoundsException {
    _data[i / 64] &= ~ (1l << (i % 64));
    _numSetBits = -1; // invalidate cache
		if (i==_lastSetBit) _lastSetBit = -1;  // invalidate cache
  }

	
  /**
   * set the i-th bit of this vector to the value b.
   * @param i int
   * @param b boolean
   * @throws IndexOutOfBoundsException if i is not in [0,capacity()-1]
   */
  public void set(int i, boolean b) throws IndexOutOfBoundsException {
    if (b) set(i);
    else unset(i);
    // no reason to invalidate caches here again: set/unset do that already
  }
	
	
	/**
	 * set the bits of all indices contained in the argument.
	 * @param bits Set  // Set&lt;Integer&gt;
	 * @throws IndexOutOfBoundsException if any of the numbers in the set is not
	 * in the range [0,capacity()-1]
	 */
	public void setAll(Set bits) throws IndexOutOfBoundsException {
		Iterator it = bits.iterator();
		while (it.hasNext()) {
			Integer i = (Integer) it.next();
			set(i.intValue());
		}
	}

	
  /**
   * method used to iterate over the set-bits in this vector. Use as in the
   * following example:
   * <pre>
   * <CODE>
   * for(int i=bv.nextSetBit(0); i&gt;=0; i=bv.nextSetBit(i+1)) {
   *   // operate on index i
   * }
   * </CODE>
   * </pre>
   * @param i int
   * @return int -1 when there is no set bit after the input argument position.
   */
  public int nextSetBit(int i) {
    int x = i / 64;
    if (x >= _data.length)return -1;
    long w = _data[x];
    w >>>= (i % 64);
    if (w != 0) {
      // return i + Long.numberOfTrailingZeros(w);  // JDK 1.5 method
      return i + numberOfTrailingZeros(w);
    }
    ++x;
    for (; x < _data.length; ++x) {
      if (_data[x] != 0) {
        // return x * 64 + Long.numberOfTrailingZeros(_data[x]);  // JDK 1.5 method
        return x * 64 + numberOfTrailingZeros(_data[x]);
      }
    }
    return -1;
  }
	
	
	/**
	 * get the last bit position that is currently set.
	 * @return int will return -1 if no bit is set
	 */
	public int lastSetBit() {
		if (_lastSetBit>=0) return _lastSetBit;  // use cache
		if (cardinality()==0) return -1;
		// cache invalid, do the work
		int res = -1;
		for (int i=nextSetBit(0); i>=0; i=nextSetBit(i+1)) {
			res = i;
		}
		_lastSetBit = res;
		return _lastSetBit;
	}

	
	/**
	 * check if all bits set in other are also set in this bit-vector.
	 * @param other BoolVector
	 * @return boolean
	 * @throws IllegalArgumentException if other is null 
	 */
	public boolean containsAll(BoolVector other) throws IllegalArgumentException {
    if (other == null)throw new IllegalArgumentException(
        "null argument passed in");
		long[] one = _data;
		long[] two = other._data;
		int minlen = Math.min(_data.length, other._data.length);
		for (int i=0; i<minlen; i++) {
			long oi = one[i] & two[i];
			int coi = bitCount(oi);
			int toi = bitCount(two[i]);
			if (coi < toi) return false; 
		}
		if (one.length>=two.length) return true;
		for (int i=one.length; i<two.length-1; i++) {
			if (two[i]!=0L) return false;
		}
		return true;
	}
	
	
  /**
   * equivalent to the retainAll(set) operation on Set. Utilizes parallel
   * processing when the _data array length is long enough.
   * @param other BoolVector
   * @throws IllegalArgumentException if argument is null
   */
  public void and(BoolVector other) throws IllegalArgumentException {
    if (other == null)throw new IllegalArgumentException(
        "null argument passed in");
    if (_data.length >= _MIN_CAPACITY_REQD_4_PARALLEL_OP &&
        other._data.length >= _MIN_CAPACITY_REQD_4_PARALLEL_OP && _threads != null) {
      andParallel2(other);
      return;
    }
    if (other.cardinality() == 0) {
      clear();
      return;
    }
    if (cardinality() < _data.length / 2) {
      for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
        if (i >= other.capacity())break;
        if (!other.get(i)) unset(i);
      }
      // no reason to invalidate caches here; if it was needed, unset(i) did it
    }
    else { // probably this is faster
      for (int i = 0; i < _data.length; i++) {
        if (other._data.length <= i)break;
        _data[i] &= other._data[i];
      }
      _numSetBits = -1; // invalidate cache
			_lastSetBit = -1;  // invalidate cache
    }
  }

	
  /**
   * parallel version of the <CODE>and()</CODE> operation, with the
   * same semantics and signature (except exceptions thrown) as the serial
   * version.
   * @param other BoolVector
   * @throws IllegalStateException if the thread-pool has been shut-down.
   */
  private void andParallel2(BoolVector other) throws IllegalStateException {
    if (_threads == null)throw new IllegalStateException(
        "pool has been shut-down");
    final int chunk_size = _data.length / _NUM_THREADS;
    final int min_len = Math.min(_data.length, other._data.length);
    int start = 0;
    int end = start + chunk_size;
    if (end > other._data.length) end = other._data.length;
    int i = 0;
    // prepare and run tasks
    while (end <= min_len) {
      BVAux ati = _threads[i++].getBVAux();
      ati.runFromTo(start, end, _data, other._data, BVAux._AND_OP);
      start = end;
      end += chunk_size;
    }
    if (i != _NUM_THREADS)throw new Error("wrong counting in parallelAnd(bv)");
    // wait for tasks to end
    for (i = 0; i < _NUM_THREADS; i++) {
      _threads[i].getBVAux().waitForTask();
    }
    if (end > min_len) end = min_len;
    for (i = end; i < _data.length; i++) _data[i] = 0L;  // finalize
    _numSetBits = -1;  // invalidate cache
		_lastSetBit = -1;  // invalidate cache
  }

	
  /**
   * equivalent to the addAll(set) operation on Set. Utilizes parallel
   * processing when the _data array length is long enough. The _data array will
   * expand if necessary as a result of this operation.
   * @param other BoolVector
   * @throws IllegalArgumentException if argument is null
   */
  public void or(BoolVector other) throws IllegalArgumentException {
    if (other == null)throw new IllegalArgumentException(
        "null argument passed in");
    if (other._data.length > _data.length) resize(other.capacity());
    if (_data.length >= _MIN_CAPACITY_REQD_4_PARALLEL_OP &&
        other._data.length >= _MIN_CAPACITY_REQD_4_PARALLEL_OP && 
			  _threads != null) {
      orParallel2(other);
      return;
    }
    if (other.cardinality() == 0) {
      return;
    }
    for (int i = 0; i < _data.length; i++) {
      if (other._data.length <= i)break;
      _data[i] |= other._data[i];
    }
    _numSetBits = -1;  // invalidate cache
		_lastSetBit = -1;  // invalidata cache
  }

	
  /**
   * parallel version of the <CODE>or()</CODE> operation. The BoolVector must
   * have enough capacity to hold the result of the union with the other object.
   * @param other BoolVector
   * @throws IllegalStateException if the thread-pool has been shut-down.
   */
  private void orParallel2(BoolVector other) throws IllegalStateException {
    if (_threads == null)throw new IllegalStateException(
        "pool has been shut-down");
    final int min_len = other._data.length;
    final int chunk_size = min_len / _NUM_THREADS;
    int start = 0;
    int end = start + chunk_size;
    int i = 0;
    // prepare and run tasks
    while (end <= min_len) {
      BVAux ati = _threads[i++].getBVAux();
      ati.runFromTo(start, end, _data, other._data, BVAux._OR_OP);
      start = end;
      end += chunk_size;
    }
    if (i != _NUM_THREADS)throw new Error("wrong counting in orParallel2(bv)");
    // wait for tasks to end
    for (i = 0; i < _NUM_THREADS; i++) {
      _threads[i].getBVAux().waitForTask();
    }
    _numSetBits = -1;  // invalidate cache
		_lastSetBit = -1;  // invalidate cache
  }
	
	
	/**
	 * returns true if and only if the <CODE>_data</CODE> data members of this
	 * vector and the argument are exactly equal, both in length, and in element
	 * values.
	 * @param o Object  // must be BoolVector
	 * @return boolean
	 */
	public boolean equals(Object o) {
		if (o instanceof BoolVector == false) return false;
		BoolVector other = (BoolVector) o;
		if (_data.length != other._data.length) return false;
		for (int i=0; i<_data.length; i++) {
			if (_data[i]!=other._data[i]) return false;
		}
		return true;
	}
	
	
	/**
	 * returns the integer part of the sum of the first and last element of the 
	 * <CODE>_data</CODE> array.
	 * @return int  // (int) (_data[0]+_data[_data.length-1])
	 */
	public int hashCode() {
		return (int) (_data[0]+_data[_data.length-1]);
	}

	
	/**
	 * element-wise comparison between two bit-vectors. empty vector comes first.
	 * If the argument has different size, then assuming the common data are the
	 * same, the vector with the less data-length comes first.
	 * @param o Object  // BoolVector
	 * @return int -1 if this vector comes before o in element-wise number order, 
	 * 0 if they are the same
	 */
	public int compareTo(Object o) {
		BoolVector other = (BoolVector) o;
		int min_len = Math.min(_data.length, other._data.length);
		for (int i=0; i<min_len; i++) {
			long di = _data[i];
			long oi = other._data[i];
			int comp = Long.compare(di, oi);
			if (comp!=0) return comp;
		}
		// check lengths
		return Integer.compare(_data.length, other._data.length);
	}

	
	/**
	 * prints out the elements of _data as long values. Mostly used for debugging
	 * purposes.
	 * @return String
	 */
	public String toString() {
		String result="[";
		for (int i=_data.length-1; i>=0; i--) {
			result += "d["+i+"]="+_data[i];
			if (i>0) result += ",";
		}
		result += "]";
		return result;
	}
	
	
  /*
    private static int bitCountSlow(long l) {
   // this loop takes time proportional to the number of bits set in l.
   int count;
     for (count = 0; l > 0; ++count) {
       l &= l - 1;
     }
   return count;
    }
   */


  /**
   * method exists because we need the library to compile under JDK 1_4.
   * Code directly copied from Open JDK 1.8.
   * Notice that even though the code is identical to the source code of the
   * JVM's code, when testing, it is about 2 times slower than calling
   * <CODE>Long.bitCount(long l)</CODE> under JRE 1.7, probably due to
   * compiler optimizations.
   * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/8u40-b25/java/lang/Long.java#Long.bitCount%28long%29">code repository</a>
   * @param i long
   * @return int the number of bits set in the 2's complement representation of
   * the number.
   */
  private static int bitCount(long i) {
    i = i - ( (i >>> 1) & 0x5555555555555555L);
    i = (i & 0x3333333333333333L) + ( (i >>> 2) & 0x3333333333333333L);
    i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
    i = i + (i >>> 8);
    i = i + (i >>> 16);
    i = i + (i >>> 32);
    return (int) i & 0x7f;
  }

	
  /**
   * method exists because we need the library to compile under JDK 1_4.
   * Code directly copied from OpenJDK 1.6.
   * <CODE>Long.numberOfTrailingZeros(long w)</CODE> source.
   * @see <a href="http://grepcode.com/file/repository.grepcode.com/java/root/jdk/openjdk/6-b14/java/lang/Long.java#Long.numberOfTrailingZeros%28long%29">original code</a>
   * @param i long
   * @return int
   */
  private static int numberOfTrailingZeros(long i) {
    int x, y;
    if (i == 0)return 64;
    int n = 63;
    y = (int) i;
    if (y != 0) {
      n = n - 32;
      x = y;
    }
    else x = (int) (i >>> 32);
    y = x << 16;
    if (y != 0) {
      n = n - 16;
      x = y;
    }
    y = x << 8;
    if (y != 0) {
      n = n - 8;
      x = y;
    }
    y = x << 4;
    if (y != 0) {
      n = n - 4;
      x = y;
    }
    y = x << 2;
    if (y != 0) {
      n = n - 2;
      x = y;
    }
    return n - ( (x << 1) >>> 31);
  }

	
  /**
   * auxiliary nested class, not part of the public API.
   */
  static class BVAux {
    static final int _AND_OP = 0;
    static final int _OR_OP = 1;
    // ... other ops enumerated here
		
    private int _starti = -1;
    private int _endi = -1;
    private boolean _finish = false;
    private long[] _data = null;
    private long[] _otherData = null;
    private int _what2Run = -1;

    BVAux() {}

    void go() {
      while (!getFinish()) {
        go1();
      }
    }

    synchronized boolean getFinish() {
      return _finish;
    }

    synchronized void setFinish() {
      _finish = true;
      notify();
    }

    synchronized void waitForTask() {
      while (_starti != -1) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }

    synchronized void runFromTo(int starti, int endi, long[] data,
                                long[] other_data, int what2run) {
      while (_starti != -1) {
        try {
          wait();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      // ok, set values
      _starti = starti;
      _endi = endi;
      _data = data;
      _otherData = other_data;
      _what2Run = what2run;
      notify();
    }

    private synchronized void go1() {
      while (_starti == -1) {
        if (_finish)return; // finished
        try {
          wait();
        }
        catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      // ok, now run the code for the specified operation
      switch (_what2Run) {
        case _AND_OP:
          for (int i = _starti; i < _endi; i++) _data[i] &= _otherData[i];
          break;
        case _OR_OP:
          for (int i = _starti; i < _endi; i++) _data[i] |= _otherData[i];
          break;
          // other cases here
        default:
          System.err.println("OPERATION NOT SUPPORTED");
      }
      // reset values
      _starti = -1;
      _endi = -1;
      _data = null;
      _otherData = null;
      _what2Run = -1;
      notify();
    }
  }

	
  /**
   * auxiliary nested class not part of the public API.
   */
  static class BVThread extends Thread {
    private BVAux _r;
    BVThread(BVAux r) {
      _r = r;
    }

    public void run() {
      _r.go();
    }

    BVAux getBVAux() {
      return _r;
    }
  }
}
