package parallel.distributed;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import popt4jlib.ObserverIntf;
import popt4jlib.SubjectIntf;

/**
 * small bridge program that connects to a <CODE>BCastSrv</CODE> server as 
 * another client, as well as to a <CODE>DAccumulatorSrv</CODE> for getting its
 * notifications, and submitting them to the broadcast server.
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2016</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class BCastSrv2DAccumulatorBridge implements ObserverIntf {
	// data members are connection data to BCastSrv
	private static ObjectOutputStream _oos;
	private static ObjectInputStream _ois;
	private static Socket _s;
	
	/**
	 * sole constructor.
	 */
	public BCastSrv2DAccumulatorBridge() {
		// no-op
	}
	
	
	/**
	 * invoke as:
	 * <CODE>java -cp &lt;classpath&gt; 
	 * parallel.distributed.BCastSrv2DAccumulatorBridge 
	 * [acchost(localhost)] [accport(7900)] [notificationsport(9900)] 
	 * [bcasthost(localhost)] [bcastport(9901)] 
	 * [notification_type(MAX)]</CODE>.
	 * @param args 
	 */
	public static void main(String[] args) {
		String acchost = "localhost";
		if (args.length>0) acchost = args[0];
		int accport = 7900;
		if (args.length>1) accport = Integer.parseInt(args[1]);
		int notificationsport = 9900;
		if (args.length>2) notificationsport = Integer.parseInt(args[2]);
		String bcasthost = "localhost";
		if (args.length>3) bcasthost = args[3];
		int bcastport = 9901;
		if (args.length>4) bcastport = Integer.parseInt(args[4]);
		int not_type = DAccumulatorNotificationType._MAX;
		if (args.length>5) not_type = Integer.parseInt(args[5]);
		try {
			_s = new Socket(bcasthost, bcastport);
			_oos = new ObjectOutputStream(_s.getOutputStream());
			_oos.flush();
			_ois = new ObjectInputStream(_s.getInputStream());  // useless
			DAccumulatorClt.setHostPort(acchost, accport, acchost, notificationsport);
			DAccumulatorClt.registerListener(new BCastSrv2DAccumulatorBridge(), not_type);
			// now, sleep for ever
			while (true) {
				Thread.sleep(1000);  // if interrupted, the thread will exit, and with
				                     // it, the whole program will exit as the 
				                     // AsynchUpdateThread has deamon status.
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	/**
	 * gets a value and submits it to the <CODE>BCastSrv</CODE>. This is a 
	 * call-back method executed from the 
	 * <CODE>DAccumulatorClt.AsynchUpdateThread.notifyObservers()</CODE> which
	 * is called whenever a new incumbent is sent to the DAccumulatorSrv. It is
	 * made public because it's an interface method.
	 * @param subject 
	 */
	public void notifyChange(SubjectIntf subject) {
		double value = ((Double) subject.getIncumbent()).doubleValue();
		try {
			_oos.writeObject(new Double(value));
			_oos.flush();
			_oos.reset();  // force object to be written anew to the stream
		}
		catch (Exception e) {
			try {
				_oos.close();
				_s.close();
			}
			catch (Exception e2) {
				// ignore
			}
		}
	}

}
