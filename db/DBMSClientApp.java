/*
	day1116일차에 구현했던 데이터베이스 클라이언트 프로그램에서 JTable 생성자의 Vector 방식을 이용하던 동적인
	테이블 선택시 유지보수성이 거의 불가능한 수준이므로 ,이를 개선해본다.
	즉, 유저가 어떤 테이블을 선택할지 알 수 없으므로, 선택한 테이블의 컬럼수, 구성 등을 예측할 수 없는 상황에
	대처해본다.
*/

package day1117.db;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

public class DBMSClientApp extends JFrame{
	JPanel p_west;//서쪽 영역 패널
	Choice ch_users; //유저명이 출력될 초이스 컴포넌트
	JPasswordField t_pass;//비밀번호 텍스트 필드
	JButton bt_login;//접속 버튼
	
	JPanel p_center;//그리드가 적용될 센터 패널
	JPanel p_upper;//테이블과 시퀀스를 포함할 패널(그리드 레이아웃 예정)
	JPanel p_middle;//sql편집기가 위치할 미들패널(BorderLayout)
	JPanel p_bottom;
	
	JTable t_tables;//유저의 테이블 정보를 출력할 JTable 
	JTable t_seq;//유저의 시퀀스 정보를 출력할 JTable 
	JTable t_record;//유저가 선택한 테이블의 레코드를 출력할 JTable
	JTable t_attribute;
	
	JTextArea area;
	JButton bt_execute;
	
	JScrollPane s1, s2, s3, s4, s5;//스크롤 4개 준비
	
	Connection con;
	String url = "jdbc:oracle:thin:@localhost:1521:XE";
	String user = "system";
	String password = "1234";
	
	//테이블을 출력할 백터 및 컬럼
	Vector tableList  = new Vector();//이 백터안에는 추후 또다른 백터가 들어갈 예정
													//단, 이차원 배열보다는 크기가 자유로워서 유연함... 코딩하기 쉬움
	Vector<String> tableColumn = new Vector<String>();
	
	Vector seqList = new Vector();
	Vector<String> seqColumn = new Vector<String>();
	
	//TableModel 보유
	MyTableModel model;
	MyTableModel columnModel;
	public DBMSClientApp() {
		tableColumn.add("table_name");
		tableColumn.add("tablespace_name");
		seqColumn.add("sequence_name");
		seqColumn.add("last_number");
		//생성
		p_west = new JPanel();
		ch_users = new Choice();
		t_pass = new JPasswordField();
		bt_login = new JButton("접속");
		
		p_center = new JPanel();
		p_upper = new JPanel();
		p_middle = new JPanel();
		p_bottom = new JPanel();
		
		p_center.setLayout(new GridLayout(3, 1));//3층에 1호수
		p_upper.setLayout(new GridLayout(1, 2));//1층에 2호수
		p_middle.setLayout(new BorderLayout());
		p_bottom.setLayout(new GridLayout(1, 2));
		
		area = new JTextArea();
		bt_execute = new JButton("SQL문 실행");
		
		t_tables = new JTable(tableList, tableColumn);
		t_seq = new JTable(seqList, seqColumn);
		s1 = new JScrollPane(t_tables);
		s2 = new JScrollPane(t_seq);
		s3 = new JScrollPane(area);//추후, 컬럼 정보 보여줄 스크롤
		
		//선택한 테이블의 레코드를 보여줄 JTable 생성
		t_record = new JTable(null);//MyTableModel을 대입할 예정
		s4 = new JScrollPane(t_record);
		
		t_attribute = new JTable(null);
		s5 = new JScrollPane(t_attribute);
		
		//스타일
		p_west.setPreferredSize(new Dimension(150, 350));
		ch_users.setPreferredSize(new Dimension(145, 30));
		t_pass.setPreferredSize(new Dimension(145, 30));
		bt_login.setPreferredSize(new Dimension(145, 30));
		p_middle.setBackground(Color.GREEN);
		area.setFont(new Font("Arial Black", Font.BOLD, 20));
		
		//조립
		p_west.add(ch_users);
		p_west.add(t_pass);
		p_west.add(bt_login);
		p_upper.add(s1);
		p_upper.add(s2);
		p_middle.add(s3);
		p_middle.add(bt_execute, BorderLayout.SOUTH);
		p_center.add(p_upper);//그리드의 1층
		p_center.add(p_middle);//그리드의 2층
		p_center.add(p_bottom);//그리드의 3층
		p_bottom.add(s4);
		p_bottom.add(s5);
		
		add(p_west, BorderLayout.WEST);
		add(p_center);
		
		setVisible(true);
		//setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				disconnect();
				System.exit(0);
			}
		});
		setSize(900, 750);
		setLocationRelativeTo(null);
		connect();
		getUserList();

		bt_login.addActionListener((e)->{
			login();
		});
		
		//테이블과 리스너 연결
		t_tables.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				//선택한 좌표의 테이블명 얻기!!
				int row = t_tables.getSelectedRow();//선택한 row 구하기
				int column = t_tables.getSelectedColumn();//선택한 column 구하기
				
				String tableName = (String)t_tables.getValueAt(row, column);
				tableName = tableName.toLowerCase();//소문자로 변환
				
				//구해진 테이블명을 select()메서드의 인수로 넘기자!!
				select(tableName);
				t_record.updateUI();//jtable 갱신
				//System.out.println("모델의 컬럼 카운트는 "+t_record.getColumnCount());
			}
		});
		
		bt_execute.addActionListener((e)->{
			select(null);
		});
	}
	
	//시퀀스 가져오기
	public void getSeqList() {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String sql = "select sequence_name, last_number from user_sequences";
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			seqList.removeAllElements();
			
			while(rs.next()) {
				Vector v = new Vector();
				v.add(rs.getString("sequence_name"));
				v.add(rs.getString("last_number"));
				seqList.add(v);
			}
			t_seq.updateUI();
			
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				if(rs != null) rs.close();
				if(pstmt != null) pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
	}
	
	
	//현재 접속 유저의 테이블 목록 가져오기
	public void getTableList() {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		
		String sql = "select table_name, tablespace_name from user_tables";
		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			
			tableList.removeAllElements();
			
			while(rs.next()) {
				Vector v = new Vector();//tablelList백터에 담겨질 백터
				v.add(rs.getString("table_name"));
				v.add(rs.getString("tablespace_name"));
				
				tableList.add(v);//멤버변수 백터에 담기..
				
			}
			//완성된 이차원 백터를 JTable에 반영해야 함, 생성자의 인수로 넣자
			t_tables.updateUI();
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				if(rs != null) rs.close();
				if(pstmt != null) pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
	}

	//유저가 선택한 테이블의 레코드 가져오기
	//이 메서드를 호출하는 者는 select문의 매개변수로 테이블명을 넘겨야 한다.
	//매개변수가 넘어오면, 테이블명만 사용하고 안넘어오면 전체 sql문 대체하자
	public void select(String tableName) {
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "";
		
		if(tableName != null) {//테이블 명을 매개변수로 넘기면 아래의 쿼리문 수행
			sql = "select * from "+tableName;
		}else {
			sql = area.getText();
		}
		

		try {
			pstmt = con.prepareStatement(sql);
			rs = pstmt.executeQuery();
			//이 rs를 어디에 담아야 할까??
			
			/*--------------------------------------
			 	컬럼 정보 만들기 위한 코드
			 --------------------------------------*/
			Vector tableColumn = new Vector();//이 백터는 새로운 TableModel 객체의 인스턴스가 가진 컬럼백터에 대입될 예정 
			Vector metaColumn = new Vector();
			
			ResultSetMetaData meta= rs.getMetaData();//메타데이터: 일련의 테이블 데이터를 가져올 수 있다.
			int columnCount = meta.getColumnCount();//총 컬럼 수
			//출력만 확인하지말고, MyTableModel이 보유한 컬럼용 백터에 정보를 채워넣자!!
			for (int i = 1; i <= columnCount; i++) {
				tableColumn.add(meta.getColumnName(i));
			}
			
			metaColumn.add("column_name");
			metaColumn.add("column_type");
			
			
			

			Vector data = new Vector();
			for (int i = 1; i <= columnCount; i++) {
				Vector vec = new Vector();
				vec.add(meta.getColumnName(i));
				vec.add(meta.getColumnTypeName(i));
				data.add(vec);
			}
			
			
			Vector record = new Vector();//이차원 백터
			while(rs.next()) {
				Vector vec = new Vector();//비어있는 일차원 백터(여기에 레코드 1건이 담겨질 예정)
				/*rs도 일정의 배열이므로, index로 컬럼을 접근할 수 있다.
				주의) 1부터 시작
				문제점)1부터 몇까지 컬럼이 존재하는 지 알 수가 없다!!
				그럼 어떻게 알 수 있을까?
				-이럴땐 테이블에 대한 메타 정보를 가져오면 된다.
				*/
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					vec.add(rs.getString(i));//데이터 체우기
				}
				record.add(vec);//MYTableModel이 보유한 백터에 추가하자~!(복습)
			}
			//데이터를 담은 이차원 백터와, 컬럼을 담은 일차원 백터를 새로운 모델객체를 생성하면서 전달하자!!
			model = new MyTableModel(record, tableColumn);
			columnModel = new MyTableModel(data, metaColumn);
			t_record.setModel(model);//테이블에 모델을 생성자가 아닌 메서드로 적용하자!!
			t_attribute.setModel(columnModel);
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				if(rs != null) rs.close();
				if(pstmt != null) pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void login() {
		disconnect();//접속끊기
		user = ch_users.getSelectedItem();//현재 초이스 컴포넌트에 선택된 아이템 값
		password = new String(t_pass.getPassword());
		connect();
		getTableList();//바로 이 시점에 로그인하자마자, 이 사람의 테이블 정보를 보여주는 게 좋다
		getSeqList();
		System.out.println("보유한 테이블 갯수: "+tableList.size());
	}
	
	
	//유저목록 가져오기
	public void getUserList() {
		//pstmt와 result는 소모품이므로 매 쿼리문마다 1개씩 대응
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sql = "select username from dba_users order by username asc";
		
		try {
			pstmt = con.prepareStatement(sql);//쿼리문 준비하기
			rs = pstmt.executeQuery();
			
			while(rs.next()) {
				ch_users.add(rs.getString("username"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				if(rs != null) rs.close();
				if(pstmt != null) pstmt.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
	public void connect() {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			con = DriverManager.getConnection(url, user, password);//접속시도
			if(con == null) {
				JOptionPane.showMessageDialog(this, user+"계정의 접속에 실패하였습니다.");
			}else {
				this.setTitle(user+" 계정으로 접속 중...");//프레임 제목에 성공 출력
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void disconnect() {
		try {
			if(con != null)con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) {
		new DBMSClientApp();
	}

}
















