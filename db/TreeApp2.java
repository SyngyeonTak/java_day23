package day1117.db;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class TreeApp2 extends JFrame{
	JTree tree;
	JScrollPane scroll;
	
	String[] arr_accessory = {"반지", "목걸이", "귀걸이", "팔찌"};
	String[] arr_shoes= {"운동화", "구두", "슬리퍼", "샌들"};
	
	public TreeApp2() {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("옷");
		DefaultMutableTreeNode accessory= new DefaultMutableTreeNode("액세서리");
		DefaultMutableTreeNode shoes= new DefaultMutableTreeNode("신발");
		top.add(accessory);
		top.add(shoes);
		
		createNode(accessory, arr_accessory);
		createNode(shoes, arr_shoes);
		
		tree = new JTree(top);
		scroll = new JScrollPane(tree);
		
		add(scroll);
		
		setSize(400, 500);
		setVisible(true);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		
	}
	
	public void createNode(DefaultMutableTreeNode top, String[] strArr) {
		DefaultMutableTreeNode[] node = new DefaultMutableTreeNode[strArr.length];
		for (int i = 0; i < node.length; i++) {
			node[i] = new DefaultMutableTreeNode(strArr[i]);
			top.add(node[i]);
		}
	}
	
	public static void main(String[] args) {
		new TreeApp2();
	}
}














