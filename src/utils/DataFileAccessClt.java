package utils;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.List;
import parallel.distributed.DFileAccessSrvStatsRequest;
import parallel.distributed.DFileDataVectorReadRequest;
import parallel.distributed.SimpleMessage;
import popt4jlib.DblArray1Vector;

/**
 * class implementing clients for requesting reading a range of vectors residing
 * in a remote file in a remote file-system.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2018</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class DataFileAccessClt {
  private String _host="localhost";
  private int _port = 7899;


  /**
   * default constructor assumes the file-access server lives in the same host
   * (localhost) listening in port 7899.
   * @throws UnknownHostException
   */
  public DataFileAccessClt() throws UnknownHostException {
    InetAddress ia = InetAddress.getLocalHost();
    _host = ia.getHostAddress();
  }


  /**
   * constructor specifies the host and port where the msg passing coordinator
   * server lives, and also the name of the coordinator with whom all sends and
   * receives will coordinate.
   * @param host String
   * @param port int
   * @throws UnknownHostException
   */
  public DataFileAccessClt(String host, int port) throws UnknownHostException {
    _host = InetAddress.getByName(host).getHostAddress();
    _port = port;
  }

	
	/**
	 * reads a vector of <CODE>VectorIntf</CODE> objects specified as a range in
	 * a sequence of such objects specified in a file residing in a remote file-
	 * system of the host specified in the constructor of the object.
	 * @param filename String fully qualified (posibly network) path-name of the 
	 * file to read vectors from
	 * @param fromind int the starting index of the range (inclusive, including 
	 * zero)
	 * @param toind int the ending index of the range (inclusive, including zero)
	 * @return List // List&lt;VectorIntf&gt;
	 * @throws IOException
	 * @throws ParallelException
	 * @throws IllegalArgumentException
	 * @throws IndexOutOfBoundsException
	 * @throws ClassNotFoundException 
	 */
	public synchronized List readVectorsFromRemoteFile(String filename, 
		                                                 int fromind, int toind)
					throws IOException, ParallelException, IllegalArgumentException, 
					       IndexOutOfBoundsException, ClassNotFoundException {
    if (filename==null || filename.length()==0) 
			throw new IllegalArgumentException("wrong filename");
		if (fromind>toind) 
			throw new IllegalArgumentException("fromindex > toindex");
		if (fromind < 0) throw new IndexOutOfBoundsException("fromindex < 0");
		Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(
				      new BufferedOutputStream(s.getOutputStream()));
      oos.flush();
      ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
      // no need to call oos.reset() first
      oos.writeObject(new DFileDataVectorReadRequest(filename, fromind, toind));
      oos.flush();
      Object reply = ois.readObject();
			if (!DataFileAccessSrv._SEND_COMPACT_ARRAY) {
				if (reply instanceof List) {
					return (List) reply;
				}
				else {
					SimpleMessage srvmsg = (SimpleMessage) reply;
					throw new ParallelException("readVectorsFromRemoteFile(file,from,to)"+
						                          " failed w/ msg from server="+srvmsg);
				}
			} else {  // data is one big 1-D array of doubles
				if (reply instanceof double[]) {
					double[] data_array = (double[]) reply;
					int n = (int) data_array[0];
					int nkp1 = data_array.length;
					int k = (nkp1-1)/n;
					int pos = 1;
					List result = new java.util.ArrayList(k);
					for (int i=0; i<k; i++) {
						double[] xi = new double[n];
						System.arraycopy(data_array, pos, xi, 0, n);
						DblArray1Vector x = new DblArray1Vector(xi);
						result.add(x);
					}
					return result;
				}
				else {
					SimpleMessage srvmsg = (SimpleMessage) reply;
					throw new ParallelException("readVectorsFromRemoteFile(file,from,to)"+
						                          " failed w/ msg from server="+srvmsg);
				}
			}
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }		
	}
	
	
	/**
	 * return a string representation of the server cache for the given filename.
	 * @param filename String
	 * @return String
	 * @throws IOException
	 * @throws ParallelException
	 * @throws IllegalArgumentException
	 * @throws ClassNotFoundException 
	 */
	public synchronized String getServerCacheStatsForFile(String filename)
					throws IOException, ParallelException, IllegalArgumentException, 
					ClassNotFoundException {
    if (filename==null || filename.length()==0) 
			throw new IllegalArgumentException("wrong filename");
		Socket s = new Socket(_host, _port);
    ObjectOutputStream oos = null;
    ObjectInputStream ois = null;
    try {
      oos = new ObjectOutputStream(
				      new BufferedOutputStream(s.getOutputStream()));
      oos.flush();
      ois = new ObjectInputStream(new BufferedInputStream(s.getInputStream()));
      // no need to call oos.reset() here
      oos.writeObject(new DFileAccessSrvStatsRequest(filename));
      oos.flush();
      Object reply = ois.readObject();
      if (reply instanceof String) {
        return (String) reply;
      }
      else {
				SimpleMessage srvmsg = (SimpleMessage) reply;
        throw new ParallelException("readVectorsFromRemoteFile(file,from,to) "+
					                          " failed w/ msg from server="+srvmsg);
      }
    }
    finally {
      if (oos!=null) oos.close();
      if (ois!=null) ois.close();
      s.close();
    }		
	}
	
}

