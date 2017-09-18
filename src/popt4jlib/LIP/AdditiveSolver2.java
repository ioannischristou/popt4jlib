package popt4jlib.LIP;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.StringTokenizer;
import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;


/**
 * class implements a multi-threaded version of Balas' ADDITIVE algorithm for 
 * Linear Integer Programming.
 * In particular, the solver solves Integer Programming problems of the form:
 * min. c'x
 * s.t. Ax &ge; b
 *      x &ge; 0
 *      x integer vector
 * where the matrix A, and the vectors b,c are given and contain integer values.
 * In case the vector x is not restricted to binary values, then the system
 * Ax &ge; b must be able to upper-bound each component of the vector x, so as
 * to bring the program into the so-called standard binary format. 
 * Notice that this implementation uses sparse vectors which is likely to be
 * useful in Integer Programs arising from applications of Graph Theory (e.g.
 * packing problems etc., especially on sparse graphs).
 * Also notice that this program is well-suited to the Fork/Join Task 
 * programming framework (Doug Lea and before him, the Cilk project at MIT).
 * Nevertheless, in several experiments conducted, each Branch-and-Bound Node
 * opened has significant processing to do before generating its new branches
 * and therefore the contention among the threads is kept to very acceptable
 * levels.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class AdditiveSolver2 implements Runnable {
	
	private static Node _optimal;
	private static ON _head;
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
	
	private static boolean _conversionNeeded=true;

	private static boolean _inited=false;
	private static boolean _inprogress=false;
	
	
	/**
	 * auxiliary inner class holding nodes in the Branch-and-Bound Tree of the
	 * algorithm.
	 */
	private static class Node {
		private int _id;                   // node id
		private IntArray1SparseVector _x;  // node vars
		//private Node _l;                   // link to the 0-branch
		//private Node _r;                   // link to the 1-branch
		private int _z=Integer.MAX_VALUE;  // lower bound
		private boolean _status=true;      // open<--T, closed<--F
	}
	
	
	/**
	 * alternative to using <CODE>java.util.LinkedList&lt;Node&gt;</CODE> (which 
	 * is a doubly-linked list implementation of list).
	 */
	private static class ON {
		private ON _prev;
		private ON _next;
		private Node _v;

		private static int _numOpenNodes=0;
		
		private static void incrNumOpenNodes() { 
			synchronized (AdditiveSolver2.class) {
				++_numOpenNodes; 
			}
		}
		private static void decrNumOpenNodes() { 
			synchronized(AdditiveSolver2.class) {			
				--_numOpenNodes; 
				if (_numOpenNodes==0) {
					AdditiveSolver2.class.notifyAll();
				}
			}
		}
		private static int getNumOpenNodes() { 
			int ret=0;
			synchronized (AdditiveSolver2.class) {
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
		int nz=x.getNumNonZeros();
		for(int i=0; i<nz; i++) {
			if (x.getIthNonZeroVal(i)==-1) return false;
		}
		return true;
	}
	
	
	/**
	 * matrix-to-vector multiplication. 
	 * @param A IntArray2SparseMatrix
	 * @param x IntArray1SparseMatrix
	 * @return IntArray1SparseVector
	 */
	private static IntArray1SparseVector multiply(IntArray2SparseMatrix A, 
		                                            IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(A.getNumRows());
		for (int i=0; i<result.getNumCoords(); i++) {
			result.setCoord(i, A.getIthRow(i).innerProduct(x));
		}
		return result;
	}
	
	
	/**
	 * checks if Ax&ge;b.
	 * @param Ax IntArray1SparseVector
	 * @param b IntArray1SparseVector
	 * @return boolean true iff Ax&ge;b
	 */
	private static boolean check(IntArray1SparseVector Ax, 
		                           IntArray1SparseVector b) {
		if (Ax.getNumCoords()!=b.getNumCoords())
			throw new IllegalArgumentException("Ax and b dims don't match");
		int Ax_nz = Ax.getNumNonZeros();
		for (int i=0; i<Ax_nz; i++) {
			int axipos = Ax.getIthNonZeroPos(i);
			int axival = Ax.getIthNonZeroVal(i); // (int) Ax.getCoord(axipos);
			int bival = (int) b.getCoord(axipos);
			if (axival<bival) return false;
		}
		int b_nz = b.getNumNonZeros();
		for (int i=0; i<b_nz; i++) {
			int bipos = b.getIthNonZeroPos(i);
			int bival = b.getIthNonZeroVal(i); // (int) b.getCoord(bipos);
			int axival = (int) Ax.getCoord(bipos);
			if (axival<bival) return false;
		}
		return true;
	}
	
	
	/**
	 * return the number of components that have value -1.
	 * @param x IntArray1SparseVector
	 * @return int
	 */
	private static int countFreeVars(IntArray1SparseVector x) {
		int n=0;
		int nz = x.getNumNonZeros();
		for (int i=0; i<nz; i++) {
			//int ipos = x.getIthNonZeroPos(i);
			//if (Integer.compare((int)x.getCoord(ipos),-1)==0) ++n;
			if (x.getIthNonZeroVal(i)==-1) ++n;
		}
		return n;
	}

	
	/**
	 * sort the values of the argument in ascending order and return them in a new
	 * sparse vector. Leaves the argument intact (doesn't modify argument).
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 */
	static IntArray1SparseVector sortAsc(final IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(x.getNumCoords());
		int[] data = x.getNonDefValues();
		if (data==null) return result;
		Arrays.sort(data);
		int firstPosPos = data.length;
		for(int i=0; i<data.length; i++) {
			if (data[i]>0) {
				firstPosPos=i;
				break;
			}
		}
		int i=0;
		for (; i<firstPosPos; i++) {
			result.setCoord(i, data[i]);
		}
		i = x.getNumCoords()-(data.length-firstPosPos);
		for (int j=firstPosPos; j<data.length; j++) {
			result.setCoord(i++, data[j]);
		} 
		return result;
	}

	
	/**
	 * same as <CODE>sortDesc(x)</CODE> but sorts elements in descending order.
	 * Leaves argument intact (doesn't modify argument).
	 * @param x IntArray1SparseVector
	 * @return IntArray1SparseVector
	 */
	static IntArray1SparseVector sortDesc(final IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(x.getNumCoords());
		int[] data = x.getNonDefValues();
		if (data==null) {  // no non-zero values
			return result;
		}
		Arrays.sort(data);
		int firstPosPos = data.length;
		for(int i=0; i<data.length; i++) {
			if (data[i]>0) {
				firstPosPos=i;
				break;
			}
		}
		int i=data.length-1;
		int j=0;
		for (; i>=firstPosPos; i--) {
			result.setCoord(j++, data[i]);
		}
		i = x.getNumCoords()-firstPosPos;
		for (j=firstPosPos-1; j>=0; j--) {
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
		int res=0;
		for (int i=start;i<=end;i++) res += x.getCoord(i);
		return res;
	}
	
	
	/**
	 * set all free (ie with value -1) components of the vector passed in as first 
	 * argument to the value specified in the second argument.
	 * @param x IntArray1SparseVector
	 * @param l int
	 */
	private static void setFreeVars(IntArray1SparseVector x, int l) {
		for (int i=0; i<x.getNumCoords(); i++) {
			int ival = (int)x.getCoord(i);
			if (ival==-1) x.setCoord(i, l);
		}
	}
	
	
	// routines for node selection 
	
	
	/**
	 * puts the <CODE>Node</CODE> nd inside the list pointed to by 
	 * <CODE>_head</CODE>, in order so that the Nodes' <CODE>_v._z</CODE> value is 
	 * in ascending order (smallest value first). Synchronized operation.
	 * @param nd Node
	 */
	private synchronized static void putInOrder(Node nd) {
		int z = nd._z;
		ON t = _head;
		ON g = null;
		while (t!=null && t._v._z<=z) {
			g=t;
			t=t._next;
		}
		ON tnew = new ON();
		tnew._v=nd;
		tnew._next=t;
		tnew._prev=g;
		if (g!=null) {
			g._next=tnew;
		} else {
			if (_head!=null) _head._prev=tnew;
			_head=tnew;
		}
		if (t!=null) {
			t._prev=tnew;
		}
		AdditiveSolver2.class.notifyAll();
	}
	
	
	/**
	 * removes Node nd from the list pointed to by <CODE>_head</CODE>. Because we
	 * are picking nodes from the top of the list, this is a constant time 
	 * operation (the node to be deleted is essentially the one that was just 
	 * selected and processed, take or leave its two children). Synchronized
	 * operation.
	 * @param nd Node
	 */
	private synchronized static void remove(Node nd) {
		ON t = _head;
		while (t._v!=nd) {
			t=t._next;
		}
		ON par = t._prev;
		ON nxt = t._next;
		if (par!=null) {
			par._next=nxt;
		}
		if (nxt!=null) {
			nxt._prev=par;
		}
		if (t==_head) {
			_head=_head._next;
			if (_head!=null) _head._prev=null;
		}
		// dispose(t);
	}
	
	
	/**
	 * return the first element in the list pointed to by <CODE>_head</CODE>. It
	 * also removes the node containing this element from the list.
	 * Constant-time (synchronized) operation.
	 * @return Node
	 */
	private synchronized static Node select() {
		while (_head==null && ON.getNumOpenNodes()>0) {
			try {
				AdditiveSolver2.class.wait();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		if (_head==null) return null;
		Node ret = _head._v;
		remove(_head._v);
		return ret;
	}
	
	
	/**
	 * sets the status of the Node argument to false (closed).
	 * @param current Node
	 */
	private void close(Node current) {
		if (current._status) ON.decrNumOpenNodes();
		current._status=false;
		//System.err.println("-- closing node "+current._id);
		//remove(current);
	}
	
	
	// computing routines

	
	/**
	 * updates the argument's <CODE>_z</CODE> value, as well as
	 * <CODE>_zstar,_zbar,_optimal</CODE> members if appropriate. Synchronized
	 * operation.
	 * Notice that the current node will always be complete (will have no free 
	 * variables) when this method is invoked.
	 * @param current Node
	 */
	private synchronized static void updateZ(Node current) {
		IntArray1SparseVector xc = current._x;
		//int z = (int) VecUtil.innerProduct(_c, xc);
		// faster inner-product computation as we know xc' non-zeros are 1s.
		int z = 0;
		int xnz = xc.getNumNonZeros();
		for (int i=0; i<xnz; i++) z += _c.getCoord(xc.getIthNonZeroPos(i));
		// end z computation
		current._z = z;
		if (z < _zbar) _zbar=z;
		if (z < _zstar) {
			_zstar=z;
			_optimal=current;
			System.err.println("found new better solution z="+z+" (xc="+xc+")");
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
		if (zbar<_csum) {
			_b.setCoord(_m-1, _cstar+1-zbar);  
			for (int j=0;j<_n;j++) {
				if (((int)current._x.getCoord(j))==-1) 
					_A.setCoord(_m-1, j, (int)(-_c.getCoord(j))); 
				else
					_A.setCoord(_m-1, j, 0); 
			}
		}
	}
	
	
	/**
	 * processes the argument Node, possibly modifying the 2nd argument as well.
	 * @param current Node
	 * @param bbar IntArray1SparseVector
	 * @return int the sum of the <CODE>_c</CODE> vector's coefficients for those
	 * components for which <CODE>current._x</CODE>' components are set to 1. 
	 */
	private int computeNode(Node current, IntArray1SparseVector bbar) {
		int cstar = 0;
		bbar.reset();
		int nz = current._x.getNumNonZeros();
		for (int i=0; i<nz; i++) {
			int ipos = current._x.getIthNonZeroPos(i);
			int val = (int) current._x.getIthNonZeroVal(i);
			if (val==1) {
				cstar += (int)_c.getCoord(ipos);
				for (int j=0; j<_m; j++) {
					int aji = _A.getCoord(j, ipos);
					if (aji!=0) bbar.setCoord(j, bbar.getCoord(j)+aji);
				}
			}
		}
		// set bbar=_b - bbar
		/*
		IntArray1SparseVector bbar_old = (IntArray1SparseVector) bbar.newInstance();
		int bbarnz = bbar.getNumNonZeros();
		for (int i=0; i<bbarnz; i++) {
			int ipos = bbar_old.getIthNonZeroPos(i);
			int val = bbar_old.getIthNonZeroVal(i);
			bbar.setCoord(ipos, _b.getCoord(ipos)-val);
		}
		int bnz = _b.getNumNonZeros();
		for (int i=0; i<bnz; i++) {
			int ipos = _b.getIthNonZeroPos(i);
			int val = _b.getIthNonZeroVal(i);
			int bbar_old_val = (int)bbar_old.getCoord(ipos);
			bbar.setCoord(ipos, val-bbar_old_val);
		}
		*/
		for (int i=0; i<_m; i++) {
			int nvi = (int)_b.getCoord(i)-(int)bbar.getCoord(i);
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
		int i=0;
		do {
			if (_bbar.getCoord(i)>0) {
				_Pplus.setCoord(i, 0);
				int cxnz = current._x.getNumNonZeros();
				for (int j=0; j<cxnz; j++) {
					int jpos = current._x.getIthNonZeroPos(j);
					int jval = current._x.getIthNonZeroVal(j);
					if (jval==-1) {
						int Aij = _A.getCoord(i, jpos);
						if (Aij>0) {
							int pplusival = (int)_Pplus.getCoord(i);
							_Pplus.setCoord(i, pplusival+Aij);
						}
					}
				}
				if (_Pplus.getCoord(i)<_bbar.getCoord(i)) feasible=false;
			}
			++i;
		} while (feasible && i!=_m);
		if (!feasible) close(current);
		return feasible;
	}
	
	
	/**
	 * performs the Additive Algorithm's fix-sum-test to set all variables that
	 * can be set to 0 or 1 at their values, and checks for completeness of 
	 * result.
	 * @param current Node 
	 */
	private void fixSumTestVars(Node current) {
		IntArray1SparseVector copy = (IntArray1SparseVector) current._x.newInstance();
		int cxnz = copy.getNumNonZeros();
		for (int j=0; j<cxnz; j++) {
			int jpos = copy.getIthNonZeroPos(j);
			int jval = copy.getIthNonZeroVal(j);
			if (jval==-1) {  // j-var is free
				int pplusnz = _Pplus.getNumNonZeros();
				for (int i=0; i<pplusnz; i++) {
					int ipos = _Pplus.getIthNonZeroPos(i);
					int ival = _Pplus.getIthNonZeroVal(i);
					int Aij = _A.getCoord(ipos, jpos);
					if (ival>0 && ival-Math.abs(Aij)<_bbar.getCoord(ipos)) {
						if (Aij>0) current._x.setCoord(jpos, 1);
						else if (Aij<0) current._x.setCoord(jpos, 0);
					}
				}
			}
		}
		if (complete(current._x)) {
			//System.err.println("Node complete");
			close(current);
			IntArray1SparseVector Ax = multiply(_A,current._x);
			boolean feasible = check(Ax,_b);
			if (feasible) updateZ(current);
		}
	}
	
	
	/**
	 * upper- and lower-bounds the argument <CODE>Node</CODE> object. As a result
	 * the operation may close the node (as infeasible)
	 * @param current 
	 */
	private void LU(Node current) {
		int L=0;
		int U=_n+1;  // or is it n?
		for (int i=0; i<_m; i++) {
			IntArray1SparseVector s = sortDesc(_A.getIthRow(i));
			int Li=0;
			int Ui=_n;  // or is it n?
			int laux=0;
			int uaux = sum(0,_n-1,s);
			int cxnz = current._x.getNumNonZeros();
			int bbari = (int) _bbar.getCoord(i);
			for (int j=0; j<cxnz; j++) {
				int jpos = current._x.getIthNonZeroPos(j);
				int jval = current._x.getIthNonZeroVal(j);
				if (jval==-1) {  // x[j] is free
					if (laux < bbari) {
						++Li;
						laux += s.getCoord(jpos);
					}
					if (uaux < bbari) {
						--Ui;
						uaux -= s.getCoord(_n-1-jpos);
					}
				}
			}  // for j
			if (uaux < bbari) {  // adjust values
				--Ui;
			}
			if (laux < bbari) {
				Li=_n+1;  // or is it n?
			}
			if (Li>L) L=Li;
			if (Ui<U) U=Ui;
		}  // for i
		
		if (L>U) close(current);  // infeasible
		if (L==U) {
			int f = countFreeVars(current._x);
			if (f==L) {
				setFreeVars(current._x,1);  // set all free vars to 1
				updateZ(current);
				close(current);
			}
			if (L==0) {
				setFreeVars(current._x,0);  // set all free vars to 0
				updateZ(current);
				close(current);
			}
		}
		if (L>0) {
			IntArray1SparseVector s1 = sortAsc(_c);
			int Lsum=sum(0,L-1,s1);
			current._z = _cstar + Lsum;
			int zbar = getZBar();
			if (current._z > zbar) {
				close(current);  // current is useless
			}
		}
	}
	
	
	/**
	 * return the free variable index j (column) that minimizes the quantity
	 * \sum_i{(_bbar[i]-_A[i][j])_{+}} (the sum includes the last -augmented- row 
	 * of _A and component of _b).
	 * @param current Node
	 * @return int
	 */
	private int infSum(Node current) {
		int col = 0;
		int mi=Integer.MAX_VALUE;
		int cxnz = current._x.getNumNonZeros();
		for (int j=0; j<cxnz; j++) {
			int jpos = current._x.getIthNonZeroPos(j);
			int jval = current._x.getIthNonZeroVal(j);
			if (jval==-1) {  // x[jpos] is free
				int sm=0;
				for (int i=0; i<_m; i++) {
					int bbari = (int) _bbar.getCoord(i);
					int Aij = _A.getCoord(i, jpos);
					if (bbari > Aij) {
						sm += (bbari-Aij);
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
	 * x_col variable), and puts them in the shared list pointed by 
	 * <CODE>_head</CODE>.
	 * @param col int
	 * @param current Node 
	 */
	private void branch(int col, Node current) {
		// create and compute new Node 1-branch
		Node t1 = new Node();
		synchronized (AdditiveSolver2.class) {
			t1._id = ++_maxnum;
		}
		//System.err.println("cur_x="+current._x);
		t1._x = (IntArray1SparseVector) current._x.newInstance();
		t1._x.setCoord(col, 1);  // 1-Branch
		t1._status = true;  // node is OPEN for business
		ON.incrNumOpenNodes();
		IntArray1SparseVector b1 = new IntArray1SparseVector(_m);
		int cstar1 = computeNode(t1,b1);
		t1._z = cstar1;
		putInOrder(t1);
		//current._l = t1;  // update current node
		//System.err.println("creating new node "+_maxnum);
		// create and compute new Node 0-branch
		Node t2 = new Node();
		synchronized (AdditiveSolver2.class) {
			t2._id = ++_maxnum;
		}
		t2._x = (IntArray1SparseVector) current._x.newInstance();
		t2._x.setCoord(col, 0);  // 0-Branch
		t2._status = true;  // node is OPEN for business
		ON.incrNumOpenNodes();
		IntArray1SparseVector b2 = new IntArray1SparseVector(_m);
		int cstar2 = computeNode(t2,b2);
		t2._z = cstar2;
		putInOrder(t2);
		//current._r = t2;  // update current node
		//System.err.println("creating new node "+_maxnum);
		close(current);
	}
	
	
	// miscellaneous routines
	
	
	/**
	 * converts a (bounded) Linear Integer Program to a Binary Program.
	 * @throws IllegalStateException if the problem cannot be converted to binary
	 * format.
	 */
	private void convertToBin() {
		double[] bound = new double[_n];
		boolean format = true;
		boolean pass = true;
		for (int k=0; k<_n; k++) bound[k] = -1.0;
		while (format && pass) {
			pass=false;
			for (int j=0; j<_n; j++) {
				int cn=0;
				while (_A.getCoord(cn, j)>=0 && cn<_m-1) { 
					++cn;
				}	
				if (cn==_m-1) {  // x_j has no upper bound
					throw new IllegalStateException("Problem cannot be converted to "+
						                              "Additive Format");
					//System.err.println("Problem cannot be converted to Additive Format");
					//format=false;
				}
				else {  // _A[cn,j]<0
					int cni = cn;
					for (cn=cni; cn<_m-1; cn++) {
						double bsum = _b.getCoord(cn) / _A.getCoord(cn, j);
						int k=0;  
						boolean nobsum = false;
						while (k<=_n-1) {  // or is it _n?
							double coef = (double)_A.getCoord(cn, k) / (double)_A.getCoord(cn, j);
							if (k!=j) {
								if (coef<0) {
									if (Double.compare(bound[k],-1.0)==0) {
										nobsum=true;
									} else bsum -= Math.floor(bound[k])*coef;
								}
							}
							++k;
						}
						if (!nobsum) {
							double tbsum = Math.floor(bsum);
							if (bound[j]>tbsum || 
								  Double.compare(bound[j], -1.0)==0) {
								bound[j]=tbsum;
								//System.err.println("improving bound for j="+j+" bound="+bound[j]);
								pass=true;
							}
						}
					}
				}
			}  // for xj vars
		}  // while format and pass
		
		if (format) {
			double cr = Math.log(2);
			int vs = 0;  // #bin_vars
			for (int k=0; k<_n; k++) {
				if (bound[k]<=0) {
					throw new IllegalStateException("problem cannot be converted to "+
						                              "Additive format");
				}
				double v = Math.log(bound[k])/cr;
				int vi = (int)Math.ceil(v);
				_l.setCoord(k, vi);  // #bin vars for x[k] 
				vs += vi;	
			}
			//System.err.println("#bin vars="+vs);
			// update vector c
			int j=0;
			_remainder=0;
			double[] nc = new double[vs];
			for (int i=0; i<_n; i++) {
				int li = (int)_l.getCoord(i);
				for (int k=0; k<=li-1; k++) {  // or is it?
					int ci = (int) _c.getCoord(i);
					nc[k+j]=Math.abs(Math.pow(2,k)*ci);
					if (ci<0) _remainder += nc[k+j];  // or is it nc[k+j+1]?
				}
				j += li;
			}
			// update matrix A
			IntArray2SparseMatrix NA = new IntArray2SparseMatrix(_m,vs);
			double[] nb = new double[_m];
			for (int p=0; p<_m-1; p++) {
				j=0;
				nb[p]=_b.getCoord(p);
				for (int i=0; i<_n; i++) {
					int li = (int)_l.getCoord(i);
					for (int k=0; k<=li-1; k++) {
						NA.setCoord(p, k+j, (int)(_A.getCoord(p, i)*Math.pow(2,k)));
						if (_c.getCoord(i)<0) {
							int na = NA.getCoord(p,k+j);
							nb[p] -= na;
							NA.setCoord(p, k+j, -na);
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
			for (j=0; j<_n; j++) {
				_c.setCoord(j, nc[j]);
				/*
				_A.setCoord(_m-1, j, 0);  // update last row
				for (int i=0; i<_m-1; i++) {
					_A.setCoord(i, j, NA.getCoord(i, j));
					_b.setCoord(i, nb[i]);
				}
				*/
			}
			_A = NA;
			// update _b
			for (int i=0; i<_m-1; i++) _b.setCoord(i, nb[i]);
		}  // if format
		
		// debug
		System.err.println("Binary Format:");
		System.err.println("Constraints: ");
		for (int i=0; i<_m; i++) {
			System.err.println("");
			for (int j=0; j<_n; j++) {
				System.err.print(_A.getCoord(i, j)+" ");
			}
			System.err.println(">= " + _b.getCoord(i));
		}
		System.err.println("");
		System.err.println("objective coefficients vector c:");
		System.err.println(_c);
		System.err.println("Conversion Procedure Completed.");
	}
	
	
	/**
	 * initialize the data structures for the solver, reading data from the file
	 * containing the problem data. Each thread participating in the program 
	 * execution calls this method to read the matrix A and the vector b in its
	 * own memory.
	 * @param filename String
	 * @throws Exception 
	 */
	private void initialize(String filename) throws Exception {
		synchronized (AdditiveSolver2.class) {
			while (_inprogress) {
				try {
					AdditiveSolver2.class.wait();
				}
				catch(InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			if (!_inited) _inprogress=true;
			else if (_inited) {
				// still need to set _A and _b for this object
				BufferedReader br = new BufferedReader(new FileReader(filename));
				String line = br.readLine();
				Integer.parseInt(line);  // _m 
				Integer.parseInt(br.readLine());  // _n 
				// read-in (again) matrix A
				_A = new IntArray2SparseMatrix(_m,_n);
				for (int i=0; i<_m-1; i++) {
					line = br.readLine();
					StringTokenizer st = new StringTokenizer(line);
					int j=0;
					while (st.hasMoreTokens()) {
						int aij = Integer.parseInt(st.nextToken());
						_A.setCoord(i, j++, aij);
					}
				}
				// last row of _A is already zero.
				// read-in constraints rhs vector b
				_b = new IntArray1SparseVector(_m);
				StringTokenizer st = new StringTokenizer(br.readLine());
				for (int i=0; i<_m-1; i++) {
					int bi = Integer.parseInt(st.nextToken());
					//System.err.println("read b["+i+"]="+bi);
					_b.setCoord(i, bi);
				}
				// last component of _b is already zero
				br.close();
				// debug 
				System.err.println("Thread completed initialize() routine.");
				return;
			}
		}
		BufferedReader br = new BufferedReader(new FileReader(filename));
		String line = br.readLine();
		_m = Integer.parseInt(line);  // _m is the "augmented" number of constraints
		_n = Integer.parseInt(br.readLine());  // _n is the number of columns
		// read-in matrix A
		_A = new IntArray2SparseMatrix(_m,_n);
		for (int i=0; i<_m-1; i++) {
			line = br.readLine();
			StringTokenizer st = new StringTokenizer(line);
			int j=0;
			while (st.hasMoreTokens()) {
				int aij = Integer.parseInt(st.nextToken());
				_A.setCoord(i, j++, aij);
			}
		}
		// last row of _A is already zero.
		// read-in constraints rhs vector b
		_b = new IntArray1SparseVector(_m);
		StringTokenizer st = new StringTokenizer(br.readLine());
		for (int i=0; i<_m-1; i++) {
			int bi = Integer.parseInt(st.nextToken());
			//System.err.println("read b["+i+"]="+bi);
			_b.setCoord(i, bi);
		}
		// last component of _b is already zero
		// read-in objective coefficients c
		_c = new IntArray1SparseVector(_n);
		st = new StringTokenizer(br.readLine());
		for (int j=0; j<_n; j++) {
			int cj = Integer.parseInt(st.nextToken());
			_c.setCoord(j, cj);
		}
		br.close();
		
		_co = new IntArray1SparseVector(_n);
		_l = new IntArray1SparseVector(_m);
		
		if (_conversionNeeded) convertToBin();
				
		Node current = new Node();
		current._id = 0;  // redundant
		//_current._l = null;  // redundant
		//_current._r = null;  // redundant
		current._z = Integer.MAX_VALUE;
		current._status = true;
		ON.incrNumOpenNodes();
		current._x = new IntArray1SparseVector(_n);
		for (int i=0; i<_n; i++) 
			current._x.setCoord(i, -1);  // initially, all vars are free
		
		_head = new ON();
		_head._v = current;
		_head._next = null;  // redundant
		_head._prev = null;  // redundant
		
		_maxnum = 0;  // redundant
		_optimal = current;
		_zbar = Integer.MAX_VALUE;
		_zstar = Integer.MAX_VALUE;
		
		synchronized (AdditiveSolver2.class) {
			_inprogress=false;
			_inited=true;
			AdditiveSolver2.class.notifyAll();
		}
		
		// debug
		System.err.println("initialize() routine completed.");
	}
	

	/**
	 * prints results.
	 */
	private static synchronized void printResults() {
		if (_optimal!=null) {
			System.out.println("***OPTIMAL SOLUTION FOUND***");
			System.out.println("X*="+_optimal._x);
			System.out.println("Z*="+_optimal._z);
			System.out.print("STATS: #nodes="+_maxnum);
			System.out.println(" Optimal nodeid="+_optimal._id);
		} else {
			System.out.println("***PROBLEM INFEASIBLE***");
		}
	}
	
	
	/**
	 * prints results for the original problem if conversion was needed.
	 */
	private static synchronized void printFinalResults() {
		if (_optimal!=null) {
			int z = _optimal._z - _remainder;
			System.out.println("***ORIGINAL PROBLEM RESULTS:***");
			System.out.println("ORIGINAL Z*="+z);
			System.out.print("ORIGINAL X*=[ ");
			int k=0;
			for (int i=0; i<_no; i++) {
				int xs=0;
				for (int j=0; j<=_l.getCoord(i)-1; j++) {
					if (_co.getCoord(i)>=0) {
						xs += Math.pow(2, j)*_optimal._x.getCoord(k+j);
					}
					else {
						xs += Math.pow(2, j)*(1-_optimal._x.getCoord(k+j));
					}
				}
				System.out.print(xs+" ");
				k += _l.getCoord(i);
			}
			System.out.println("]");
		}
	}
	

	/**
	 * the <CODE>run()</CODE> method of the <CODE>Runnable</CODE> interface, 
	 * runs the main loop of the solver until the optimal solution is found.
	 * It iteratively selects the first (best) <CODE>Node</CODE> n from the 
	 * (shared)list of open nodes, imposes any constraints on it, computes it
	 * (updating the <CODE>_bbar</CODE> vector along the way), applies the tests
	 * it knows on it, and either closes the node as infeasible or optimal, or 
	 * else branches on the chosen variable, by putting two new <CODE>Node</CODE>
	 * objects with appropriate values for the chosen variable on the shared list
	 * of open nodes.
	 */
	public void run() {
    _bbar = new IntArray1SparseVector(_m);
		_Pplus = new IntArray1SparseVector(_m);
		_csum = 0;
		int cnz = _c.getNumNonZeros();
		for (int i=0; i<cnz; i++) {
			_csum += _c.getIthNonZeroVal(i);
		}
		do {
			Node current = select();
			if (current==null) break;
			IntArray1SparseVector x = current._x;
			_cstar = 0;
			cnz = x.getNumNonZeros();
			for (int i=0; i<cnz; i++) {
				if (x.getIthNonZeroVal(i)==1) {
					int ipos = x.getIthNonZeroPos(i);
					_cstar += _c.getCoord(ipos);
				}
			}
			imposeConstraint(current);
			_cstar = computeNode(current, _bbar);
			//System.err.println("processing node "+current._id +" x="+x);
			boolean feasible = sumTest(current);
			if (feasible) {
				fixSumTestVars(current);
				if (current._status) {
					LU(current);
					if (current._status) {
						int column = infSum(current);
						branch(column, current);						
					}
				}
			}
		} while (true);  // _head!=null
	}
	
	
	/**
	 * run the program from the command-line as:
	 * <CODE>
	 * java &lt;vm_args&gt; -cp &lt;classpath&gt; popt4jlib.LIP.AdditiveSolver2 
	 * &lt;filename&gt; [contains_integer_variables?(T)] [num_threads(1)]
	 * </CODE>
	 * @param args 
	 */
	public static void main(String[] args) {
		String datafile = args[0];
		if (args.length>1) {
			_conversionNeeded = Boolean.parseBoolean(args[1]);
		}
		int nt = 1;
		if (args.length>2) {
			nt = Integer.parseInt(args[2]);
		}
		Thread[] threads = new Thread[nt];
		try {
		  for (int i=0; i<nt; i++) {
			  AdditiveSolver2 as2 = new AdditiveSolver2();
			  as2.initialize(datafile);
			  threads[i] = new Thread(as2);
			  threads[i].start();
		  }
			for (int i=0; i<nt; i++) 
				threads[i].join();
			AdditiveSolver2.printResults();
			if (_conversionNeeded) AdditiveSolver2.printFinalResults();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}		
	}
}
