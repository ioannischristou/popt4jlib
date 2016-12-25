/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

import java.util.Random;

/**
 * test-driver for <CODE>BoundedBufferArray</CODE> class.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BoundedBufferArrayTest {
	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; parallel.BoundedBufferArrayTest</CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		try {
			BoundedBufferArray buf = new BoundedBufferArray(1000);
			for (int i=0; i<1000; i++)
				buf.addElement(new Integer(i));
			Random r = new Random();
			for (int i=0; i<1000; i++) {
				int j = r.nextInt(buf.size());
				int vj = ((Integer)buf.elementAt(j)).intValue();
				Integer ij = (Integer) buf.remove(j);
				if (ij.intValue()!=vj) {
					System.err.print("Buffer=[");
					for (int k=0; k<buf.size(); k++) {
						System.err.print(buf.elementAt(k)+" ");
					}
					System.err.println("]");
					System.err.println("j="+j+" vj="+vj);
					System.exit(-1);
				}
			}
			System.err.println("ok. Buffer.size()="+buf.size());
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
