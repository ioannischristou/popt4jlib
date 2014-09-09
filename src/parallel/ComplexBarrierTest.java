package parallel;

public class ComplexBarrierTest {
  public ComplexBarrierTest() {
  }

  public static void main(String[] args) {
    try {
      // create
      CBThread arr[] = new CBThread[100];
      for (int i=0; i<arr.length; i++) {
        arr[i] = new CBThread(i);
        ComplexBarrier.addThread(arr[i]);
      }
      CBThread arr2[] = new CBThread[100];
      for (int i=0; i<arr2.length; i++) {
        arr2[i] = new CBThread(100+i);
        ComplexBarrier.addThread(arr2[i]);
      }
      // run
      for (int i = 0; i < arr.length; i++) {
        arr[i].start();
        arr2[i].start();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}


class CBThread extends Thread {
  private int _id;
  public CBThread(int id) { _id = id; }
  /**
   * all threads perform barrier 50 times, but the threads with _id>=100
   * exit the barrier after this number.
   */
  public void run() {
    for (int i=0; i<100; i++) {
      try {
        if (i<50 || _id<100) {
          System.out.println("t-id="+_id+" i="+i);
          System.out.flush();
          ComplexBarrier.getInstance().barrier();
        }
        else if (_id>=100) {
          ComplexBarrier.removeCurrentThread();
          break;
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
