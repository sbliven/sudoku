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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;



/**
 * @author Spencer Bliven
 */
public class SudokuSolver implements SudokuSquareChangedListener {
	//private static final long serialVersionUID = -4653395471212077342L;
	
	private JFrame frame;
	
	protected UndoManager undo;
    protected UndoAction undoAction;
    protected RedoAction redoAction;
	
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
	
	private List<SudokuAlgorithm> algorithms;
	
	public SudokuSolver() {
		algorithms = new ArrayList<>();
		addAlgorithm(new AutoRejectWithinGroups());
		addAlgorithm(new AutoSelectLastOption());
		addAlgorithm(new AutoSelectLastInGroup());
		
		undo = new UndoManager();
		
		frame = new JFrame("Sudoku Solver");
		
		// Build Menu);
		frame.setJMenuBar(buildMenu());
		
		// Populate main panel
		
		JPanel main = new JPanel(new BorderLayout());
		
		rows = new SudokuSquare[9][9];
		cols = new SudokuSquare[9][9];
		sectors = new SudokuSquare[9][9];
		groupsForSquare = new HashMap<SudokuSquare,SudokuSquare[][]>(9*9, 1f); //Zero-collision hashing

		UndoableEditListener undoListener = new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent e) {
		        undo.addEdit(e.getEdit());
		        undoAction.updateUndoState();
		        redoAction.updateRedoState();		
			}
		};
		for(int r=0;r<9;r++)
			for(int c=0;c<9;c++) {
				SudokuSquare s = new SudokuSquare(c,r);
				s.addSudokuSquareChangedListener(this);
				s.addUndoableEditListener(undoListener);
				
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
		
	private void addAlgorithm(SudokuAlgorithm alg) {
		algorithms.add(alg);
	}

	public void setAlgorithmsEnabled(boolean enabled) {
		for(SudokuAlgorithm alg : algorithms) {
			alg.setEnabled(enabled);
		}
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
		singleOption.select(lastReject, false, true);
	}

	@Override
	public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType, boolean chainChanges) {
		System.out.println(changeType + " "+element+" from "+target.getX()+","+target.getY());

		for(SudokuAlgorithm alg : algorithms) {
			if(alg.isEnabled())
				alg.squareChanged(target, element, changeType, chainChanges);
		}
	}

	
	/**
	 * Sets the specified square to the given number
	 * @param column Column of the square (1-9)
	 * @param row Row of the square (1-9)
	 * @param number Number to put in the square (1-9)
	 */
	public void setSquare(int column, int row, int number) {
		cols[column-1][row-1].select(SudokuSquare.Elements[number-1], false, true);
		
	}
	
	@SuppressWarnings("serial")
	private JMenuBar buildMenu() {
		
		int shortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		
		JMenuBar menuBar = new JMenuBar();
		
		// File Menu
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(fileMenu);


		Action resetAction = new AbstractAction("New") {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				SudokuSolver.this.reset();
			}
		};
		resetAction.putValue(Action.SHORT_DESCRIPTION, "New Puzzle");
		resetAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
        resetAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_N, shortcutKeyMask));
        
        JMenuItem resetMenuItem = new JMenuItem(resetAction);
		fileMenu.add(resetMenuItem);
		
		Action openAction = new AbstractAction("Open") {
			public void actionPerformed(ActionEvent e) {
				SudokuSolver.this.open();
			};
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, "Open");
		openAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_O);
		openAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_O, shortcutKeyMask));
		
		JMenuItem openMenuItem = new JMenuItem(openAction);
		fileMenu.add(openMenuItem);
		
		Action saveAction = new AbstractAction("Save") {
			public void actionPerformed(ActionEvent e) {
				SudokuSolver.this.save();
			};
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save");
		saveAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
		saveAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, shortcutKeyMask));
		
		JMenuItem saveMenuItem = new JMenuItem(saveAction);
		fileMenu.add(saveMenuItem);
		
		
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
				
		undoAction = new UndoAction();
        undoAction.putValue(Action.SHORT_DESCRIPTION, "Undo Last Move");
        undoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_U);
        undoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_Z, shortcutKeyMask));
        
		JMenuItem undoMenuItem = new JMenuItem(undoAction);
		editMenu.add(undoMenuItem);
		
		redoAction = new RedoAction();
        redoAction.putValue(Action.SHORT_DESCRIPTION, "Redo Last Move");
        redoAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        redoAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_Y, shortcutKeyMask));
        
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
		for(SudokuAlgorithm alg : algorithms) {
			if(alg.getMnemonic() != null) {
				JCheckBoxMenuItem checkBox = new JCheckBoxMenuItem();
				checkBox.setSelected(alg.isEnabled());
				solverMenu.add(checkBox);

				Action action = new AbstractAction(alg.getName()) {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						//TODO Toggling only works if this is the only place it can be changed
						alg.setEnabled(!alg.isEnabled());
					}
				};
				action.putValue(Action.SHORT_DESCRIPTION, alg.getName());
				action.putValue(Action.MNEMONIC_KEY, alg.getMnemonic());
				checkBox.setAction(action);
			}
		}
		
		return menuBar;
	}
	
	public void save() {
		final String DEFAULT_FILENAME = String.format("puzzle.%s",SudokuTextFormat.EXTENSIONS[0]);
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Save puzzle");
		fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		fileChooser.setSelectedFile(new File(DEFAULT_FILENAME));
	    FileFilter filter = new FileNameExtensionFilter(
	    		String.format("Sudoku boards (%s)", String.join(", ", SudokuTextFormat.EXTENSIONS)),
	    		SudokuTextFormat.EXTENSIONS);
	    fileChooser.addChoosableFileFilter(filter);
	    fileChooser.setFileFilter(filter);
		 
		int userSelection = fileChooser.showSaveDialog(frame);
		 
		if (userSelection == JFileChooser.APPROVE_OPTION) {
		    File fileToSave = fileChooser.getSelectedFile();
		    System.out.println("Save as file: " + fileToSave.getAbsolutePath());
		    
		    try(Writer writer = new BufferedWriter(new FileWriter(fileToSave))) {
				SudokuTextFormat.write(writer, getSelectedGrid(), true, ' ');
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame,
						String.format("Error saving to %s\n\n%s",fileToSave.getAbsolutePath(), e.getMessage()),
						"Error Saving", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
	public int[][] getSelectedGrid() {
		int[][] board = new int[9][9];
		
		for(int row=0; row < 9; row++) {
			for(int col=0; col < 9; col++) {
				SudokuSquare square = rows[row][col];
				if(square.isSelected()) {
					board[row][col] = square.getSelected();
				}
			}
		}
		return board;
	}

	public void open() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setDialogTitle("Open puzzle");
		fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
	    FileFilter filter = new FileNameExtensionFilter(
	    		String.format("Sudoku boards (%s)", String.join(", ", SudokuTextFormat.EXTENSIONS)),
	    		SudokuTextFormat.EXTENSIONS);
	    fileChooser.addChoosableFileFilter(filter);
	    fileChooser.setFileFilter(filter);
		 
		int userSelection = fileChooser.showOpenDialog(frame);
		 
		if (userSelection == JFileChooser.APPROVE_OPTION) {
		    File fileToOpen = fileChooser.getSelectedFile();
		    System.out.println("Open file: " + fileToOpen.getAbsolutePath());
		    
		    int[][] grid;
		    try(Reader reader = new BufferedReader(new FileReader(fileToOpen))) {
				grid = SudokuTextFormat.read(reader);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(frame,
						String.format("Error opening %s\n\n%s",fileToOpen.getAbsolutePath(), e.getMessage()),
						"Error Opening", JOptionPane.ERROR_MESSAGE);
				return;
			}
		    
			load(grid);
		}

	}

	public void load(int[][] grid) {
		reset();
		
		for(int row=0; row < 9; row++) {
			for(int col=0; col < 9; col++) {
				if(grid[row][col] > 0) {
					setSquare(col+1, row+1, grid[row][col]);
				}
			}
		}
	}

	public void reset() {
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

	class UndoAction extends AbstractAction {
        /**
		 * 
		 */
		private static final long serialVersionUID = 6124791851042096951L;

		public UndoAction() {
            super("Undo");
            setEnabled(false);
        }
 
        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                System.out.println("Unable to undo: " + ex);
                ex.printStackTrace();
            }
            updateUndoState();
            redoAction.updateRedoState();
        }
 
        protected void updateUndoState() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Undo");
            }
        }
    }
 
    class RedoAction extends AbstractAction {
        /**
		 * 
		 */
		private static final long serialVersionUID = 3106594194738828746L;

		public RedoAction() {
            super("Redo");
            setEnabled(false);
        }
 
        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                System.out.println("Unable to redo: " + ex);
                ex.printStackTrace();
            }
            updateRedoState();
            undoAction.updateUndoState();
        }
 
        protected void updateRedoState() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, "Redo");
            }
        }
    }
    
    /**
	 * After marking a square as solved, reject that number from everything that square's groups
	 */
	public class AutoRejectWithinGroups extends AbstractSudokuAlgorithm {
		public AutoRejectWithinGroups() {
			super("Reject options after a selection");
		}

		@Override
		public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType, boolean chainChanges) {
			if(chainChanges && changeType == ChangeType.SELECTED) {
				for(SudokuSquare[] group : groupsForSquare.get(target)) {
					for(SudokuSquare s : group) {
						if(s != target) {
							s.reject(element, false, true);
						}
					}
				}
			}
		}

		@Override
		public Integer getMnemonic() {
			return KeyEvent.VK_1;
		}
	};
	/**
	 * When only one possibility remains for a square, select it.
	 */
	public class AutoSelectLastOption extends AbstractSudokuAlgorithm {
		public AutoSelectLastOption() {
			super("Select the last remaining option");
		}

		@Override
		public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType, boolean chainChanges) {
			if(chainChanges && changeType == ChangeType.REJECTED) {
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
				target.select(singleOption, false, true);
			}
		};
		
		@Override
		public Integer getMnemonic() {
			return KeyEvent.VK_2;
		}
	}
		
	/**
	 * When only once square in a group can support an element, select it.
	 */
	public class AutoSelectLastInGroup extends AbstractSudokuAlgorithm {

		public AutoSelectLastInGroup() {
			super("Detect when only one box is available");
		}

		@Override
		public void squareChanged(SudokuSquare target, Integer element, ChangeType changeType, boolean chainChanges) {
			if(chainChanges && changeType == ChangeType.REJECTED) {

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
		}
		@Override
		public Integer getMnemonic() {
			return KeyEvent.VK_3;
		}
	};
    
	public static void main(String[] args) {
		new SudokuSolver();
	}
}
