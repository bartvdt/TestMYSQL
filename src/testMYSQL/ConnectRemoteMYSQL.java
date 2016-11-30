package testMYSQL;

import com.mysql.jdbc.Driver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.Inet4Address;

public class ConnectRemoteMYSQL {

	static String clientIP;
	static String clientName;
	
	static String dbHost = "MacBartPro-ubuntu";
	static String dbDatabase = "EUROEVENTS";
	static String dbUser = "EUROEVENTS_USER";
	static String dbUserPassword = "Raadsel";
	static String dbUrl;
	
	static Properties connectionProperties = new Properties();

	static Connection dbConnection = null;
	
	static Statement mysqlStatement = null;
	static ResultSet mysqlResultSet = null;

	private static void openConnection()
	{
		System.out.print("openConnection: ");
		
		dbUrl = "jdbc:mysql://" + dbHost + "/" + dbDatabase;
		
		connectionProperties.setProperty("user", dbUser);
		connectionProperties.setProperty("password", dbUserPassword);
		connectionProperties.setProperty("useSSL", "false");
		connectionProperties.setProperty("autoReconnect", "false");
		
		System.out.println("dbUrl: " + dbUrl);
		
		try{
			DriverManager.registerDriver(new com.mysql.jdbc.Driver());
			
			dbConnection = DriverManager.getConnection(dbUrl, connectionProperties);
		} catch(SQLException e)
		{
			System.out.println("No connection: " + e.getMessage());
			System.exit(0);
		}
	}
	
	private static void closeConnection()
	{
		try {
			dbConnection.close();
		} catch(SQLException e)
		{
			System.out.println("No connection: " + e.getMessage());	
			System.exit(0);
		}
		dbConnection = null;
	}
	
	private static void executeStatement(String strQRCode)
	{
		System.out.println("ExcecuteStatement("+strQRCode+")");
		try {
			dbConnection.setAutoCommit(false);
			
			mysqlStatement = dbConnection.createStatement();
			mysqlResultSet = mysqlStatement.executeQuery("select amount from QR_CODE where datetime_dispense is NULL and qr_code = " + "'" + strQRCode + "'");
			mysqlResultSet = mysqlStatement.getResultSet();

			String resultAmount = null;
			
			int numberRecords = 0; // should be 0 or 1
			while (mysqlResultSet.next())
			{
				numberRecords += 1;
				if (numberRecords > 1)
				{
					System.out.println("Error: QR_CODE more than once in database");
					dbConnection.rollback();
					return;
				}
				resultAmount = mysqlResultSet.getString("amount");
				System.out.println("QR_CODE/amount: " + strQRCode + "/" +  resultAmount);
				
			}
			System.out.println("Number of records retrieved: " + numberRecords);
			
		    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    Date now = new Date();
		    String strDate = sdfDate.format(now);
		    
		    int mysqlCount = 0;
		    if (resultAmount != null)
		    {
		    	PreparedStatement mysqlPreparedStatement;
		    	mysqlPreparedStatement = dbConnection.prepareStatement("update QR_CODE set client_id = ? , datetime_dispense = ? where datetime_dispense is NULL and qr_code = " + "'" + strQRCode + "'");
		    	mysqlPreparedStatement.setString(1,clientIP);
		    	mysqlPreparedStatement.setString(2,strDate);
		    	mysqlCount = mysqlPreparedStatement.executeUpdate();
		    	
		    	System.out.println("Number of QR_CODE records updated: " + mysqlCount );
		    }
		    
		    dbConnection.commit();
		    
		} catch(SQLException e)
		{
			e.printStackTrace();
		}
		mysqlStatement = null;
		mysqlResultSet = null;		
		
	}
	
	private static void obtainIPInfo()
	{
		System.out.print("obtainIPInfo: ");
		try {
	        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        while (interfaces.hasMoreElements()) {
	            NetworkInterface iface = interfaces.nextElement();
	            // filters out 127.0.0.1, inactive interfaces and non IPV4 addresses
	            if (iface.isLoopback() || !iface.isUp())
	                continue;
	            
	            Enumeration<InetAddress> addresses = iface.getInetAddresses();
	            while(addresses.hasMoreElements()) {
	                InetAddress addr = addresses.nextElement();
	                if (addr instanceof Inet4Address)
	                {
	                	clientIP = addr.getHostAddress();
	    			    clientName = addr.getCanonicalHostName();
	    				System.out.println("ClientIP/ClientName: " + clientIP + "/" + clientName);	    

	    			    return; // Take the first one
	                }
	            }
	        }
	    } catch (SocketException e) {
	        throw new RuntimeException(e);
	    }
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		System.out.println("Main");
		
		String logString="";
		for (int i=0; i<args.length; i++)
			logString=logString+ " " + args[i];
			
		System.out.println("Commandline: TestMYSQL" + logString);
		
		obtainIPInfo();
		
		openConnection();
		
		for (int i=0; i<args.length; i++)
			executeStatement(args[i].toString());
		
		closeConnection();
	}

}
