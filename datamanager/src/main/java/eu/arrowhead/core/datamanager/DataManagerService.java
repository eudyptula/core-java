/*
 *  Copyright (c) 2018 Jens Eliasson, Luleå University of Technology
 *
 *  This work is part of the Productive 4.0 innovation project, which receives grants from the
 *  European Commissions H2020 research and innovation programme, ECSEL Joint Undertaking
 *  (project no. 737459), the free state of Saxony, the German Federal Ministry of Education and
 *  national funding authorities from involved countries.
 */

package eu.arrowhead.core.datamanager;

import eu.arrowhead.common.DatabaseManager;
import eu.arrowhead.common.misc.TypeSafeProperties;
import eu.arrowhead.common.Utility;
import eu.arrowhead.core.datamanager.ArrowheadSystem;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.messages.SigMLMessage;
import eu.arrowhead.common.messages.SenMLMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.ServiceConfigurationError;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
/*import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;*/
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

final class DataManagerService {
  private static final Logger log = Logger.getLogger(DataManagerResource.class.getName());
  //private static final DatabaseManager dm = DatabaseManager.getInstance();
  //private static SessionFactory factory;
  private static TypeSafeProperties props;
  private static Connection connection = null;
  private static String dbAddress;
  private static String dbUser;
  private static String dbPassword;

  //private static List<String> endpoints = new ArrayList<>();


  static boolean Init(TypeSafeProperties propss){
    props = propss;
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      System.out.println("Where is your MySQL JDBC Driver?");
      e.printStackTrace();
      return false;
    }

    System.out.println("MySQL JDBC Driver Registered!");
    try {
      connection = DriverManager.getConnection(props.getProperty("db_address")+props.getProperty("db_database"),props.getProperty("db_user"), props.getProperty("db_password"));
      //connection = getConnection();
      checkTables(connection, props.getProperty("db_database"));
      connection.close();
    } catch (SQLException e) {
      System.out.println("Connection Failed! Check output console");
      e.printStackTrace();
      return false;
    }

    return true;
  }

  private static Connection getConnection() throws SQLException {
    Connection conn = DriverManager.getConnection(props.getProperty("db_address")+props.getProperty("db_database"), props.getProperty("db_user"), props.getProperty("db_password"));

    return conn;
  }


  private static void closeConnection(Connection conn) throws SQLException {
    conn.close();
  }

  public static int checkTables(Connection conn, String database) {
    //if ( enable_database == false)
    //return -1;

    String sql = "CREATE DATABASE IF NOT EXISTS "+database;
    try {
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
    } catch(SQLException se){
      return -1;
    }

    sql = "CREATE TABLE IF NOT EXISTS iot_devices (\n" 
      + "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n" 
      + "name varchar(64) NOT NULL UNIQUE,\n" 
      + "alias varchar(64),\n" 
      + "last_update datetime" 
      + ")\n";

    try {
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
    } catch(SQLException se){
      return -1;
    }

    sql = "CREATE TABLE IF NOT EXISTS iot_files (\n"
      + "id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n"
      + "did INT NOT NULL,\n"
      + "fid INT,\n"
      + "stored datetime NOT NULL,\n"
      + "cf int,\n"
      + "content blob,\n"
      +" filename varchar(64) NOT NULL,\n"
      + "len int,\n"
      + "crc32 int,\n"
      + "FOREIGN KEY(did) REFERENCES iot_devices(id) ON DELETE CASCADE"
      + ")\n";

    try {
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
    } catch(SQLException se){
      return -2;
    }

    sql = "CREATE TABLE IF NOT EXISTS iot_messages (\n"
      + "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n"
      + "did INT(8) NOT NULL,\n"
      + "ts BIGINT UNSIGNED NOT NULL,\n"
      + "stored datetime,\n"
      + "FOREIGN KEY(did) REFERENCES iot_devices(id) ON DELETE CASCADE"
      + ")\n";

    try {
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
    } catch(SQLException se){
      se.printStackTrace();
      return -2;
    }

    sql = "CREATE TABLE IF NOT EXISTS iot_entries (\n"
      + "id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,\n"
      + "did INT NOT NULL,\n"
      + "mid INT NOT NULL,\n"
      + "n varchar(32) NOT NULL,\n"
      + "t BIGINT UNSIGNED NOT NULL,\n"
      + "u varchar(32) NOT NULL,\n"
      + "v  DOUBLE,\n"
      + "sv varchar(32),\n"
      + "bv BOOLEAN,\n"
      + "FOREIGN KEY(did) REFERENCES iot_devices(id) ON DELETE CASCADE,\n"
      + "FOREIGN KEY(mid) REFERENCES iot_messages(id) ON DELETE CASCADE"
      + ")\n";

    try {
      Statement stmt = conn.createStatement();
      stmt.execute(sql);
      stmt.close();
    } catch(SQLException se){
      se.printStackTrace();
      return -2;
    }

    return 0;
  }


  /**
   * Returns the database ID of a specific system
   *
   */
  static int macToID(String name, Connection conn) {
    int id=-1;

    System.out.println("macToID('"+name+"')");
    Statement stmt = null;
    try {
      //Class.forName("com.mysql.jdbc.Driver");

      stmt = conn.createStatement();
      String sql;
      sql = "SELECT id FROM iot_devices WHERE name='"+name+"';";
      ResultSet rs = stmt.executeQuery(sql);

      rs.next();
      id  = rs.getInt("id");

      rs.close();
      stmt.close();
    }catch(SQLException se){
      id = -1;
      //se.printStackTrace();
    }catch(Exception e){
      id = -1;
      e.printStackTrace();
    }

    //System.out.println("macToID('"+name+"')="+id);
    return id;
  }


  static boolean updateEndpoint(String name, Vector<SenMLMessage> msg) {
    boolean ret = false;
    try {
      Connection conn = getConnection();
      int id = macToID(name, conn);
      if (id != -1) {
	SigMLMessage m = new SigMLMessage();
	m.setSenML(msg);
	ret = updateEndpoint(name, m);
	closeConnection(conn);
      } else {
      }
    } catch (SQLException e) {
      ret = false;
    }
    return ret;
  }

  static boolean updateEndpoint(String name, SigMLMessage msg) {
    boolean ret = false;
    try {
      Connection conn = getConnection();
      int id = macToID(name, conn);
      System.out.println("Got id of: " + id);
      if (id != -1) {
	Statement stmt = conn.createStatement();
	String sql = "INSERT INTO iot_messages(did, ts, msg, stored) VALUES("+id+", 0, '"+msg.toString()+"',NOW());"; //how to escape "
	System.out.println(sql);
	int mid = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	ResultSet rs = stmt.getGeneratedKeys();
	rs.next();
	mid = rs.getInt(1);
	rs.close();

	closeConnection(conn);
      } else {
      }
    } catch (SQLException e) {
      System.err.println(e.toString());
    }

    return false;
  }


  static boolean createEndpoint(String name) {
    try {
      Connection conn = getConnection();
      int id = macToID(name, conn);
      System.out.println("createEndpoint: found " + id);
      if (id != -1) {
	closeConnection(conn);
	return true; //already exists
      } else {
	Statement stmt = conn.createStatement();
	String sql = "INSERT INTO iot_devices(name) VALUES(\""+name+"\");"; //bug: check name for SQL injection!
	//System.out.println(sql);
	int mid = stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
	ResultSet rs = stmt.getGeneratedKeys();
	rs.next();
	id = rs.getInt(1);
	rs.close();
	System.out.println("createEndpoint: created " + id);

	closeConnection(conn);
      }
  
    } catch (SQLException e) {
    }
    return true;
  }


  static SenMLMessage fetchEndpoint(String name) {
    return null;
  }

  static boolean deleteEndpoint(String name) { //XXX: do not support this now right now
    return false;
  }

}