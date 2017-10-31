package popt4jlib.LIP;

import popt4jlib.*;

/**
 * sparse 2-D matrix class containing integers. The representation is row-based.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class IntArray2SparseMatrix {
	private IntArray1SparseVector[] _rows;
	
	public IntArray2SparseMatrix(int rows,int cols) {
		_rows = new IntArray1SparseVector[rows];
		for (int i=0; i<rows; i++) {
			_rows[i] = new IntArray1SparseVector(cols);
		}
	}
	
	
	/**
	 * copy constructor performs deep copy of the argument passed in.
	 * @param A 2D matrix
	 */
	public IntArray2SparseMatrix(IntArray2SparseMatrix A) {
		int rows = A.getNumRows();
		_rows = new IntArray1SparseVector[rows];
		for (int i=0; i<rows; i++) {
			_rows[i] = (IntArray1SparseVector) A.getIthRow(i).newInstance();
		}
	}
	
	
	public int getCoord(int i, int j) {
		return (int) _rows[i].getCoord(j);
	}
	
	
	public int getNumRows() { return _rows.length; }
	
	
	public int getNumCols() { return _rows[0].getNumCoords(); }
	
	
	public void setCoord(int i, int j, int val) {
		_rows[i].setCoord(j, val);
	}
	
	
	public IntArray1SparseVector getIthRow(int r) {
		return _rows[r];
	}
	
	
	public String toString() {
		String res = "";
		for (int row=0; row<_rows.length; row++) {
			res += "| ";
			for (int col=0; col<_rows[0].getNumCoords(); col++) {
				res += _rows[row].getCoord(col);
				if (col<_rows[0].getNumCoords()-1) res += " ";
				else res += " |\n";
			}
		}
		return res;
	}
}
