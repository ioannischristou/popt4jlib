package popt4jlib.MSSC1D;

public class CException extends Exception {
  private String _msg;
  public CException(String msg) {
    _msg = msg;
	System.err.println(msg);
  }
}
