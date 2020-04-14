package chat8;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Date;

public class MultiServer {
	
	// jdbc멤버변수
	static Connection con; // db연결을 위한 객체
	static Statement stmt; // 쿼리전송 및 실행을 위한 객체
	static PreparedStatement psmt; // 쿼리전송 및 실행을 위한 객체
	static ResultSet rs; // execteQuery의 명령에 대한 반환값
	
	// db 연결
	static String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	static String ORACLE_URL = "jdbc:oracle:thin://@localhost:1521:orcl";
	
	// 소켓 정의
	static ServerSocket serverSocket = null;
	static Socket socket = null;
	// 클라이언트 정보 저장을 위한 Map컬렉션 정의
	static Map<String, PrintWriter> clientMap;
	
	// 생성자
	public MultiServer() {
		// 클라이언트의 이름과 출력스트림을 저장할 HashMap 생성
		clientMap = new HashMap<String, PrintWriter>();
		// HashMap 동기화 설정. 쓰레드가 사용자정보에 동시에 접근하는 것을 차단한다.
		Collections.synchronizedMap(clientMap);
		
		try {
			//1.오라클 드라이버 로드
			Class.forName("oracle.jdbc.OracleDriver");
			
			//2.커넥션 객체를 통해 DB연결
			con = DriverManager.getConnection(
					"jdbc:oracle:thin://@localhost:1521:orcl",
					"kosmo",
					"1234");
			
			System.out.println("오라클 DB 연결성공");
		}
		
		 catch (Exception e) {
			 System.out.println("알수 없는 예외발생");
			 e.printStackTrace();
		 }
	}
	
	// 서버의 초기화를 담당할 메소드
	public void init() {
		
		try {
			// 9999포트를 열고 클라이언트의 접속을 대기
			serverSocket = new ServerSocket(9999);
			System.out.println("서버가 시작되었습니다.");
			
			/*
			 1명의 클라이언트가 접속할 때마다 접속을 허용(accept())해주고
			 동시에 MultiSeverT 쓰레드를 생성한다.
			 해당 쓰레드는 1명의 클라이언트가 전송하는 메세지를 읽어서
			 Echo해주는 역할을 담당한다.
			 */
			while(true) {
				socket = serverSocket.accept();
				System.out.println(socket.getInetAddress()+":"+socket);
				/*
				 클라이언트의 메세지를 모든 클라이언트에게 전달하기 위한 스레드 생성 및 start
				 */
				
				Thread mst = new MultiServerT(socket);
				mst.start();
			}
		}
		catch (Exception e) {
			System.out.println("예외1 : "+e);		
		}
		finally {
			try {
				serverSocket.close();
			}
			catch(Exception e) {
				System.out.println("예외2 : "+e);
			}
		}
	}
			
	// 메인메소드 : Server객체를 생성한 후 초기화한다.		
	public static void main(String[] args) {
		MultiServer ms = new MultiServer();
		ms.init();		
	}
	
	// 접속된 모든 클라이언트에게 메세지를 전달하는 역할의 메소드
	public void sendAllMsg(String name, String msg) {
		
		// Map에 저장된 객체의 키값을 먼저 얻어온다.
		Iterator<String> it = clientMap.keySet().iterator();
		
		// 저장된 객체의 갯수만큼 반복한다.
		while(it.hasNext()) {
			try {
				// 각 클라이언트의 PrintWriter객체를 얻어온다.
				PrintWriter it_out = (PrintWriter)
				clientMap.get(it.next());
				
				// 클라이언트에게 메세지를 전달한다.
				/*
				 매개변수 name이 있는 경우에는 이름 + 메세지
				 없는 경우에는 메세지만 클라이언트로 전달한다.
				 */
				// 클라이언트로 한글을 보낼 때 인코딩
				if(name.equals(" ")) {
					it_out.println(URLEncoder.encode(msg, "UTF-8"));
				}
				else {
					it_out.println("["+name+"]:" + msg);
				}
			}						
			catch(Exception e) {
				System.out.println("예외:"+e);
				e.printStackTrace();
			}
		}
	}
	
	// 내부클래스
	class MultiServerT extends Thread{
		
		// 멤버변수
		Socket socket;
		PrintWriter out = null;
		BufferedReader in = null;
		
		// 생성자 : Socket을 기반으로 입출력 스트림을 생성한다.
		public MultiServerT(Socket socket) {
			this.socket = socket;
			try {
				out = new PrintWriter(this.socket.getOutputStream(), true);
				// [서버] 클라이언트에서 올라온 한글 데이터 받을 때 : UTF-8로 인코딩
				in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),"UTF-8"));
			}			
			catch(Exception e) {
				System.out.println("예외:"+e);
				e.printStackTrace();
			}			
		}
		
		@Override
		public void run() {
			
			// 클라이언트로부터 전송된 "대화명"을 저장할 변수
			String name = " ";
			// 메세지 저장용 변수
			String s = " ";
			
			try {
				// 클라이언트의 이름을 읽어와서 저장
				name = in.readLine();				
				
				// [서버] 클라이언트에서 올라온 한글 데이터 사용할 때 디코딩
				name = URLDecoder.decode(name, "UTF-8");
				
				// 접속한 클라이언트에게 새로운 사용자의 입장을 알림.
				// 접속자를 제외한 나머지 클라이언트만 입장메세지를 받는다.
				sendAllMsg(" ", name+"님이 입장하셨습니다.");
				
				// 현재 접속한 클라이언트를 HashMap에 저장한다.
				clientMap.put(name, out);
				
				// HashMap에 저장된 객체의 수로 접속자 수를 파악할 수 있다.
				System.out.println(name + " 접속");
				System.out.println(" 현재 접속자 수는 "+clientMap.size()+"명입니다.");
				
//				if(s.equalsIgnoreCase("/list")) {
//					System.out.println("저장된 객체수:"+ clientMap.size());
//					System.out.println("키값을 알때:"+ clientMap.get("name"));
//				}						
//				
											
				// 입력한 메세지는 모든 클라이언트에게 Echo된다.
				while(in != null) {
					s = in.readLine();
					// [서버] 클라이언트에서 올라온 한글 데이터 사용할 때 디코딩
					s = URLDecoder.decode(s, "UTF-8");
					System.out.println(s);
					
					if(s == null) break;
					
					System.out.println(name + ">>" + s);
					sendAllMsg(name, s);
					
					/////////////////추가된 부분///////////////////////////
					try	{
						//1.쿼리문준비 : 값의 세팅이 필요한 부분을 ?로 대체한다.
						String Query = "insert into chatting_tb values (seq_num.nextval, ?,?,?)";
						
						//2.prepared객체 생성 : 생성시 준비한 쿼리문을 인자로 전달한다.
						psmt = con.prepareStatement(Query);
						
						//3.인파라미터 설정하기 : ?의 순서대로 설정하고 DB이므로 인덱스는 1부터 시작.
						psmt.setString(1, name);
						psmt.setString(2, s);
						
						SimpleDateFormat dateformat = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" );
						Date date = new Date();
						String time1 = dateformat.format(date);
						psmt.setString(3, time1);
						
						
						//4.쿼리실행을 위해 prepared객체를 실행한다. 
						psmt.executeUpdate();
						System.out.println("DB저장 성공");	
					}
					
					catch (Exception e) {
						System.out.println("DB저장 실패");
						e.printStackTrace();
					}
					
					finally {
						if(psmt != null) {
							try {
								psmt.close();
							}
							catch(SQLException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
			
			catch(Exception e) {
				System.out.println("예외:"+e);
				e.printStackTrace();
			}
			finally {
				/*
				 클라이언트가 접속을 종효하면 예외가 발생하게 되어 finally로
				 넘어오게 된다. 이때 "대화명"을 통해 remove()시켜준다.
				 */
				clientMap.remove(name);
				sendAllMsg(" ", name + "님이 퇴장하셨습니다.");
				// 퇴장하는 클라이언트의 쓰레드명을 보여준다.
				System.out.println(name + " [" + 
				Thread.currentThread().getName()+ "] 퇴장");
				System.out.println("현재 접속자 수는"
						+ clientMap.size()+"명 입니다.");
				
				try {
					in.close();
					out.close();
					socket.close();
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}