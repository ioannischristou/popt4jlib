/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package graph.packing;

import java.io.*;
import graph.Graph;
import parallel.distributed.PDAsynchInitCmd;
import parallel.distributed.PDBatchTaskExecutorSrv;

/**
 * auxiliary class used for the initialization of distributed 
 * <CODE>PDAsynchBatchTaskExecutorWrk</CODE> objects running code for the 
 * distributed version of the BBGASPPacker. Not part of the public API.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDAInitDBBTreeCmd extends PDAsynchInitCmd {
	private Graph _g;
	private double _initbound;
	private String _pdahost;
	private int _pdaport;
	private String _cchost;
	private int _ccport;
	private String _acchost;
	private int _accport;
	
	
	public PDAInitDBBTreeCmd(Graph g, double initbound, 
		                       String pdahost, int pdaport, 
													 String cchost, int ccport, 
													 String acchost, int accport) {
		_g = g;
		_initbound = initbound;
		_pdahost = pdahost;
		_pdaport = pdaport;
		_cchost = cchost;
		_ccport = ccport;
		_acchost = acchost;
		_accport = accport;
	}
	
	
	public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) {
		DBBTree.init(_g, _initbound, _pdahost, _pdaport, _cchost, _ccport, _acchost, _accport);
	}
	
	
}
