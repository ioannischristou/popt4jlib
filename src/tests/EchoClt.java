package tests;

import java.net.*;
import java.io.*;

/**
 * test together with EchoSrv, to see what happens when you send an array object
 * twice over the wire. Unless a reset occurs on the output-stream before an
 * object is sent, the second time an object is serialized on the same stream, 
 * the previous (back-ref) is used and the client reading from that stream 
 * "sees" the initial object state.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2017</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class EchoClt {
	public static void main(String[] args) {
		String host = "localhost";
		int port = 2007;
		if (args.length>0) host = args[0];
		if (args.length>1) port = Integer.parseInt(args[1]);
		try {
		  Socket s = new Socket(host,port);
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			oos.flush();
			ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
			int[] arr = {1,2,3,4,5};
			System.out.println("SENDING:"); printArray(arr);
			oos.reset();  // force object to be written anew
			oos.writeObject(arr);
			oos.flush();
			int[] result = (int[]) ois.readObject();
			System.out.println("RETRIEVED:"); printArray(result);
			for (int i=0; i<arr.length; i++) arr[i]=10;
			System.out.println("SENDING:"); printArray(arr);	
			oos.reset();  // force object to be written anew
			oos.writeObject(arr);
			oos.flush();
			int[] result2 = (int[]) ois.readObject();
			System.out.println("RETRIEVED:"); printArray(result2);			
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void printArray(int[] data) {
		System.out.print("data: [");
		for (int i=0; i<data.length; i++) {
			System.out.print(data[i]);
			if (i<data.length-1) System.out.print(",");
		}
		System.out.println("]");
		System.out.flush();
	}

}
