package parallel.distributed;

import parallel.*;
import java.io.*;
import java.net.*;
import java.util.*;


/**
 * class implements the "Client" for networks of PDBatchTaskExecutorWrk workers.
 * Connects to a host server represented by the PDBatchTaskExecutorSrv object,
 * to a specific host/port IP address.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class PDBatchTaskExecutorClt {
  private String _host="localhost";  // default host
  private int _port = 7891;  // default client port

  /**
   * public no-arg constructor, will assume connection is to be made on
   * localhost, port 7891.
   */
  public PDBatchTaskExecutorClt() {

  }


  /**
   * constructor provides explicitly the server host/port connection parameters.
   * @param hostipaddress String
   * @param port int
   */
  public PDBatchTaskExecutorClt(String hostipaddress, int port) {
    _host = hostipaddress;
    _port = port;
  }


  /**
   * the (internet) IP address of the server this client will be connecting to when
   * submitting work.
   * @return String
   */
  public String getHostIPAddress() {
    if ("localhost".equals(_host)) {
      // figure out the localhost IP address
      try {
        InetAddress i = InetAddress.getLocalHost();
        return i.getHostAddress();
      }
      catch (Exception e) {
        e.printStackTrace();
        return null;
      }
    } else return _host;
  }


  /**
   * the port of the server this client will be connecting to when submitting
   * work.
   * @return int
   */
  public int getPort() { return _port; }


  /**
   * the main method of the class. Sends over the network the tasks parameter
   * to the PDBatchTaskExecutorSrv server, who then distributes them to one
   * of the available workers in the network. Method blocks until results are
   * retrieved.
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDBatchTaskExecutorException
   * @return Object[]
   */
  public Object[] submitWorkFromSameHost(TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(tasks): null or empty tasks passed in.");
    Socket s = null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
    try {
      s = new Socket(_host, _port);
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      /*
      byte[] localipaddr = s.getLocalAddress().getAddress();
      StringBuffer buf = new StringBuffer();
      for (int i=0; i<localipaddr.length; i++) {
        buf.append(String.valueOf(localipaddr[i]));
        if (i < localipaddr.length-1) buf.append('.');
      }
      String client_addr = new String(buf);
      */
      InetAddress ia = InetAddress.getLocalHost();
      String client_addr_port = ia.getHostAddress()+"_"+_port;
      TaskObjectsExecutionRequest req = new TaskObjectsExecutionRequest(client_addr_port, tasks);
      oos.writeObject(req);
      oos.flush();
      Object response = ois.readObject();
      if (response instanceof TaskObjectsExecutionResults) {
        return ( (TaskObjectsExecutionResults) response)._results;
      }
      else if (response instanceof NoWorkerAvailableResponse)
        throw new PDBatchTaskExecutorException("no worker was available...");
      else throw new PDBatchTaskExecutorException("cannot parse response...");
    }
    finally {
      if (s!=null) s.close();
    }
  }


  /**
   * the main method of the class. Sends over the network the tasks parameter
   * to the PDBatchTaskExecutorSrv server, who then distributes them to one
   * of the available workers in the network. Method blocks until results are
   * retrieved.
   * @param originating_client String <host>_<port> frmt
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDBatchTaskExecutorException
   * @return Object[]
   */
  public Object[] submitWork(String originating_client, TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(clientname, tasks): null or empty tasks passed in.");
    if (originating_client==null || originating_client.length()==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(clientname, tasks): null or empty clientname passed in.");
    Socket s = null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
    try {
      s = new Socket(_host, _port);
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      TaskObjectsExecutionRequest req = new TaskObjectsExecutionRequest(originating_client, tasks);
      oos.writeObject(req);
      oos.flush();
      Object response = ois.readObject();
      if (response instanceof TaskObjectsExecutionResults) {
        return ( (TaskObjectsExecutionResults) response)._results;
      }
      else if (response instanceof NoWorkerAvailableResponse)
        throw new PDBatchTaskExecutorException("no worker was available...");
      else throw new PDBatchTaskExecutorException("cannot parse response...");
    }
    finally {
      if (s!=null) s.close();
    }
  }


  /**
   * same method as <CODE>submitWork(client, tasks)</CODE> but carries all
   * the names of the hosts that have submitted the tasks to various servers
   * (only applies when there exists more than one server in the system of
   * servers, clients and workers). Method blocks until results are retrieved.
   * @param originating_clients Vector<String>
   * @param tasks TaskObject[]
   * @throws IOException
   * @throws ClassNotFoundException
   * @throws PDBatchTaskExecutorException
   * @return Object[]
   */
  public Object[] submitWork(Vector originating_clients, TaskObject[] tasks)
      throws IOException, ClassNotFoundException, PDBatchTaskExecutorException {
    if (tasks==null || tasks.length==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(clientsnames, tasks): null or empty tasks passed in.");
    if (originating_clients==null || originating_clients.size()==0)
      throw new PDBatchTaskExecutorException("PDBatchTaskExecutorClt.submitWork(clientsnames, tasks): null or empty clientname passed in.");
    Socket s = null;
    ObjectInputStream ois=null;
    ObjectOutputStream oos=null;
    try {
      s = new Socket(_host, _port);
      oos = new ObjectOutputStream(s.getOutputStream());
      oos.flush();
      ois = new ObjectInputStream(s.getInputStream());
      TaskObjectsExecutionRequest req = new TaskObjectsExecutionRequest(originating_clients, tasks);
      oos.writeObject(req);
      oos.flush();
      Object response = ois.readObject();
      if (response instanceof TaskObjectsExecutionResults) {
        return ( (TaskObjectsExecutionResults) response)._results;
      }
      else if (response instanceof NoWorkerAvailableResponse)
        throw new PDBatchTaskExecutorException("no worker was available...");
      else throw new PDBatchTaskExecutorException("cannot parse response...");
    }
    finally {
      if (s!=null) s.close();
    }
  }

}

