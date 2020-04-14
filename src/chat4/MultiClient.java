package chat4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class MultiClient {

	public static void main(String[] args) {

		System.out.println("이름을 입력하세요:");
		Scanner scanner = new Scanner(System.in);
		String s_name = scanner.nextLine();
		
		PrintWriter out = null;
		// 서버의 메세지를 읽어오는 기능이 Receiver로 옮겨짐.
		// BufferedReader in = null;
		
		try {
			/*
			 - c:\> java 패키지명.MultiClient 접속할 IP주소
			 	=> 위와 같이 하면 해당 IP주소로 접속 할 수 있다.
			 	만약 IP주고사 없다면 localhost(127.0.0.1)로 접속된다.
			 	
			 - 본인 IP주소 확인하는 법 : cmd창에서 ipconfig 입력
			 */			
			String SeverIP = "localhost";
			// 클라이언트 실행시 매개변수가 있는 경우 아이피로 설정함
			if(args.length > 0) {
				SeverIP = args[0];
			}
			
			// IP주소와 포트를 기반으로 소켓객체를 생성하여 서버에 접속함
			Socket socket = new Socket(SeverIP, 9999);
			// 서버와 연결되면 콘솔에 메세지 출력
			System.out.println("서버와 연결되었습니다...");
			
			// 서버에서 보내는 메세지를 읽어올 Receiver쓰레드 시작
			Thread receiver = new Receiver(socket);
			// setDaemon(true)가 없으므로 독립쓰레드로 생성됨
			receiver.start();
			
			/*
			 InputStreamReader / OutputStreamReader는
			 바이트스트림과 문자스트림의 상호변환을 제공하는 입출력스트림이다.
			 바이트를 읽어서 지정된 문자인코딩에 따라 문자로 변환하는데 사용된다.
			 */
			out = new PrintWriter(socket.getOutputStream(), true);
						
			//접속자의 "대화명"을 서버측으로 최초 전송한다.
			out.println(s_name);
			
			/*
			 소켓이 close되기전이라면 클라이언트는 지속적으로 서버측으로 메세지를 보낼 수 있다.
			 */
			while(out != null) {
				try	{
					String s2 = scanner.nextLine();				
					if(s2.equals("q") || s2.equals("Q")) {
						// 입력값이 Q(q)이면 while 루프 탈출
						break;
					}
					else {
						// 클라이언트의 메세지를 서버로 전송한다.						
						out.println(s2);
					}
				}
				catch (Exception e) {
					System.out.println("예외:"+ e);
				}
			}
			
			// 클라이언트가 q를 입력하면 소켓과 스트림이 모두 종료됨.		
			out.close();
			socket.close();
		}
		catch (Exception e) {
			System.out.println("예외발생[MultiClient]"+e);
		}
	}	
}
