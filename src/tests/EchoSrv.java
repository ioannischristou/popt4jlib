package tests;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * the purpose of this class is to allow clients to pass into it array arguments
 * and see what happens.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class EchoSrv {
	private static int _port=2007;  // default port for echo server is 7 (<1024)
	
	
	public static void main(String[] args) {
		if (args.length>0) _port = Integer.parseInt(args[0]);
		try {
			ServerSocket ss = new ServerSocket(_port);
			while (true) {
				Socket s = ss.accept();
				handle(s);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	public static void handle(final Socket s) {
		Thread t = new Thread(new Runnable() {
			                      public void run(){
															try {
																ObjectOutputStream oos = new ObjectOutputStream(
																	                         s.getOutputStream());
																oos.flush();
																ObjectInputStream ois = new ObjectInputStream(
																	                        s.getInputStream());
																while (true) {
																	Object input = ois.readObject();
																	if (input instanceof int[]) {
																		int[] data = (int[]) input;
																		printArray(data);
																	}
																	else System.out.println("READ: "+input);
																	oos.reset();  // force object to be written
																	oos.writeObject(input);  // do your ECHO job!
																	oos.flush();
																}
															} catch (Exception e) {
																// silently ignore
															}
														}
		                      });
		t.start();
	}
	
	
	public static void printArray(int[] data) {
		System.out.print("READ: [");
		for (int i=0; i<data.length; i++) {
			System.out.print(data[i]);
			if (i<data.length-1) System.out.print(",");
		}
		System.out.println("]");
		System.out.flush();
	}
	
}
