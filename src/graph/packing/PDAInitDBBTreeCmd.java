package graph.packing;

import java.io.*;
import graph.Graph;
import parallel.ParallelException;
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
	private String _graphfile;
	private double _initbound;
	private String _pdahost;
	private int _pdaport;
	private String _cchost;
	private int _ccport;
	private String _acchost;
	private int _accport;
	private int _accnotificationsport;
	private boolean _localsearch=false;
	private AllChromosomeMakerClonableIntf _localsearchtype=null;
	private double _ff=0.85;
	private int _tightenboundlevel=Integer.MAX_VALUE;
	private int _maxitersinGBNS2A=100000;
	private boolean _sortmaxsubsets=false;
	private double _avgpercextranodes2add=0.0;
	private boolean _useGWMIN2criterion=false;
	private double _expandlocalsearchfactor=1.0;
	private double _minknownbound=Double.NEGATIVE_INFINITY;
	private int _maxnodechildren=Integer.MAX_VALUE;
	private DBBNodeComparatorIntf _dbbnodecomparator=null;
	private long _seed=0;
	private int _maxNodesAllowed = Integer.MAX_VALUE;
	private int _dbglvl=Integer.MAX_VALUE;
	
	
	/**
	 * sole constructor.
	 * @param graphfile String the path-name of the file containing the graph.
	 * @param initbound
	 * @param pdahost
	 * @param pdaport
	 * @param cchost
	 * @param ccport
	 * @param acchost
	 * @param accport
	 * @param accnotificationsport
	 * @param localsearch
	 * @param localsearchtype
	 * @param ff
	 * @param tightenboundlevel
	 * @param maxitersinGBNS2A
	 * @param sortmaxsubsets
	 * @param avgpercextranodes2add
	 * @param useGWMIN2criterion
	 * @param expandlocalsearchfactor
	 * @param minknownbound
	 * @param maxnodechildren
	 * @param dbbnodecomparator
	 * @param seed
	 * @param maxnodesallowed
	 * @param dbglvl 
	 */
	public PDAInitDBBTreeCmd(String graphfile, double initbound, 
		                       String pdahost, int pdaport, 
													 String cchost, int ccport, 
													 String acchost, int accport, int accnotificationsport,
													 boolean localsearch,
													 AllChromosomeMakerClonableIntf localsearchtype,
													 double ff,
													 int tightenboundlevel,
													 int maxitersinGBNS2A,
													 boolean sortmaxsubsets,
													 double avgpercextranodes2add,
													 boolean useGWMIN2criterion,
													 double expandlocalsearchfactor,
													 double minknownbound,
													 int maxnodechildren,
													 DBBNodeComparatorIntf dbbnodecomparator,
													 long seed,
													 int maxnodesallowed,
													 int dbglvl) {
		_graphfile = graphfile;
		_initbound = initbound;
		_pdahost = pdahost;
		_pdaport = pdaport;
		_cchost = cchost;
		_ccport = ccport;
		_acchost = acchost;
		_accport = accport;
		_accnotificationsport = accnotificationsport;
		_localsearch = localsearch;
		_localsearchtype = localsearchtype;
		_ff = ff;
		_tightenboundlevel = tightenboundlevel;
		_maxitersinGBNS2A = maxitersinGBNS2A;
		_sortmaxsubsets = sortmaxsubsets;
		_avgpercextranodes2add = avgpercextranodes2add;
		_useGWMIN2criterion = useGWMIN2criterion;
		_expandlocalsearchfactor = expandlocalsearchfactor;
		_minknownbound = minknownbound;
		_maxnodechildren = maxnodechildren;
		_dbbnodecomparator = dbbnodecomparator;
		_seed = seed;
		_maxNodesAllowed = maxnodesallowed;
		_dbglvl = dbglvl;
	}
	
	
	/**
	 * sets the debug level for <CODE>utils.Messenger</CODE> class.
	 */
	public void applyOnServer() {
		utils.Messenger.getInstance().setDebugLevel(_dbglvl);
	}
	
	
	/**
	 * calls the <CODE>DBBTree.init()</CODE> method to initialize the 
	 * <CODE>DBBTree</CODE> data structure before the thread-pool of the executor
	 * is created and started.
	 * @param srv PDBatchTaskExecutorSrv unused
	 * @param ois ObjectInputStream unused
	 * @param oos ObjectOutputStream unused
	 */
	public void runProtocol(PDBatchTaskExecutorSrv srv, ObjectInputStream ois, ObjectOutputStream oos) {
		DBBTree.init(_graphfile, null, _initbound, 
			           _pdahost, _pdaport, _cchost, _ccport, 
								 _acchost, _accport, _accnotificationsport,
								 _localsearch,_localsearchtype, _ff, _tightenboundlevel,
								 _maxitersinGBNS2A, _sortmaxsubsets,
								 _avgpercextranodes2add, _useGWMIN2criterion, 
								 _expandlocalsearchfactor, _minknownbound, _maxnodechildren,
								 _dbbnodecomparator, _seed, false, 
								 _maxNodesAllowed, _dbglvl);
	}
	
}
