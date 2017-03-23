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
		// functionality tests
		buf = new UnboundedBufferArrayUnsynchronized(10);
		System.out.println("1. buf="+buf);
		for (int i=0; i<20; i++) {
			buf.addElement(new Integer(i));
		}
		System.out.println("2. buf="+buf);
		for(int i=0; i<10; i++) {
			buf.remove();
		}
		System.out.println("3. buf="+buf);
		for(int i=9; i>=0; i--) {
			buf.remove(i);
			System.out.println("4."+i+". buf="+buf);
		}
		buf.addElement(new Integer(1));
		System.out.println("5. buf="+buf);
		System.out.println("5. buf.size()="+buf.size()+" buf.elementAt(0)="+buf.elementAt(0));
		for (int i=0; i<100; i++) {
			buf.addElement(new Integer(i));
		}
		while (buf.size()>0) {
			System.out.print("buf.size()="+buf.size()+" removing first.");
			Object bi = buf.remove();
			System.out.println("bi="+bi);
		}
	}
}
