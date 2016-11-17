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
import popt4jlib.AllChromosomeMakerClonableIntf;

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
	private boolean _localsearch=false;
	private AllChromosomeMakerClonableIntf _localsearchtype=null;
	private double _ff=0.85;
	private int _tightenboundlevel=Integer.MAX_VALUE;
	private boolean _usemaxsubsets=true;
	private int _maxitersinGBNS2A=100000;
	private boolean _sortmaxsubsets=false;
	private double _avgpercextranodes2add=0.0;
	private boolean _useGWMIN2criterion=false;
	private double _expandlocalsearchfactor=1.0;
	private double _minknownbound=Double.NEGATIVE_INFINITY;
	private int _maxnodechildren=Integer.MAX_VALUE;
	private DBBNodeComparatorIntf _dbbnodecomparator=null;

	
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
	
	
	public PDAInitDBBTreeCmd(Graph g, double initbound, 
		                       String pdahost, int pdaport, 
													 String cchost, int ccport, 
													 String acchost, int accport,
													 boolean localsearch,
													 AllChromosomeMakerClonableIntf localsearchtype,
													 double ff,
													 int tightenboundlevel,
													 boolean usemaxsubsets,
													 int maxitersinGBNS2A,
													 boolean sortmaxsubsets,
													 double avgpercextranodes2add,
													 boolean useGWMIN2criterion,
													 double expandlocalsearchfactor,
													 double minknownbound,
													 int maxnodechildren,
													 DBBNodeComparatorIntf dbbnodecomparator) {
		_g = g;
		_initbound = initbound;
		_pdahost = pdahost;
		_pdaport = pdaport;
		_cchost = cchost;
		_ccport = ccport;
		_acchost = acchost;
		_accport = accport;
		_localsearch = localsearch;
		_localsearchtype = localsearchtype;
		_ff = ff;
		_tightenboundlevel = tightenboundlevel;
		_usemaxsubsets = usemaxsubsets;
		_maxitersinGBNS2A = maxitersinGBNS2A;
		_sortmaxsubsets = sortmaxsubsets;
		_avgpercextranodes2add = avgpercextranodes2add;
		_useGWMIN2criterion = useGWMIN2criterion;
		_expandlocalsearchfactor = expandlocalsearchfactor;
		_minknownbound = minknownbound;
		_maxnodechildren = maxnodechildren;
		_dbbnodecomparator = dbbnodecomparator;
	}
	
	
	public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) {
		DBBTree.init(_g, _initbound, _pdahost, _pdaport, _cchost, _ccport, _acchost, _accport,
								 _localsearch,_localsearchtype, _ff, _tightenboundlevel,
								 _usemaxsubsets, _maxitersinGBNS2A, _sortmaxsubsets,
								 _avgpercextranodes2add, _useGWMIN2criterion, 
								 _expandlocalsearchfactor, _minknownbound, _maxnodechildren,
								 _dbbnodecomparator);
	}
	
	
}
