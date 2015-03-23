/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import parallel.DMCoordinator;
import parallel.DynamicAsynchTaskExecutor;
import parallel.ParallelException;
import parallel.distributed.DFileAccessSrvStatsRequest;
import parallel.distributed.DFileDataVectorReadRequest;
import parallel.distributed.DMsgIntf;
import parallel.distributed.DMsgPassingCoordinatorSrv;
import popt4jlib.VectorIntf;


/**
 * The server is responsible for accessing data files residing in its local
 * file-system (or any file-system it has network access to), according to 
 * requests it receives over the network via sockets, from 
 * <CODE>DataFileAccessClt</CODE> objects and returning the requested vector of
 * <CODE>popt4jlib.VectorIntf</CODE> objects to the requestor.
 * Every file that is requested to be read, must be read-only.
 * Note: This class is optimized for caching VectorIntf objects read from files, 
 * via the SoftReference mechanism etc. 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2014-2015</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataFileAccessSrv {
  private int _port = 7899;  // default port
  private DynamicAsynchTaskExecutor _pool;
  private long _countConns=0;
	
	private Hashtable _dataCache;  // map<String filename, 
	                               //     List<DataVectorChunk{
	                               //            int start, 
	                               //            int to, 
	                               //            SoftRef<Vector<VectorIntf>> data}>>
	
	private final static boolean _SCAN_FOR_PIECES=true;

	
  /**
   * constructor specifying the port the server will listen to, and
   * the max. number of threads in the thread-pool.
   * @param port int if < 1024, the number 7899 is used.
   * @param maxthreads int if < 10000, the number 10000 is used.
   */
  DataFileAccessSrv(int port, int maxthreads) {
    if (port >= 1024)
      _port = port;
    try {
      if (maxthreads<10000)
        maxthreads = 10000;  // the max. number of threads
                            // cannot be restricted as it
                            // may introduce starvation locks.
      _pool=DynamicAsynchTaskExecutor.newDynamicAsynchTaskExecutor(2,
																																	 maxthreads);
			_dataCache = new Hashtable();
    }
    catch (ParallelException e) {
      e.printStackTrace();  // cannot get here
    }
  }


  /**
   * enters an infinite loop, listening for socket connections, and handling
   * the incoming sockets as new Runnable tasks (DFATask objects) that are
   * given to the associated thread-pool.
   * @throws IOException
   * @throws ParallelException
   */
  void run() throws IOException, ParallelException {
    ServerSocket ss = new ServerSocket(_port);
    while (true) {
      Messenger.getInstance().msg("DataFileAccessSrv: waiting for socket connection",0);
      Socket s = ss.accept();
      ++_countConns;
      System.err.println("Total "+_countConns+" socket connections arrived.");
      handle(s, _countConns);
    }
  }


  /**
   * invoke as:
   * <CODE>java -cp &ltclasspath&gt utils.DataFileAccessSrv [port(7899)] [maxthreads(10000)]</CODE>
   * @param args String[]
   */
  public static void main(String[] args) {
    int port = -1;
    int maxthreads = 10000;
    DataFileAccessSrv srv=null;
		Messenger.getInstance().setDebugLevel(1);
    if (args.length>0) {
      port = Integer.parseInt(args[0]);
      if (args.length>1)
        maxthreads = Integer.parseInt(args[1]);
    }
    srv = new DataFileAccessSrv(port, maxthreads);
    try {
      srv.run();
    }
    catch (Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }


  private void handle(Socket s, long connum) throws IOException, ParallelException {
    DFATask t = new DFATask(s, connum);
    _pool.execute(t);
  }

	
	/**
	 * gets the data from the cache if available, else reads them from the file.
	 * If it has to read the data from the file, this method also sends the data
	 * through the <CODE>ObjectOutputStream<CODE> parameter passed in and returns 
	 * null, otherwise the caller is responsible for sending the data that the 
	 * method returns.
	 * @param fadmsg DFileDataVectorReadRequest
	 * @param oos ObjectOutputStream
	 * @return Vector Vector<VectorIntf>
	 * @throws IOException 
	 */
	private Vector getData(DFileDataVectorReadRequest fadmsg, ObjectOutputStream oos) 
					throws IOException, ParallelException {
		String filename = fadmsg.getFileName();
		DMCoordinator dvchunks_lock = DMCoordinator.getInstance(filename);
		dvchunks_lock.getReadAccess();
		List dvchunks = (List) _dataCache.get(filename);
		if (dvchunks==null) {
			// upgrade read -> write lock without danger of exception throwing
			dvchunks_lock.releaseReadAccess();
			dvchunks_lock.getWriteAccess();
			if (_dataCache.get(filename)==null) {  // essentially the Double-Check-Locking idiom
				dvchunks = new ArrayList();
				_dataCache.put(filename, dvchunks);
			}
			dvchunks_lock.releaseWriteAccess();
			dvchunks_lock.getReadAccess();
		}
		final int from = fadmsg.getFromIndex();
		final int to = fadmsg.getToIndex();
		Vector res = null;
		// first, see if it's possible to collect all data from DataVectorChunks
		if (_SCAN_FOR_PIECES) {
			VectorIntf[] resA = new VectorIntf[to-from+1];  // from <-> 0 , to <-> resA.length-1+from
			int num_added = 0;
			for (int i=0; i<dvchunks.size(); i++) {
				DataVectorChunk dvi = (DataVectorChunk) dvchunks.get(i);
				res = dvi.getData();
				if (res==null) continue;  // oops! data's gone
				if (from >= dvi.getFromIndex() && from <= dvi.getToIndex()) {  // from inside the chunk
					final int f1 = from-dvi.getFromIndex();
					final int t1 = to-dvi.getFromIndex();
					for (int j=f1; j<=t1; j++) {
						if (j>=res.size()) break;
						if (resA[j-f1]==null) {
							num_added++;  // count addition only first time
							resA[j-f1] = (VectorIntf) res.get(j);
						}  
					}
				}
				else if (to >= dvi.getFromIndex() && to <= dvi.getToIndex()) {  // to inside the chunk
					final int t1 = to-dvi.getFromIndex();
					final int f1 = dvi.getFromIndex()-from;
					for (int j=t1; j>=0; j--) {
						if (resA[j+f1]==null) { 
							num_added++;  // count addition only first time
							resA[j+f1] = (VectorIntf) res.get(j);
						}  
					}
				}
				else if (from <= dvi.getFromIndex() && to >= dvi.getToIndex()) { // all of chunk inside the [from,to] intvl
					final int offset = dvi.getFromIndex()-from;
					for (int j=0; j<res.size(); j++) {
						if (resA[j+offset]==null) {
							num_added++;  // count addition only first time
							resA[j+offset] = (VectorIntf) res.get(j);
						}
					}
				}
			}
			if (num_added==to-from+1) {
				res = new Vector();
				for (int i=0; i<resA.length; i++) {
					res.add(resA[i]);
				}
				dvchunks_lock.releaseReadAccess();
				return res;
			}
			res = null;  // invalidate res
		}  // if _SCAN_FOR_PIECES
		for (int i=0; i<dvchunks.size(); i++) {
			DataVectorChunk dvi = (DataVectorChunk) dvchunks.get(i);
			if (from>=dvi.getFromIndex() && to<=dvi.getToIndex()) {  // chunk has all of the data
				res = dvi.getData();
				if (res!=null) {  // get the right part of the data
					Vector rres = new Vector();
					for (int j=from-dvi.getFromIndex(); j<=to-dvi.getFromIndex(); j++) {
						rres.add(res.get(j));
					}
					dvchunks_lock.releaseReadAccess();
					return rres;
				}
				else {  // invalidate data
					// first upgrade read -> write lock without danger of exception throwing
					dvchunks_lock.releaseReadAccess();
					dvchunks_lock.getWriteAccess();
					// then check if condition still holds: yet another form of Double-Check Locking idiom
					dvi = (DataVectorChunk) dvchunks.get(i);
					if (from >= dvi.getFromIndex() && to <= dvi.getToIndex()) {
						res = dvi.getData();
						if (res!=null) {
							Vector rres = new Vector();
							for (int j=from-dvi.getFromIndex(); j<=to-dvi.getFromIndex(); j++) {
								rres.add(res.get(j));
							}							
							dvchunks_lock.releaseWriteAccess();
							return rres;
						}
					}
					// nope, condition still holds, do the work
					dvchunks.remove(i);
					Messenger.getInstance().msg("DataFileAccessSrv.getData(): reading from file", 1);
					try {
						fadmsg.execute(oos);  // may throw if socket closes
						res = fadmsg.getData();  // cannot be null now
						dvi = new DataVectorChunk(res, fadmsg.getFromIndex(), fadmsg.getToIndex());
						dvchunks.add(i, dvi);
					}
					finally {
						dvchunks_lock.releaseWriteAccess();
					}
					break;
				}
			} 
		}
		if (res==null) { // not found at all
			// upgrade read -> write lock without danger of exception throwing
			dvchunks_lock.releaseReadAccess();
			dvchunks_lock.getWriteAccess();
			// check if the work still needs doing
			for (int i=0; i<dvchunks.size(); i++) {
				DataVectorChunk dvi = (DataVectorChunk) dvchunks.get(i);
				if (from >= dvi.getFromIndex() && to <= dvi.getToIndex()) {
					// another thread did the job for us
					res = dvi.getData();
					if (res!=null) {
						Vector rres = new Vector();
						for (int j=from-dvi.getFromIndex(); j<=to-dvi.getFromIndex(); j++) {
							rres.add(res.get(j));
						}							
						dvchunks_lock.releaseWriteAccess();
						return rres;
					}
					else {  // res is null again
						dvchunks.remove(i);
						break;
					}
				}
			}
			// do the work and add result in cache
			Messenger.getInstance().msg("DataFileAccessSrv.getData(): reading from file", 1);
			try {
				fadmsg.execute(oos);  // may throw if socket closes
				DataVectorChunk dv = new DataVectorChunk(fadmsg.getData(), fadmsg.getFromIndex(), fadmsg.getToIndex());
				dvchunks.add(dv);
			}
			finally {
				dvchunks_lock.releaseWriteAccess();
			}
		}
		return null;
	}
	
	
	/**
	 * invoke as:
	 * java -cp &ltclasspath&gt [port(7894)] [maxthreads(10000)]
	 * @deprecated 
	 * @param args 
	 */
	public static void mainOld(String[] args) {
		DMsgPassingCoordinatorSrv.main(args);		
	}


  /**
   * inner helper class
   * <p>Title: popt4jlib</p>
   * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
   * <p>Copyright: Copyright (c) 2011</p>
   * <p>Company: </p>
   * @author Ioannis T. Christou
   * @version 1.0
   */
  class DFATask implements Runnable {
    private Socket _s;
    private long _conId;
		
    DFATask(Socket s, long conid) {
      _s = s;
      _conId = conid;
    }

    public void run() {
      Messenger.getInstance().msg("DFATask with id="+_conId+" running...",1);
      ObjectInputStream ois = null;
      ObjectOutputStream oos = null;
      try {
        oos = new ObjectOutputStream(_s.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(_s.getInputStream());
        DMsgIntf msg = (DMsgIntf) ois.readObject();  // read a msg (read -or write?- data request)
        if (msg instanceof DFileDataVectorReadRequest) {
					// fetch from cache or read and store in cache
					DFileDataVectorReadRequest fadmsg = (DFileDataVectorReadRequest) msg;
					Vector data = getData(fadmsg, oos);
					if (data!=null) {
						oos.writeObject(data);
					} else {
						// no-op: if the result of getData() is null, it means that the
						// data wasn't there in the cache, or was cleared, and therefore
						// the method first retrieved the data, then sent them to the client
						// and finally stored them in the cache as soft refs.
					}
					return;
				}
				if (msg instanceof DFileAccessSrvStatsRequest) {
					DFileAccessSrvStatsRequest dfassr = (DFileAccessSrvStatsRequest) msg;
					String filename = dfassr.getFileName();
					DMCoordinator dvchunks_lock = DMCoordinator.getInstance(filename);
					dvchunks_lock.getReadAccess();
					List dvchunks = (List) _dataCache.get(filename);
					int len = dvchunks==null ? 0 : dvchunks.size();
					int[] data = new int[3*len];
					if (len==0) {  // no cache yet for this filename
						data = new int[]{0, 0, 0};
					} else {
						int j=0;
						for (int i=0; i<dvchunks.size(); i++) {
							DataVectorChunk dvi = (DataVectorChunk) dvchunks.get(i);
							data[j++] = dvi.getFromIndex();
							data[j++] = dvi.getToIndex();
							Vector dvidata = dvi.getData();
							data[j++] = dvidata==null ? 0 : dvidata.size();
						}
					}
					dfassr.setAnswer(data);
					dvchunks_lock.releaseReadAccess();
				}
				// fall-back
				msg.execute(oos);
      }
      catch (IOException e) {
        e.printStackTrace();
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      catch (ParallelException e) {
        e.printStackTrace();
      }
      finally {
        try {
          if (oos != null) {
            oos.flush();
            oos.close();
          }
          if (ois != null) ois.close();
          _s.close();
        }
        catch (IOException e) {
          e.printStackTrace();  // shouldn't get here
        }
      }
    }
  }
	
}
