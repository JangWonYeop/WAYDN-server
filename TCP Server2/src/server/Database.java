package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Database {
	
	Connection connection;
	
	public Database(){
		try {
			connection = DriverManager.getConnection("jdbc:mysql://localhost/androidappdata?autoReconnect=true&useSSL=false", "root", "1234");
			if(connection != null){
				System.out.println("Database Connected");
			}
			else{
				System.out.println("Database Connection failed");
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void registerNumber(String mynum, String otherNum, String name){
		String sql = "insert into registerednumber values(?, ?, ?, ?)";
		try {
			PreparedStatement pstmt = connection.prepareStatement(sql);
			pstmt.setInt(1, 0);
			pstmt.setString(2, mynum);
			pstmt.setString(3, otherNum);
			pstmt.setString(4, name);
			
			pstmt.executeUpdate();
			if(pstmt != null) try{pstmt.close();}catch(SQLException sqle){} 
			
			System.out.println("Number Regist done");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String deleteNumber(String myPhone, String otherPhone, String name){
		System.out.println(myPhone + " " + otherPhone + " " + name);
		String sql = "delete from registerednumber where myNumber = '" + myPhone + "' and otherNumber = '" + otherPhone + "' and name = '" + name + "'";
		try{
			PreparedStatement pstmt = connection.prepareStatement(sql);
			pstmt.executeUpdate();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		/*
		synchronized(MainClass.syncObj2){
			MainClass.syncObj2.notify();
		}
		*/
		
		return "4";
	}
	
	// no, phoneNumber, date, time, name, soundMode, speed, battery, network, latitude, longitude, screen
	public void inputData(String str[]){
		String sql = "insert into numberdata values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try {
			PreparedStatement pstmt = connection.prepareStatement(sql);
			pstmt.setInt(1, 0);
			pstmt.setString(2, str[0]);
			
			SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
			try {
				Date parsed = format.parse(str[1]);
				java.sql.Date date = new java.sql.Date(parsed.getTime());
				pstmt.setDate(3, date);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			format = new SimpleDateFormat("HH:mm:ss");
			try {
				Date parsed = format.parse(str[2]);
				java.sql.Time time = new java.sql.Time(parsed.getTime());
				pstmt.setTime(4, time);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			pstmt.setString(5, str[3]);
			pstmt.setString(6, str[4]);
			pstmt.setFloat(7, Float.parseFloat(str[5]));
			pstmt.setInt(8, Integer.parseInt(str[6]));
			pstmt.setString(9, str[7]);
			pstmt.setDouble(10, Double.parseDouble(str[8]));
			pstmt.setDouble(11, Double.parseDouble(str[9]));
			pstmt.setString(12, str[10]);
			
			pstmt.executeUpdate();
			if(pstmt != null) try{pstmt.close();}catch(SQLException sqle){} 
			
			System.out.println("insert done");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("insert fail");
		} 
	}
	
	public ResultSet getData(String table, String col, String val){
		String sql = "Select * from " + table + " Where " + col + " = " + val;
		ResultSet rs = null;
		try {
			PreparedStatement pstmt = connection.prepareStatement(sql);
			rs = pstmt.executeQuery();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return rs;
	}
}
