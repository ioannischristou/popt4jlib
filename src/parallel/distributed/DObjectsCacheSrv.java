package parallel.distributed;

import java.util.*;
import java.io.*;
import java.net.*;
import parallel.DynamicAsynchTaskExecutor;
import parallel.ParallelException;


/**
 * Server class that allows storage and retrieval of sets of objects into an 
 * in-memory cache identified by name. Network clients send requests for adding
 * objects into the global cache, retrieving an entire cache by name, or 
 * clearing a named cache. Useful for storing QuantRule objects in the 
 * distributed versions of the QARMA algorithm.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class DObjectsCacheSrv {
	private static Set _objects;  // the cache of objects to manage
	
	private static void handle(final Socket s, DynamicAsynchTaskExecutor extor) 
		throws ParallelException {
		Runnable rs = new Runnable() {
			public void run() {
				try {
		      utils.Messenger.getInstance().msg("DObjectsCacheSrv: "+
						                                "handling new request",0);
					ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
					oos.flush();
					ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
					Object cmd = ois.readObject();
					if (cmd instanceof DObjCacheAddReq) {
						DObjCacheAddReq addcmd = (DObjCacheAddReq) cmd;
						Collection objs = addcmd.getObjects();
						synchronized (DObjectsCacheSrv.class) {
							_objects.addAll(objs);
							oos.writeObject(new OKReply());
							oos.flush();
						}
					}
					else if (cmd instanceof DObjCacheGetAllReq) {
						synchronized (DObjectsCacheSrv.class) {
							oos.reset();
							oos.writeObject(_objects);
							oos.flush();
						}
					}
					else if (cmd instanceof DObjCacheClearReq) {
						synchronized (DObjectsCacheSrv.class) {
							_objects.clear();
							oos.writeObject(new OKReply());
							oos.flush();
						}						
					}
					else {
						System.err.println("cannot parse request");
						throw new Error("wrong request");
					}
				}
				catch (Exception e) {
					e.printStackTrace();
					try {
						if (!s.isClosed()) {
							s.shutdownOutput();
							s.shutdownInput();
						}
					}
					catch (Exception e2) {
						e2.printStackTrace();  // ignore
					}
				}
			}
		};
		extor.execute(rs);
	}
	
	
	public static void main(String[] args) {
		try {
			int port = Integer.parseInt(args[0]);
			_objects = new HashSet();
			ServerSocket ss = new ServerSocket(port);
			DynamicAsynchTaskExecutor extor = 
				DynamicAsynchTaskExecutor.newDynamicAsynchTaskExecutor(1, 10000);
      utils.Messenger.getInstance().msg("DObjectsCacheSrv.main(): running...",0);			
			while (true) {
				Socket s = ss.accept();
				handle(s,extor);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
