package day1117.db;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class MyTableModel extends AbstractTableModel{

	//아래의 두 백터는 현재는 0이지만 유저가 원하는 오라클의 테이블명을 JTable에서 선택할 때,
	//레코드와 컬럼에 대한 정보를 조사해서 동적으로 채워넣을 예정임!!!!
	Vector<Vector> record = new Vector<>();//테이블에 보여질 레코드를 처리하는 백터 선언(지금은 0)
	Vector<String> column = new Vector<String>();//테이블에 보여질 컬럼 정보를 갖는 백터 선언(현재는 0)
	
	public MyTableModel(Vector record, Vector column) {
		this.record = record;
		this.column = column;
	}
	
	public int getRowCount() {
		return record.size();
	}
	
	@Override
	public int getColumnCount() {
		return column.size();
	}
	
	public String getColumnName(int col) {
		return column.get(col);
	}
	
	@Override
	public Object getValueAt(int row, int col) {
		return record.get(row).get(col);//층을 먼저 뽑는다. 추출된 층(row)수 Vector의 호(col)수를 가져온다.
	}

}
