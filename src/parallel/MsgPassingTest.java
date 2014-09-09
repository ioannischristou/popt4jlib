package parallel;

public class MsgPassingTest {
  static private int _numThreads = 10;
  public static int getNumThreads() { return _numThreads; }

  public MsgPassingTest() {
  }

  public static void main(String[] args) {
    MPThread t0 = new MPThread(0);
    t0.start();
    for (int i=1; i<_numThreads; i++) {
      MPThread ti = new MPThread(i);
      ti.start();
    }
    try {
      MsgPassingCoordinator.getInstance().sendData( -1, 0, new Object());
    }
    catch (Exception e) { e.printStackTrace(); }
  }
}


class MPThread extends Thread {
  private int _id;

  public MPThread(int i) {
    _id = i;
  }

  public void run() {
    final int numthreads = MsgPassingTest.getNumThreads();
    int sendTo = _id+1;
    if (sendTo>=numthreads) sendTo = 0;
    try {
      for (int i = 0; i < 10; i++) {
        MsgPassingCoordinator.getInstance().recvData(_id);
        System.out.println("Thread-" + _id + " doing iter " + i);
        MsgPassingCoordinator.getInstance().sendData(_id, sendTo, new Object());
      }
    }
    catch (Exception e) { e.printStackTrace(); }
  }
}
