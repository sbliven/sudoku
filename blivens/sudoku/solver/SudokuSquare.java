/* Sudoku
 * file SudokuSquare.java
 * 
 * Created on Aug 24, 2005 by blivens
 */
package blivens.sudoku.solver;

import java.awt.event.*;

import javax.swing.JComboBox;

/**
 * @author Spencer Bliven
 */
public class SudokuSquare extends JComboBox implements ActionListener {

	
	
	private SudokuSolver solver;
	private final int x,y;
	private MutableString lbl;
	
	public SudokuSquare(SudokuSolver s, int x, int y) {
		super();
		Object[] items = new Object[] { "1","2","3","4","5","6","7","8","9" };
		
		lbl = new MutableString(toGraphical(9));
		addItem(lbl);
		
		for(int i=0;i<items.length;i++)
			addItem(items[i]);
		this.setSelectedIndex(0);
		solver = s;
		this.x=x;
		this.y=y;
		
		addActionListener(this);
	}

	public int getGridX() {
		return x;
	}
	public int getGridY() {
		return y;
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void actionPerformed(ActionEvent e) {

		if(getSelectedIndex() == 0 || !isEnabled())
			return;
		String sel = (String)getSelectedItem();
		removeAllItemsBut(sel);
		setEnabled(false);
		solver.selected(x,y,sel);
	}
	
	/* NOTE doesn't reference overridden @see{removeItem(Object a)} */
	private void removeAllItemsBut(Object sel) {
		removeAllItemsBut(sel, 1);
	}
	private void removeAllItemsBut(Object sel, int n) {
		if(getItemCount()<=n)
			return;
		if(getItemAt(n).equals(sel)) {
			removeAllItemsBut(sel, n+1);
			return;
		}
		removeItemAt(n);
		removeAllItemsBut(sel, n);
	}

	public void removeItem(Object anObject) {
		super.removeItem(anObject);
		fixItemCountLabel();
		//int n = Integer.parseInt((String) anObject) -1;
		//solver.updateMetaData(x,y,n);
	}
	
	
	private void fixItemCountLabel() {
		int numItems = getItemCount();
		lbl.setString(toGraphical(numItems-1));
		solver.repaint();
	}
	
	static class MutableString {
		private String contents;
		MutableString(String s) {
			contents=s;
		}
		void setString(String s) {
			contents = s;
		}
		public String toString() { return contents; }
	}
	
	/**
	 * converts small ints to a graphical tick-type display.
	 * # 8
	 * H 4
	 * | 2
	 * . 1
	 */
	public static String toGraphical(int n) throws IllegalArgumentException {
		/*if(n>=4)
			return "#";
		switch (n) {
		case 3:
			return "Y";
		case 2:
			return "|";
		case 1:
			return ".";
		default:
			throw new IllegalArgumentException("argument must be greater than 0: "+ n +" not valid");
		}*/
		return toGraphical("",n);
	}
	private static String toGraphical(String str, int n) throws IllegalArgumentException {
		if(n >= 8)
			return toGraphical(str+'#', n-8);
		if(n >= 4)
			return toGraphical(str+'H', n-4);
		if(n >= 2)
			return toGraphical(str+'|', n-2);
		if(n >= 1)
			return str+'.';
		if(n ==0 )
			return str;
		throw new IllegalArgumentException("argument must be greater than 0: "+ n +" not valid");
		
	}
}
