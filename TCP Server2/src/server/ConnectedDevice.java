package server;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class ConnectedDevice extends Thread {
	
	Socket socket = null;
	
	String phoneNumber = null;
	String msg = null;
	
	DataInputStream  input;
	DataOutputStream output;
	
	public ConnectedDevice(Socket socket){
		this.socket = socket;
		try{
			input = new DataInputStream(socket.getInputStream());
			output = new DataOutputStream(socket.getOutputStream());
		} catch(Exception e){
			e.getMessage();
		}
	}
	
	public void run(){
		try{
			phoneNumber = input.readUTF();
			// 전화번호는 무조건 010 으로 시작하도록 한다.
			// +82 로 시작하는 번호는 0으로 바꾼다.
			
			if(Integer.compare(phoneNumber.indexOf("+82"), -1) != 0 ){
				phoneNumber = phoneNumber.replace("+82", "0");
			}
			
			System.out.println(phoneNumber + " connected");
			
			// 전화번호와 socket을 map으로 저장한다.
			// 동기화한다.
			synchronized(MainClass.syncObj){
				MainClass.map.put(phoneNumber, socket);
			}
			
			while(input != null){
				String temp = input.readUTF();
				System.out.println(temp);
				msgRecvTestFunc(temp);
			}
			
		}catch(Exception e){
			e.getLocalizedMessage();
		}
	}
	
	public void msgRecvTestFunc(String str){
		
		// 0은 테스트를 의미한다.
		if(str.equals("0")){
			try{
			String to = input.readUTF();
			String msg = input.readUTF();
			
			System.out.println("to : " + to + ", msg : " + msg);
			
			// 다른 클라이언트에게 전달.. 어떻게..?
			// map에서 전화번호를 이용해 소켓을 불러와 전송한다.
			Socket tempSocket = MainClass.map.get(to);
			
			//System.out.println(tempSocket.getInetAddress());
			
			DataInputStream  tempInput = new DataInputStream(tempSocket.getInputStream());
			DataOutputStream tempOutput = new DataOutputStream(tempSocket.getOutputStream());
			tempOutput.writeUTF("0");
			tempOutput.writeUTF(msg);
			
			}catch(Exception e){
				e.getMessage();
			}
		}
		
		// -1은 연결 종료를 의미한다.
		else if(str.equals("-1")){
			synchronized(MainClass.syncObj){
				try {
					MainClass.map.get(phoneNumber).close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				MainClass.map.remove(phoneNumber);
				System.out.println(phoneNumber + " disConnected");
			}
		}
		
		// 1은 데이터 갱신을 의미한다.
		else if(str.equals("1")){
			try{
				String data = input.readUTF();
				System.out.println(data);
				String parsed[] = new String[11];
				String temp[] = parseData(data);
				parsed[0] = phoneNumber;
				for(int i=0; i<10; i++){
					parsed[i+1] = temp[i];
				}
				
				// 데이터베이스 연결
				Database database = new Database();
				database.inputData(parsed);
				
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		// 2는 새로운 번호 등록을 의미한다.
		else if(str.equals("2")){
			
			try{
				// 번호와 이름을 받아온다.
				String num = input.readUTF();
				String name = input.readUTF();
				
				// 번호가 이미 등록되어있는지 확인한다.
				Database database = new Database();
				ResultSet rs = database.getData("registerednumber", "otherNumber", num);
				while(rs.next()){
					String no = rs.getString("no");
					String mynumber = rs.getString("myNumber");
					String otherNumber = rs.getString("otherNumber");
					String _name = rs.getString("name");
					
					// 번호가 이미 등록되어 있다면 100번 송신 후 종료
					if(mynumber.equals(phoneNumber) && otherNumber.equals(num)){
						output.writeUTF("100"); // 100번은 이미 존재하는 번호라는 뜻.
						return;
					}
				}
				
				// 등록되어있지 않다면 등록하고 101번 송신 후 종료
				database.registerNumber(phoneNumber, num, name);
				output.writeUTF("101");
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		// 등록된 번호에 대한 정보 요청
		else if(str.equals("3")){
			
			Database database = new Database();
			ResultSet rs = database.getData("registerednumber", "myNumber", phoneNumber);
			int size = -1;
			
			// 200은 등록된 번호 요청에 대한 응답을 의미한다.
			try {
				output.writeUTF("200");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// 데이터 사이즈를 보낸다.
			try{
				rs.last();
				size = rs.getRow();
				rs.beforeFirst();
				output.writeUTF(String.valueOf(size));
				System.out.println(String.valueOf(size));
			}catch(Exception e){
				e.printStackTrace();
			}
			
			String Data[] = new String[size];
			int cnt = 0;
			try {
				while(rs.next()){
					String no = rs.getString("no");
					String mynumber = rs.getString("myNumber");
					String otherNumber = rs.getString("otherNumber");
					String _name = rs.getString("name");
					
					Data[cnt] += no + " " + mynumber + " " + otherNumber + " " + _name + " " + machineLearning(otherNumber);
					cnt++;
				}
				
				for(int i=0; i<size; i++){
					try {
						output.writeUTF(Data[i]);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// 등록된 번호 삭제.
		else if(str.equals("4")){
			// 번호와 이름을 받아온다.
			String phoneNum = null;
			String Name = null;
			try {
				phoneNum = input.readUTF();
				Name = input.readUTF();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// DB에서 내번호, 상대 번호, 상대 이름이 일치하는 데이터 삭제
			Database database = new Database();
			String returnVal = database.deleteNumber(phoneNumber, phoneNum, Name);
			
			/*
			synchronized(MainClass.syncObj2){
				try {
					MainClass.syncObj2.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			*/
			
			try {
				output.writeUTF(returnVal);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// 데이터베이스에서 해당 번호에 대한 최신 데이터를 전송한다.
		else if(str.equals("5")){
			
			String phone = null;
			//int last = -1;
			
			try {
				phone = input.readUTF();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			Database database = new Database();
			ResultSet rs = database.getData("numberdata", "phonenumber", phone);
			
			// 데이터가 없다면 500을 리턴하고 끝낸다.
			try {
				if(rs.first() == false){
					try {
						output.writeUTF("500");
						return;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (SQLException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
			// 응답 전송
			try {
				output.writeUTF("5");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			// 가장 마지막 데이터 얻기
			
			String str_date = null;
			String str_time = null;
			String name = null;
			String soundmode = null;
			String speed = null;
			String battery = null;
			String network = null;
			String latitude = null;
			String longitude = null;
			String screen = null;
			
			try {
				rs.last();
				//last = rs.getRow();
				
				Date date = new Date();
				date = rs.getDate("date");
				SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
				str_date = transFormat.format(date);
				
				Date time = new Date();
				time = rs.getTime("time");
				SimpleDateFormat transFormat2 = new SimpleDateFormat("HH:mm:ss");
				str_time = transFormat2.format(time);
				System.out.println(str_time);
				
				name = rs.getString("name");
				soundmode = rs.getString("soundmode");
				speed = String.valueOf(rs.getFloat("speed"));
				battery = String.valueOf(rs.getInt("battery"));
				network = rs.getString("network");
				latitude = String.valueOf(rs.getFloat("latitude"));
				longitude = String.valueOf(rs.getFloat("longitude"));
				screen = rs.getString("screen");
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// 가장 최근 데이터 하나만 전송
			try {
				output.writeUTF(str_date);
				output.writeUTF(str_time);
				output.writeUTF(name);
				output.writeUTF(soundmode);
				output.writeUTF(speed);
				output.writeUTF(battery);
				output.writeUTF(network);
				output.writeUTF(latitude);
				output.writeUTF(longitude);
				output.writeUTF(screen);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public String[] parseData(String str){
		String strArr[] = new String[10];
		
		int index;
		int endindex;
		
		// date
		index = str.indexOf("date");
		strArr[0] = str.substring(index+7, index+17);
		//System.out.println(strArr[0]);
		
		// time
		strArr[1] = str.substring(index+18, index+26);
		//System.out.println(strArr[1]);
		
		// app name
		index = str.indexOf("name");
		endindex = str.indexOf("soundMode");
		strArr[2] = str.substring(index+7, endindex-2);
		//System.out.println(strArr[2]);
		
		// sound mode
		index = endindex;
		endindex = str.indexOf(", speed");
		strArr[3] = str.substring(index+12, endindex);
		//System.out.println(strArr[3]);
		
		// speed
		index = str.indexOf("speed");
		endindex = str.indexOf("battery");
		strArr[4] = str.substring(index+8, endindex-2);
		//System.out.println(strArr[4]);
		
		// battery
		index = endindex;
		endindex = str.indexOf("network");
		strArr[5] = str.substring(index+10, endindex-2);
		//System.out.println(strArr[5]);
		
		// network
		index = endindex;
		endindex = str.indexOf("latitude");
		strArr[6] = str.substring(index+10, endindex-2);
		//System.out.println(strArr[6]);
		
		// latitude
		index = endindex;
		endindex = str.indexOf("longitude");
		strArr[7] = str.substring(index+11, endindex-2);
		//System.out.println(strArr[7]);
		
		// longitude
		index = endindex;
		endindex = str.indexOf("screen");
		strArr[8] = str.substring(index+12, endindex-2);
		//System.out.println(strArr[8]);
		
		// screen
		index = endindex;
		strArr[9] = str.substring(index+9);
		
		return strArr;
	}
	
	// 머신러닝
	// 1. 등록된 번호에 대한 최신 데이터 요청
	// 2. 해당 데이터로 머신러닝 돌리기
	// 3. 결과를 파싱하여 리턴
	public char machineLearning(String phone){
		char aChar = 0;
		
		Database database = new Database();
		ResultSet rs = database.getData("numberdata", "phonenumber", phone);
		
		// 데이터가 없다면 C를 리턴하고 끝낸다.
		try {
			if(rs.first() == false){
					return 'C';	
			}
		} catch (SQLException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		// 가장 마지막 데이터 얻기
		String str_date = null;
		String str_time = null;
		String name = null;
		String soundmode = null;
		String speed = null;
		String battery = null;
		String network = null;
		String latitude = null;
		String longitude = null;
		String screen = null;
		
		String day = null;
		
		try {
			rs.last();
			//last = rs.getRow();
			
			Date date = new Date();
			date = rs.getDate("date");
			SimpleDateFormat transFormat = new SimpleDateFormat("yyyy-MM-dd");
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			int daynum = cal.get(Calendar.DAY_OF_WEEK);
			switch(daynum){
	        case 1:
	            day = "7";
	            break ;
	        case 2:
	            day = "1";
	            break ;
	        case 3:
	            day = "2";
	            break ;
	        case 4:
	            day = "3";
	            break ;
	        case 5:
	            day = "4";
	            break ;
	        case 6:
	            day = "5";
	            break ;
	        case 7:
	            day = "6";
	            break ;    
	    }
			
			str_date = transFormat.format(date);
			
			Date time = new Date();
			time = rs.getTime("time");
			SimpleDateFormat transFormat2 = new SimpleDateFormat("HH:mm:ss");
			str_time = transFormat2.format(time);
			System.out.println(str_time);
			
			name = rs.getString("name");
			soundmode = rs.getString("soundmode");
			speed = String.valueOf(rs.getFloat("speed"));
			battery = String.valueOf(rs.getInt("battery"));
			network = rs.getString("network");
			latitude = String.valueOf(rs.getFloat("latitude"));
			longitude = String.valueOf(rs.getFloat("longitude"));
			screen = rs.getString("screen");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// 머신러닝에 맞게 파싱하여 파일로 저장.
		FileOutputStream output = null;
		try {
			output = new FileOutputStream("c:/MachineLearning/out.txt");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// 속도, 베터리, 위치, 시간, 앱번호, 소리, 네트워크상태, 스크린상태, 요일번호, 클래스번호
		str_time = str_time.substring(0, 2);
		switch(name){
		case "지금 뭐해?" : name = "1"; break;
		case "지니 뮤직" : name = "2"; break;
		case "카카오톡" : name = "3"; break;
		case "이메일" : name = "4"; break;
		case "TouchWiz 홈" : name = "5"; break;
		case "설정" : name = "6"; break;
		case "안드로이드 시스템" : name = "7"; break;
		case "Facebook" : name = "8"; break;
		case "커플 위젯" : name = "9"; break;
		case "알람/시간" : name = "10"; break;
		case "Syrup 테이블" : name = "11"; break;
		case "Clean Master" : name = "12"; break;
		case "전화" : name = "13"; break;
		case "연락처" : name = "14"; break;
		case "KB스타뱅킹" : name = "15"; break;
		case "Google Play 스토어" : name = "16"; break;
		case "메시지" : name = "17"; break;
		case "LINE" : name = "18"; break;
		case "Smart Manager Provider" : name = "19"; break;
		case "시스템 UI" : name = "21"; break;
		case "Chrome" : name = "22"; break;
		case "Samsung Pay" : name = "23"; break;
		case "Sankaku Black" : name = "24"; break;
		case "갤러리" : name = "25"; break;
		case "스마트 매니저" : name = "26"; break;
		case "후후" : name = "27"; break;
		default : name = "-1"; break;
		}
		
		switch(soundmode){
		case "Normal" : soundmode = "1"; break;
		case "Silent" : soundmode = "2"; break;
		case "Vibrate" : soundmode = "3"; break;
		default : soundmode = "-1"; break;
		}
		
		switch(network){
		case "Mobile data" : network = "1"; break;
		case "Wifi" : network = "2"; break;
		case "Not connected" : network = "3"; break;
		default : network = "-1"; break;
		}
		
		switch(screen){
		case "ON" : screen = "1"; break;
		case "OFF" : screen = "2"; break;
		default : screen = "-1"; break;
		}
		
		String str = speed + " " + battery + " " + latitude + " " + longitude + " " + str_time 
				+ " " + name + " " + soundmode + " " + network + " " + screen + " " + day
				+ " " + "-1";
		
		try {
			output.write(str.getBytes());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        try {
			output.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        String s = null;
        String totalStr = null;
        try {
        	
        	/*
        	String param1 = "C:\\MachineLearning";
            ProcessBuilder builder = new ProcessBuilder("openCV.exe", "train_data.txt", "out.txt");
            builder.directory(new File(param1));
            Process oProcess = builder.start();
            */
        	
        	String[] cmdArray = {"C:\\MachineLearning\\openCV.exe", "C:\\MachineLearning\\training_data.txt", "C:\\MachineLearning\\out.txt"};
        	Process oProcess = new ProcessBuilder(cmdArray).start();
        	
            // 외부 프로그램 출력 읽기
            BufferedReader stdOut   = new BufferedReader(new InputStreamReader(oProcess.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(oProcess.getErrorStream()));
            stdError.close();
            
            // "표준 출력"과 "표준 에러 출력"을 출력
            while ((s =  stdOut.readLine()) != null){
            	totalStr += s;
            } 
            
            System.out.println(totalStr);
            stdOut.close();

          } catch (IOException e) { // 에러 처리
              System.err.println("에러! 외부 명령 실행에 실패했습니다.\n" + e.getMessage());
              System.exit(-1);
          }
          
        aChar = totalStr.charAt(48);
        System.out.println(aChar);
		
		return aChar;
	}
	
}
