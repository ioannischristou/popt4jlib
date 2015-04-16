/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package tests;

import popt4jlib.*;
import graph.*;
import java.util.*;


/**
 * class implements the continuous formulation of the MWIS problem in 
 * combinatorial optimization. The global minimum of this function in [0,1]^n
 * is the negative of the maximum weight independent set on the associated graph
 * as proved in Butenko's Ph.D. thesis.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class MWISNLPFunction implements FunctionIntf {
	private Graph _g;
	
	/**
	 * public sole constructor.
	 * @param g Graph
	 */
	public MWISNLPFunction(Graph g) {
		_g = g;
	}
	

	/**
	 * evaluates the function f(x)=-\Sum_{i=1}^n (1-x_i) \Pi_{(i,j) \in E(G)} x_j.
	 * The domain of definition is [0,1]^n.
	 * @param x Object // double[] or VectorIntf
	 * @param params Hashtable unused
	 * @return double 
	 * @throws IllegalArgumentException 
	 */
	public double eval(Object x, Hashtable params) throws IllegalArgumentException {
		double res = 0.0;
		if (x instanceof double[]) {
			double[] xa = (double[]) x;
			if (xa.length != _g.getNumNodes()) 
				throw new IllegalArgumentException("x not of same length as nodes in _g");
			for (int i=0; i<xa.length; i++) {
				double xai = xa[i];
				if (xai < 0 || xai > 1.0) throw new IllegalArgumentException("x not in [0,1]^n");
				double v = 1-xai;
				Node ni = _g.getNode(i);
				Set nibors = ni.getNborsUnsynchronized();
				Iterator it = nibors.iterator();
				while (it.hasNext()) {
					Node nj = (Node) it.next();
					v *= xa[nj.getId()];
				}
				res += v;
			}
			return -res;
		}
		if (x instanceof VectorIntf) {
			VectorIntf xa = (VectorIntf) x;
			if (xa.getNumCoords() != _g.getNumNodes()) 
				throw new IllegalArgumentException("x not of same length as nodes in _g");
			for (int i=0; i<xa.getNumCoords(); i++) {
				double xai = xa.getCoord(i);
				if (xai < 0 || xai > 1.0) throw new IllegalArgumentException("x not in [0,1]^n");
				double v = 1-xai;
				Node ni = _g.getNode(i);
				Set nibors = ni.getNborsUnsynchronized();
				Iterator it = nibors.iterator();
				while (it.hasNext()) {
					Node nj = (Node) it.next();
					v *= xa.getCoord(nj.getId());
				}
				res += v;
			}
			return -res;
		}
		throw new IllegalArgumentException("x must be double[] or VectorIntf");
	}
}
