/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package parallel;

/**
 * test driver class for the <CODE>BoundedBufferArrayUnsynchronized</CODE> class
 * and in particular its <CODE>remove(Object,int)</CODE> method.
 * @author itc
 */
public class BBAUTest {
	
	public static void main(String[] args) {
		int len = 1000;
		// 1. create
		BoundedBufferArrayUnsynchronized buf = new BoundedBufferArrayUnsynchronized(len);
		// 2. populate
		for (int i=0; i<len; i++) {
			buf.addElement(new Integer(i));
		}
		// 3. remove one by one
		for (int i=buf.size()-1; i>=0; i--) {
			int j = (int) Math.floor(Math.random()*buf.size());
			int k = (int) Math.floor(Math.random()*buf.size());
			Integer ii = (Integer) buf.elementAt(j);
			int pos = buf.remove(ii, k);
			if (pos!=j) {
				System.err.println("pos="+pos+" ii="+ii.intValue()+" j="+j);
				System.exit(-1);
			}
		}
		System.out.println("buf.size()="+buf.size()+"... Done.");
	}
	
}
