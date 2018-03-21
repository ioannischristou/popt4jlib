package popt4jlib.LIP;

import popt4jlib.IntArray2SparseMatrix;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.StringTokenizer;
import popt4jlib.*;
import popt4jlib.GradientDescent.VecUtil;


/**
 * class implements the ADDITIVE algorithm for Linear Integer Programming.
 * In particular, the solver solves Integer Programming problems of the form:
 * min. c'x
 * s.t. Ax &ge; b
 *      x &ge; 0
 *      x integer vector
 * where the matrix A, and the vectors b,c are given and contain integer values.
 * Notice: this class is deprecated and will soon be removed from the code base.
 * It served as "interim" code translating an old Pascal program into Java.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @deprecated
 * @version 1.0
 */
public class AdditiveSolverDepr {

	private Node _current, _optimal;
	private ON _head;
	private int _maxnum;
	private int _zbar;
	private int _zstar;
	private int _cstar;
	private int _csum;
	private int _remainder;
	private int _row, _col, _column, _m, _n, _no, _dummy;
	private boolean _feasible;
	private IntArray1SparseVector _b, _bbar, _Pplus, _c, _co, _l;
	private IntArray2SparseMatrix _A;

	private static boolean _conversionNeeded=true;


	static class Node {
		private int _id;                   // node id
		private IntArray1SparseVector _x;  // node vars
		private Node _l;                   // link to the 0-branch
		private Node _r;                   // link to the 1-branch
		private Node _p;                   // link to the parent node
		private int _z=Integer.MAX_VALUE;  // lower bound
		private boolean _status=true;      // open<--T, closed<--F

	}


	static class ON {
		private ON _prev;
		private ON _next;
		private Node _v;

	}


	// general purpose routines


	private static boolean complete(IntArray1SparseVector x) {
		int nz=x.getNumNonZeros();
		for(int i=0; i<nz; i++) {
			if (x.getIthNonZeroVal(i)==-1) return false;
		}
		return true;
	}


	private static IntArray1SparseVector multiply(IntArray2SparseMatrix A,
		                                            IntArray1SparseVector x) {
		IntArray1SparseVector result = new IntArray1SparseVector(A.getNumRows());
		for (int i=0; i<result.getNumCoords(); i++) {
			try {
			result.setCoord(i, A.getIthRow(i).innerProduct(x));
			}
			catch (Exception e) {
				System.err.println("i="+i);
				System.err.println("Ai="+A.getIthRow(i));
				System.err.println("x="+x);
				throw new IllegalStateException("multiply() failed");
			}
		}
		return result;
	}


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


	static IntArray1SparseVector sortAsc(IntArray1SparseVector x) {
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


	static IntArray1SparseVector sortDesc(IntArray1SparseVector x) {
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


	private static int sum(int start, int end, IntArray1SparseVector x) {
		int res=0;
		for (int i=start;i<=end;i++) res += x.getCoord(i);
		return res;
	}


	private static void setVars(IntArray1SparseVector x, int l) {
		for (int i=0; i<x.getNumCoords(); i++)
			if (x.getCoord(i)==-1)
				x.setCoord(i, l);
	}


	// routines for node selection


	private static ON putInOrder(Node nd, ON head) {
		int z = nd._z;
		ON t = head;
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
			head._prev=tnew;
			head=tnew;
		}
		if (t!=null) {
			t._prev=tnew;
		}
		return head;
	}


	private static ON remove(Node nd, ON head) {
		ON t = head;
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
		if (t==head) {
			head=head._next;
			if (head!=null) head._prev=null;
		}
		// dispose(t);
		return head;
	}


	private static Node select(ON head) {
		return head._v;
	}


	private static ON close(Node current, ON head) {
		current._status=false;
		System.err.println("-- closing node "+current._id);
		head = remove(current, head);
		return head;
	}


	// computing routines


	private void updateZ(Node current) {
		IntArray1SparseVector xc = current._x;
		int z = (int) VecUtil.innerProduct(_c, xc);
		current._z = z;
		if (z <= _zbar) _zbar=z;
		if (z <= _zstar) {
			_zstar=z;
			_optimal=current;
			System.err.println("found new better solution z="+z+" (xc="+xc+")");
		}
	}


	private void imposeConstraint(Node current, int cstar) {
		if (_zbar<_csum) {
			_b.setCoord(_m-1, cstar+1-_zbar);
			for (int j=0;j<_n;j++) {
				if (((int)current._x.getCoord(j))==-1)
					_A.setCoord(_m-1, j, (int)(-_c.getCoord(j)));
				else
					_A.setCoord(_m-1, j, 0);
			}
		}
	}


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


	private boolean sumTest(Node current,
		                      IntArray1SparseVector bbar,
													IntArray1SparseVector Pplus) {
		boolean feasible = true;
		Pplus.reset();
		int i=0;
		do {
			if (bbar.getCoord(i)>0) {
				Pplus.setCoord(i, 0);
				int cxnz = current._x.getNumNonZeros();
				for (int j=0; j<cxnz; j++) {
					int jpos = current._x.getIthNonZeroPos(j);
					int jval = current._x.getIthNonZeroVal(j);
					if (jval==-1) {
						int Aij = _A.getCoord(i, jpos);
						if (Aij>0) {
							int pplusival = (int)Pplus.getCoord(i);
							Pplus.setCoord(i, pplusival+Aij);
						}
					}
				}
				if (Pplus.getCoord(i)<bbar.getCoord(i)) feasible=false;
			}
			++i;
		} while (feasible && i!=_m);
		if (!feasible) _head = close(current, _head);
		return feasible;
	}


	private void fixSumTestVars(Node current,
		                          IntArray1SparseVector bbar,
															IntArray1SparseVector Pplus) {
		int cxnz = current._x.getNumNonZeros();
		for (int j=0; j<cxnz; j++) {
			int jpos = current._x.getIthNonZeroPos(j);
			int jval = current._x.getIthNonZeroVal(j);
			if (jval==-1) {  // j-var is free
				int pplusnz = Pplus.getNumNonZeros();
				for (int i=0; i<pplusnz; i++) {
					int ipos = Pplus.getIthNonZeroPos(i);
					int ival = Pplus.getIthNonZeroVal(i);
					int Aij = _A.getCoord(ipos, jpos);
					if (ival>0 && ival-Math.abs(Aij)<bbar.getCoord(ipos)) {
						if (Aij>0) current._x.setCoord(jpos, 1);
						else if (Aij<0) current._x.setCoord(jpos, 0);
					}
				}
			}
		}
		if (complete(current._x)) {
			System.err.println("Node complete");
			_head = close(current, _head);
			IntArray1SparseVector Ax = multiply(_A,current._x);
			boolean feasible = check(Ax,_b);
			if (feasible) updateZ(current);
		}
	}


	private void LU(Node current, IntArray1SparseVector bbar, int cstar) {
		int L=0;
		int U=_n+1;  // or is it n?
		for (int i=0; i<_m; i++) {
			IntArray1SparseVector s = sortDesc(_A.getIthRow(i));
			int Li=0;
			int Ui=_n;  // or is it n?
			int laux=0;
			int uaux = sum(0,_n-1,s);
			int cxnz = current._x.getNumNonZeros();
			int bbari = (int) bbar.getCoord(i);
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

		if (L>U) _head = close(current,_head);  // infeasible
		if (L==U) {
			int f = countFreeVars(current._x);
			if (f==L) {
				setVars(current._x,1);  // set all free vars to 1
				updateZ(current);
				_head = close(current,_head);
			}
			if (L==0) {
				setVars(current._x,0);  // set all free vars to 0
				updateZ(current);
				_head = close(current,_head);
			}
		}
		if (L>0) {
			IntArray1SparseVector s1 = sortAsc(_c);
			int Lsum=sum(0,L-1,s1);
			current._z = cstar + Lsum;
			if (current._z > _zbar) {
				_head = close(current,_head);  // current is useless
			}
		}
	}


	private int infSum(IntArray1SparseVector bbar, Node current) {
		int col = 0;
		int mi=Integer.MAX_VALUE;
		int cxnz = current._x.getNumNonZeros();
		for (int j=0; j<cxnz; j++) {
			int jpos = current._x.getIthNonZeroPos(j);
			int jval = current._x.getIthNonZeroVal(j);
			if (jval==-1) {  // x[jpos] is free
				int sm=0;
				for (int i=0; i<_m; i++) {
					int bbari = (int) bbar.getCoord(i);
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


	private ON branch(int col, Node current, ON head) {
		// create and compute new Node 1-branch
		Node t1 = new Node();
		t1._id = ++_maxnum;
		System.err.println("cur_x="+current._x);
		t1._x = (IntArray1SparseVector) current._x.newInstance();
		t1._x.setCoord(col, 1);  // 1-Branch
		t1._p = current;  // parent
		t1._status = true;  // node is OPEN for business
		IntArray1SparseVector b1 = new IntArray1SparseVector(_m);
		int cstar1 = computeNode(t1,b1);
		t1._z = cstar1;
		head = putInOrder(t1,head);
		current._l = t1;  // update current node
		System.err.println("creating new node "+_maxnum);
		// create and compute new Node 0-branch
		Node t2 = new Node();
		t2._id = ++_maxnum;
		t2._x = (IntArray1SparseVector) current._x.newInstance();
		t2._x.setCoord(col, 0);  // 0-Branch
		t2._p = current;  // parent
		t2._status = true;  // node is OPEN for business
		IntArray1SparseVector b2 = new IntArray1SparseVector(_m);
		int cstar2 = computeNode(t2,b2);
		t2._z = cstar2;
		head = putInOrder(t2,head);
		current._r = t2;  // update current node
		System.err.println("creating new node "+_maxnum);
		head = close(current,head);
		return head;
	}


	// miscellaneous routines


	private void convertToBin() {
		double[] bound = new double[_n];
		boolean format = true;
		boolean pass = true;
		for (int k=0; k<_n; k++) bound[k] = -1.0;
		while (format && pass) {
			pass=false;
			for (int j=0; j<_n; j++) {
				int cn=0;
				while (_A.getCoord(cn, j)>0 && cn<_m-1) {
					++cn;
				}
				if (cn==_m-1) {  // x_j has no upper bound
					throw new IllegalArgumentException("Problem cannot be converted to Additive Format");
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
								System.err.println("improving bound for j="+j+" bound="+bound[j]);
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
				double v = Math.log(bound[k])/cr;
				int vi = (int)Math.ceil(v);
				_l.setCoord(k, vi);  // #bin vars for x[k]
				vs += vi;
			}
			System.err.println("#bin vars="+vs);
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


	private void initialize(String filename) throws Exception {
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

		_bbar = new IntArray1SparseVector(_m);
		_Pplus = new IntArray1SparseVector(_m);
		_co = new IntArray1SparseVector(_n);
		_l = new IntArray1SparseVector(_m);

		if (_conversionNeeded) convertToBin();

		_csum = 0;
		int cnz = _c.getNumNonZeros();
		for (int i=0; i<cnz; i++) {
			_csum += _c.getIthNonZeroVal(i);
		}

		_current = new Node();
		_current._p = null;  // redundant
		_current._id = 0;  // redundant
		_current._l = null;  // redundant
		_current._r = null;  // redundant
		_current._z = Integer.MAX_VALUE;
		_current._status = true;
		_current._x = new IntArray1SparseVector(_n);
		for (int i=0; i<_n; i++)
			_current._x.setCoord(i, -1);  // initially, all vars are free

		_head = new ON();
		_head._v = _current;
		_head._next = null;  // redundant
		_head._prev = null;  // redundant

		_maxnum = 0;  // redundant
		_optimal = _current;
		_zbar = Integer.MAX_VALUE;
		_zstar = Integer.MAX_VALUE;

		// debug
		System.err.println("initialize() routine completed.");
	}


	private void printResults() {
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


	private void printFinalResults() {
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


	public static void main(String[] args) {
		String datafile = args[0];
		if (args.length>1) {
			_conversionNeeded = args[1].startsWith("t");
		}
		AdditiveSolverDepr solver = new AdditiveSolverDepr();
		try {
			solver.initialize(datafile);
			do {
				solver._current = select(solver._head);  // select a node
				solver._cstar=0;
				int cnz = solver._current._x.getNumNonZeros();
				for (int i=0; i<cnz; i++) {
					if (solver._current._x.getIthNonZeroVal(i)==1) {
						int ipos = solver._current._x.getIthNonZeroPos(i);
						solver._cstar += solver._c.getCoord(ipos);
					}
				}
				solver.imposeConstraint(solver._current, solver._cstar);
				solver._cstar = solver.computeNode(solver._current, solver._bbar);
				System.err.println("processing node "+solver._current._id+" x="+solver._current._x);
				solver._feasible = solver.sumTest(solver._current,
					                                solver._bbar,
																					solver._Pplus);
				if (solver._feasible) {
					solver.fixSumTestVars(solver._current, solver._bbar, solver._Pplus);
					System.err.println("after fixSumTestVars scstatus="+solver._current._status);
					if (solver._current._status) {
						solver.LU(solver._current, solver._bbar, solver._cstar);
						System.err.println("after LU scstatus="+solver._current._status);
						if (solver._current._status) {
							int column = solver.infSum(solver._bbar, solver._current);
							solver._head = solver.branch(column, solver._current, solver._head);
						}
					}
				}
			} while (solver._head!=null);

			solver.printResults();
			solver.printFinalResults();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
