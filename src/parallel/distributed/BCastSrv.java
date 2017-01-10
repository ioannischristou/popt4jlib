package parallel.distributed;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


/**
 * A broadcast object server, that broadcasts to all clients connected to it,
 * any double value received by any of the connected clients. Useful with the 
 * <CODE>DAccumulator[Srv|Clt]</CODE> set of classes, and any case where it is 
 * needed to broadcast objects to all connected parties.
 * <p> Notice the use of the <CODE>java.nio</CODE> package for this server's 
 * functionalities. The class is inspired after Greg Travis's Server class
 * example on nio. See the article on JavaWorld:
 * http://www.javaworld.com/article/2073344/core-java/use-select-for-high-speed-networking.html
 * </p>
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public final class BCastSrv {
	
  private int _port;
	private Set _socketChannels = new HashSet();  // Set<SocketChannel>
	private final ByteBuffer _buffer;  // length 8 should suffice

	/**
	 * sole constructor specifies the port and the length of the data buffer in
	 * bytes.
	 * @param port int
	 * @param bufferlen int
	 */
  public BCastSrv(int port, int bufferlen) {
    _port = port;
		_buffer = ByteBuffer.allocate(bufferlen);
  }

	
	/**
	 * the sole method of the class, loops for ever listening for any incoming
	 * socket connection, or for any existing connection that has data available
	 * to read from, and sends them to all existing connections.
	 */
  public void run() {
		utils.Messenger mger = utils.Messenger.getInstance();
    try {
      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();
      // Set it to non-blocking, so we can use select
      ssc.configureBlocking(false);
      // Get the Socket connected to this channel, and bind it
      // to the listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress(_port);
      ss.bind(isa);
      // Create a new Selector for selecting
      Selector selector = Selector.open();
      // Register the ServerSocketChannel, so we can
      // listen for incoming connections
      ssc.register(selector, SelectionKey.OP_ACCEPT);
			mger.msg("BCastSrv.run(): listening on port "+_port,0);
      while (true) {
        // See if we've had any activity -- either an incoming connection, or 
				// incoming data on an existing connection
        int num = selector.select();
        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;  // not spinning on busy-waiting behavior: select() blocks if no activity
        }
        // Get the keys corresponding to the activity that has been detected, 
				// and process them one at a time
        Set keys = selector.selectedKeys();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
          // Get a key representing one of bits of I/O activity
          SelectionKey key = (SelectionKey)it.next();
          // What kind of activity is it?
          if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
						// System.out.println( "acc" );
            // It's an incoming connection. Register this socket with Selector
            // so we can listen for input on it
            Socket s = ss.accept();
            // System.out.println( "Got connection from "+s );
            // make it non-blocking, so we can use a selector on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking(false);
            // Register it with the selector, for reading
            sc.register(selector, SelectionKey.OP_READ );
						// add it to the map of connections
						_socketChannels.add(sc);
          } else if ((key.readyOps() & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
            SocketChannel sc = null;
            try {
              // It's incoming data on a connection, so process it
              sc = (SocketChannel)key.channel();
              if (!broadcastData(sc)) {
								key.cancel();
								try {
									Socket s = sc.socket();
									s.close();
								}
								catch (IOException e) {
									mger.msg("BCastSrv: error closing socket...", 0);
								}
							}
            } 
						catch(IOException ie) {
              // On exception, remove this channel from the selector
              key.cancel();
              try {
                sc.close();
              } 
							catch( IOException ie2 ) {
								mger.msg("BCastSrv: error closing channel: '"+ie2+"'",0);
							}
              mger.msg("BCastSrv: Closed channel "+sc,0);
            }
          }
        }
        // remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } 
		catch( IOException ie ) {
			mger.msg("BCastSrv: caught error: "+ie,0);
    }
  }

	
	private boolean broadcastData(SocketChannel sc) throws IOException {
		try {
			// send double value through all other socket connections
			_buffer.clear();
			sc.read(_buffer);
			_buffer.flip();
			if (_buffer.limit()==0) return false;  // socket failed?
			utils.Messenger.getInstance().msg("BCastSrv: broadcasting to "+(_socketChannels.size()-1)+" connections...", 2);
			Iterator it = _socketChannels.iterator();
			while (it.hasNext()) {
				SocketChannel sit = (SocketChannel) it.next();
				if (sit==sc) continue;  // don't send object back to sender
				ByteBuffer buffer = it.hasNext() ? _buffer.duplicate() : _buffer;
				try {
					sit.write(buffer); 
				}
				catch (IOException e) {
					utils.Messenger.getInstance().msg("BCastSrv: error broadcasting data to client, closing connection", 0);
					it.remove();
					try {
						sit.close();
					}
					catch (IOException e2) {
						utils.Messenger.getInstance().msg("BCastSrv: caught IOException trying to close faulty channel", 0);
					}
				}
			}
			return true;
		}
		catch (Exception e2) {
			utils.Messenger.getInstance().msg("caught exception '"+e2+"'", 0);  // ignore
			return false;
		}
	}
		

	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.distributed.BCastSrv [port(9901)] [bufferlen(64)]</CODE>.
	 * @param args
	 * @throws Exception 
	 */
  static public void main( String args[] ) throws Exception {
    int port = 9901;
		if (args.length>0) {
			port = Integer.parseInt(args[0]);
		}
		int bufferlen=64;
		if (args.length>1) {
			bufferlen = Integer.parseInt(args[1]);
		}
    BCastSrv bsrv = new BCastSrv(port,bufferlen);
		bsrv.run();
  }
	
}

