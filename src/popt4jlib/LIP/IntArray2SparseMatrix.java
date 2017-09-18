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
}
