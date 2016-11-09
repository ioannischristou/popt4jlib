package parallel;

public class BarrierTest {
  public BarrierTest() {
  }

  public static void main(String[] args) {
    try {
      BThread arr[] = new BThread[10];
      Barrier.setNumThreads(arr.length);
      for (int i = 0; i < arr.length; i++) {
        arr[i] = new BThread(i);
        arr[i].start();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}


class BThread extends Thread {
  private int _id;
  public BThread(int id) { _id = id; }
  public void run() {
    for (int i=0; i<10; i++) {
      System.out.println("t-id="+_id+" i="+i);
      Barrier.getInstance().barrier();
    }
  }
}
