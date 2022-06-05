package popt4jlib.LIP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.StringTokenizer;
import parallel.UnboundedMinHeapUnsynchronized;
import utils.Pair;
import popt4jlib.*;


/**
 * class implements a multi-threaded version of Balas' ADDITIVE algorithm for
 * Linear Integer Programming. In particular, the solver solves Integer
 * Programming problems of the form:
 * <PRE>
 * min. c'x
 * s.t. Ax &ge; b
 *      x &ge; 0
 *      x integer vector
 * </PRE> where the matrix A, and the vectors b,c are given and contain integer
 * values. The vector c should contain non-negative numbers in particular. In
 * case the vector x is not restricted to binary values, then the system Ax &ge;
 * b must be able to upper-bound each component of the vector x, so as to bring
 * the program into the so-called standard binary format. Notice that this
 * implementation uses sparse vectors which is likely to be useful in Integer
 * Programs arising from applications of Graph Theory (e.g. packing problems
 * etc., especially on sparse graphs, but also problems relating to allocation 
 * of hotel rooms to potential customers bidding for them in a hotelier induced
 * auction). Also notice that this program is well-suited to the Fork/Join Task 
 * programming framework (Doug Lea and before him, the Cilk project at MIT). 
 * Nevertheless, in several experiments conducted, each Branch-and-Bound Node 
 * opened has significant processing to do before generating its new branches 
 * and therefore the contention among the threads is kept to very acceptable 
 * levels (in fact, CPU utilization remains always at 100% when all cores are 
 * used.) 
 * <p>Note: this implementation uses a min-heap data structure to store the open 
 * nodes which is efficient in the sense that all operations are O(logN) in this 
 * data structure.
 * <p>Other notes:
 * <p>2022-02-07: fixed bug in fix-sum-test-variables method.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2022</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.1
 */
public class AdditiveSolverMT implements Runnable {
	private static Node _optimal;
	private static UnboundedMinHeapUnsynchronized _heap;  // maintains open nodes
	private static int _maxnum;
	private static int _zbar;  // best upper bound to z known so far.
	private static int _zstar;  // optimal solution value
	private int _cstar;
	private int _csum;
	private static int _remainder;
	private static int _m, _n, _no;
	private static IntArray1SparseVector _c, _co, _l;
	private IntArray1SparseVector _b, _bbar, _Pplus;
	private IntArray2SparseMatrix _A;

	private static volatile boolean _conversionNeeded = true;

	private final static Object _classLock = new Object();  // needed when 
	                                                        // invoked using
	                                                        // runMain*().

	private static ThreadWID[] _threads;  // must be able to check for idle

	
	/**
	 * auxiliary inner class holding nodes in the Branch-and-Bound Tree of the
	 * algorithm. Not part of the public API.
	 */
	static class Node implements Comparable {
		private int _id;                   // node id
		private IntArray1SparseVector _x;  // node vars
		private int _z = Integer.MAX_VALUE;  // lower bound
		private boolean _status = true;      // open<--T, closed<--F

		
		protected IntArray1SparseVector getX() {
			return _x;
		}

		
		public int compareTo(Object o) {
			return Integer.compare(_id, ((Node) o)._id);
		}
	}

	
	/**
	 * auxiliary counter class. Not part of the public API.
	 */
	private static class ON {
		private static int _numOpenNodes = 0;

		
		private static void incrNumOpenNodes() {
			synchronized (AdditiveSolverMT.class) {
				++_numOpenNodes;
			}
		}

		
		private static void decrNumOpenNodes() {
			synchronized (AdditiveSolverMT.class) {
				--_numOpenNodes;
				if (_numOpenNodes == 0) {
					AdditiveSolverMT.class.notifyAll();
				}
			}
		}

		
		private static int getNumOpenNodes() {
			int ret = 0;
			synchronized (AdditiveSolverMT.class) {
				ret = _numOpenNodes;
			}
			return ret;
		}
	}

	
  // general purpose routines
	
	
	/**
	 * return the current value of the <CODE>_zbar</CODE> variable, namely the
	 * currently best known upper bound on the optimal value z. Method is
	 * synchronized.
	 * @return int
	 */
	private static synchronized int getZBar() {
		return _zbar;
	}

	
	/**
	 * check if x has no component valued at -1.
	 * @param x IntArray1SparseVector
	 * @return true iff no component has value -1.
	 */
	private static boolean complete(IntArray1SparseVector x) {
		int nz = x.getNumNonZeros();
		for (int i = 0; i < nz; i++) {
			if (x.getIntIthNonZeroVal(i) == -1) {
				return false;
			}
		}
		return true;
	}

	
	/**
	 * matrix-to-vector multiplication.
	 * @param A IntArray2SparseMatrix
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 */
	private static IntArray1SparseVector multiply(IntArray2SparseMatrix A,
		                                            IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(A.getNumRows());
		for (int i = 0; i < result.getNumCoords(); i++) {
			result.setCoord(i, A.getIthRow(i).innerProduct(x));
		}
		return result;
	}

	
	/**
	 * checks if Ax &ge; b.
	 * @param Ax IntArray1SparseVector
	 * @param b IntArray1SparseVector
	 * @return boolean true iff Ax &ge; b
	 */
	private static boolean check(IntArray1SparseVector Ax,
		                           IntArray1SparseVector b) {
		if (Ax.getNumCoords() != b.getNumCoords()) {
			throw new IllegalArgumentException("Ax and b dims don't match");
		}
		int Ax_nz = Ax.getNumNonZeros();
		for (int i = 0; i < Ax_nz; i++) {
			int axipos = Ax.getIthNonZeroPos(i);
			int axival = Ax.getIntIthNonZeroVal(i); // (int) Ax.getCoord(axipos);
			int bival = (int) b.getCoord(axipos);
			if (axival < bival) {
				return false;
			}
		}
		int b_nz = b.getNumNonZeros();
		for (int i = 0; i < b_nz; i++) {
			int bipos = b.getIthNonZeroPos(i);
			int bival = b.getIntIthNonZeroVal(i); // (int) b.getCoord(bipos);
			int axival = (int) Ax.getCoord(bipos);
			if (axival < bival) {
				return false;
			}
		}
		return true;
	}

	
	/**
	 * return the number of components that have value -1.
	 * @param x IntArray1SparseVector
	 * @return int
	 */
	private static int countFreeVars(IntArray1SparseVector x) {
		int n = 0;
		int nz = x.getNumNonZeros();
		for (int i = 0; i < nz; i++) {
      //int ipos = x.getIthNonZeroPos(i);
			//if (Integer.compare((int)x.getCoord(ipos),-1)==0) ++n;
			if (x.getIntIthNonZeroVal(i) == -1) ++n;
		}
		return n;
	}

	
	/**
	 * sort the values of the argument in ascending order and return them in a new
	 * sparse vector. Leaves the argument intact (doesn't modify argument).
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 */
	static IntArray1SparseVector sortAsc(IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(x.getNumCoords());
		int[] data = x.getNonDefValues();
		if (data == null) {
			return result;
		}
		Arrays.sort(data);
		int firstPosPos = data.length;
		for (int i = 0; i < data.length; i++) {
			if (data[i] > 0) {
				firstPosPos = i;
				break;
			}
		}
		int i = 0;
		for (; i < firstPosPos; i++) {
			result.setCoord(i, data[i]);
		}
		i = x.getNumCoords() - (data.length - firstPosPos);
		for (int j = firstPosPos; j < data.length; j++) {
			result.setCoord(i++, data[j]);
		}
		return result;
	}

	
	/**
	 * same as <CODE>sortAsc(x)</CODE> but sorts elements in descending order.
	 * Leaves argument intact (doesn't modify argument).
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 */
	static IntArray1SparseVector sortDesc(IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(x.getNumCoords());
		int[] data = x.getNonDefValues();
		if (data == null) {  // no non-zero values
			return result;
		}
		Arrays.sort(data);
		int firstPosPos = data.length;
		for (int i = 0; i < data.length; i++) {
			if (data[i] > 0) {
				firstPosPos = i;
				break;
			}
		}
		int i = data.length - 1;
		int j = 0;
		for (; i >= firstPosPos; i--) {
			result.setCoord(j++, data[i]);
		}
		i = x.getNumCoords() - firstPosPos;
		for (j = firstPosPos - 1; j >= 0; j--) {
			result.setCoord(i++, data[j]);
		}
		return result;
	}

	
	/**
	 * sum elements of vector from (including) start position, up to (and
	 * including) end position.
	 * @param start int
	 * @param end int
	 * @param x IntArray1SparseVector
	 * @return int
	 */
	private static int sum(int start, int end, IntArray1SparseVector x) {
		int res = 0;
		for (int i = start; i <= end; i++) {
			int xi = (int) x.getCoord(i);
			res += xi;
		}
		return res;
	}

	
	/**
	 * set all free (ie with value -1) components of the vector passed in as first
	 * argument to the value specified in the second argument.
	 * @param x IntArray1SparseVector
	 * @param l int
	 */
	private static void setFreeVars(IntArray1SparseVector x, int l) {
		for (int i = 0; i < x.getNumCoords(); i++) {
			int ival = (int) x.getCoord(i);
			if (ival == -1) {
				x.setCoord(i, l);
			}
		}
	}

	
	/**
	 * auxiliary getter method for sub-classes needing access to <CODE>_c</CODE>.
	 * @return IntArray1SparseVector
	 */
	static IntArray1SparseVector getCVector() {
		return _c;
	}

	
	/**
	 * auxiliary getter method for sub-classes needing access to <CODE>_b</CODE>.
	 * @return IntArray1SparseVector
	 */
	protected IntArray1SparseVector getBVector() {
		return _b;
	}

	/**
	 * auxiliary getter method for sub-classes needing access to <CODE>_A</CODE>.
	 * @return IntArray2SparseMatrix
	 */
	protected IntArray2SparseMatrix getAMatrix() {
		return _A;
	}

	
  // routines for node selection
	
	
	/**
	 * puts the <CODE>Node</CODE> nd inside the min-heap pointed to by
	 * <CODE>_heap</CODE>, in order so that the Nodes' <CODE>_v._z</CODE> value is
	 * in ascending order (smallest value first). Synchronized operation, but
	 * works in time O(logN) in the size of the queue.
	 * @param nd Node
	 */
	private synchronized static void putInOrder(Node nd) {
		ON.incrNumOpenNodes();
		_heap.addElement(new Pair(nd, new Double(nd._z)));
		AdditiveSolverMT.class.notifyAll();
	}

	
	/**
	 * return the first element in the queue pointed to by <CODE>_heap</CODE>. It
	 * also removes the node containing this element from the queue. O(logN)
	 * (synchronized) operation.
	 * @return Node
	 */
	private synchronized static Node selectNode() {
		final int id = ((ThreadWID) Thread.currentThread()).getID();
		while (_heap.size() == 0 && ON.getNumOpenNodes() > 0) {
			try {
				((ThreadWID) Thread.currentThread()).setIdle(true);
				AdditiveSolverMT.class.wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (_heap.size() == 0) {
			((ThreadWID) Thread.currentThread()).setIdle(true);
			return null;
		}
		((ThreadWID) Thread.currentThread()).setIdle(false);
		Node ret = (Node) ((Pair) _heap.remove()).getFirst();
		return ret;
	}

	
	/**
	 * sets the status of the Node argument to false (closed).
	 * @param current Node
	 */
	private void close(Node current) {
		if (!current._status) {
			return;  // itc-20220123: current already closed
		}
		ON.decrNumOpenNodes();
		current._status = false;
	}

	
  // computing routines
	
	
	/**
	 * updates the argument's <CODE>_z</CODE> value, as well as
	 * <CODE>_zstar,_zbar,_optimal</CODE> members if appropriate. Synchronized
	 * operation. Notice that the current node will always be complete (will have
	 * no free variables) when this method is invoked.
	 * @param current Node
	 */
	private synchronized static void updateZ(Node current) {
		IntArray1SparseVector xc = current._x;
		int z = 0;
		int xnz = xc.getNumNonZeros();
		for (int i = 0; i < xnz; i++) {
			z += _c.getCoord(xc.getIthNonZeroPos(i));
		}
		current._z = z;
		if (z < _zbar) {
			_zbar = z;
		}
		if (z < _zstar) {
			_zstar = z;
			_optimal = current;
			System.err.println("found new better solution z=" + z +
				                 " (xc=" + xc + ")");
		}
	}

	
	/**
	 * modifies this solver's vector's <CODE>_b</CODE> last (augmented) component,
	 * as well the matrix's <CODE>_A</CODE> last (augmented) row, if the condition
	 * <CODE>_zbar &lt; _csum</CODE> holds.
	 * @param current Node
	 */
	private void imposeConstraint(Node current) {
		int zbar = getZBar();
		if (zbar < _csum) {
			_b.setCoord(_m - 1, _cstar + 1 - zbar);
			for (int j = 0; j < _n; j++) {
				if (((int) current._x.getCoord(j)) == -1) {
					_A.setCoord(_m - 1, j, (int) (-_c.getCoord(j)));
				} else {
					_A.setCoord(_m - 1, j, 0);
				}
			}
			/*
			System.err.println("imposeConstraint(): for node " + current._id +
				                 " _A_m.=" + _A.getIthRow(_m - 1) +
				                 " _b_m=" + _b.getCoord(_m - 1));
			*/
		}
	}

	
	/**
	 * processes the current <CODE>Node</CODE>, possibly modifying the 2nd 
	 * argument as well.
	 * @param current Node
	 * @param bbar IntArray1SparseVector
	 * @return int the sum of the <CODE>_c</CODE> vector's coefficients for those
	 * components for which <CODE>current._x</CODE>' components are set to 1.
	 */
	private int computeNode(Node current, IntArray1SparseVector bbar) {
		int cstar = 0;
		bbar.reset();
		int nz = current._x.getNumNonZeros();
		for (int i = 0; i < nz; i++) {
			int ipos = current._x.getIthNonZeroPos(i);
			int val = current._x.getIntIthNonZeroVal(i);
			if (val == 1) {
				cstar += (int) _c.getCoord(ipos);
				for (int j = 0; j < _m; j++) {
					int aji = _A.getCoord(j, ipos);
					if (aji != 0) {
						bbar.setCoord(j, bbar.getCoord(j) + aji);
					}
				}
			}
		}
		for (int i = 0; i < _m; i++) {
			int nvi = (int) _b.getCoord(i) - (int) bbar.getCoord(i);
			bbar.setCoord(i, nvi);
		}
		return cstar;
	}

	
	/**
	 * performs the sum-test check of the Additive Algorithm.
	 * @param current Node
	 * @return boolean true iff feasibility of the Node is OK so far.
	 */
	private boolean sumTest(Node current) {
		boolean feasible = true;
		_Pplus.reset();
		int i = 0;
		do {
			_Pplus.setCoord(i, 0);
			int cxnz = current._x.getNumNonZeros();
			for (int j = 0; j < cxnz; j++) {
				int jpos = current._x.getIthNonZeroPos(j);
				int jval = current._x.getIntIthNonZeroVal(j);
				if (jval == -1) {
					int Aij = _A.getCoord(i, jpos);
					if (Aij > 0) {
						int pplusival = (int) _Pplus.getCoord(i);
						_Pplus.setCoord(i, pplusival + Aij);
					}
				}
			}
			if (_bbar.getCoord(i) > 0 && _Pplus.getCoord(i) < _bbar.getCoord(i)) {
				feasible = false;
			}
			++i;
		} while (feasible && i != _m);
		if (!feasible) {
			close(current);
		}
		//System.err.println("sumTest(): node-id "+current._id+
		//                   " feasible="+feasible);
		return feasible;
	}

	
	/**
	 * updates the b&#772; (bbar) and P+ (Pplus) vectors.
	 * @param current Node
	 * @param bbar IntArray1SparseVector
	 * @param Pplus IntArray1SparseVector
	 */
	private void updateBbarPplus(Node current,
		                           IntArray1SparseVector bbar,
		                           IntArray1SparseVector Pplus) {
		bbar.reset();
		Pplus.reset();
		for (int i = 0; i < _m; i++) {
			bbar.setCoord(i, _b.getCoord(i));
			final int xnz = current._x.getNumNonZeros();
			int ppi = 0;
			for (int j = 0; j < xnz; j++) {
				int jpos = current._x.getIthNonZeroPos(j);
				int jval = current._x.getIntIthNonZeroVal(j);
				int Aij = _A.getCoord(i, jpos);
				if (jval == -1 && Aij > 0) {
					ppi += Aij;
				} else if (jval == 1 && Aij != 0) {
					int bbari = (int) bbar.getCoord(i);
					bbar.setCoord(i, bbari - Aij);
				}
			}
			Pplus.setCoord(i, ppi);
		}
	}

	
	/**
	 * fixes free variables to 0 or 1 depending on the constraints.
	 * @param current Node
	 * @param bbar IntArray1SparseVector represents the vector b - 
	 * &Sigma;<sub>i &isin; B_1</sub> A<sub>.i</sub>
	 * @param Pplus IntArray1SparseVector represents the vector 
	 * &Sigma;<sub>i &isin;F</sub>max(A<sub>.i</sub>,0)
	 * @return boolean true if there was at least one variable fixed
	 */
	private boolean fixSumTestVars(Node current,
		                             IntArray1SparseVector bbar,
		                             IntArray1SparseVector Pplus) {
		boolean ret = false;
		for (int j = 0; j < _n; j++) {
			int jval = (int) current._x.getCoord(j);
			if (jval == -1) {  // j-var is free
				for (int i = 0; i < _m; i++) {  // itc-20220207: used to loop only
					                              // over the non-zero elements but 
					                              // this is wrong
					int ival = (int) Pplus.getCoord(i);
					int Aij = _A.getCoord(i, j);
					if (ival - Math.abs(Aij) < bbar.getCoord(i)) {
						if (Aij > 0) {
							current._x.setCoord(j, 1);
              //System.err.println("FSMTV(): for nid="+current._id+
							//                   " set x"+j+"=1 because of row-"+ipos);
							ret = true;
							break;  // itc20220122: stop checking rows
						} else if (Aij < 0) {
							current._x.setCoord(j, 0);
              //System.err.println("FSMTV(): for nid="+current._id+
							//                   " set x"+j+"=0 because of row-"+ipos);
							ret = true;
							break;  // itc20220122: stop checking rows
						}
					}
				}
			}
		}
		if (complete(current._x)) {
			//System.err.println("Node complete");
			close(current);
			IntArray1SparseVector Ax = multiply(_A, current._x);
			boolean feasible = check(Ax, _b);
			//System.err.println("check(_Ax,_b) returns "+feasible);
			if (feasible) updateZ(current);
		}
		return ret;
	}

	
	/**
	 * return a new vector containing only the components of s for dimensions for
	 * which x[i]==-1. Obviously the return vector has dimension equal to the
	 * number of components in x for which x[i]==-1.
	 * @param s IntArray1SparseVector
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 * @throws IllegalStateException if s and x dimensions don't match
	 */
	private IntArray1SparseVector freeCoeffs(IntArray1SparseVector s,
		                                       IntArray1SparseVector x) {
		if (s.getNumCoords() != x.getNumCoords())
			throw new IllegalStateException("dimensions mismatch");
		int free_xs = 0;
		for (int i = 0; i < x.getNumNonZeros(); i++) {
			if (x.getIntIthNonZeroVal(i) == -1) {
				++free_xs;
			}
		}
		IntArray1SparseVector free_vars = new IntArray1SparseVector(free_xs);
		int j = 0;
		for (int i = 0; i < x.getNumNonZeros(); i++) {
			int xi = x.getIntIthNonZeroVal(i);
			if (xi == -1) {
				free_vars.setCoord(j++, (int) s.getCoord(x.getIthNonZeroPos(i)));
			}
		}
		return free_vars;
	}

	
	/**
	 * upper- and lower-bounds the current <CODE>Node</CODE> object. As a result
	 * the operation may close the node (as infeasible).
	 * @param current Node
	 */
	private void LU(Node current) {
    // Li is the minimum number of free variables that must be set to 1
		// in order to satisfy constraint row i, ie sum above bbar_i.
		// Ui is the maximum number of free variables that may be set to 1
		// and still satisfy constraint row i, ie sum above bbar_i.
		int L = 0;       // L <- max(Li)
		int U = _n + 1;  // U <- min(Ui) 
		for (int i = 0; i < _m; i++) {
			IntArray1SparseVector a0 = freeCoeffs(_A.getIthRow(i), current._x);
			int a0_len = a0.getNumCoords();
			IntArray1SparseVector s = sortDesc(a0);
			//System.err.println("Af_"+i+"="+s+" bbar_"+i+"="+_bbar.getCoord(i));
			int Li = 0;
			int Ui = a0_len;
			int laux = 0;
			int uaux = sum(0, a0_len - 1, s);
			int bbari = (int) _bbar.getCoord(i);
			for (int j = 0; j < a0_len; j++) {
				if (laux < bbari) {
					final int sj = (int) s.getCoord(j);
					++Li;
					laux += sj;
				}
				if (uaux < bbari) {
					final int sendj = (int) s.getCoord(a0_len - 1 - j);
					--Ui;
					uaux -= sendj;
				}
			}  // for j
			if (laux < bbari) {
				Li = _n + 2;  // used to be _n + 1
			}
			//System.err.println("Li="+Li+" Ui="+Ui);
			if (Li > L) {
				L = Li;
			}
			if (Ui < U) {
				U = Ui;
			}
		}  // for i
		//System.err.println("LU(): L="+L+" U="+U);
		if (L > U) {  // infeasible node
			close(current);
			//System.err.println("LU(): node-id "+current._id+" INFEASIBLE");
			return;
		} 
		if (L == U) {
			int f = countFreeVars(current._x);
			if (f == L) {
				setFreeVars(current._x, 1);  // set all free vars to 1
				updateZ(current);
				close(current);
				return;
			}
			if (L == 0) {
				setFreeVars(current._x, 0);  // set all free vars to 0
				updateZ(current);
				close(current);
				return;
			}
		}
		if (L > 0) {
			IntArray1SparseVector s1 = freeCoeffs(_c, current._x);
			IntArray1SparseVector s1_asc = sortAsc(s1);
			int Lsum = sum(0, L - 1, s1_asc);
			current._z = _cstar + Lsum;
			int zbar = getZBar();
			if (current._z > zbar) {
				close(current);  // current is useless
			}
		}
	}

	
	/**
	 * protected method used to select a free variable given an open node. Current
	 * method chooses the free variable that setting to 1 provides the least
	 * "infeasibility" in the program, i.e. it's a greedy method.
	 * @param current
	 * @return int the index of the chosen variable (in [0,num_vars-1))
	 */
	protected int selectVariable(Node current) {
		return infSum(current);
	}

	
	/**
	 * return the free variable index j (column) that minimizes the quantity
	 * &Sigma;<sub>i</sub>{(_bbar[i]-_A[i][j])<sub>+</sub>} (the sum includes the 
	 * last -augmented- row of <CODE>_A</CODE> and component of <CODE>_b</CODE>).
	 * @param current Node
	 * @return int
	 */
	private int infSum(Node current) {
		int col = 0;
		int mi = Integer.MAX_VALUE;
		int cxnz = current._x.getNumNonZeros();
		for (int j = 0; j < cxnz; j++) {
			int jpos = current._x.getIthNonZeroPos(j);
			int jval = current._x.getIntIthNonZeroVal(j);
			if (jval == -1) {  // x[jpos] is free
				int sm = 0;
				for (int i = 0; i < _m; i++) {
					int bbari = (int) _bbar.getCoord(i);
					int Aij = _A.getCoord(i, jpos);
					if (bbari > Aij) {
						sm += (bbari - Aij);
					}
				}
				if (mi > sm) {
					mi = sm;
					col = jpos;
				}
			}
		}
		return col;
	}

	
	/**
	 * creates two new <CODE>Node</CODE> objects (with different values for the
	 * x_col variable), and puts them in the min-heap <CODE>_heap</CODE>.
	 * @param col int
	 * @param current Node
	 */
	private void branch(int col, Node current) {
		// create and compute new Node 1-branch
		Node t1 = new Node();
		synchronized (AdditiveSolverMT.class) {
			t1._id = ++_maxnum;
		}
		//System.err.println("cur_x="+current._x);
		t1._x = (IntArray1SparseVector) current._x.newInstance();
		t1._x.setCoord(col, 1);  // 1-Branch
		t1._status = true;  // node is OPEN for business
		IntArray1SparseVector b1 = new IntArray1SparseVector(_m);
		int cstar1 = computeNode(t1, b1);
		t1._z = cstar1;
		putInOrder(t1);
		//System.err.println("-- creating new node "+t1._id);
		// create and compute new Node 0-branch
		Node t2 = new Node();
		synchronized (AdditiveSolverMT.class) {
			t2._id = ++_maxnum;
		}
		t2._x = (IntArray1SparseVector) current._x.newInstance();
		t2._x.setCoord(col, 0);  // 0-Branch
		t2._status = true;  // node is OPEN for business
		IntArray1SparseVector b2 = new IntArray1SparseVector(_m);
		int cstar2 = computeNode(t2, b2);
		t2._z = cstar2;
		putInOrder(t2);
		//System.err.println("-- creating new node "+t2._id);
		close(current);
	}

	
  // miscellaneous routines
	
	
	/**
	 * converts a (bounded) Linear Integer Program to a Binary Program.
	 * @param debug boolean prints out diagnostics if true
	 * @throws IllegalStateException if the problem cannot be converted to binary
	 * format.
	 */
	private void convertToBin(boolean debug) {
		double[] bound = new double[_n];
		boolean format = true;  // additive format possible
		boolean pass = true;
		for (int k = 0; k < _n; k++) {
			bound[k] = -1.0;
		}
		while (format && pass) {
			pass = false;  // in each pass, bounds are needed
			for (int j = 0; j < _n; j++) {
				int cn = 0;  // current constraint
				while (_A.getCoord(cn, j) >= 0 && cn < _m - 1) {
					++cn;
				}
				if (cn == _m - 1) {  // x_j has no upper bound
					throw new IllegalStateException("Problem cannot be " +
						                              "converted to " +
						                              "Additive Format: variable" +
						                              " index " + j + " cannot be bounded");
				} else {  // _A[cn,j]<0
					int cni = cn;
					for (cn = cni; cn < _m - 1; cn++) {  // keep going down rows
						if (_A.getCoord(cn, j) >= 0) {
							continue;  // ensure _A[cn,j] < 0
						}
						double bsum = _b.getCoord(cn) / _A.getCoord(cn, j);
						int k = 0;
						boolean nobsum = false;
						while (k <= _n - 1) {  // or is it _n?
							double coef = (double) _A.getCoord(cn, k) /
								            (double) _A.getCoord(cn, j);
							if (k != j) {
								if (coef < 0) {
									if (bound[k] == -1.0) {
										nobsum = true;
									} else {
										bsum -= Math.floor(bound[k]) * coef;
									}
								}
							}
							++k;
						}
						if (!nobsum) {
							double tbsum = Math.floor(bsum);
							if (tbsum < 0) {  // itc-20220116: added
								throw new IllegalStateException("Problem is INFEASIBLE: " +
									                              "bound[" + j + "]=" + tbsum);
							}
							if (bound[j] > tbsum || bound[j] == -1.0) {
								bound[j] = tbsum;
								if (debug) {
									System.err.println("improving bound for j=" + j + 
										                 " bound=" + bound[j]);
								}
								pass = true;
							}
						}
					}
				}
			}  // for xj vars
		}  // while format and pass

		if (format) {
			double cr = Math.log(2);
			int vs = 0;  // #bin_vars
			for (int k = 0; k < _n; k++) {
				if (bound[k] < 0) {
					throw new IllegalStateException("problem cannot be " +
						                              "converted to " +
						                              "Additive format: variable " +
						                              "index " + k + " bound is at " +
						                              bound[k]);
				}
				if (bound[k] <= 1.0) {
					_l.setCoord(k, 1);
					vs++;
				} else {
					double v = Math.log(bound[k]) / cr;
					int vi = (int) Math.ceil(v);
					if ((int) v == vi) {
						++vi;  // above formula doesn't work for bounds that are powers of 2
					}
					_l.setCoord(k, vi);  // #bin vars for x[k]
					vs += vi;
				}
			}
			if (debug) {
				System.err.println("#bin vars=" + vs);
			}
			if (vs < _n) {
				throw new IllegalStateException("vs < _n?");
			}
			// update vector c
			int j = 0;
			_remainder = 0;
			double[] nc = new double[vs];
			for (int i = 0; i < _n; i++) {
				int li = (int) _l.getCoord(i);
				for (int k = 0; k <= li - 1; k++) {  // or is it?
					int ci = (int) _c.getCoord(i);
					nc[k + j] = Math.abs(Math.pow(2, k) * ci);
					if (ci < 0) {
						_remainder += nc[k + j];  // or is it nc[k+j+1]?
					}
				}
				j += li;
			}
			// update matrix A
			IntArray2SparseMatrix NA = new IntArray2SparseMatrix(_m, vs);
			double[] nb = new double[_m];
			for (int p = 0; p < _m - 1; p++) {
				j = 0;
				nb[p] = _b.getCoord(p);
				for (int i = 0; i < _n; i++) {
					int li = (int) _l.getCoord(i);
					for (int k = 0; k <= li - 1; k++) {
						NA.setCoord(p, k + j,
							(int) (_A.getCoord(p, i) * Math.pow(2, k)));
						if (_c.getCoord(i) < 0) {
							int na = NA.getCoord(p, k + j);
							nb[p] -= na;
							NA.setCoord(p, k + j, -na);
						}
					}  // for k
					j += li;
				}
			}  // for p
			// store vector c
			_co.reset();
			_co.addMul(1.0, _c);
			// store integer vars
			_no = _n;
			_n = vs;
			_c = new IntArray1SparseVector(_n);  // vector _c is now larger
			for (j = 0; j < _n; j++) {
				_c.setCoord(j, nc[j]);
			}
			_A = NA;
			// update _b
			for (int i = 0; i < _m - 1; i++) {
				_b.setCoord(i, nb[i]);
			}
		}  // if format

		// debug
		if (debug) {
			System.err.println("Binary Format:");
			System.err.println("Constraints: ");
			for (int i = 0; i < _m; i++) {
				System.err.println("");
				for (int j = 0; j < _n; j++) {
					System.err.print(_A.getCoord(i, j) + " ");
				}
				System.err.println(">= " + _b.getCoord(i));
			}
			System.err.println("");
			System.err.println("objective coefficients vector c:");
			System.err.println(_c);
			System.err.println("Conversion Procedure Completed.");
		}
	}

	
	/**
	 * initialize the data structures for the solver, reading data from the file
	 * containing the problem data. Each thread participating in the program
	 * execution calls this method to read the matrix A and the vector b in its
	 * own memory; the thread reads also the vector c. The file must be formatted
	 * like this:
	 * <PRE>
	 * &lt;m&gt; // _m number of constraints (augmented by 1)
	 * &lt;n&gt; // _n number of variables
	 * A_[0,.] row 0 values (one line): val1, val2, ... valn
	 * ...
	 * A_[m-2,.] row m-2 values (one line): val1, val2, ... valn
	 * b_[0] first constraint rhs value
	 * ...
	 * b_[m-2] last constraint rhs value
	 * c_[.] objective coefficients values (one line): val1, val2, ... valn
	 * </PRE>
	 * @param filename String
	 * @throws Exception
	 */
	private void initialize(String filename) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		_m = Integer.parseInt(line);  // _m is the "augmented" # of constraints
		_n = Integer.parseInt(br.readLine());  // _n is the # of columns
		// read-in matrix A
		_A = new IntArray2SparseMatrix(_m, _n);
		for (int i = 0; i < _m - 1; i++) {
			line = br.readLine();
			StringTokenizer st = new StringTokenizer(line);
			int j = 0;
			while (st.hasMoreTokens()) {
				int aij = Integer.parseInt(st.nextToken());
				_A.setCoord(i, j++, aij);
			}
		}
    // last row of _A is already zero.
		// read-in constraints rhs vector b
		_b = new IntArray1SparseVector(_m);
		StringTokenizer st = new StringTokenizer(br.readLine());
		for (int i = 0; i < _m - 1; i++) {
			int bi = Integer.parseInt(st.nextToken());
			_b.setCoord(i, bi);
		}
    // last component of _b is already zero
		// read-in objective coefficients c
		_c = new IntArray1SparseVector(_n);
		st = new StringTokenizer(br.readLine());
		for (int j = 0; j < _n; j++) {
			int cj = Integer.parseInt(st.nextToken());
			_c.setCoord(j, cj);
		}
		br.close();

		_co = new IntArray1SparseVector(_n);
		_l = new IntArray1SparseVector(_m);

		if (_conversionNeeded) {
			convertToBin(false);
		}

		if (_heap == null) {
			Node current = new Node();
			current._id = 0;  // redundant
			current._z = Integer.MAX_VALUE;
			current._status = true;
			current._x = new IntArray1SparseVector(_n);
			for (int i = 0; i < _n; i++) {
				current._x.setCoord(i, -1);  // initially, all vars are free
			}
			_heap = new UnboundedMinHeapUnsynchronized(1024);
			putInOrder(current);
			_maxnum = 0;  // redundant
			// _optimal = current;  // itc-20220116: commented out
			_zbar = Integer.MAX_VALUE;
			_zstar = Integer.MAX_VALUE;
		}
		// debug
		System.err.println("initialize() routine completed.");
	}

	
	/**
	 * initialize the data structures for the solver. Each thread participating in
	 * the program execution calls this method to read the matrix A and the vector
	 * b in its own memory.
	 * @param A matrix A
	 * @param b RHS vector b
	 * @param c cost vector c
	 * @param debug boolean if true prints diagnostic information
	 */
	void initialize(IntArray2SparseMatrix A,
		IntArray1SparseVector b,
		IntArray1SparseVector c,
		boolean debug) {
		_m = A.getNumRows();
		_n = A.getNumCols();
		_A = new IntArray2SparseMatrix(A);
		_b = (IntArray1SparseVector) b.newInstance();
		_c = (IntArray1SparseVector) c.newInstance();
		_co = new IntArray1SparseVector(_n);
		_l = new IntArray1SparseVector(_m);

		if (_conversionNeeded) {
			convertToBin(debug);
		}

		if (_heap == null) {
			Node current = new Node();
			current._id = 0;  // redundant
			current._z = Integer.MAX_VALUE;
			current._status = true;
			current._x = new IntArray1SparseVector(_n);
			for (int i = 0; i < _n; i++) {
				current._x.setCoord(i, -1);  // initially, all vars are free
			}
			_heap = new UnboundedMinHeapUnsynchronized(1024);
			putInOrder(current);
			_maxnum = 0;  // redundant
			// _optimal = current;  // itc-20220116: commented out
			_zbar = Integer.MAX_VALUE;
			_zstar = Integer.MAX_VALUE;
		}

		// debug
		if (debug) {
			System.err.println("initialize() routine completed.");
		}
	}

	
	/**
	 * prints results.
	 */
	private static synchronized void printResults() {
		if (_optimal != null) {
			System.out.println("***OPTIMAL SOLUTION FOUND***");
			System.out.println("X*=" + _optimal._x);
			System.out.println("Z*=" + _optimal._z);
			System.out.print("STATS: #nodes=" + _maxnum);
			System.out.println(" Optimal nodeid=" + _optimal._id);
		} else {
			System.out.println("***PROBLEM INFEASIBLE***");
		}
	}

	
	/**
	 * prints results for the original problem if conversion was needed.
	 */
	private static synchronized void printFinalResults() {
		if (_optimal != null) {
			int z = _optimal._z - _remainder;
			System.out.println("***ORIGINAL PROBLEM RESULTS:***");
			System.out.println("ORIGINAL Z*=" + z);
			System.out.print("ORIGINAL X*=[ ");
			int k = 0;
			for (int i = 0; i < _no; i++) {
				int xs = 0;
				for (int j = 0; j <= _l.getCoord(i) - 1; j++) {
					if (_co.getCoord(i) >= 0) {
						xs += Math.pow(2, j) * _optimal._x.getCoord(k + j);
					} else {
						xs += Math.pow(2, j) * (1 - _optimal._x.getCoord(k + j));
					}
				}
				System.out.print(xs + " ");
				k += _l.getCoord(i);
			}
			System.out.println("]");
		} else {
			System.out.println("***PROBLEM INFEASIBLE***");
		}
	}
	
	
	/**
	 * get the original x vector (not necessarily binary) at its optimal settings.
	 * This method is only called from method <CODE>runMainXXX()</CODE> and only
	 * if the optimal solution has been found.
	 * @return IntArray1SparseVector
	 */
	private static IntArray1SparseVector getOptimalOriginalX() {
		IntArray1SparseVector res = new IntArray1SparseVector(_no);
		int k = 0;
		for (int i = 0; i < _no; i++) {
			int xs = 0;
			for (int j = 0; j <= _l.getCoord(i) - 1; j++) {
				if (_co.getCoord(i) >= 0) {
					xs += Math.pow(2, j) * _optimal._x.getCoord(k + j);
				} else {
					xs += Math.pow(2, j) * (1 - _optimal._x.getCoord(k + j));
				}
			}
			res.setCoord(i, xs);
			k += _l.getCoord(i);
		}		
		return res;
	}

	
	/**
	 * the <CODE>run()</CODE> method of the <CODE>Runnable</CODE> interface, runs
	 * the main loop of the solver until the optimal solution is found. It
	 * iteratively selects the first (best) <CODE>Node</CODE> n from the (shared)
	 * list of open nodes, imposes any constraints on it, computes it (updating
	 * the <CODE>_bbar</CODE> vector along the way), applies the tests it knows on
	 * it, and either closes the node as infeasible or optimal, or else branches
	 * on the chosen variable, by putting two new <CODE>Node</CODE> objects with
	 * appropriate values for the chosen variable on the shared list of open
	 * nodes.
	 */
	public void run() {
		final int id = ((ThreadWID) Thread.currentThread()).getID();
		//System.err.println("Thread-"+id+" starting");
		final int MAX_DUR_4_PRINT = 60000;  // 1 minute
		long last_check_time = System.currentTimeMillis();
		_bbar = new IntArray1SparseVector(_m);
		_Pplus = new IntArray1SparseVector(_m);
		_csum = 0;
		int cnz = _c.getNumNonZeros();
		for (int i = 0; i < cnz; i++) {
			_csum += _c.getIntIthNonZeroVal(i);
		}
		do {
			long now = System.currentTimeMillis();
			if (now - last_check_time > MAX_DUR_4_PRINT) {  // print progress info
				int nn;
				synchronized (AdditiveSolverMT.class) {
					nn = _maxnum;
				}
				System.err.println("TID=" + id + " #nodes so far created=" + nn
					+ " #open=" + ON.getNumOpenNodes());
				last_check_time = now;
			}
			Node current = selectNode();
			while (current == null) {
				// make sure there isn't any other thread still working
				if (allThreadsIdle()) {
					//System.err.println("Thread-"+id+" exits");
					return;  // done
				} else {
					current = selectNode();  // wait for a node if needed
				}
			}
			IntArray1SparseVector x = current._x;
			_cstar = 0;
			cnz = x.getNumNonZeros();
			for (int i = 0; i < cnz; i++) {
				if (x.getIntIthNonZeroVal(i) == 1) {
					int ipos = x.getIthNonZeroPos(i);
					_cstar += _c.getCoord(ipos);
				}
			}
			imposeConstraint(current);
			_cstar = computeNode(current, _bbar);
			boolean feasible = sumTest(current);
			if (feasible) {
				while (fixSumTestVars(current, _bbar, _Pplus)) {  // update vars
					updateBbarPplus(current, _bbar, _Pplus);
				}
				if (current._status) {
					LU(current);
					if (current._status) {
						int column = selectVariable(current);
						branch(column, current);
					}
				}
			}
		} while (true);  // _heap.size()!=0
	}

	
	/**
	 * returns true iff all threads running to solve the problem are set to idle.
	 * @return boolean
	 */
	private static boolean allThreadsIdle() {
		for (int i = 0; i < _threads.length; i++) {
			if (!_threads[i].isIdle()) {
				return false;
			}
		}
		return true;
	}

	
	/**
	 * API method to invoke the algorithm, for Binary Programming only (no needing
	 * of any kind of conversion.)
	 * @param A the _m &times; _n augmented matrix
	 * @param b the _m &times; 1 augmented RHS vector
	 * @param c the _n &times; 1 cost vector
	 * @param nt the number of threads to use
	 * @param solverFac AddSlvrFac maybe null
	 * @return IntArray1SparseVector the best solution found
	 */
	public static IntArray1SparseVector runMainWOConversion(
		                                    IntArray2SparseMatrix A,
		                                    IntArray1SparseVector b,
		                                    IntArray1SparseVector c,
		                                    int nt,
		                                    AddSlvrFac solverFac) {
		synchronized (_classLock) {
			AdditiveSolverMT[] as1s = new AdditiveSolverMT[nt];
			_conversionNeeded = false;
			for (int i = 0; i < nt; i++) {
				as1s[i] = solverFac==null ? 
					          new AdditiveSolverMT() : 
					          solverFac.newInstance();
				as1s[i].initialize(A, b, c, i == 0);  // debug only for 1st thread
			}
			_threads = new ThreadWID[nt];
			try {
				for (int i = 0; i < nt; i++) {
					_threads[i] = new ThreadWID(i, as1s[i]);
					_threads[i].setDaemon(true);
					_threads[i].start();
				}
				for (int i = 0; i < nt; i++) {
					_threads[i].join();
				}
				if (_optimal != null) {
					return _optimal._x;
				} else {
					return null;  // itc-20220116: added if-else
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	
	/**
	 * API method to invoke the algorithm. Forces problem conversion (LIP to
	 * Binary) procedure to take place.
	 * @param A the _m &times; _n augmented matrix
	 * @param b the _m &times; 1 augmented RHS vector
	 * @param c the _n &times; 1 cost vector
	 * @param nt the number of threads to use
	 * @param solverFac AddSlvrFac maybe null
	 * @return IntArray1SparseVector the best solution found
	 */
	public static IntArray1SparseVector runMainWithConversion(
		                                    IntArray2SparseMatrix A,
		                                    IntArray1SparseVector b,
		                                    IntArray1SparseVector c,
		                                    int nt,
		                                    AddSlvrFac solverFac) {
		synchronized (_classLock) {
			final long start = System.currentTimeMillis();
			final AdditiveSolverMT[] as1s = new AdditiveSolverMT[nt];
			try {
				_conversionNeeded = true;
				for (int i = 0; i < nt; i++) {
          as1s[i] = solverFac == null ?
					            new AdditiveSolverMT() : solverFac.newInstance();
					as1s[i].initialize(A, b, c, i == 0);  // debug only 1st thread
				}
				_threads = new ThreadWID[nt];
				for (int i = 0; i < nt; i++) {
					_threads[i] = new ThreadWID(i, as1s[i]);
					_threads[i].setDaemon(true);
					_threads[i].start();
				}
				for (int i = 0; i < nt; i++) {
					_threads[i].join();
				}
				final long dur = System.currentTimeMillis()-start;
				System.out.println("Done in "+dur+" msecs (max #nodes="+_maxnum+")");
				AdditiveSolverMT.printFinalResults();
				if (_optimal != null) {
					return getOptimalOriginalX();
				} else {
					return null;  // itc-20220116: added if-else
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	
	/**
	 * run the program from the command-line as: 	 
	 * <CODE>
   * java &lt;vm_args&gt; -cp &lt;classpath&gt;
	 * popt4jlib.LIP.AdditiveSolverMT
	 * &lt;filename&gt; [contains_integer_variables?(T)] [num_threads(1)]
	 * </CODE>. 
	 * The file containing the problem data must have the following format:
	 * <PRE>
	 * &lt;m&gt; // _m number of constraints (augmented by 1)
	 * &lt;n&gt; // _n number of variables
	 * A_[0,.] row 0 values (one line): val1, val2, ... valn
	 * ...
	 * A_[m-2,.] row m-2 values (one line): val1, val2, ... valn
	 * b_[0] first constraint rhs value
	 * ...
	 * b_[m-2] last constraint rhs value
	 * c_[.] objective coefficients values (one line): val1, val2, ... valn
	 * </PRE>.
	 * @param args String[] see the top-line description.
	 */
	public static void main(String[] args) {
		String datafile = args[0];
		if (args.length > 1) {
			_conversionNeeded = args[1].toLowerCase().startsWith("t");
		}
		int nt = 1;
		if (args.length > 2) {
			nt = Integer.parseInt(args[2]);
		}
		synchronized (_classLock) {
			_threads = new ThreadWID[nt];
			AdditiveSolverMT[] as1s = new AdditiveSolverMT[nt];
			try {
				for (int i = 0; i < nt; i++) {
					as1s[i] = new AdditiveSolverMT();
					as1s[i].initialize(datafile);
				}
				for (int i = 0; i < nt; i++) {
					_threads[i] = new ThreadWID(i, as1s[i]);
					_threads[i].setDaemon(true);
					_threads[i].start();
				}
				for (int i = 0; i < nt; i++) {
					_threads[i].join();
				}
				AdditiveSolverMT.printResults();
				if (_conversionNeeded) {
					AdditiveSolverMT.printFinalResults();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}
}


/**
 * auxiliary class provides id for each thread. Not part of the public API.
 */
final class ThreadWID extends Thread {
	private int _id;
	private boolean _isIdle;
	private Runnable _r;

	
	public ThreadWID(int id, Runnable r) {
		_id = id;
		_r = r;
	}

	
	public int getID() {
		return _id;
	}

	
	public synchronized void setIdle(boolean v) {
		_isIdle = v;
	}

	
	public synchronized boolean isIdle() {
		return _isIdle;
	}

	
	public void run() {
		_r.run();
	}
}

