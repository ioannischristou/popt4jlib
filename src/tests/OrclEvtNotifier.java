package tests;

import java.io.*;
import java.net.*;


/**
 * class meant to be uploaded on the Oracle RDBMS to allow sending messages to 
 * a broadcast server outside of the database whenever a database trigger is 
 * executed.
 * Once this class has been compiled with javac, it can be loaded onto Oracle
 * from the command-prompt by doing
 * 
 * loadjava -user &lt;username&gt;/&lt;password&gt;@&lt;dbname&gt; 
 * OrclEvtNotifier.java
 * 
 * dbname could be orclpdb (or whatever the Oracle pluggable DB name is where 
 * the schema for username lives)
 * 
 * Then, a stored procedure needs to be written in Oracle PL/SQL to declare the
 * method <CODE>sendMsg</CODE> of the class. This stored procedure would be 
 * something like:
 * CREATE OR REPLACE PROCEDURE evt_notification_proc(host IN VARCHAR2, 
 *                                                   port IN NUMBER,
 *                                                   msg IN VARCHAR2)
 * AS LANGUAGE JAVA 
 * NAME 'OrclEvtNotifier.sendMsg(java.lang.String, int, java.lang.String)';
 * /
 * 
 * Finally, assuming a trigger is for a table TBL, the trigger would be like:
 * CREATE OR REPLACE TRIGGER TBL_insert_happened 
 * AFTER INSERT ON TBL
 * FOR EACH ROW
 * BEGIN
 *   evt_notification_proc('localhost', 9901, :new.ID);
 * END;
 * /
 * 
 * With this code, after an insert on TBL (that's supposed to have an ID column
 * that is of type VARCHAR2) it's id is broadcasted to all connected parties to
 * our BCastSrv!
 * Notice that for all this to work, the &lt;username&gt; schema must have the
 * resolve and connect privileges on Java java.net.SocketPermission; this is 
 * obtained by the SYS (AS SYSDBA) of the Oracle RDBMS issuing the following
 * PL/SQL command:
 * 
 * CALL dbms_java.grant_permission('&lt;username&gt;', 
 * 'SYS:java.net.SocketPermission', '&lt;host&lt;:&lt;port&gt;',
 * 'connect,resolve');
 * /
 * 
 * <p>Title: popt4jlib</p>
 * <p>Description: A Parallel Meta-Heuristic Optimization Library in Java</p>
 * <p>Copyright: Copyright (c) 2011-2020</p>
 * <p>Company: </p>
 * @author Ioannis T. Christou
 * @version 1.0
 */
public class OrclEvtNotifier {
  public static void sendMsg(String hostname, int port, String msg) 
	  throws Exception {
    Socket s = null;
    try {
			s = new Socket(hostname, port);
      ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream());
      os.flush();
      os.writeObject(msg);
      os.flush();
      os.close();
    }
    catch (Exception e) {
			e.printStackTrace();
    }
    finally {
			if (s!=null) s.close();
    }
  }
}

