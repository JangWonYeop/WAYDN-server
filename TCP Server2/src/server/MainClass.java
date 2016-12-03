package server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
//import java.util.*;


public class MainClass {
	
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	OutputStream outputStream = null;
	InputStream inputStream = null;
	
	static int portNum = 12321;
	
	static HashMap<String, Socket> map = new HashMap<String, Socket>();
	
	// HashMap 동기화를 위한 오브젝트
	static final Object syncObj = new Object();
	// 통신 동기화를 위한 오브젝트
	static final Object syncObj2 = new Object();
	//static boolean order = false;
	
	public static void main(String[] args){
		try{
			serverSocket = new ServerSocket(portNum);
			
			while(true){
				System.out.println("접속 대기중");
				socket = serverSocket.accept();
				new ConnectedDevice(socket).start();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
