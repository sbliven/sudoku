/**
 * 
 */
package blivens.sudoku.solver2;

import static java.awt.BorderLayout.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * @author blivens
 *
 */
public class SudokuGridSquare extends JPanel implements KeyListener, MouseListener, SudokuSquareChangedListener, FocusListener {
	private static final long serialVersionUID = 6046898070146170077L;

	private SudokuSquare model;
	private JPanel[] subGrids;
	private JLabel bigLab;
	/* Indicates whether we're currently showing subGrids (false) or bigLab (true).
	 * Should closely mirror model.isSelected()
	 */
	private boolean selected; 
	
	public SudokuGridSquare(SudokuSquare ss) {
		super();
		
		this.model = ss;
		ss.addSudokuSquareChangedListener(this);
		addMouseListener(this);
		addKeyListener(this);
		addFocusListener(this);
		
		this.setPreferredSize(new Dimension(50,50));
		
		this.setFocusable(true);
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		this.setBackground(Color.WHITE);
		
		
		this.setLayout(new GridLayout(3,3));
		selected = false;
		subGrids = new JPanel[SudokuSquare.Elements.length];
		for(int i = 0; i<SudokuSquare.Elements.length; i++) {
			subGrids[i] = new SubGrid(SudokuSquare.Elements[i]);
			this.add(subGrids[i]);
		}
		
		// Create bigLab but don't add it
		bigLab = new JLabel();
		Font bigLabFont = new Font("SansSerif", Font.PLAIN, 24);
		bigLab.setFont(bigLabFont);
		bigLab.setForeground(Color.BLACK);
		bigLab.setHorizontalAlignment(SwingConstants.CENTER);
		bigLab.setVerticalAlignment(SwingConstants.CENTER);
	}
	
	/**
	 * Respond to model changes
	 */
	@Override
	public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType) {
		//System.out.println("Selected "+element);
		refresh();
		/*setVisible(false);
		removeAll();
		setLayout(new BorderLayout());

		bigLab.setText(element.toString());
		add(bigLab, CENTER);
		invalidate();
		setVisible(true);*/
	}
	
	/*@Override
	public void elementRejected(SudokuSquare target, Integer element) {
		//System.out.println("Rejected "+element);
		refresh();
		//subGrids[element-1].setVisible(false);
	}*/
	
	public void refresh() {
		if( model.isSelected() ) {
			bigLab.setText(model.getSelected().toString());
			
			if( !selected) {
				// Just became selected
				selected = true;
				setVisible(false);
				removeAll();
				setLayout(new BorderLayout());
				add(bigLab, CENTER);
				invalidate();
				setVisible(true);
			}
		} else { // model not selected
			for(int i=0;i<subGrids.length;i++) {
				// If an element is available, make it visible
				subGrids[i].setVisible(model.isAvailable(SudokuSquare.Elements[i]));
			}
			
			if( selected ) {
				// Just became unselected
				selected = false;
				setVisible(false);
				removeAll();
				setLayout(new GridLayout(3,3));
				for(int i=0; i< subGrids.length; i++) {
					add(subGrids[i]);
				}
				invalidate();
				setVisible(true);
			}
		}
			
	}
	
	/*
	 * Invoke model changes 
	 */
	
	@Override
	public void keyPressed(KeyEvent e) {}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
		try {
			int i = Integer.parseInt(e.getKeyChar()+"");
			if(i>0) {
				model.select(SudokuSquare.Elements[i-1]);
			}
		} catch(NumberFormatException ex) {
			//ignore keys besides 0-9
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {
		//System.out.println("Entered "+model.getX()+","+model.getY());
		//this.requestFocusInWindow();
		if(!model.isSelected())
			this.requestFocus();
	}

	@Override
	public void mouseExited(MouseEvent e) {
		//System.out.println("Exited "+model.getX()+","+model.getY());
	}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {}
	
	@Override
	public void focusGained(FocusEvent e) {
		//System.out.println("Focused on "+model.getX()+","+model.getY());
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
	}

	@Override
	public void focusLost(FocusEvent e) {
		//System.out.println("Lost focus on "+model.getX()+","+model.getY());
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
	}


	private static final Font subGridFont = new Font("SansSerif", Font.PLAIN, 9);
	private class SubGrid extends JPanel implements MouseListener {
		private static final long serialVersionUID = -8245201653180058470L;
		private Integer element;
		

		public SubGrid(Integer elem) {
			this.element = elem;
			
			JLabel lab = new JLabel(elem.toString());
			lab.setHorizontalAlignment(SwingConstants.CENTER);
			lab.setFont(subGridFont);
			lab.setForeground(Color.LIGHT_GRAY);
			
			//setBorder(BorderFactory.createLineBorder(Color.BLUE));
			setBackground(SudokuGridSquare.this.getBackground());
			add(lab);
			addMouseListener(this);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			//System.out.println("Clicked on subGrid "+element);
			if((e.getModifiersEx() & ( MouseEvent.BUTTON3_DOWN_MASK 
					| MouseEvent.SHIFT_DOWN_MASK
					| MouseEvent.META_DOWN_MASK
					| MouseEvent.ALT_DOWN_MASK )) != 0 ) {
				model.reject(element);
			} else {
				model.select(element);
			}
		}
		@Override
		public void mouseEntered(MouseEvent e) {
			if(!SudokuGridSquare.this.model.isSelected())
				SudokuGridSquare.this.requestFocus();
		}
		@Override
		public void mouseExited(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}
		@Override
		public void mouseReleased(MouseEvent e) {}
	}

	/**
	 * Testing
	 * @param args
	 */
	public static void main(String[] args) {

		int size=4;
		
		JFrame frame = new JFrame("Sudoku Solver");
		JPanel main = new JPanel(new GridLayout(size,size));

		SudokuSquare[][] model = new SudokuSquare[size][size];
		SudokuGridSquare[][] view = new SudokuGridSquare[size][size];
		
		for(int r=0;r<size;r++) {
			for(int c=0;c<size;c++) {
				model[r][c] = new SudokuSquare(c,r);
				view[r][c] = new SudokuGridSquare(model[r][c]);
				main.add(view[r][c]);
			}
		}
		
		frame.getContentPane().add(main);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);

		
		// Test removing 4
		model[1][2].reject(SudokuSquare.Elements[4-1]);
		model[2][0].select(SudokuSquare.Elements[7-1]);
	}


}
