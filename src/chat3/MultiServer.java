package chat3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiServer {
	
	static ServerSocket severSocket = null;
	static Socket socket = null;
	static PrintWriter out = null;
	static BufferedReader in = null;
	static String s = ""; // 클라이언트의 메세지를 저장
	
	// 생성자
	public MultiServer() {
		//실행부 없음
	}
	
	// 서버의 초기화를 담당할 메소드
	public static void init() {
		
		// 클라이언트로부터 전송받은 이름을 저장
		String name = ""; 
		
		try {
			// 9999포트를 열고 클라이언트의 접속을 대기
			severSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			///.......접속대기중........
			
			//클라이언트가 접속 요청을 하면 accept()메소드를 통해 받아들인다.
			socket = severSocket.accept();
			System.out.println(socket.getInetAddress() +":"+socket.getPort());
			
			// 서버 -> 클라이언트 측으로 메세지를 전송(출력)하기위한 스트림을 생성
			out = new PrintWriter(socket.getOutputStream(), true);
			// 클라이언트로부터 메세지를 받기 위한 스트림을 생성
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			
			/*
			 클라이언트가 서버로 전송하는 최초의 메세지는 접속자의 이름
			 메세지를 읽은 후 변수에 저장하고 클라이언트쪽으로 Echo해준다.
			 */
			if(in != null){
				name = in.readLine();
				// 이름을 콘솔에 출력하고...
				System.out.println(name + "접속");
				// 클라이언트로 Echo해준다.
				out.println(">"+name+"님이 접속했습니다.");
			}
			
			// 클라이언트가 전송하는 메세지를 계속해서 읽어옴			 
			while(in != null) {
				s = in.readLine();
				if(s==null) {
					break;
				}
				// 읽어온 메세지를 콘솔에 출력하고...
				System.out.println(name +" ==> "+s);
				// 클라이언트에세 Echo해준다.
				sendAllMsg(name, s);
			}
			System.out.println("Bye...!!!");			
		}
		catch (Exception e) {
			System.out.println("예외1: "+e);
			// e.printStackTrace();
		}
		finally {
			try {
				// 입출력스트림 종료
				in.close();
				out.close();
				// 소켓(자원반납) 종료
				socket.close();
				severSocket.close();
			}
			catch (Exception e) {
				System.out.println("예외2:" + e);
				//e.printStackTrace();
			}
		}
	}
	
	// 서버가 클라이언트에게 메세지를 Echo해주는 메소드
	public static void sendAllMsg(String name, String msg) {
		try {
			out.println("> "+ name +" ==> "+msg);
		}
		catch (Exception e) {
			System.out.println("예외:" + e);
		}
	}
	
	public static void main(String[] args) {
		init();
	}
}