/* Sudoku
 * file SudokuSolver.java
 * 
 * Created on Aug 24, 2005 by blivens
 */
package us.bliven.sudoku;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.undo.UndoManager;



/**
 * @author Spencer Bliven
 */
public class SudokuSolver implements SudokuSquareChangedListener {
	private static final long serialVersionUID = -4653395471212077342L;
	
	private UndoManager undo;
	
	/*
	 * Index the hell out of the grid for simplicity
	 */
	private SudokuSquare[][] rows;
	private SudokuSquare[][] cols;
	private SudokuSquare[][] sectors;
	private Map<SudokuSquare,SudokuSquare[][]> groupsForSquare;
	
	//private int[][] rows;// rows[ row# (0-8)][int n (1-9)-1] = number of squares in that row that could be n
	//private int[][] cols;
	//private int[][][] boxes;//boxes[ col (0-2) ][ row (0-2) ][n]
	
	/*
	 * Automation levels
	 */
	/**
	 * After marking a square as solved, reject that number from everything that square's groups
	 */
	private boolean autoRejectWithinGroups;
	/**
	 * When only one possibility remains for a square, select it.
	 */
	private boolean autoSelectLastOption;
	/**
	 * When only once square in a group can support an element, select it.
	 */
	private boolean autoSelectLastInGroup;
	
	public SudokuSolver() {
		autoRejectWithinGroups = true;
		autoSelectLastOption = true;
		autoSelectLastInGroup = true;
		
		undo = new UndoManager();
		
		JFrame frame = new JFrame("Sudoku Solver");
		
		// Build Menu);
		frame.setJMenuBar(buildMenu());
		
		// Populate main panel
		
		JPanel main = new JPanel(new BorderLayout());
		
		rows = new SudokuSquare[9][9];
		cols = new SudokuSquare[9][9];
		sectors = new SudokuSquare[9][9];
		groupsForSquare = new HashMap<SudokuSquare,SudokuSquare[][]>(9*9, 1f); //Zero-collision hashing

		for(int r=0;r<9;r++)
			for(int c=0;c<9;c++) {
				SudokuSquare s = new SudokuSquare(c,r);
				s.addSudokuSquareChangedListener(this);
				s.addUndoableEditListener(undo);
				
				cols[c][r] = s;
				rows[r][c] = s;
				sectors[c/3+(r/3)*3][c%3+(r%3)*3] = s;
				groupsForSquare.put( s,
						new SudokuSquare[][] { cols[c], rows[r], sectors[c/3+r/3*3] });
			}
		
		JPanel sudoku = createGridPane(cols);
		main.add(sudoku, BorderLayout.CENTER);
		/*
		JPanel savePanel = new JPanel();
		savePanel.setBorder(BorderFactory.createTitledBorder("Save/Load"));
		savePanel.add(new SaveButton());
		savePanel.add(new LoadButton());
		main.add(savePanel, SOUTH);
		*/
		frame.getContentPane().add(main);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setVisible(true);
	}
		
	/**
	 * Generates Viewer to represent grid.
	 * Elements are assumed to be indexed (column, row).
	 * @param grid
	 * @return
	 */
	private static JPanel createGridPane(SudokuSquare[][] grid) {
		JPanel sudoku = new JPanel(new GridLayout(3,3));
		for(int j=0;j<9;j+=3)
			for(int i=0;i<9;i+=3) {
				JPanel p = new JPanel(new GridLayout(3,3));
				p.setBorder(BorderFactory.createEtchedBorder());
				
				for(int r=j;r<j+3;r++)
					for(int c=i;c<i+3;c++) {
						p.add(new SudokuGridSquare(grid[c][r]));
					}
				
				sudoku.add(p);
			}
		return sudoku;
	}
	

	/**
	 * If one element is available in target, select it.
	 * @param target
	 */
	private void selectLastOption(SudokuSquare target) {
		if(target.isSelected()) {
			// Already selected
			return;
		}
		Integer singleOption = null;
		for(Integer elem : SudokuSquare.Elements) {
			if(target.isAvailable(elem)) {
				if( singleOption == null) { //first available elem
					singleOption = elem;
				} else { // 2+ available elems
					return;
				}
			}
		}
		
		if( singleOption == null ) {
			throw new InconsistentSudokuException("No options left for "+target.getX()+","+target.getY());
		}
		// Found single element
		System.out.println("Single option "+singleOption+" from "+target.getX()+","+target.getY());
		target.select(singleOption);
	}
	
	
	private void selectLastInAllGroups(Integer element) {
		for(SudokuSquare[] grp : rows) {
			selectLastInGroup(grp, element);
		}
		for(SudokuSquare[] grp : cols) {
			selectLastInGroup(grp, element);
		}
		for(SudokuSquare[] grp : sectors) {
			selectLastInGroup(grp, element);
		}
	}
	/**
	 * Checks if grp contains only a single square in which lastReject is still available
	 * @param grp
	 * @param lastReject
	 */
	private void selectLastInGroup(SudokuSquare[] grp, Integer lastReject) {
		SudokuSquare singleOption = null;
		for(SudokuSquare s : grp) {
			if(s.isAvailable(lastReject)) {
				if( singleOption == null) {
					singleOption = s;
					if(s.isSelected()) {
						// already selected. no need continue
						return;
					}
				} else {
					// 2+ available squares
					return;
				}
			}
		}

		if( singleOption == null ) {
			throw new InconsistentSudokuException("No options left for "+lastReject+" in this group.");
		}
		// Found single element
		System.out.println("Single square for "+lastReject+" in this group.");
		singleOption.select(lastReject);
	}

	@Override
	public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType) {
		switch( changeType ) {
		case SELECTED:
			elementSelected(target,element);
			break;
		case REJECTED:
			elementRejected(target,element);
			break;
		case RESET:
			//ignore
			break;
		}
	}
	
	protected void elementSelected(SudokuSquare target, Integer element) {
		System.out.println("Selected "+element+" from "+target.getX()+","+target.getY());

		// Reject element from everything in target's groups
		if(autoRejectWithinGroups) {
			for(SudokuSquare[] group : groupsForSquare.get(target)) {
				for(SudokuSquare s : group) {
					if(s != target) {
						s.reject(element);
					}
				}
			}
		}
	}
	
	protected void elementRejected(SudokuSquare target, Integer element) {
		//System.out.println("Rejected "+element+" from "+target.getX()+","+target.getY());
		
		//check for single choices
		if(autoSelectLastOption) {
			selectLastOption(target);
		}
		
		// check groups for single choices
		if(autoSelectLastInGroup) {
			selectLastInAllGroups(element);
		}
	}
	

	
	/**
	 * Sets the specified square to the given number
	 * @param column Column of the square (1-9)
	 * @param row Row of the square (1-9)
	 * @param number Number to put in the square (1-9)
	 */
	public void setSquare(int column, int row, int number) {
		cols[column-1][row-1].select(SudokuSquare.Elements[number-1]);
		
	}
	
	@SuppressWarnings("serial")
	private JMenuBar buildMenu() {
		
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		JMenuBar menuBar = new JMenuBar();
		
		// File Menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(fileMenu);

		Action resetAction = new AbstractAction("Reset") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SudokuSolver.this.reset();
			}
		};
		resetAction.putValue(Action.SHORT_DESCRIPTION, "Reset Puzzle");
		resetAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        resetAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_R, shortcutKeyMask));
        
        JMenuItem resetMenuItem = new JMenuItem(resetAction);
		fileMenu.add(resetMenuItem);
		
		Action loadSample1Action = new AbstractAction("Load Sample Game") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SudokuSolver.this.loadSampleGame1();
			}
		};
		loadSample1Action.putValue(Action.SHORT_DESCRIPTION, "Load Sample Game");
		loadSample1Action.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_1);
        
		fileMenu.add(new JMenuItem(loadSample1Action));
		
		Action quitAction = new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				System.out.println("Quitting");
				System.exit(0);
				
			}
		};
        quitAction.putValue(Action.SHORT_DESCRIPTION, "Exit Now");
        quitAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_Q);
        quitAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_Q, shortcutKeyMask));
        
        JMenuItem quitMenuItem = new JMenuItem(quitAction);
		fileMenu.add(quitMenuItem);
		

		// Edit Menu
		JMenu editMenu = new JMenu("Edit");
		editMenu.setMnemonic(KeyEvent.VK_E);
		menuBar.add(editMenu);
		
		Action undoAction = new AbstractAction("Undo") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				undo.undo();
			}
		};
        undoAction.putValue(Action.SHORT_DESCRIPTION, "Undo Last Move");
        undoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
        undoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_Z, shortcutKeyMask));
        undoAction.setEnabled(undo.canUndo());
        
		JMenuItem undoMenuItem = new JMenuItem(undoAction);
		editMenu.add(undoMenuItem);
		
		Action redoAction = new AbstractAction("Redo") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				undo.redo();
			}
		};
        redoAction.putValue(Action.SHORT_DESCRIPTION, "Redo Last Move");
        redoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        redoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_Y, shortcutKeyMask));
        redoAction.setEnabled(undo.canRedo());
        
		JMenuItem redoMenuItem = new JMenuItem(redoAction);
		editMenu.add(redoMenuItem);
		
		
		// Solver Menu
		
		JMenu solverMenu = new JMenu("Solver");
		solverMenu.setMnemonic(KeyEvent.VK_S);
		menuBar.add(solverMenu);
		
		/* TODO The following code assumes that the menu is the only way to 
		 * modify the auto* properties. This violates MVC.
		 * 
		 * Better would be to make some sort of AIProperties bean, which 
		 * dispatches PropertyChangeEvents of some sort to the JCheckBoxMenuItems
		 * whenever the model changes.
		 */
		JCheckBoxMenuItem autoRejectWithinGroupsMI = new JCheckBoxMenuItem();
		autoRejectWithinGroupsMI.setSelected(autoRejectWithinGroups);
		solverMenu.add(autoRejectWithinGroupsMI);
		
		Action autoRejectWithinGroupsAction = new AbstractAction("Reject options after a selection") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//TODO Toggling only works if this is the only place it can be changed
				SudokuSolver.this.autoRejectWithinGroups = !SudokuSolver.this.autoRejectWithinGroups;
				System.out.println("autoRejectWithinGroups = "+Boolean.toString(SudokuSolver.this.autoRejectWithinGroups));
			}
		};
        autoRejectWithinGroupsAction.putValue(Action.SHORT_DESCRIPTION, "Reject options after a selection");
        autoRejectWithinGroupsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_1);
        autoRejectWithinGroupsMI.setAction(autoRejectWithinGroupsAction);
        
		
		JCheckBoxMenuItem autoSelectLastOptionMI = new JCheckBoxMenuItem();
		autoSelectLastOptionMI.setSelected(autoSelectLastInGroup);
		solverMenu.add(autoSelectLastOptionMI);
		
		Action autoSelectLastOptionAction = new AbstractAction("Select the last remaining option") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//TODO Toggling only works if this is the only place it can be changed
				SudokuSolver.this.autoSelectLastOption = !SudokuSolver.this.autoSelectLastOption;
				System.out.println("autoSelectLastOption = "+Boolean.toString(SudokuSolver.this.autoSelectLastOption));
			}
		};
        autoSelectLastOptionAction.putValue(Action.SHORT_DESCRIPTION, "Select the last remaining option");
        autoSelectLastOptionAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_2);
        autoSelectLastOptionMI.setAction(autoSelectLastOptionAction);

		
		JCheckBoxMenuItem autoSelectLastInGroupMI = new JCheckBoxMenuItem();
		autoSelectLastInGroupMI.setSelected(autoSelectLastInGroup);
		solverMenu.add(autoSelectLastInGroupMI);
		
		Action autoSelectLastInGroupAction = new AbstractAction("Detect when only one box is available") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//TODO Toggling only works if this is the only place it can be changed
				SudokuSolver.this.autoSelectLastInGroup = !SudokuSolver.this.autoSelectLastInGroup;
				System.out.println("autoSelectLastInGroup = "+Boolean.toString(SudokuSolver.this.autoSelectLastInGroup));
			}
		};
        autoSelectLastInGroupAction.putValue(Action.SHORT_DESCRIPTION, "Detect when only one box is available");
        autoSelectLastInGroupAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_3);
        autoSelectLastInGroupMI.setAction(autoSelectLastInGroupAction);
		
		return menuBar;
	}
	
	protected void reset() {
		for(SudokuSquare[] col : cols) {
			for(SudokuSquare square : col) {
				square.reset();
			}
		}
	}
	
	/**
	 * Loads a sample 5-star sudoku
	 */
	public void loadSampleGame1() {
		this.reset();
		this.setSquare(3, 1, 7);
		this.setSquare(5, 1, 1);
		this.setSquare(7, 1, 5);
		this.setSquare(9, 2, 6);
		this.setSquare(4, 3, 2);
		this.setSquare(6, 3, 8);
		this.setSquare(7, 3, 3);
		this.setSquare(9, 3, 9);
		this.setSquare(2, 4, 1);
		this.setSquare(7, 3, 3);
		this.setSquare(3, 4, 2);
		this.setSquare(4, 4, 9);
		this.setSquare(3, 5, 3);
		this.setSquare(4, 5, 8);
		this.setSquare(6, 5, 2);
		this.setSquare(7, 5, 1);
		this.setSquare(6, 6, 1);
		this.setSquare(7, 6, 7);
		this.setSquare(8, 6, 5);
		this.setSquare(1, 7, 1);
		this.setSquare(3, 7, 8);
		this.setSquare(4, 7, 5);
		this.setSquare(6, 7, 9);
		this.setSquare(1, 8, 3);
		this.setSquare(3, 9, 5);
		this.setSquare(5, 9, 4);
		this.setSquare(7, 9, 9);
	}

	public static void main(String[] args) {
		new SudokuSolver();
	}
}
