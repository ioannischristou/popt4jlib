/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;
import java.util.ArrayList;
//import java.util.ArrayDeque;


/**
 * tests the <CODE>UnboundedBufferArrayUnsynchronized</CODE> against
 * <CODE>java.util.{ArrayList,ArrayDeque}</CODE> performance.
 * Notice: the test against <CODE>java.util.ArrayDeque</CODE> is commented out
 * as this class is part of JDK1.5 and later editions of Java. If you are 
 * running Java 5 or later and want to compare performance against this class 
 * then uncomment the relevant part of the code in the <CODE>main(args)</CODE>
 * method. As it turns out, <CODE>UnboundedBufferArrayUnsynchronized</CODE> is a
 * very fast implementation!
 * @author itc
 */
public class UBAUTest {
	/**
	 * Inserts, removes, and then inserts again a number of objects into
	 * an UnboundedBufferArrayUnsynchronized, then into an ArrayList/ArrayDeque.
	 * invoke as:
	 * java -cp &lt;classpath&gt; parallel.UBAUTest [num_objs(1000000)]
	 * @param args
	 */
	public static void main(String[] args) {
		int numobjs = 1000000;
		if (args.length>0) numobjs = Integer.parseInt(args[0]);

		long start = System.currentTimeMillis();
		ArrayList list = new ArrayList(1000);
		// 1. insert
		for (int i=0; i<numobjs; i++) {
			list.add(new Integer(i));
		}
		// 2. remove
		for (int i=numobjs-1; i>=0; i--) {
			list.remove(i);
		}
		// 3. insert back again
		for (int i=numobjs-1; i>=0; i--) {
			list.add(new Integer(i));
		}
		long dur = System.currentTimeMillis()-start;
		System.out.println("ArrayList takes "+dur+" msecs.");
    /* 
		// ArrayDeque comparison stats gathering starts here.
		start = System.currentTimeMillis();
		ArrayDeque deque = new ArrayDeque(1000);
		// 1. insert
		for (int i=0; i<numobjs; i++) {
			deque.offer(new Integer(i));
		}
		// 2. remove
		for (int i=numobjs-1; i>=0; i--) {
			deque.poll();
		}
		// 3. insert back again
		for (int i=numobjs-1; i>=0; i--) {
			deque.offer(new Integer(i));
		}
		dur = System.currentTimeMillis()-start;
		System.out.println("ArrayDeque takes "+dur+" msecs.");
		// ArrayDeque comparison stats gathering ends here.
    */
    start = System.currentTimeMillis();
		UnboundedBufferArrayUnsynchronized buf = new UnboundedBufferArrayUnsynchronized(1000);
		// 1. insert
		for (int i=0; i<numobjs; i++) {
			buf.addElement(new Integer(i));
		}
		// 2. remove
		for (int i=numobjs-1; i>=0; i--) {
			buf.remove();
		}
		// 3. insert back again
		for (int i=numobjs-1; i>=0; i--) {
			buf.addElement(new Integer(i));
		}
		dur = System.currentTimeMillis()-start;
		System.out.println("UnboundedBufferArrayUnsynchronized takes "+dur+" msecs.");
	}
}
