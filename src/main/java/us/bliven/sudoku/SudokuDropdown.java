/**
 * 
 */
package us.bliven.sudoku;

import static java.awt.BorderLayout.CENTER;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * @author blivens
 *
 */
public class SudokuDropdown extends JComboBox<String> implements ActionListener, SudokuSquareChangedListener {
	private static final long serialVersionUID = -725336491223766575L;

	private SudokuSquare model;
	
	private static String dummyItem = " "; 
	public SudokuDropdown(SudokuSquare ss) {
		super();
		this.addItem(dummyItem);
		for(Integer i :SudokuSquare.Elements) {
			this.addItem(i.toString());
		}
		this.setSelectedIndex(0);
		this.addActionListener(this);
		
		this.model = ss;
		ss.addSudokuSquareChangedListener(this);
	}
	
	/**
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		Object selectedItem = this.getSelectedItem();
		if(selectedItem == null) {
			return; //Only occurs from RemoveAll events
		}
		if(selectedItem.equals(dummyItem)) {
			return; //Ignore clicking back to dummy
		}
		if(selectedItem.equals(model.getSelected())) {
			return; //Ignore reselecting
		}

		if( (e.getModifiers() & (ActionEvent.SHIFT_MASK | ActionEvent.ALT_MASK | ActionEvent.META_MASK)) != 0) {
			//removeItem();
			model.reject((Integer)selectedItem, true);
		}
		else {
			//selectItem();
			model.select((Integer)selectedItem, true);
		}
	}

	@Override
	public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType) {
		switch( changeType) {
		case SELECTED: {
			//System.out.println("Selected "+element);
			this.removeAllItems();
			this.addItem(element.toString());
			this.setSelectedIndex(0);
			break;
		}
		case REJECTED: {
			//System.out.println("Rejected "+element);	
			this.setSelectedIndex(0);
			this.removeItem(element.toString());		
			break;
		}
		default:
			throw new UnsupportedOperationException("Don't know how to respond to a "+changeType+" change to a square.");
		}

	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SudokuSquare ss = new SudokuSquare(0,0);
		SudokuDropdown sd = new SudokuDropdown(ss);
		
		
		
		JFrame frame = new JFrame("Sudoku Solver");
		JPanel main = new JPanel(new BorderLayout());
		
		main.add(sd, CENTER);
		frame.getContentPane().add(main);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		
		// Test removing 4
		ss.reject(SudokuSquare.Elements[4-1], true);

	}



}
