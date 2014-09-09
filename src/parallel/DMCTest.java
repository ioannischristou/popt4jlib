package parallel;

/**
 * <p>Title: parallel</p>
 * <p>Description: minimal test</p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */

public class DMCTest {


  public static void main(String[] args) {
    int n = 10;
    for (int i=0; i<n; i++) {

      DMCThread di;
      if (i%3==0)
        di = new DMCThread(1);  // write
      else di = new DMCThread(0);  // read
      di.start();
    }
  }

}
